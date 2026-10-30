package uk.gov.justice.digital.hmpps.nomismappingservice.bedassignment

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.collect
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BedAssignmentMappingService(
  private val repository: BedAssignmentMappingRepository,
  private val telemetryClient: TelemetryClient,
  @Value("\${bed-assignments.average-per-prisoner}") private val averageBedAssignmentsPerPrisoner: Int,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun alreadyExistsMessage(
    duplicateMapping: BedAssignmentMappingDto,
    existingMapping: BedAssignmentMappingDto,
  ) = """Bed assignment mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
  """.trimMargin()

  @Transactional
  suspend fun createMapping(createMappingRequest: BedAssignmentMappingDto) {
    repository.findOneByNomisBookingIdAndNomisSequence(
      createMappingRequest.nomisBookingId,
      createMappingRequest.nomisSequence,
    )
      ?.let { mapping ->
        if (mapping.dpsBedAssignmentId.toString() == createMappingRequest.dpsBedAssignmentId) {
          log.debug(
            "Not creating. All OK: {}",
            alreadyExistsMessage(
              duplicateMapping = createMappingRequest,
              existingMapping = BedAssignmentMappingDto(mapping),
            ),
          )
          return
        }
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = BedAssignmentMappingDto(mapping),
          ),
          duplicate = createMappingRequest,
          existing = BedAssignmentMappingDto(mapping),
        )
      }

    repository.save(createMappingRequest.fromDto())
    telemetryClient.trackEvent(
      "bed-assignment-mapping-created",
      mapOf(
        "dpsBedAssignmentId" to createMappingRequest.dpsBedAssignmentId,
        "nomisBookingId" to createMappingRequest.nomisBookingId.toString(),
        "nomisSequence" to createMappingRequest.nomisSequence.toString(),
        "batchId" to createMappingRequest.label,
      ),
      null,
    )
  }

  @Transactional
  suspend fun createMappings(mappings: List<BedAssignmentMappingDto>) {
    repository.saveAll(mappings.map { it.fromDto() }).collect()
  }

  /**
   * Replaces any earlier mappings for the prisoner's bookings, so a prisoner can be migrated again. There is
   * no prisoner number on the mapping: a booking belongs to one prisoner, and its id does not change when
   * NOMIS merges prisoners or moves the booking, so the booking ids identify what to replace.
   */
  @Transactional
  suspend fun createMappings(prisonerMapping: PrisonerBedAssignmentMappingsDto) {
    repository.deleteAllByNomisBookingIdIn(prisonerMapping.mappings.map { it.nomisBookingId }.distinct())
    repository.saveAll(
      prisonerMapping.mappings.map {
        BedAssignmentMapping(
          dpsBedAssignmentId = UUID.fromString(it.dpsBedAssignmentId),
          nomisBookingId = it.nomisBookingId,
          nomisSequence = it.nomisSequence,
          label = prisonerMapping.label,
          mappingType = prisonerMapping.mappingType,
        )
      },
    ).collect()
  }

  suspend fun getMappingByNomisId(nomisBookingId: Long, sequence: Int): BedAssignmentMappingDto = repository
    .findOneByNomisBookingIdAndNomisSequence(nomisBookingId, sequence)
    ?.let { BedAssignmentMappingDto(it) }
    ?: throw NotFoundException("Bed assignment with booking id =$nomisBookingId, seq = $sequence not found")

  suspend fun getMappingByDpsId(dpsBedAssignmentId: String): BedAssignmentMappingDto = repository
    .findById(UUID.fromString(dpsBedAssignmentId))
    ?.let { BedAssignmentMappingDto(it) }
    ?: throw NotFoundException("Bed assignment with dpsBedAssignmentId=$dpsBedAssignmentId not found")

  suspend fun getMappingForLatestMigrated(): BedAssignmentMappingDto = repository
    .findFirstByMappingTypeOrderByWhenCreatedDesc(BedAssignmentMappingType.MIGRATED)
    ?.let { BedAssignmentMappingDto(it) }
    ?: throw NotFoundException("No migrated mapping found")

  suspend fun getCountByMigrationIdGroupedByPrisoner(
    pageRequest: Pageable,
    migrationId: String,
  ): Long = repository.count() / averageBedAssignmentsPerPrisoner // Approx estimate

  @Transactional
  suspend fun deleteMapping(dpsBedAssignmentId: String) = repository
    .deleteById(UUID.fromString(dpsBedAssignmentId))

  @Transactional
  suspend fun deleteMapping(nomisBookingId: Long, sequence: Int) = repository
    .deleteByNomisBookingIdAndNomisSequence(nomisBookingId, sequence)

  fun BedAssignmentMapping.toDto() = BedAssignmentMappingDto(this)

  fun BedAssignmentMappingDto.fromDto() = BedAssignmentMapping(
    dpsBedAssignmentId = UUID.fromString(dpsBedAssignmentId),
    nomisBookingId = nomisBookingId,
    nomisSequence = nomisSequence,
    label = label,
    mappingType = mappingType,
    whenCreated = whenCreated,
  )
}
