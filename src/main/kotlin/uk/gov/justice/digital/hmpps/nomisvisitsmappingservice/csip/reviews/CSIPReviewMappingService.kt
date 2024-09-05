package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPReviewMappingService(
  private val csipReviewMappingRepository: CSIPReviewMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createCSIPReviewMapping(createMappingRequest: CSIPReviewMappingDto) =
    with(createMappingRequest) {
      log.debug("creating csip review {}", createMappingRequest)

      csipReviewMappingRepository.save(
        CSIPReviewMapping(
          dpsCSIPReviewId = dpsCSIPReviewId,
          nomisCSIPReviewId = nomisCSIPReviewId,
          dpsCSIPReportId = dpsCSIPReportId,
          label = label,
          mappingType = mappingType,
        ),
      )
      telemetryClient.trackEvent(
        "csip-review-mapping-created",
        mapOf(
          "dpsCSIPReviewId" to dpsCSIPReviewId,
          "nomisCSIPReviewId" to nomisCSIPReviewId.toString(),
          "dpsCSIPReportId" to dpsCSIPReportId,
          "batchId" to label,
        ),
        null,
      )
      log.debug("Mapping created with dpsCSIPReviewId = $dpsCSIPReviewId, nomisCSIPReviewId=$nomisCSIPReviewId")
    }

  suspend fun getMappingByNomisId(nomisCSIPReviewId: Long): CSIPReviewMappingDto =
    csipReviewMappingRepository.findOneByNomisCSIPReviewId(
      nomisCSIPReviewId = nomisCSIPReviewId,
    )
      ?.toCSIPReviewDto()
      ?: throw NotFoundException("No CSIP Review mapping for  nomisCSIPReviewId=$nomisCSIPReviewId")

  suspend fun getMappingByDpsId(dpsCSIPReviewId: String): CSIPReviewMappingDto =
    csipReviewMappingRepository.findById(dpsCSIPReviewId)
      ?.toCSIPReviewDto()
      ?: throw NotFoundException("No CSIP review mapping found for dpsCSIPReviewId=$dpsCSIPReviewId")

  suspend fun getMappingByDpsCSIPReportId(dpsCSIPReportId: String): List<CSIPReviewMappingDto> =
    csipReviewMappingRepository.findAllByDpsCSIPReportId(dpsCSIPReportId)
      .map { it.toCSIPReviewDto() }

  @Transactional
  suspend fun deleteMappingByDpsId(dpsCSIPReviewId: String) =
    csipReviewMappingRepository.deleteById(dpsCSIPReviewId)

  fun alreadyExistsMessage(
    duplicateMapping: CSIPReviewMappingDto,
    existingMapping: CSIPReviewMappingDto,
  ) =
    """CSIPReview Review mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
    """.trimMargin()
}

fun CSIPReviewMapping.toCSIPReviewDto() = CSIPReviewMappingDto(
  nomisCSIPReviewId = nomisCSIPReviewId,
  dpsCSIPReviewId = dpsCSIPReviewId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
