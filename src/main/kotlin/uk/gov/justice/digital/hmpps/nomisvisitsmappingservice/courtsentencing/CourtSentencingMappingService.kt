package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.api.NomisSentenceId
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CourtSentencingMappingService(
  private val courtCaseMappingRepository: CourtCaseMappingRepository,
  private val courtCasePrisonerMappingRepository: CourtCasePrisonerMigrationRepository,
  private val courtAppearanceMappingRepository: CourtAppearanceMappingRepository,
  private val courtChargeMappingRepository: CourtChargeMappingRepository,
  private val sentenceMappingRepository: SentenceMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createMapping(createMappingRequest: CourtCaseAllMappingDto) = with(createMappingRequest) {
    try {
      courtCaseMappingRepository.save(
        createMappingRequest.toCourtCaseMapping(),
      )
    } catch (e: Exception) {
      log.info(
        "Failed to create court case mapping for dpsCourtCaseId=${createMappingRequest.dpsCourtCaseId} and nomisCourtCaseId=${createMappingRequest.nomisCourtCaseId}",
        e,
      )
      throw e
    }
    createMappingRequest.courtAppearances.forEach {
      try {
        createCourtAppearanceMapping(it)
      } catch (e: Exception) {
        log.info(
          "Failed to create court appearance mapping for dpsCourtAppearanceId=${it.dpsCourtAppearanceId}, nomisCourtAppearanceId=${it.nomisCourtAppearanceId}, dpsCourtCaseId=${createMappingRequest.dpsCourtCaseId} and nomisCourtCaseId=${createMappingRequest.nomisCourtCaseId}",
          e,
        )
        throw e
      }
    }
    createMappingRequest.courtCharges.forEach {
      try {
        createCourtChargeMapping(it)
      } catch (e: Exception) {
        log.info(
          "Failed to create court charge mapping for dpsCourtChargeId=${it.dpsCourtChargeId}, nomisCourtAppearanceId=${it.nomisCourtChargeId}, dpsCourtCaseId=${createMappingRequest.dpsCourtCaseId} and nomisCourtCaseId=${createMappingRequest.nomisCourtCaseId}",
          e,
        )
        throw e
      }
    }
    createMappingRequest.sentences.forEach {
      try {
        createSentenceAllMapping(it)
      } catch (e: Exception) {
        log.info(
          "Failed to create sentence mapping for dpsSentenceId=${it.dpsSentenceId}, nomisSentenceSeq=${it.nomisSentenceSequence}, nomisBooking = ${it.nomisBookingId}, dpsCourtCaseId=${createMappingRequest.dpsCourtCaseId} and nomisCourtCaseId=${createMappingRequest.nomisCourtCaseId}",
          e,
        )
        throw e
      }
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

  @Transactional
  suspend fun createAllMappingsForOffenderIdempotent(createMappingRequest: CourtCaseMigrationMappingDto) = with(createMappingRequest) {
    createMappingRequest.courtCases.map { courtCaseMappingRequest ->
      try {
        deleteCourtCaseMappingByNomisId(courtCaseMappingRequest.nomisCourtCaseId)
        createCourtCaseMapping(courtCaseMappingRequest)
      } catch (e: Exception) {
        log.info(
          "Failed to create court case mapping for dpsCourtCaseId=${courtCaseMappingRequest.dpsCourtCaseId} and nomisCourtCaseId=${courtCaseMappingRequest.nomisCourtCaseId}",
          e,
        )
        throw e
      }
    }
    createMappingRequest.courtAppearances.forEach {
      try {
        deleteCourtAppearanceMappingByNomisId(it.nomisCourtAppearanceId)
        createCourtAppearanceMapping(it)
      } catch (e: Exception) {
        log.info(
          "Failed to create court appearance mapping for dpsCourtAppearanceId=${it.dpsCourtAppearanceId}, nomisCourtAppearanceId=${it.nomisCourtAppearanceId}",
          e,
        )
        throw e
      }
    }
    createMappingRequest.courtCharges.forEach {
      try {
        deleteCourtChargeMappingByNomisId(it.nomisCourtChargeId)
        createCourtChargeMapping(it)
      } catch (e: Exception) {
        log.info(
          "Failed to create court charge mapping for dpsCourtChargeId=${it.dpsCourtChargeId}, nomisCourtChargeId=${it.nomisCourtChargeId}",
          e,
        )
        throw e
      }
    }
    createMappingRequest.sentences.forEach {
      try {
        deleteSentenceMappingByNomisId(it.nomisBookingId, it.nomisSentenceSequence)
        createSentenceAllMapping(it)
      } catch (e: Exception) {
        log.info(
          "Failed to create sentence mapping for dpsSentenceId=${it.dpsSentenceId}, nomisSentenceSeq=${it.nomisSentenceSequence}, nomisBooking = ${it.nomisBookingId}",
          e,
        )
        throw e
      }
    }
  }

  @Transactional
  suspend fun createMigrationMapping(offenderNo: String, createMappingRequest: CourtCaseMigrationMappingDto) {
    createAllMappingsForOffenderIdempotent(createMappingRequest).also {
      courtCasePrisonerMappingRepository.deleteById(offenderNo)
      courtCasePrisonerMappingRepository.save(
        CourtCasePrisonerMigration(
          offenderNo = offenderNo,
          count = createMappingRequest.courtCases.size,
          mappingType = createMappingRequest.mappingType,
          label = createMappingRequest.label,
        ),
      )
    }
  }

  suspend fun getCourtCaseMappingByDpsId(courtCaseId: String): CourtCaseMappingDto = courtCaseMappingRepository.findById(courtCaseId)?.toCourtCaseMappingDto()
    ?: throw NotFoundException("DPS Court case Id =$courtCaseId")

  suspend fun getCourtCaseMappingByNomisId(courtCaseId: Long): CourtCaseMappingDto = courtCaseMappingRepository.findByNomisCourtCaseId(courtCaseId)?.toCourtCaseMappingDto()
    ?: throw NotFoundException("Nomis Court case Id =$courtCaseId")

  suspend fun getCourtCaseAllMappingByDpsId(courtCaseId: String): CourtCaseAllMappingDto = courtCaseMappingRepository.findById(courtCaseId)?.toCourtCaseAllMappingDto()
    ?: throw NotFoundException("DPS Court case Id =$courtCaseId")

  suspend fun getCourtCaseAllMappingByDpsIdOrNull(courtCaseId: String): CourtCaseAllMappingDto? = courtCaseMappingRepository.findByDpsCourtCaseId(courtCaseId)?.toCourtCaseAllMappingDto()

  suspend fun getCourtCaseAllMappingByNomisId(courtCaseId: Long): CourtCaseAllMappingDto = courtCaseMappingRepository.findByNomisCourtCaseId(courtCaseId)?.toCourtCaseAllMappingDto()
    ?: throw NotFoundException("Nomis Court case Id =$courtCaseId")

  suspend fun getCourtCaseMigrationSummaryForOffender(offenderNo: String): CourtSentencingMigrationSummary = courtCasePrisonerMappingRepository.findById(offenderNo)?.toCourtSentencingMigrationSummary()
    ?: throw NotFoundException("Court sentencing offender migration summary not found. offenderNo=$offenderNo")

  @Transactional
  suspend fun deleteCourtCaseMigrationSummaryForOffender(offenderNo: String) = courtCasePrisonerMappingRepository.deleteById(offenderNo)

  @Transactional
  suspend fun deleteCourtCaseMappingByDpsId(courtCaseId: String) = courtCaseMappingRepository.deleteById(courtCaseId)

  @Transactional
  suspend fun deleteCourtCaseMappingByNomisId(courtCaseId: Long) = courtCaseMappingRepository.deleteByNomisCourtCaseId(courtCaseId)

  @Transactional
  suspend fun createCourtAppearanceMapping(createMappingRequest: CourtAppearanceMappingDto) = with(createMappingRequest) {
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
  suspend fun createCourtCaseMapping(createMappingRequest: CourtCaseMappingDto) = with(createMappingRequest) {
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

  @Transactional
  suspend fun deleteCourtAppearanceMappingByDpsId(courtAppearanceId: String) = courtAppearanceMappingRepository.deleteById(courtAppearanceId)

  @Transactional
  suspend fun deleteCourtAppearanceMappingByNomisId(courtAppearanceId: Long) = courtAppearanceMappingRepository.deleteByNomisCourtAppearanceId(courtAppearanceId)

  @Transactional
  suspend fun createCourtChargeMapping(createMappingRequest: CourtChargeMappingDto) = with(createMappingRequest) {
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
  suspend fun createCourtChargeMappings(courtCharges: List<CourtChargeMappingDto>) = courtCharges.forEach { createCourtChargeMapping(it) }

  @Transactional
  suspend fun createSentenceAllMapping(createSentenceMappingRequest: SentenceMappingDto) = with(createSentenceMappingRequest) {
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
  suspend fun deleteSentenceMappingByNomisId(bookingId: Long, sentenceSequence: Int) = sentenceMappingRepository.deleteByNomisBookingIdAndNomisSentenceSequence(
    nomisBookingId = bookingId,
    nomisSentenceSeq = sentenceSequence,
  ).also {
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
  suspend fun deleteSentenceMappingByDpsId(sentenceId: String) = sentenceMappingRepository.deleteById(sentenceId).also {
    telemetryClient.trackEvent(
      "sentence-mapping-deleted",
      mapOf(
        "dpsSentenceId" to sentenceId,
      ),
      null,
    )
  }

  private suspend fun deleteCourtChargeMappings(courtCharges: List<CourtChargeNomisIdDto>) = courtCharges.forEach {
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

  suspend fun getCourtAppearanceMappingByDpsId(courtAppearanceId: String): CourtAppearanceMappingDto = courtAppearanceMappingRepository.findById(courtAppearanceId)?.toCourtAppearanceMappingDto()
    ?: throw NotFoundException("DPS Court appearance Id =$courtAppearanceId")

  suspend fun getCourtAppearanceAllMappingByDpsId(courtAppearanceId: String): CourtAppearanceAllMappingDto = courtAppearanceMappingRepository.findById(courtAppearanceId)?.toCourtAppearanceAllMappingDto()
    ?: throw NotFoundException("DPS Court appearance Id =$courtAppearanceId")

  suspend fun getCourtAppearanceMappingByNomisId(courtAppearanceId: Long): CourtAppearanceMappingDto = courtAppearanceMappingRepository.findByNomisCourtAppearanceId(courtAppearanceId)?.toCourtAppearanceMappingDto()
    ?: throw NotFoundException("Nomis Court appearance Id =$courtAppearanceId")

  suspend fun getCourtAppearanceAllMappingByNomisId(courtAppearanceId: Long): CourtAppearanceAllMappingDto = courtAppearanceMappingRepository.findByNomisCourtAppearanceId(courtAppearanceId)?.toCourtAppearanceAllMappingDto()
    ?: throw NotFoundException("Nomis Court appearance Id =$courtAppearanceId")

  suspend fun getCourtChargeMappingByDpsId(courtChargeId: String): CourtChargeMappingDto = courtChargeMappingRepository.findById(courtChargeId)?.toCourtChargeMappingDto()
    ?: throw NotFoundException("DPS Court charge Id =$courtChargeId")

  suspend fun getCourtChargeMappingByNomisId(courtChargeId: Long): CourtChargeMappingDto = courtChargeMappingRepository.findByNomisCourtChargeId(courtChargeId)?.toCourtChargeMappingDto()
    ?: throw NotFoundException("NOMIS Court charge Id =$courtChargeId")

  suspend fun getSentenceAllMappingByDpsId(dpsSentenceId: String): SentenceMappingDto = sentenceMappingRepository.findById(dpsSentenceId)?.toSentenceAllMappingDto()
    ?: throw NotFoundException("Sentence mapping not found with dpsSentenceId =$dpsSentenceId")

  suspend fun getSentenceAllMappingByNomisId(nomisBookingId: Long, nomisSentenceSeq: Int): SentenceMappingDto = sentenceMappingRepository.findByNomisBookingIdAndNomisSentenceSequence(
    nomisBookingId = nomisBookingId,
    nomisSentenceSeq = nomisSentenceSeq,
  )?.toSentenceAllMappingDto()
    ?: throw NotFoundException("Sentence mapping not found with nomisBookingId =$nomisBookingId, nomisSentenceSeq =$nomisSentenceSeq")

  suspend fun getSentencesByNomisIds(nomisSentenceIds: List<NomisSentenceId>): Flow<SentenceMapping> = nomisSentenceIds.asFlow().map {
    sentenceMappingRepository.findByNomisBookingIdAndNomisSentenceSequence(
      nomisBookingId = it.nomisBookingId,
      nomisSentenceSeq = it.nomisSentenceSequence,
    )
  }.filterNotNull()

  @Transactional
  suspend fun deleteCourtChargeMappingByNomisId(courtChargeId: Long) = courtChargeMappingRepository.deleteByNomisCourtChargeId(courtChargeId)

  suspend fun getCourtCaseMappingsByMigrationId(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<CourtSentencingMigrationSummary> = coroutineScope {
    val migrationOffenders = async {
      courtCasePrisonerMappingRepository.findAllByLabel(
        label = migrationId,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      courtCasePrisonerMappingRepository.countAllByLabel(
        label = migrationId,
      )
    }

    PageImpl(
      migrationOffenders.await().toList().map { it.toCourtSentencingMigrationSummary() },
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

fun CourtCaseMappingDto.toCourtCaseMapping(): CourtCaseMapping = CourtCaseMapping(
  dpsCourtCaseId = this.dpsCourtCaseId,
  nomisCourtCaseId = this.nomisCourtCaseId,
  label = this.label,
  mappingType = mappingType ?: CourtCaseMappingType.DPS_CREATED,
)

// on duplicate the calling service are expecting CourtCaseAllMappingDto to be returned, child entities are NOT populated
fun CourtCaseMapping.toCourtCaseAllMappingDto(): CourtCaseAllMappingDto = CourtCaseAllMappingDto(
  dpsCourtCaseId = this.dpsCourtCaseId,
  nomisCourtCaseId = this.nomisCourtCaseId,
  label = this.label,
  mappingType = this.mappingType,
  whenCreated = this.whenCreated,
)

fun CourtAppearanceMapping.toCourtAppearanceMappingDto(): CourtAppearanceMappingDto = CourtAppearanceMappingDto(
  dpsCourtAppearanceId = this.dpsCourtAppearanceId,
  nomisCourtAppearanceId = this.nomisCourtAppearanceId,
  label = this.label,
  mappingType = this.mappingType,
  whenCreated = this.whenCreated,
)

// on duplicate the calling service are expecting CourtAppearanceAllMappingDto to be returned, child entities are NOT populated
fun CourtAppearanceMapping.toCourtAppearanceAllMappingDto(): CourtAppearanceAllMappingDto = CourtAppearanceAllMappingDto(
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

fun CourtCasePrisonerMigration.toCourtSentencingMigrationSummary(): CourtSentencingMigrationSummary = CourtSentencingMigrationSummary(
  offenderNo = this.offenderNo,
  mappingsCount = this.count,
  whenCreated = this.whenCreated,
  migrationId = this.label,
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
