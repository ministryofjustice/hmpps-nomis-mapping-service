package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CourtSentencingMappingService(
  private val courtCaseMappingRepository: CourtCaseMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createMapping(createMappingRequest: CourtCaseMappingDto) =
    with(createMappingRequest) {
      courtCaseMappingRepository.save(createMappingRequest.toCourtCaseMapping()).also {
        telemetryClient.trackEvent(
          "court-case-mapping-created",
          mapOf(
            "dpsCourtCaseId" to dpsCourtCaseId,
            "nomisCourtCaseId" to nomisCourtCaseId.toString(),
          ),
          null,
        )
      }
    }

  suspend fun getCourtCaseMappingByDpsId(courtCaseId: String): CourtCaseMappingDto =
    courtCaseMappingRepository.findById(courtCaseId)?.toCourtCaseMappingDto()
      ?: throw NotFoundException("DPS Court case Id =$courtCaseId")

  suspend fun getCourtCaseMappingByNomisId(courtCaseId: Long): CourtCaseMappingDto =
    courtCaseMappingRepository.findByNomisCourtCaseId(courtCaseId)?.toCourtCaseMappingDto()
      ?: throw NotFoundException("Nomis Court case Id =$courtCaseId")
}

fun CourtCaseMapping.toCourtCaseMappingDto(): CourtCaseMappingDto = CourtCaseMappingDto(
  dpsCourtCaseId = this.dpsCourtCaseId,
  nomisCourtCaseId = this.nomisCourtCaseId,
  label = this.label,
  mappingType = this.mappingType,
  whenCreated = this.whenCreated,
)

fun CourtCaseMappingDto.toCourtCaseMapping(): CourtCaseMapping = CourtCaseMapping(
  dpsCourtCaseId = this.dpsCourtCaseId,
  nomisCourtCaseId = this.nomisCourtCaseId,
  label = this.label,
  mappingType = mappingType ?: CourtCaseMappingType.DPS_CREATED,
)
