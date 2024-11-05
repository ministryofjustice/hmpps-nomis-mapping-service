package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CourtSentencingMappingService(
  private val courtCaseMappingRepository: CourtCaseMappingRepository,
  private val courtAppearanceMappingRepository: CourtAppearanceMappingRepository,
  private val courtChargeMappingRepository: CourtChargeMappingRepository,
  private val sentenceMappingRepository: SentenceMappingRepository,
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
  suspend fun deleteCourtCaseMappingByDpsId(courtCaseId: String) =
    courtCaseMappingRepository.deleteById(courtCaseId)

  @Transactional
  suspend fun deleteCourtCaseMappingByNomisId(courtCaseId: Long) =
    courtCaseMappingRepository.deleteByNomisCourtCaseId(courtCaseId)

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
  suspend fun createCourtAppearanceAllMapping(createMappingRequest: CourtAppearanceAllMappingDto) =
    with(createMappingRequest) {
      courtAppearanceMappingRepository.save(createMappingRequest.toCourtAppearanceMapping()).also {
        createMappingRequest.courtCharges.forEach {
          createCourtChargeMapping(it)
        }
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
  suspend fun deleteCourtAppearanceMappingByDpsId(courtAppearanceId: String) =
    courtAppearanceMappingRepository.deleteById(courtAppearanceId)

  @Transactional
  suspend fun deleteCourtAppearanceMappingByNomisId(courtAppearanceId: Long) =
    courtAppearanceMappingRepository.deleteByNomisCourtAppearanceId(courtAppearanceId)

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

  @Transactional
  suspend fun createAndDeleteCourtChargeMappings(request: CourtChargeBatchUpdateMappingDto) {
    createCourtChargeMappings(request.courtChargesToCreate)
    deleteCourtChargeMappings(request.courtChargesToDelete)
  }

  @Transactional
  suspend fun createCourtChargeMappings(courtCharges: List<CourtChargeMappingDto>) =
    courtCharges.forEach { createCourtChargeMapping(it) }

  @Transactional
  suspend fun createSentenceAllMapping(createSentenceMappingRequest: SentenceMappingDto) =
    with(createSentenceMappingRequest) {
      sentenceMappingRepository.save(createSentenceMappingRequest.toSentenceMapping())
        .also {
          telemetryClient.trackEvent(
            "sentence-mapping-created",
            mapOf(
              "dpsSentenceId" to dpsSentenceId,
              "nomisBookingId" to nomisBookingId.toString(),
              "nomisSentenceSeq" to nomisSentenceSequence.toString(),
            ),
            null,
          )
        }
    }

  @Transactional
  suspend fun deleteSentenceMappingByNomisId(bookingId: Long, sentenceSequence: Int) =
    sentenceMappingRepository.deleteByNomisBookingIdAndNomisSentenceSequence(nomisBookingId = bookingId, nomisSentenceSeq = sentenceSequence).also {
      telemetryClient.trackEvent(
        "sentence-mapping-deleted",
        mapOf(
          "nomisBookingId" to bookingId.toString(),
          "nomisSentenceSeq" to sentenceSequence.toString(),
        ),
        null,
      )
    }

  @Transactional
  suspend fun deleteSentenceMappingByDpsId(sentenceId: String) =
    sentenceMappingRepository.deleteById(sentenceId).also {
      telemetryClient.trackEvent(
        "sentence-mapping-deleted",
        mapOf(
          "dpsSentenceId" to sentenceId,
        ),
        null,
      )
    }

  private suspend fun deleteCourtChargeMappings(courtCharges: List<CourtChargeNomisIdDto>) =
    courtCharges.forEach {
      courtChargeMappingRepository.deleteByNomisCourtChargeId(
        it.nomisCourtChargeId,
      )
      telemetryClient.trackEvent(
        "court-charge-mapping-deleted",
        mapOf(
          "nomisCourtChargeId" to it.nomisCourtChargeId.toString(),
        ),
        null,
      )
    }

  suspend fun getCourtAppearanceMappingByDpsId(courtAppearanceId: String): CourtAppearanceMappingDto =
    courtAppearanceMappingRepository.findById(courtAppearanceId)?.toCourtAppearanceMappingDto()
      ?: throw NotFoundException("DPS Court appearance Id =$courtAppearanceId")

  suspend fun getCourtAppearanceMappingByNomisId(courtAppearanceId: Long): CourtAppearanceMappingDto =
    courtAppearanceMappingRepository.findByNomisCourtAppearanceId(courtAppearanceId)?.toCourtAppearanceMappingDto()
      ?: throw NotFoundException("Nomis Court appearance Id =$courtAppearanceId")

  suspend fun getCourtChargeMappingByDpsId(courtChargeId: String): CourtChargeMappingDto =
    courtChargeMappingRepository.findById(courtChargeId)?.toCourtChargeMappingDto()
      ?: throw NotFoundException("DPS Court charge Id =$courtChargeId")

  suspend fun getCourtChargeMappingByNomisId(courtChargeId: Long): CourtChargeMappingDto =
    courtChargeMappingRepository.findByNomisCourtChargeId(courtChargeId)?.toCourtChargeMappingDto()
      ?: throw NotFoundException("NOMIS Court charge Id =$courtChargeId")

  suspend fun getSentenceAllMappingByDpsId(dpsSentenceId: String): SentenceMappingDto =
    sentenceMappingRepository.findById(dpsSentenceId)?.toSentenceAllMappingDto()
      ?: throw NotFoundException("Sentence mapping not found with dpsSentenceId =$dpsSentenceId")

  suspend fun getSentenceAllMappingByNomisId(nomisBookingId: Long, nomisSentenceSeq: Int): SentenceMappingDto =
    sentenceMappingRepository.findByNomisBookingIdAndNomisSentenceSequence(nomisBookingId = nomisBookingId, nomisSentenceSeq = nomisSentenceSeq)?.toSentenceAllMappingDto()
      ?: throw NotFoundException("Sentence mapping not found with nomisBookingId =$nomisBookingId, nomisSentenceSeq =$nomisSentenceSeq")

  @Transactional
  suspend fun deleteCourtChargeMappingByNomisId(courtChargeId: Long) =
    courtChargeMappingRepository.deleteByNomisCourtChargeId(courtChargeId)

  suspend fun getCourtCaseMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<CourtCaseMappingDto> = coroutineScope {
    val mappings = async {
      courtCaseMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        mappingType = CourtCaseMappingType.MIGRATED,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      courtCaseMappingRepository.countAllByLabelAndMappingType(
        migrationId = migrationId,
        mappingType = CourtCaseMappingType.MIGRATED,
      )
    }

    PageImpl(
      mappings.await().toList().map { it.toCourtCaseMappingDto() },
      pageRequest,
      count.await(),
    )
  }
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

fun CourtAppearanceAllMappingDto.toCourtAppearanceMapping(): CourtAppearanceMapping = CourtAppearanceMapping(
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

fun SentenceMapping.toSentenceAllMappingDto(): SentenceMappingDto = SentenceMappingDto(
  dpsSentenceId = this.dpsSentenceId,
  nomisSentenceSequence = this.nomisSentenceSequence,
  nomisBookingId = this.nomisBookingId,
  label = this.label,
  mappingType = this.mappingType,
  whenCreated = this.whenCreated,
)

fun SentenceMappingDto.toSentenceMapping(): SentenceMapping = SentenceMapping(
  dpsSentenceId = this.dpsSentenceId,
  nomisSentenceSequence = this.nomisSentenceSequence,
  nomisBookingId = this.nomisBookingId,
  label = this.label,
  mappingType = mappingType ?: SentenceMappingType.DPS_CREATED,
)
