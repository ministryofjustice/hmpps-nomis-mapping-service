package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.casenotes

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.collect
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CaseNoteMappingService(
  private val repository: CaseNoteMappingRepository,
  private val telemetryClient: TelemetryClient,
  @Value("\${casenotes.average-case-notes-per-prisoner}") private val averageCaseNotesPerPrisoner: Int,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun alreadyExistsMessage(
    duplicateMapping: CaseNoteMappingDto,
    existingMapping: CaseNoteMappingDto,
  ) =
    """CaseNote mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
    """.trimMargin()

  @Transactional
  suspend fun createMapping(createMappingRequest: CaseNoteMappingDto) {
    repository.findById(createMappingRequest.nomisCaseNoteId)?.let { mapping ->
      if (mapping.dpsCaseNoteId.toString() == createMappingRequest.dpsCaseNoteId) {
        log.debug(
          "Not creating. All OK: {}",
          alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = CaseNoteMappingDto(mapping),
          ),
        )
        return
      }
      throw DuplicateMappingException(
        messageIn = alreadyExistsMessage(
          duplicateMapping = createMappingRequest,
          existingMapping = CaseNoteMappingDto(mapping),
        ),
        duplicate = createMappingRequest,
        existing = CaseNoteMappingDto(mapping),
      )
    }

    repository.save(createMappingRequest.fromDto())
    telemetryClient.trackEvent(
      "casenotes-mapping-created",
      mapOf(
        "dpsCaseNoteId" to createMappingRequest.dpsCaseNoteId,
        "nomisCaseNoteId" to createMappingRequest.nomisCaseNoteId.toString(),
        "batchId" to createMappingRequest.label,
      ),
      null,
    )
    log.debug("Mapping created with dpsCaseNoteId = ${createMappingRequest.dpsCaseNoteId}, nomisCaseNoteId = ${createMappingRequest.nomisCaseNoteId}")
  }

  @Transactional
  suspend fun createMappings(mappings: List<CaseNoteMappingDto>) {
    repository.saveAll(mappings.map { it.fromDto() }).collect()
  }

  @Transactional
  suspend fun createMappings(offenderNo: String, prisonerMapping: PrisonerCaseNoteMappingsDto) {
    repository.deleteAllByOffenderNo(offenderNo)
    repository.saveAll(
      prisonerMapping.mappings.map {
        CaseNoteMapping(
          dpsCaseNoteId = UUID.fromString(it.dpsCaseNoteId),
          nomisCaseNoteId = it.nomisCaseNoteId,
          nomisBookingId = it.nomisBookingId,
          offenderNo = offenderNo,
          label = prisonerMapping.label,
          mappingType = prisonerMapping.mappingType,
        )
      },
    ).collect()
  }

  suspend fun getMappingByNomisId(nomisCaseNoteId: Long): CaseNoteMappingDto =
    repository.findById(nomisCaseNoteId)
      ?.let { CaseNoteMappingDto(it) }
      ?: throw NotFoundException("CaseNote with nomisCaseNoteId=$nomisCaseNoteId not found")

  suspend fun getMappingsByNomisId(nomisCaseNoteIds: List<Long>): List<CaseNoteMappingDto> =
    repository.findByNomisCaseNoteIdIn(nomisCaseNoteIds).map {
      CaseNoteMappingDto(it)
    }

  // compatibility for old endpoint
  suspend fun getMappingByDpsId(dpsCaseNoteId: String): CaseNoteMappingDto =
    repository.findByDpsCaseNoteId(UUID.fromString(dpsCaseNoteId)).firstOrNull()
      ?.let { CaseNoteMappingDto(it) }
      ?: throw NotFoundException("CaseNote with dpsCaseNoteId=$dpsCaseNoteId not found")

  suspend fun getMappingsByDpsId(dpsCaseNoteId: String): List<CaseNoteMappingDto> =
    repository.findByDpsCaseNoteId(UUID.fromString(dpsCaseNoteId))
      .also {
        if (it.isEmpty()) {
          throw NotFoundException("CaseNote with dpsCaseNoteId=$dpsCaseNoteId not found")
        }
      }
      .map { CaseNoteMappingDto(it) }

//  suspend fun getMappingsByMigrationId(
//    pageRequest: Pageable,
//    migrationId: String,
//  ): Page<CaseNoteMappingDto> =
//    coroutineScope {
//      val caseNoteMapping = async {
//        repository.findAllByLabelAndMappingTypeOrderByLabelDesc(
//          label = migrationId,
//          CaseNoteMappingType.MIGRATED,
//          pageRequest,
//        )
//      }
//
//      val count = async {
//        repository.countAllByLabelAndMappingType(migrationId, mappingType = CaseNoteMappingType.MIGRATED)
//      }
//
//      PageImpl(
//        caseNoteMapping.await().toList().map { CaseNoteMappingDto(it) },
//        pageRequest,
//        count.await(),
//      )
//    }

  suspend fun getMappingForLatestMigrated(): CaseNoteMappingDto =
    repository.findFirstByMappingTypeOrderByWhenCreatedDesc(CaseNoteMappingType.MIGRATED)
      ?.let { CaseNoteMappingDto(it) }
      ?: throw NotFoundException("No migrated mapping found")

  suspend fun getCountByMigrationIdGroupedByPrisoner(
    pageRequest: Pageable,
    migrationId: String,
  ): Long = repository.count() / averageCaseNotesPerPrisoner // Approx estimate

  @Transactional
  suspend fun deleteMappings(dpsCaseNoteId: String) = repository.deleteByDpsCaseNoteId(UUID.fromString(dpsCaseNoteId))

  @Transactional
  suspend fun deleteMapping(nomisCaseNoteId: Long) = repository.deleteById(nomisCaseNoteId)

  suspend fun getMappings(offenderNo: String): AllPrisonerCaseNoteMappingsDto =
    repository.findAllByOffenderNoOrderByNomisBookingIdAscNomisCaseNoteIdAsc(offenderNo)
      .map { it.toDto() }
      .let { AllPrisonerCaseNoteMappingsDto(it) }

  @Transactional
  suspend fun updateMappingsByNomisId(oldOffenderNo: String, newOffenderNo: String) {
    val count = repository.updateOffenderNo(oldOffenderNo, newOffenderNo)
    telemetryClient.trackEvent(
      "casenotes-mapping-prisoner-merged",
      mapOf(
        "count" to count.toString(),
        "oldOffenderNo" to oldOffenderNo,
        "newOffenderNo" to newOffenderNo,
      ),
      null,
    )
  }

  @Transactional
  suspend fun updateMappingsByBookingId(bookingId: Long, newOffenderNo: String): List<CaseNoteMappingDto> {
    val caseNotes = repository.updateOffenderNoByBooking(bookingId, newOffenderNo)

    // also update mappings for any case notes linked to these by old merges
    val related = caseNotes.flatMap { caseNoteMapping ->
      repository.findByDpsCaseNoteId(caseNoteMapping.dpsCaseNoteId)
        .filter {
          it.nomisCaseNoteId != caseNoteMapping.nomisCaseNoteId
        }
        .onEach { relatedNoteMapping ->
          repository.updateOffenderNoById(newOffenderNo, relatedNoteMapping.nomisCaseNoteId)
          // No JPA but set this for function return value:
          relatedNoteMapping.offenderNo = newOffenderNo
        }
    }

    telemetryClient.trackEvent(
      "casenotes-mapping-booking-moved",
      mapOf(
        "count" to caseNotes.size.toString(),
        "related-count" to related.size.toString(),
        "bookingId" to bookingId.toString(),
        "newOffenderNo" to newOffenderNo,
      ),
      null,
    )
    return (caseNotes + related).map { it.toDto() }
  }

  fun CaseNoteMapping.toDto() = CaseNoteMappingDto(
    dpsCaseNoteId = dpsCaseNoteId.toString(),
    nomisCaseNoteId = nomisCaseNoteId,
    nomisBookingId = nomisBookingId,
    offenderNo = offenderNo,
    label = label,
    mappingType = mappingType,
    whenCreated = whenCreated,
  )

  fun CaseNoteMappingDto.fromDto() = CaseNoteMapping(
    dpsCaseNoteId = UUID.fromString(dpsCaseNoteId),
    nomisCaseNoteId = nomisCaseNoteId,
    offenderNo = offenderNo,
    nomisBookingId = nomisBookingId,
    label = label,
    mappingType = mappingType,
  )
}
