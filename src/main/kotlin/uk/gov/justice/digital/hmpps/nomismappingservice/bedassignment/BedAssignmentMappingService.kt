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

  @Transactional
  suspend fun createMappings(offenderNo: String, prisonerMapping: PrisonerBedAssignmentMappingsDto) {
    repository.deleteAllByOffenderNo(offenderNo)
    repository.saveAll(
      prisonerMapping.mappings.map {
        BedAssignmentMapping(
          dpsBedAssignmentId = UUID.fromString(it.dpsBedAssignmentId),
          nomisBookingId = it.nomisBookingId,
          nomisSequence = it.nomisSequence,
          offenderNo = offenderNo,
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

  suspend fun getMappings(offenderNo: String): AllPrisonerBedAssignmentMappingsDto = repository
    .findAllByOffenderNoOrderByNomisBookingIdAscNomisSequenceAsc(offenderNo)
    .map { it.toDto() }
    .let { AllPrisonerBedAssignmentMappingsDto(it) }

  @Transactional
  suspend fun updateMappingsByNomisId(oldOffenderNo: String, newOffenderNo: String) {
    val count = repository.updateOffenderNo(oldOffenderNo, newOffenderNo)
    telemetryClient.trackEvent(
      "bed-assignment-mapping-prisoner-merged",
      mapOf(
        "count" to count.toString(),
        "oldOffenderNo" to oldOffenderNo,
        "newOffenderNo" to newOffenderNo,
      ),
      null,
    )
  }

  @Transactional
  suspend fun updateMappingsByBookingId(bookingId: Long, oldOffenderNo: String, newOffenderNo: String): List<BedAssignmentMappingDto> {
    val bedAssignments = repository.updateOffenderNoByBooking(bookingId, oldOffenderNo, newOffenderNo)

    telemetryClient.trackEvent(
      "bed-assignment-mapping-booking-moved",
      mapOf(
        "count" to bedAssignments.size.toString(),
        "bookingId" to bookingId.toString(),
        "oldOffenderNo" to oldOffenderNo,
        "newOffenderNo" to newOffenderNo,
      ),
      null,
    )
    return bedAssignments.map { it.toDto() }
  }

  fun BedAssignmentMapping.toDto() = BedAssignmentMappingDto(this)

  fun BedAssignmentMappingDto.fromDto() = BedAssignmentMapping(
    dpsBedAssignmentId = UUID.fromString(dpsBedAssignmentId),
    nomisBookingId = nomisBookingId,
    nomisSequence = nomisSequence,
    offenderNo = offenderNo,
    label = label,
    mappingType = mappingType,
    whenCreated = whenCreated,
  )
}
