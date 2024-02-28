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
  private val courtAppearanceMappingRepository: CourtAppearanceMappingRepository,
  private val courtChargeMappingRepository: CourtChargeMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createMapping(createMappingRequest: CourtCaseAllMappingDto) =
    with(createMappingRequest) {
      courtCaseMappingRepository.save(createMappingRequest.toCourtCaseMapping())
        .also {
          createMappingRequest.courtAppearances.forEach {
            createCourtAppearanceMapping(it)
          }
          createMappingRequest.courtCharges.forEach {
            createCourtChargeMapping(it)
          }
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

  @Transactional
  suspend fun createCourtAppearanceMapping(createMappingRequest: CourtAppearanceMappingDto) =
    with(createMappingRequest) {
      courtAppearanceMappingRepository.save(createMappingRequest.toCourtAppearanceMapping()).also {
        telemetryClient.trackEvent(
          "court-appearance-mapping-created",
          mapOf(
            "dpsCourtAppearanceId" to dpsCourtAppearanceId,
            "nomisCourtAppearanceId" to nomisCourtAppearanceId.toString(),
          ),
          null,
        )
      }
    }

  @Transactional
  suspend fun createCourtChargeMapping(createMappingRequest: CourtChargeMappingDto) =
    with(createMappingRequest) {
      courtChargeMappingRepository.save(createMappingRequest.toCourtChargeMapping()).also {
        telemetryClient.trackEvent(
          "court-charge-mapping-created",
          mapOf(
            "dpsCourtChargeId" to dpsCourtChargeId,
            "nomisCourtChargeId" to nomisCourtChargeId.toString(),
          ),
          null,
        )
      }
    }

  suspend fun getCourtAppearanceMappingByDpsId(courtAppearanceId: String): CourtAppearanceMappingDto =
    courtAppearanceMappingRepository.findById(courtAppearanceId)?.toCourtAppearanceMappingDto()
      ?: throw NotFoundException("DPS Court case Id =$courtAppearanceId")

  suspend fun getCourtAppearanceMappingByNomisId(courtAppearanceId: Long): CourtAppearanceMappingDto =
    courtAppearanceMappingRepository.findByNomisCourtAppearanceId(courtAppearanceId)?.toCourtAppearanceMappingDto()
      ?: throw NotFoundException("Nomis Court appearance Id =$courtAppearanceId")
}

fun CourtCaseMapping.toCourtCaseMappingDto(): CourtCaseMappingDto = CourtCaseMappingDto(
  dpsCourtCaseId = this.dpsCourtCaseId,
  nomisCourtCaseId = this.nomisCourtCaseId,
  label = this.label,
  mappingType = this.mappingType,
  whenCreated = this.whenCreated,
)

fun CourtCaseAllMappingDto.toCourtCaseMapping(): CourtCaseMapping = CourtCaseMapping(
  dpsCourtCaseId = this.dpsCourtCaseId,
  nomisCourtCaseId = this.nomisCourtCaseId,
  label = this.label,
  mappingType = mappingType ?: CourtCaseMappingType.DPS_CREATED,
)

fun CourtAppearanceMapping.toCourtAppearanceMappingDto(): CourtAppearanceMappingDto = CourtAppearanceMappingDto(
  dpsCourtAppearanceId = this.dpsCourtAppearanceId,
  nomisCourtAppearanceId = this.nomisCourtAppearanceId,
  label = this.label,
  mappingType = this.mappingType,
  whenCreated = this.whenCreated,
)

fun CourtAppearanceMappingDto.toCourtAppearanceMapping(): CourtAppearanceMapping = CourtAppearanceMapping(
  dpsCourtAppearanceId = this.dpsCourtAppearanceId,
  nomisCourtAppearanceId = this.nomisCourtAppearanceId,
  label = this.label,
  mappingType = mappingType ?: CourtAppearanceMappingType.DPS_CREATED,
)

fun CourtChargeMapping.toCourtChargeMappingDto(): CourtChargeMappingDto = CourtChargeMappingDto(
  dpsCourtChargeId = this.dpsCourtChargeId,
  nomisCourtChargeId = this.nomisCourtChargeId,
  label = this.label,
  mappingType = this.mappingType,
  whenCreated = this.whenCreated,
)

fun CourtChargeMappingDto.toCourtChargeMapping(): CourtChargeMapping = CourtChargeMapping(
  dpsCourtChargeId = this.dpsCourtChargeId,
  nomisCourtChargeId = this.nomisCourtChargeId,
  label = this.label,
  mappingType = mappingType ?: CourtChargeMappingType.DPS_CREATED,
)
