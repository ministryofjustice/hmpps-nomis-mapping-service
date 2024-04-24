package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPMappingService(
  private val csipMappingRepository: CSIPMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun alreadyExistsMessage(
    duplicateMapping: CSIPMappingDto,
    existingMapping: CSIPMappingDto,
  ) =
    """CSIP mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
    """.trimMargin()

  @Transactional
  suspend fun createCSIPMapping(createMappingRequest: CSIPMappingDto) =
    with(createMappingRequest) {
      log.debug("creating csip {}", createMappingRequest)
      csipMappingRepository.findById(dpsCSIPId)?.run {
        if (this@run.nomisCSIPId == this@with.nomisCSIPId) {
          log.debug(
            "Not creating. All OK: {}",
            alreadyExistsMessage(
              duplicateMapping = createMappingRequest,
              existingMapping = CSIPMappingDto(this@run),
            ),
          )
          return
        }
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = CSIPMappingDto(this@run),
          ),
          duplicate = createMappingRequest,
          existing = CSIPMappingDto(this@run),
        )
      }

      csipMappingRepository.findOneByNomisCSIPId(
        nomisCSIPId = nomisCSIPId,
      )?.run {
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = CSIPMappingDto(this@run),
          ),
          duplicate = createMappingRequest,
          existing = CSIPMappingDto(this),
        )
      }

      csipMappingRepository.save(
        CSIPMapping(
          dpsCSIPId = dpsCSIPId,
          nomisCSIPId = nomisCSIPId,
          label = label,
          mappingType = CSIPMappingType.valueOf(mappingType),
        ),
      )
      telemetryClient.trackEvent(
        "csip-mapping-created",
        mapOf(
          "dpsCSIPId" to dpsCSIPId,
          "nomisCSIPId" to nomisCSIPId.toString(),
          "batchId" to label,
        ),
        null,
      )
      log.debug("Mapping created with dpsCSIPId = $dpsCSIPId, nomisCSIPId=$nomisCSIPId")
    }

  suspend fun getMappingByNomisId(nomisCSIPId: Long): CSIPMappingDto =
    csipMappingRepository.findOneByNomisCSIPId(
      nomisCSIPId = nomisCSIPId,
    )
      ?.let { CSIPMappingDto(it) }
      ?: throw NotFoundException("CSIP with nomisCSIPId=$nomisCSIPId not found")

  suspend fun getMappingByDPSId(dpsCSIPId: String): CSIPMappingDto =
    csipMappingRepository.findById(dpsCSIPId)
      ?.let { CSIPMappingDto(it) }
      ?: throw NotFoundException("dpsCSIPId=$dpsCSIPId")

  @Transactional
  suspend fun deleteMappingByDPSId(dpsCSIPId: String) = csipMappingRepository.deleteById(dpsCSIPId)

  @Transactional
  suspend fun deleteMappings(onlyMigrated: Boolean) =
    onlyMigrated.takeIf { it }?.apply {
      csipMappingRepository.deleteByMappingTypeEquals(CSIPMappingType.MIGRATED)
    } ?: run {
      csipMappingRepository.deleteAll()
    }

  suspend fun getMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<CSIPMappingDto> =
    coroutineScope {
      val csipMapping = async {
        csipMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
          label = migrationId,
          CSIPMappingType.MIGRATED,
          pageRequest,
        )
      }

      val count = async {
        csipMappingRepository.countAllByLabelAndMappingType(migrationId, mappingType = CSIPMappingType.MIGRATED)
      }

      PageImpl(
        csipMapping.await().toList().map { CSIPMappingDto(it) },
        pageRequest,
        count.await(),
      )
    }

  suspend fun getMappingForLatestMigrated(): CSIPMappingDto =
    csipMappingRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(CSIPMappingType.MIGRATED)
      ?.let { CSIPMappingDto(it) }
      ?: throw NotFoundException("No migrated mapping found")
}
