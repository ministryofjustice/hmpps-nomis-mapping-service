package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AllocationMigrationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AllocationMigrationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.AllocationMigrationMappingRepository

@Service
@Transactional(readOnly = true)
class AllocationMigrationService(
  private val allocationMigrationMappingRepository: AllocationMigrationMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createMapping(createMappingRequest: AllocationMigrationMappingDto) {
    with(createMappingRequest) {
      allocationMigrationMappingRepository.findById(nomisAllocationId)?.run {
        if (this@run.activityAllocationId == this@with.activityAllocationId) {
          log.debug("Allocation mapping already exists for allocation Id: $activityAllocationId and nomisAllocationId: $nomisAllocationId so not creating. All OK")
          return
        }
        throw ValidationException("Nomis mapping id = $nomisAllocationId already exists")
      }

      allocationMigrationMappingRepository.findOneByActivityAllocationId(activityAllocationId)?.run {
        throw ValidationException("Allocation migration mapping with allocation id=$activityAllocationId already exists")
      }

      allocationMigrationMappingRepository.save(
        AllocationMigrationMapping(
          nomisAllocationId = nomisAllocationId,
          activityAllocationId = activityAllocationId,
          activityScheduleId = activityScheduleId,
          label = label,
        ),
      )
      telemetryClient.trackEvent(
        "activity-migration-mapping-created",
        mapOf(
          "nomisAllocationId" to nomisAllocationId.toString(),
          "activityAllocationId" to activityAllocationId.toString(),
          "activityScheduleId" to activityScheduleId.toString(),
          "label" to label,
        ),
        null,
      )
    }
  }

  suspend fun getMapping(nomisAllocationId: Long): AllocationMigrationMappingDto =
    allocationMigrationMappingRepository.findById(nomisAllocationId)
      ?.let { AllocationMigrationMappingDto(it) }
      ?: throw NotFoundException("nomisAllocationId=$nomisAllocationId")

  suspend fun getLatestMigrated(): AllocationMigrationMappingDto =
    allocationMigrationMappingRepository.findFirstByOrderByWhenCreatedDesc()
      ?.let { AllocationMigrationMappingDto(it) }
      ?: throw NotFoundException("No migrated mapping found")

  suspend fun getMappings(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<AllocationMigrationMappingDto> =
    coroutineScope {
      val allocationMigrationMappings = async {
        allocationMigrationMappingRepository.findAllByLabelOrderByNomisAllocationIdAsc(label = migrationId, pageRequest)
      }

      val count = async {
        allocationMigrationMappingRepository.countAllByLabel(migrationId)
      }

      PageImpl(
        allocationMigrationMappings.await().toList().map { AllocationMigrationMappingDto(it) },
        pageRequest,
        count.await(),
      )
    }
}
