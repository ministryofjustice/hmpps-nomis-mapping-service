package uk.gov.justice.digital.hmpps.nomismappingservice.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.data.ActivityMigrationMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.ActivityMigrationMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository.ActivityMigrationMappingRepository

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
        if (this@run.activityId == this@with.activityId && this@run.activityId2 == this@with.activityId2) {
          log.debug("Activity mapping already exists for activity Ids: $activityId, $activityId2 and nomisBookingId: $nomisCourseActivityId so not creating. All OK")
          return
        }
        throw ValidationException("Nomis mapping id = $nomisCourseActivityId already exists")
      }

      if (activityId != null) {
        activityMigrationMappingRepository.findOneByActivityIdAndActivityId2(
          activityScheduleId = activityId,
          activityScheduleId2 = activityId2,
        )?.run {
          throw ValidationException("Activity migration mapping with Activity id=$activityId and 2nd Activity Id=$activityId2 already exists")
        }
      }

      activityMigrationMappingRepository.save(
        ActivityMigrationMapping(
          nomisCourseActivityId = nomisCourseActivityId,
          activityId = activityId,
          activityId2 = activityId2,
          label = label,
        ),
      )
      telemetryClient.trackEvent(
        "activity-migration-mapping-created",
        mapOf(
          "nomisCourseActivityId" to nomisCourseActivityId.toString(),
          "dpsActivityId" to activityId.toString(),
          "dpsActivityId2" to activityId2.toString(),
          "label" to label,
        ),
        null,
      )
    }
  }

  suspend fun getMapping(courseActivityId: Long): ActivityMigrationMappingDto = activityMigrationMappingRepository.findById(courseActivityId)
    ?.let { ActivityMigrationMappingDto(it) }
    ?: throw NotFoundException("nomisCourseActivityId=$courseActivityId")

  suspend fun getLatestMigrated(): ActivityMigrationMappingDto = activityMigrationMappingRepository.findFirstByOrderByWhenCreatedDesc()
    ?.let { ActivityMigrationMappingDto(it) }
    ?: throw NotFoundException("No migrated mapping found")

  suspend fun getMappings(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<ActivityMigrationMappingDto> = coroutineScope {
    val activityMigrationMappings = async {
      activityMigrationMappingRepository.findAllByLabelOrderByNomisCourseActivityIdAsc(label = migrationId, pageRequest)
    }

    val count = async {
      activityMigrationMappingRepository.countAllByLabel(migrationId, includeIgnored = false)
    }

    PageImpl(
      activityMigrationMappings.await().toList().map { ActivityMigrationMappingDto(it) },
      pageRequest,
      count.await(),
    )
  }

  suspend fun countMappings(migrationId: String, includeIgnored: Boolean): Long = activityMigrationMappingRepository.countAllByLabel(migrationId, includeIgnored)
}
