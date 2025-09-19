package uk.gov.justice.digital.hmpps.nomismappingservice.finance

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
class TransactionMappingService(
  private val repository: TransactionMappingRepository,
  private val telemetryClient: TelemetryClient,
  @Value("\${transactions.average-transactions-per-prisoner}") private val averageTransactionsPerPrisoner: Int,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun alreadyExistsMessage(
    duplicateMapping: TransactionMappingDto,
    existingMapping: TransactionMappingDto,
  ) = """Transaction mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
  """.trimMargin()

  @Transactional
  suspend fun createMapping(createMappingRequest: TransactionMappingDto) {
    repository.findById(createMappingRequest.nomisTransactionId)?.let { mapping ->
      if (mapping.dpsTransactionId.toString() == createMappingRequest.dpsTransactionId) {
        log.debug(
          "Not creating. All OK: {}",
          alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = TransactionMappingDto(mapping),
          ),
        )
        return
      }
      throw DuplicateMappingException(
        messageIn = alreadyExistsMessage(
          duplicateMapping = createMappingRequest,
          existingMapping = TransactionMappingDto(mapping),
        ),
        duplicate = createMappingRequest,
        existing = TransactionMappingDto(mapping),
      )
    }

    repository.save(createMappingRequest.fromDto())
    telemetryClient.trackEvent(
      "transactions-mapping-created",
      mapOf(
        "dpsTransactionId" to createMappingRequest.dpsTransactionId,
        "nomisTransactionId" to createMappingRequest.nomisTransactionId.toString(),
        "batchId" to createMappingRequest.label,
      ),
      null,
    )
    log.debug("Mapping created with dpsTransactionId = ${createMappingRequest.dpsTransactionId}, nomisTransactionId = ${createMappingRequest.nomisTransactionId}")
  }

  @Transactional
  suspend fun createMappings(mappings: List<TransactionMappingDto>) {
    repository.saveAll(mappings.map { it.fromDto() }).collect()
  }

  suspend fun getMappingByNomisId(nomisTransactionId: Long): TransactionMappingDto = repository.findById(nomisTransactionId)
    ?.let { TransactionMappingDto(it) }
    ?: throw NotFoundException("Transaction with nomisTransactionId=$nomisTransactionId not found")

  suspend fun getMappingsByNomisId(nomisTransactionIds: List<Long>): List<TransactionMappingDto> = repository.findByNomisTransactionIdIn(nomisTransactionIds).map {
    TransactionMappingDto(it)
  }

  suspend fun getMappingByDpsId(dpsTransactionId: String): TransactionMappingDto = repository
    .findByDpsTransactionId(UUID.fromString(dpsTransactionId))
    ?.let { TransactionMappingDto(it) }
    ?: throw NotFoundException("Transaction with dpsTransactionId=$dpsTransactionId not found")

  suspend fun getMappingForLatestMigrated(): TransactionMappingDto = repository
    .findFirstByMappingTypeOrderByWhenCreatedDesc(TransactionMappingType.MIGRATED)
    ?.let { TransactionMappingDto(it) }
    ?: throw NotFoundException("No migrated mapping found")

  suspend fun getCountByMigrationIdGroupedByPrisoner(
    pageRequest: Pageable,
    migrationId: String,
  ): Long = repository.count() / averageTransactionsPerPrisoner // Approx estimate

  @Transactional
  suspend fun deleteMapping(dpsTransactionId: String) = repository.deleteByDpsTransactionId(UUID.fromString(dpsTransactionId))

  @Transactional
  suspend fun deleteMapping(nomisTransactionId: Long) = repository.deleteById(nomisTransactionId)

  suspend fun getMappings(offenderNo: String): AllPrisonerTransactionMappingsDto = repository.findAllByOffenderNoOrderByNomisBookingIdAscNomisTransactionIdAsc(offenderNo)
    .map { it.toDto() }
    .let { AllPrisonerTransactionMappingsDto(it) }

  @Transactional
  suspend fun updateMappingsByNomisId(oldOffenderNo: String, newOffenderNo: String) {
    val count = repository.updateOffenderNo(oldOffenderNo, newOffenderNo)
    telemetryClient.trackEvent(
      "transactions-mapping-prisoner-merged",
      mapOf(
        "count" to count.toString(),
        "oldOffenderNo" to oldOffenderNo,
        "newOffenderNo" to newOffenderNo,
      ),
      null,
    )
  }

  @Transactional
  suspend fun updateMappingsByBookingId(bookingId: Long, newOffenderNo: String): List<TransactionMappingDto> {
    val transactions = repository.updateOffenderNoByBooking(bookingId, newOffenderNo)

    telemetryClient.trackEvent(
      "transactions-mapping-booking-moved",
      mapOf(
        "count" to transactions.size.toString(),
        "bookingId" to bookingId.toString(),
        "newOffenderNo" to newOffenderNo,
      ),
      null,
    )
    return transactions.map { it.toDto() }
  }

  fun TransactionMapping.toDto() = TransactionMappingDto(
    dpsTransactionId = dpsTransactionId.toString(),
    nomisTransactionId = nomisTransactionId,
    nomisBookingId = nomisBookingId,
    offenderNo = offenderNo,
    label = label,
    mappingType = mappingType,
    whenCreated = whenCreated,
  )

  fun TransactionMappingDto.fromDto() = TransactionMapping(
    dpsTransactionId = UUID.fromString(dpsTransactionId),
    nomisTransactionId = nomisTransactionId,
    offenderNo = offenderNo,
    nomisBookingId = nomisBookingId,
    label = label,
    mappingType = mappingType,
  )
}
