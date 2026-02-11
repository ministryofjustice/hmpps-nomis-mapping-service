package uk.gov.justice.digital.hmpps.nomismappingservice.csra

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
class CsraMappingService(
  private val repository: CsraMappingRepository,
  private val telemetryClient: TelemetryClient,
  @Value("\${csras.average-per-prisoner}") private val averageCsrasPerPrisoner: Int,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun alreadyExistsMessage(
    duplicateMapping: CsraMappingDto,
    existingMapping: CsraMappingDto,
  ) = """Csra mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
  """.trimMargin()

  @Transactional
  suspend fun createMapping(createMappingRequest: CsraMappingDto) {
    repository.findOneByNomisBookingIdAndNomisSequence(
      createMappingRequest.nomisBookingId,
      createMappingRequest.nomisSequence,
    )
      ?.let { mapping ->
        if (mapping.dpsCsraId.toString() == createMappingRequest.dpsCsraId) {
          log.debug(
            "Not creating. All OK: {}",
            alreadyExistsMessage(
              duplicateMapping = createMappingRequest,
              existingMapping = CsraMappingDto(mapping),
            ),
          )
          return
        }
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = CsraMappingDto(mapping),
          ),
          duplicate = createMappingRequest,
          existing = CsraMappingDto(mapping),
        )
      }

    repository.save(createMappingRequest.fromDto())
    telemetryClient.trackEvent(
      "csra-mapping-created",
      mapOf(
        "dpsCsraId" to createMappingRequest.dpsCsraId,
        "nomisBookingId" to createMappingRequest.nomisBookingId.toString(),
        "nomisSequence" to createMappingRequest.nomisSequence.toString(),
        "batchId" to createMappingRequest.label,
      ),
      null,
    )
  }

  @Transactional
  suspend fun createMappings(mappings: List<CsraMappingDto>) {
    repository.saveAll(mappings.map { it.fromDto() }).collect()
  }

  suspend fun getMappingByNomisId(nomisBookingId: Long, sequence: Int): CsraMappingDto = repository
    .findOneByNomisBookingIdAndNomisSequence(nomisBookingId, sequence)
    ?.let { CsraMappingDto(it) }
    ?: throw NotFoundException("CSRA with booking id =$nomisBookingId, seq = $sequence not found")

  suspend fun getMappingByDpsId(dpsCsraId: String): CsraMappingDto = repository
    .findById(UUID.fromString(dpsCsraId))
    ?.let { CsraMappingDto(it) }
    ?: throw NotFoundException("Csra with dpsCsraId=$dpsCsraId not found")

  suspend fun getMappingForLatestMigrated(): CsraMappingDto = repository
    .findFirstByMappingTypeOrderByWhenCreatedDesc(CsraMappingType.MIGRATED)
    ?.let { CsraMappingDto(it) }
    ?: throw NotFoundException("No migrated mapping found")

  suspend fun getCountByMigrationIdGroupedByPrisoner(
    pageRequest: Pageable,
    migrationId: String,
  ): Long = repository.count() / averageCsrasPerPrisoner // Approx estimate

  @Transactional
  suspend fun deleteMapping(dpsCsraId: String) = repository
    .deleteById(UUID.fromString(dpsCsraId))

  @Transactional
  suspend fun deleteMapping(nomisBookingId: Long, sequence: Int) = repository
    .deleteByNomisBookingIdAndNomisSequence(nomisBookingId, sequence)

  suspend fun getMappings(offenderNo: String): AllPrisonerCsraMappingsDto = repository
    .findAllByOffenderNoOrderByNomisBookingIdAscNomisSequenceAsc(offenderNo)
    .map { it.toDto() }
    .let { AllPrisonerCsraMappingsDto(it) }

  @Transactional
  suspend fun updateMappingsByNomisId(oldOffenderNo: String, newOffenderNo: String) {
    val count = repository.updateOffenderNo(oldOffenderNo, newOffenderNo)
    telemetryClient.trackEvent(
      "csra-mapping-prisoner-merged",
      mapOf(
        "count" to count.toString(),
        "oldOffenderNo" to oldOffenderNo,
        "newOffenderNo" to newOffenderNo,
      ),
      null,
    )
  }

  @Transactional
  suspend fun updateMappingsByBookingId(bookingId: Long, newOffenderNo: String): List<CsraMappingDto> {
    val csras = repository.updateOffenderNoByBooking(bookingId, newOffenderNo)

    telemetryClient.trackEvent(
      "csra-mapping-booking-moved",
      mapOf(
        "count" to csras.size.toString(),
        "bookingId" to bookingId.toString(),
        "newOffenderNo" to newOffenderNo,
      ),
      null,
    )
    return (csras).map { it.toDto() }
  }

  fun CsraMapping.toDto() = CsraMappingDto(this)

  fun CsraMappingDto.fromDto() = CsraMapping(
    dpsCsraId = UUID.fromString(dpsCsraId),
    nomisBookingId = nomisBookingId,
    nomisSequence = nomisSequence,
    offenderNo = offenderNo,
    label = label,
    mappingType = mappingType,
    whenCreated = whenCreated,
  )
}
