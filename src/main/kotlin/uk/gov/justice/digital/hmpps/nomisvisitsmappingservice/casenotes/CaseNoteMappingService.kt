package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.casenotes

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.collect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CaseNoteMappingService(
  private val repository: CaseNoteMappingRepository,
  private val telemetryClient: TelemetryClient,
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
      log.debug("creating location {}", createMappingRequest)
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
  }

  fun CaseNoteMapping.toDto() = CaseNoteMappingDto(
    dpsCaseNoteId = dpsCaseNoteId,
    nomisCaseNoteId = nomisCaseNoteId,
    label = label,
    mappingType = mappingType,
    whenCreated = whenCreated,
  )

  fun CaseNoteMappingDto.fromDto() = CaseNoteMapping(
    dpsCaseNoteId = dpsCaseNoteId,
    nomisCaseNoteId = nomisCaseNoteId,
    label = label,
    mappingType = mappingType,
  )
}
