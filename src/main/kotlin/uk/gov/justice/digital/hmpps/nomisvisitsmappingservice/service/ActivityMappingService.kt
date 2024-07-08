package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.ActivityMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.ActivityScheduleMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityScheduleMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityScheduleMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.ActivityMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.ActivityScheduleMappingRepository

@Service
@Transactional(readOnly = true)
class ActivityMappingService(
  private val activityMappingRepository: ActivityMappingRepository,
  private val activityScheduleMappingRepository: ActivityScheduleMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createMapping(createMappingRequest: ActivityMappingDto) {
    with(createMappingRequest) {
      activityMappingRepository.findById(activityScheduleId)?.run {
        if (this@run.nomisCourseActivityId == this@with.nomisCourseActivityId) {
          log.debug("Activity mapping already exists for nomisBookingId: $nomisCourseActivityId : $activityScheduleId so not creating. All OK")
          return
        }
        throw ValidationException("Activity schedule mapping id = $activityScheduleId already exists")
      }

      activityMappingRepository.findOneByNomisCourseActivityId(
        nomisCourseActivityId = nomisCourseActivityId,
      )?.run {
        throw ValidationException("Activity with Nomis id=$nomisCourseActivityId already exists")
      }

      activityMappingRepository.save(
        ActivityMapping(
          activityScheduleId = activityScheduleId,
          activityId = activityId,
          nomisCourseActivityId = nomisCourseActivityId,
          mappingType = ActivityMappingType.valueOf(mappingType),
        ),
      )
      telemetryClient.trackEvent(
        "activity-mapping-created",
        mapOf(
          "nomisCourseActivityId" to nomisCourseActivityId.toString(),
          "dpsActivityScheduleId" to activityScheduleId.toString(),
          "dpsActivityId" to activityId.toString(),
        ),
        null,
      )

      createMappingRequest.scheduledInstanceMappings.map { request ->
        ActivityScheduleMapping(
          scheduledInstanceId = request.scheduledInstanceId,
          nomisCourseScheduleId = request.nomisCourseScheduleId,
          mappingType = ActivityScheduleMappingType.valueOf(request.mappingType),
          activityScheduleId = activityScheduleId,
        ).also { entity ->
          activityScheduleMappingRepository.save(entity)
          telemetryClient.trackEvent(
            "activity-schedule-mappings-created",
            mapOf(
              "dpsActivityScheduleId" to activityScheduleId.toString(),
              "dpsScheduleInstanceId" to entity.scheduledInstanceId.toString(),
              "nomisCourseScheduleId" to entity.nomisCourseScheduleId.toString(),
            ),
            null,
          )
        }
      }
    }
  }

  @Transactional
  suspend fun updateScheduleMappings(updateRequest: ActivityMappingDto): ActivityMappingDto {
    if (!activityMappingRepository.existsById(updateRequest.activityScheduleId)) {
      throw NotFoundException("Activity schedule id=${updateRequest.activityScheduleId}")
    }

    val activityScheduleId = updateRequest.activityScheduleId
    val existingMappings = activityScheduleMappingRepository.findAllByActivityScheduleId(activityScheduleId)

    // handle updates and deletes
    existingMappings.forEach { existingMapping ->
      updateRequest.scheduledInstanceMappings.findRequestedMapping(existingMapping)
        ?.also { requestedMapping -> existingMapping.saveIfChanged(requestedMapping) }
        ?: also { activityScheduleMappingRepository.delete(existingMapping) }
    }

    // create new mappings
    updateRequest.scheduledInstanceMappings.forEach { requestedMapping ->
      if (existingMappings.doesNotExist(requestedMapping)) {
        requestedMapping.createForActivity(activityScheduleId)
      }
    }

    return getMappingById(activityScheduleId)
  }

  private fun List<ActivityScheduleMappingDto>.findRequestedMapping(existingMapping: ActivityScheduleMapping) =
    find { it.scheduledInstanceId == existingMapping.scheduledInstanceId }

  private suspend fun ActivityScheduleMappingDto.createForActivity(activityScheduleId: Long) {
    ActivityScheduleMapping(
      scheduledInstanceId = scheduledInstanceId,
      nomisCourseScheduleId = nomisCourseScheduleId,
      mappingType = ActivityScheduleMappingType.valueOf(mappingType),
      activityScheduleId = activityScheduleId,
    ).also {
      activityScheduleMappingRepository.save(it)
    }
  }

  private fun List<ActivityScheduleMapping>.doesNotExist(requestedMapping: ActivityScheduleMappingDto) =
    none { it.scheduledInstanceId == requestedMapping.scheduledInstanceId }

  private suspend fun ActivityScheduleMapping.saveIfChanged(requestedMapping: ActivityScheduleMappingDto) {
    if (nomisCourseScheduleId != requestedMapping.nomisCourseScheduleId) {
      nomisCourseScheduleId = requestedMapping.nomisCourseScheduleId
      mappingType = ActivityScheduleMappingType.valueOf(requestedMapping.mappingType)
      activityScheduleMappingRepository.save(this)
    }
  }

  suspend fun getMappingById(id: Long): ActivityMappingDto =
    activityScheduleMappingRepository.findAllByActivityScheduleId(id)
      .map { ActivityScheduleMappingDto(it) }
      .let { schedules ->
        activityMappingRepository.findById(id)
          ?.let { ActivityMappingDto(it, schedules) }
          ?: throw NotFoundException("Activity schedule id=$id")
      }

  suspend fun getScheduleMappingById(activityScheduleId: Long, scheduledInstanceId: Long): ActivityScheduleMappingDto =
    activityScheduleMappingRepository.findOneByActivityScheduleIdAndScheduledInstanceId(activityScheduleId, scheduledInstanceId)
      ?.let { ActivityScheduleMappingDto(it) }
      ?: throw NotFoundException("Activity schedule id=$activityScheduleId, Scheduled instance id=$scheduledInstanceId")

  suspend fun getScheduleMappingByScheduleId(scheduledInstanceId: Long): ActivityScheduleMappingDto =
    activityScheduleMappingRepository.findOneByScheduledInstanceId(scheduledInstanceId)
      ?.let { ActivityScheduleMappingDto(it) }
      ?: throw NotFoundException("Scheduled instance id=$scheduledInstanceId")

  @Transactional
  suspend fun deleteMapping(id: Long) =
    activityMappingRepository.deleteById(id)
      .also { activityScheduleMappingRepository.deleteAllByActivityScheduleId(id) }

  suspend fun getAllMappings(): List<ActivityMappingDto> =
    activityMappingRepository.findAll().toList().map { activityMapping ->
      activityScheduleMappingRepository.findAllByActivityScheduleId(activityMapping.activityScheduleId)
        .map { ActivityScheduleMappingDto(it) }
        .let { scheduleMappingDtos ->
          ActivityMappingDto(activityMapping, scheduleMappingDtos.toList())
        }
    }

  @Transactional
  suspend fun deleteCourseSchedulesAfterId(maxCourseScheduleId: Long) = activityScheduleMappingRepository.deleteByNomisCourseScheduleIdGreaterThan(maxCourseScheduleId)
}
