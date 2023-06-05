package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import kotlinx.coroutines.flow.map
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
        throw ValidationException("Activity mapping id = $activityScheduleId already exists")
      }

      activityMappingRepository.findOneByNomisCourseActivityId(
        nomisCourseActivityId = nomisCourseActivityId,
      )?.run {
        throw ValidationException("Activity with Nomis id=$nomisCourseActivityId already exists")
      }

      activityMappingRepository.save(
        ActivityMapping(
          activityScheduleId = activityScheduleId,
          nomisCourseActivityId = nomisCourseActivityId,
          mappingType = ActivityMappingType.valueOf(mappingType),
        ),
      )
      telemetryClient.trackEvent(
        "activity-mapping-created",
        mapOf(
          "nomisCourseActivityId" to nomisCourseActivityId.toString(),
          "activityScheduleId" to activityScheduleId.toString(),
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
              "activityScheduleId" to activityScheduleId.toString(),
              "scheduleInstanceId" to entity.scheduledInstanceId.toString(),
              "nomisCourseScheduleId" to entity.nomisCourseScheduleId.toString(),
            ),
            null,
          )
        }
      }
    }
  }

  suspend fun getMappingById(id: Long): ActivityMappingDto =
    activityScheduleMappingRepository.findAllByActivityScheduleId(id)
      .map { ActivityScheduleMappingDto(it) }
      .let { schedules ->
        activityMappingRepository.findById(id)
          ?.let { ActivityMappingDto(it, schedules.toList()) }
          ?: throw NotFoundException("Activity schedule id=$id")
      }

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
}
