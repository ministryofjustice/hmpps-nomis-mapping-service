package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPInterviewMappingService(
  private val csipInterviewMappingRepository: CSIPInterviewMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createCSIPInterviewMapping(createMappingRequest: CSIPInterviewMappingDto) =
    with(createMappingRequest) {
      log.debug("creating csip interview {}", createMappingRequest)

      csipInterviewMappingRepository.save(
        CSIPInterviewMapping(
          dpsCSIPInterviewId = dpsCSIPInterviewId,
          nomisCSIPInterviewId = nomisCSIPInterviewId,
          label = label,
          mappingType = mappingType,
        ),
      )
      telemetryClient.trackEvent(
        "csip-interview-mapping-created",
        mapOf(
          "dpsCSIPInterviewId" to dpsCSIPInterviewId,
          "nomisCSIPInterviewId" to nomisCSIPInterviewId.toString(),
          "batchId" to label,
        ),
        null,
      )
      log.debug("Mapping created with dpsCSIPInterviewId = $dpsCSIPInterviewId, nomisCSIPInterviewId=$nomisCSIPInterviewId")
    }

  suspend fun getMappingByNomisId(nomisCSIPInterviewId: Long): CSIPInterviewMappingDto =
    csipInterviewMappingRepository.findOneByNomisCSIPInterviewId(
      nomisCSIPInterviewId = nomisCSIPInterviewId,
    )
      ?.toCSIPInterviewDto()
      ?: throw NotFoundException("No CSIP Interview mapping for  nomisCSIPInterviewId=$nomisCSIPInterviewId")

  suspend fun getMappingByDpsId(dpsCSIPInterviewId: String): CSIPInterviewMappingDto =
    csipInterviewMappingRepository.findById(dpsCSIPInterviewId)
      ?.toCSIPInterviewDto()
      ?: throw NotFoundException("No CSIP interview mapping found for dpsCSIPInterviewId=$dpsCSIPInterviewId")

  @Transactional
  suspend fun deleteMappingByDpsId(dpsCSIPInterviewId: String) =
    csipInterviewMappingRepository.deleteById(dpsCSIPInterviewId)

  fun alreadyExistsMessage(
    duplicateMapping: CSIPInterviewMappingDto,
    existingMapping: CSIPInterviewMappingDto,
  ) =
    """CSIPInterview Interview mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
    """.trimMargin()
}

fun CSIPInterviewMapping.toCSIPInterviewDto() = CSIPInterviewMappingDto(
  nomisCSIPInterviewId = nomisCSIPInterviewId,
  dpsCSIPInterviewId = dpsCSIPInterviewId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
