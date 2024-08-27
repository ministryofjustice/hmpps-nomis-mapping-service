package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPPlanMappingService(
  private val csipMappingRepository: CSIPMappingRepository,
  private val csipPlanMappingRepository: CSIPPlanMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createCSIPPlanMapping(createMappingRequest: CSIPPlanMappingDto) =
    with(createMappingRequest) {
      log.debug("creating csip plan {}", createMappingRequest)
      val csipReportMappingId = csipMappingRepository.save(
        CSIPMapping(
          dpsCSIPId = "123",
          nomisCSIPId = 456,
          label = label,
          mappingType = CSIPMappingType.MIGRATED,
        ),
      ).dpsCSIPId
      csipPlanMappingRepository.save(
        CSIPPlanMapping(
          dpsCSIPPlanId = dpsCSIPPlanId,
          nomisCSIPPlanId = nomisCSIPPlanId,
          dpsCSIPReportId = csipReportMappingId,
          label = label,
          mappingType = mappingType,
        ),
      )
      telemetryClient.trackEvent(
        "csip-plan-mapping-created",
        mapOf(
          "dpsCSIPPlanId" to dpsCSIPPlanId,
          "nomisCSIPPlanId" to nomisCSIPPlanId.toString(),
          "batchId" to label,
        ),
        null,
      )
      log.debug("Mapping created with dpsCSIPPlanId = $dpsCSIPPlanId, nomisCSIPPlanId=$nomisCSIPPlanId")
    }

  suspend fun getMappingByNomisId(nomisCSIPPlanId: Long): CSIPPlanMappingDto =
    csipPlanMappingRepository.findOneByNomisCSIPPlanId(
      nomisCSIPPlanId = nomisCSIPPlanId,
    )
      ?.toCSIPPlanDto()
      ?: throw NotFoundException("No CSIP Plan mapping for  nomisCSIPPlanId=$nomisCSIPPlanId")

  suspend fun getMappingByDpsId(dpsCSIPPlanId: String): CSIPPlanMappingDto =
    csipPlanMappingRepository.findById(dpsCSIPPlanId)
      ?.toCSIPPlanDto()
      ?: throw NotFoundException("No CSIP plan mapping found for dpsCSIPPlanId=$dpsCSIPPlanId")

  @Transactional
  suspend fun deleteMappingByDpsId(dpsCSIPPlanId: String) =
    csipPlanMappingRepository.deleteById(dpsCSIPPlanId)

  fun alreadyExistsMessage(
    duplicateMapping: CSIPPlanMappingDto,
    existingMapping: CSIPPlanMappingDto,
  ) =
    """CSIPPlan Plan mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
    """.trimMargin()
}

fun CSIPPlanMapping.toCSIPPlanDto() = CSIPPlanMappingDto(
  nomisCSIPPlanId = nomisCSIPPlanId,
  dpsCSIPPlanId = dpsCSIPPlanId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
