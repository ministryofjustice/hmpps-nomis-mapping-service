package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.ActivityMigrationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMigrationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.ActivityMigrationMappingRepository

@Service
@Transactional(readOnly = true)
class ActivityMigrationService(
  private val activityMigrationMappingRepository: ActivityMigrationMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createMapping(createMappingRequest: ActivityMigrationMappingDto) {
    with(createMappingRequest) {
      activityMigrationMappingRepository.findById(nomisCourseActivityId)?.run {
        if (this@run.activityScheduleId == this@with.activityScheduleId && this@run.activityScheduleId2 == this@with.activityScheduleId2) {
          log.debug("Activity mapping already exists for activity Ids: $activityScheduleId, $activityScheduleId2 and nomisBookingId: $nomisCourseActivityId so not creating. All OK")
          return
        }
        throw ValidationException("Nomis mapping id = $nomisCourseActivityId already exists")
      }

      activityMigrationMappingRepository.findOneByActivityScheduleIdAndActivityScheduleId2(
        activityScheduleId = activityScheduleId,
        activityScheduleId2 = activityScheduleId2,
      )?.run {
        throw ValidationException("Activity migration mapping with Activity id=$activityScheduleId and 2nd Activity Id=$activityScheduleId2 already exists")
      }

      activityMigrationMappingRepository.save(
        ActivityMigrationMapping(
          nomisCourseActivityId = nomisCourseActivityId,
          activityScheduleId = activityScheduleId,
          activityScheduleId2 = activityScheduleId2,
          label = label,
        ),
      )
      telemetryClient.trackEvent(
        "activity-migration-mapping-created",
        mapOf(
          "nomisCourseActivityId" to nomisCourseActivityId.toString(),
          "activityScheduleId" to activityScheduleId.toString(),
          "activityScheduleId1" to activityScheduleId2.toString(),
          "label" to label,
        ),
        null,
      )
    }
  }

  suspend fun getMapping(courseActivityId: Long): ActivityMigrationMappingDto =
    activityMigrationMappingRepository.findById(courseActivityId)
      ?.let { ActivityMigrationMappingDto(it) }
      ?: throw NotFoundException("nomisCourseActivityId=$courseActivityId")

  suspend fun getLatestMigrated(): ActivityMigrationMappingDto =
    activityMigrationMappingRepository.findFirstByOrderByWhenCreatedDesc()
      ?.let { ActivityMigrationMappingDto(it) }
      ?: throw NotFoundException("No migrated mapping found")
}
