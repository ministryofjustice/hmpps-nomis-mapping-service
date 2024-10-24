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
  suspend fun createMapping(createMappingRequest: CaseNoteMappingDto) =
    with(createMappingRequest) {
      repository.findById(dpsCaseNoteId)?.run {
        if (this@run.nomisCaseNoteId == this@with.nomisCaseNoteId) {
          log.debug(
            "Not creating. All OK: {}",
            alreadyExistsMessage(
              duplicateMapping = createMappingRequest,
              existingMapping = CaseNoteMappingDto(this@run),
            ),
          )
          return
        }
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = CaseNoteMappingDto(this@run),
          ),
          duplicate = createMappingRequest,
          existing = CaseNoteMappingDto(this@run),
        )
      }

      repository.findOneByNomisCaseNoteId(nomisCaseNoteId)?.run {
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = CaseNoteMappingDto(this@run),
          ),
          duplicate = createMappingRequest,
          existing = CaseNoteMappingDto(this),
        )
      }

      repository.save(this.fromDto())
      telemetryClient.trackEvent(
        "casenotes-mapping-created",
        mapOf(
          "id" to dpsCaseNoteId,
          "nomisCaseNoteId" to nomisCaseNoteId.toString(),
          "batchId" to label,
        ),
        null,
      )
      log.debug("Mapping created with dpsCaseNoteId = $dpsCaseNoteId, nomisCaseNoteId = $nomisCaseNoteId")
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
    repository.findOneByNomisCaseNoteId(nomisCaseNoteId)
      ?.let { CaseNoteMappingDto(it) }
      ?: throw NotFoundException("CaseNote with nomisCaseNoteId=$nomisCaseNoteId not found")

  suspend fun getMappingsByNomisId(nomisCaseNoteIds: List<Long>): List<CaseNoteMappingDto> =
    repository.findByNomisCaseNoteIdIn(nomisCaseNoteIds).map {
      CaseNoteMappingDto(it)
    }

  suspend fun getMappingByDpsId(dpsCaseNoteId: String): CaseNoteMappingDto =
    repository.findById(dpsCaseNoteId)
      ?.let { CaseNoteMappingDto(it) }
      ?: throw NotFoundException("CaseNote with dpsCaseNoteId=$dpsCaseNoteId not found")

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
  suspend fun deleteMapping(dpsCaseNoteId: String) = repository.deleteById(dpsCaseNoteId)

  @Transactional
  suspend fun deleteMapping(nomisCaseNoteId: Long) = repository.deleteByNomisCaseNoteId(nomisCaseNoteId)

  @Transactional
  suspend fun deleteMappings(onlyMigrated: Boolean) {
    if (onlyMigrated) {
      repository.deleteByMappingTypeEquals(CaseNoteMappingType.MIGRATED)
    } else {
      repository.deleteAll()
    }
    // TODO: this will timeout!
  }

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
    val rows = repository.updateOffenderNoByBooking(bookingId, newOffenderNo)
    telemetryClient.trackEvent(
      "casenotes-mapping-booking-merged",
      mapOf(
        "count" to rows.size.toString(),
        "bookingId" to bookingId.toString(),
        "newOffenderNo" to newOffenderNo,
      ),
      null,
    )
    return rows.map { it.toDto() }
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
