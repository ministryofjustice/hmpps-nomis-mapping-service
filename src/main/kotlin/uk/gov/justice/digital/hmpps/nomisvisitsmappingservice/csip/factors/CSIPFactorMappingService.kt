package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPFactorMappingService(
  private val csipFactorMappingRepository: CSIPFactorMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createCSIPFactorMapping(createMappingRequest: CSIPFactorMappingDto) =
    with(createMappingRequest) {
      log.debug("creating csip factor {}", createMappingRequest)

      csipFactorMappingRepository.save(
        CSIPFactorMapping(
          dpsCSIPFactorId = dpsCSIPFactorId,
          nomisCSIPFactorId = nomisCSIPFactorId,
          label = label,
          mappingType = mappingType,
        ),
      )
      telemetryClient.trackEvent(
        "csip-factor-mapping-created",
        mapOf(
          "dpsCSIPFactorId" to dpsCSIPFactorId,
          "nomisCSIPFactorId" to nomisCSIPFactorId.toString(),
          "batchId" to label,
        ),
        null,
      )
      log.debug("Mapping created with dpsCSIPFactorId = $dpsCSIPFactorId, nomisCSIPFactorId=$nomisCSIPFactorId")
    }

  suspend fun getMappingByNomisId(nomisCSIPFactorId: Long): CSIPFactorMappingDto =
    csipFactorMappingRepository.findOneByNomisCSIPFactorId(
      nomisCSIPFactorId = nomisCSIPFactorId,
    )
      ?.toCSIPFactorDto()
      ?: throw NotFoundException("No CSIP Factor mapping for  nomisCSIPFactorId=$nomisCSIPFactorId")

  suspend fun getMappingByDpsId(dpsCSIPFactorId: String): CSIPFactorMappingDto =
    csipFactorMappingRepository.findById(dpsCSIPFactorId)
      ?.toCSIPFactorDto()
      ?: throw NotFoundException("No CSIP factor mapping found for dpsCSIPFactorId=$dpsCSIPFactorId")

  @Transactional
  suspend fun deleteMappingByDpsId(dpsCSIPFactorId: String) =
    csipFactorMappingRepository.deleteById(dpsCSIPFactorId)

  fun alreadyExistsMessage(
    duplicateMapping: CSIPFactorMappingDto,
    existingMapping: CSIPFactorMappingDto,
  ) =
    """CSIPFactor Factor mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
    """.trimMargin()
}

fun CSIPFactorMapping.toCSIPFactorDto() = CSIPFactorMappingDto(
  nomisCSIPFactorId = nomisCSIPFactorId,
  dpsCSIPFactorId = dpsCSIPFactorId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
