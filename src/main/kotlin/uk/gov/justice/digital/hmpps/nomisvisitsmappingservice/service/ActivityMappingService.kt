package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.ActivityMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.ActivityMappingRepository

@Service
@Transactional(readOnly = true)
class ActivityMappingService(
  private val activityMappingRepository: ActivityMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createMapping(createMappingRequest: ActivityMappingDto) =
    with(createMappingRequest) {
      log.debug("creating activity $createMappingRequest")
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
          mappingType = ActivityMappingType.valueOf(mappingType)
        )
      )
      telemetryClient.trackEvent(
        "activity-mapping-created",
        mapOf(
          "nomisCourseActivityId" to nomisCourseActivityId.toString(),
          "activityScheduleId" to activityScheduleId.toString(),
        ),
        null
      )
      log.debug("Mapping created with activityScheduleId = $activityScheduleId, nomisCourseActivityId = $nomisCourseActivityId")
    }

  suspend fun getMappingById(id: Long): ActivityMappingDto =
    activityMappingRepository.findById(id)
      ?.let { ActivityMappingDto(it) }
      ?: throw NotFoundException("Activity schedule id=$id")

  suspend fun deleteMapping(id: Long) = activityMappingRepository.deleteById(id)
}
