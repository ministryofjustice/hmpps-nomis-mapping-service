package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationAllMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationHearingMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationPunishmentBatchUpdateMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationPunishmentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationPunishmentNomisIdDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationHearingMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationPunishmentMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.AdjudicationHearingMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.AdjudicationMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.AdjudicationPunishmentMappingRepository

@Service
@Transactional(readOnly = true)
class AdjudicationMappingService(
  private val adjudicationMappingRepository: AdjudicationMappingRepository,
  private val adjudicationHearingMappingRepository: AdjudicationHearingMappingRepository,
  private val adjudicationPunishmentMappingRepository: AdjudicationPunishmentMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createMapping(createMappingRequest: AdjudicationAllMappingDto) {
    createMapping(
      createMappingRequest.adjudicationId.copy(
        mappingType = createMappingRequest.mappingType,
        label = createMappingRequest.label,
      ),
    )
    createMappingRequest.hearings.forEach {
      createMapping(
        it.copy(
          mappingType = createMappingRequest.mappingType,
          label = createMappingRequest.label,
        ),
      )
    }
    createMappingRequest.punishments.forEach {
      createMapping(
        it.copy(
          mappingType = createMappingRequest.mappingType,
          label = createMappingRequest.label,
        ),
      )
    }
  }

  @Transactional
  suspend fun createMapping(createMappingRequest: AdjudicationMappingDto) =
    with(createMappingRequest) {
      log.debug("creating adjudication {}", createMappingRequest)

      adjudicationMappingRepository.findById(chargeNumber)
        ?.let {
          throw DuplicateMappingException(
            existing = chargeNumber,
            duplicate = chargeNumber,
            messageIn = "Adjudication mapping with id $chargeNumber already exists",
          )
        }
        ?: adjudicationMappingRepository.save(
          AdjudicationMapping(
            adjudicationNumber = adjudicationNumber,
            chargeSequence = chargeSequence,
            chargeNumber = chargeNumber,
            label = label,
            mappingType = AdjudicationMappingType.valueOf(mappingType ?: "ADJUDICATION_CREATED"),
          ),
        )

      telemetryClient.trackEvent(
        "adjudication-mapping-created",
        mapOf(
          "adjudicationNumber" to adjudicationNumber.toString(),
        ),
        null,
      )
      log.debug("Mapping created with adjudicationNumber = $adjudicationNumber")
    }

  @Transactional
  suspend fun createMapping(createMappingRequest: AdjudicationHearingMappingDto) =
    with(createMappingRequest) {
      log.debug("creating adjudication hearing {}", createMappingRequest)

      adjudicationHearingMappingRepository.findById(dpsHearingId)
        ?.let {
          throw DuplicateMappingException(
            existing = dpsHearingId,
            duplicate = nomisHearingId.toString(),
            messageIn = "Adjudication hearing mapping with id $dpsHearingId already exists",
          )
        }
        ?: adjudicationHearingMappingRepository.save(
          AdjudicationHearingMapping(
            dpsHearingId = dpsHearingId,
            nomisHearingId = nomisHearingId,
            label = label,
            mappingType = AdjudicationMappingType.valueOf(mappingType ?: "ADJUDICATION_CREATED"),
          ),
        )

      telemetryClient.trackEvent(
        "adjudication-hearing-mapping-created",
        mapOf(
          "dpsHearingId" to dpsHearingId,
          "nomisHearingId" to nomisHearingId.toString(),
        ),
        null,
      )
    }

  @Transactional
  suspend fun createMapping(createMappingRequest: AdjudicationPunishmentMappingDto) =
    with(createMappingRequest) {
      log.debug("creating adjudication punishment {}", createMappingRequest)

      adjudicationPunishmentMappingRepository.findById(dpsPunishmentId)
        ?.let {
          throw DuplicateMappingException(
            existing = it,
            duplicate = createMappingRequest,
            messageIn = "Adjudication punishment mapping with id $dpsPunishmentId already exists",
          )
        }
        ?: adjudicationPunishmentMappingRepository.save(
          AdjudicationPunishmentMapping(
            dpsPunishmentId = dpsPunishmentId,
            nomisBookingId = nomisBookingId,
            nomisSanctionSequence = nomisSanctionSequence,
            label = label,
            mappingType = AdjudicationMappingType.valueOf(mappingType ?: "ADJUDICATION_CREATED"),
          ),
        )

      telemetryClient.trackEvent(
        "adjudication-punishment-mapping-created",
        mapOf(
          "dpsPunishmentId" to dpsPunishmentId,
          "nomisBookingId" to nomisBookingId.toString(),
          "nomisSanctionSequence" to nomisSanctionSequence.toString(),
        ),
        null,
      )
    }

  suspend fun getMappingByDpsId(chargeNumber: String): AdjudicationMappingDto =
    adjudicationMappingRepository.findById(chargeNumber)
      ?.let { AdjudicationMappingDto(it) }
      ?: throw NotFoundException("chargeNumber=$chargeNumber")

  suspend fun getMappingByNomisId(adjudicationNumber: Long, chargeSequence: Int): AdjudicationMappingDto =
    adjudicationMappingRepository.findByAdjudicationNumberAndChargeSequence(adjudicationNumber, chargeSequence)
      ?.let { AdjudicationMappingDto(it) }
      ?: throw NotFoundException("adjudicationNumber=$adjudicationNumber, chargeSequence=$chargeSequence")

  suspend fun getHearingMappingByDpsId(hearingId: String): AdjudicationHearingMappingDto =
    adjudicationHearingMappingRepository.findById(hearingId)
      ?.let { AdjudicationHearingMappingDto(it) }
      ?: throw NotFoundException("DPS hearing Id=$hearingId")

  suspend fun getHearingMappingByNomisId(hearingId: Long): AdjudicationHearingMappingDto =
    adjudicationHearingMappingRepository.findByNomisHearingId(hearingId)
      ?.let { AdjudicationHearingMappingDto(it) }
      ?: throw NotFoundException("NOMIS hearing Id=$hearingId")

  @Transactional
  suspend fun deleteMapping(chargeNumber: String) = adjudicationMappingRepository.deleteById(chargeNumber)

  @Transactional
  suspend fun deleteAllMappings(migrationId: String) {
    adjudicationMappingRepository.deleteByLabel(migrationId)
    adjudicationHearingMappingRepository.deleteByLabel(migrationId)
    adjudicationPunishmentMappingRepository.deleteByLabel(migrationId)
  }

  @Transactional
  suspend fun deleteAllMappings(migrationOnly: Boolean, synchronisationOnly: Boolean) {
    if (migrationOnly == synchronisationOnly) { // implies delete everything even for the weird true/true scenario
      adjudicationMappingRepository.deleteAll()
      adjudicationHearingMappingRepository.deleteAll()
      adjudicationPunishmentMappingRepository.deleteAll()
    } else {
      val type = if (migrationOnly) {
        AdjudicationMappingType.MIGRATED
      } else {
        AdjudicationMappingType.ADJUDICATION_CREATED
      }
      adjudicationMappingRepository.deleteAllByMappingType(type)
      adjudicationHearingMappingRepository.deleteAllByMappingType(type)
      adjudicationPunishmentMappingRepository.deleteAllByMappingType(type)
    }
  }

  @Transactional
  suspend fun deleteHearingMapping(dpsId: String) = adjudicationHearingMappingRepository.deleteById(dpsId)

  suspend fun getAdjudicationMappingsByMigrationId(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<AdjudicationMappingDto> =
    coroutineScope {
      val adjudicationMapping = async {
        adjudicationMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
          label = migrationId,
          AdjudicationMappingType.MIGRATED,
          pageRequest,
        )
      }

      val count = async {
        adjudicationMappingRepository.countAllByLabelAndMappingType(
          migrationId,
          mappingType = AdjudicationMappingType.MIGRATED,
        )
      }

      PageImpl(
        adjudicationMapping.await().toList().map { AdjudicationMappingDto(it) },
        pageRequest,
        count.await(),
      )
    }

  suspend fun getAdjudicationMappingForLatestMigrated(): AdjudicationMappingDto =
    adjudicationMappingRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(AdjudicationMappingType.MIGRATED)
      ?.let { AdjudicationMappingDto(it) }
      ?: throw NotFoundException("No migrated mapping found")

  suspend fun getAllMappings(): List<AdjudicationMappingDto> =
    adjudicationMappingRepository.findAll().toList().map { AdjudicationMappingDto(it) }

  @Transactional
  suspend fun createPunishmentMappings(punishments: List<AdjudicationPunishmentMappingDto>) =
    punishments.forEach { createMapping(it) }

  @Transactional
  suspend fun createAndDeletePunishmentMappings(request: AdjudicationPunishmentBatchUpdateMappingDto) {
    createPunishmentMappings(request.punishmentsToCreate)
    deletePunishmentMappings(request.punishmentsToDelete)
  }

  private suspend fun deletePunishmentMappings(punishments: List<AdjudicationPunishmentNomisIdDto>) =
    punishments.forEach {
      adjudicationPunishmentMappingRepository.deleteByNomisBookingIdAndNomisSanctionSequence(
        it.nomisBookingId,
        it.nomisSanctionSequence,
      )
    }

  suspend fun getPunishmentMappingByDpsId(dpsPunishmentId: String): AdjudicationPunishmentMappingDto =
    adjudicationPunishmentMappingRepository.findById(dpsPunishmentId)
      ?.let { AdjudicationPunishmentMappingDto(it) }
      ?: throw NotFoundException("DPS punishment Id=$dpsPunishmentId")

  @Transactional
  suspend fun deletePunishmentMappingByDpsId(dpsPunishmentId: String) =
    adjudicationPunishmentMappingRepository.deleteById(dpsPunishmentId)
}
