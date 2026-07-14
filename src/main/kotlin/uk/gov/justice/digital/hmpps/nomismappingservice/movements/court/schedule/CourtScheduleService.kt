package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule

import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.util.UUID

@Service
class CourtScheduleService(
  private val scheduleRepository: CourtScheduleRepository,
) {

  @Transactional
  suspend fun createScheduleMapping(mappingDto: CourtScheduleMappingDto) {
    scheduleRepository.save(mappingDto.toMapping())
  }

  @Transactional
  suspend fun updateNomisEventId(dpsCourtAppearanceId: UUID, newNomisEventId: Long, newBookingId: Long) = scheduleRepository.findByDpsCourtAppearanceId(dpsCourtAppearanceId)!!
    .apply {
      nomisEventId = newNomisEventId
      bookingId = newBookingId
    }
    .also { scheduleRepository.save(it) }

  suspend fun getScheduleMappingByNomisId(nomisEventId: Long) = scheduleRepository.findByNomisEventId(nomisEventId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS event id $nomisEventId not found")

  suspend fun getScheduleMappingByDpsId(dpsCourtScheduleId: UUID) = scheduleRepository.findById(dpsCourtScheduleId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS court schedule id $dpsCourtScheduleId not found")

  @Transactional
  suspend fun deleteScheduleMappingByNomisId(nomisEventId: Long) = scheduleRepository.deleteByNomisEventId(nomisEventId)

  @Transactional
  suspend fun deleteScheduleMappingByDpsId(dpsId: UUID) = scheduleRepository.deleteByDpsCourtAppearanceId(dpsId)

  suspend fun getExistingCourtScheduleMappingSimilarTo(mapping: CourtScheduleMappingDto) = runCatching {
    getScheduleMappingByNomisId(mapping.nomisEventId)
  }
    .getOrElse {
      getScheduleMappingByDpsId(mapping.dpsCourtAppearanceId)
    }

  @Transactional
  suspend fun handleUpsertDuplicateMapping(mapping: CourtScheduleMappingDto, dke: DuplicateKeyException): CourtScheduleMappingUpsertByDpsIdResponse {
    val existing = getExistingCourtScheduleMappingSimilarTo(mapping)
    // The duplicate is for an existing DPS ID
    if (existing.dpsCourtAppearanceId == mapping.dpsCourtAppearanceId) {
      // The requested NOMIS ID is different so we need to update it
      if (existing.nomisEventId != mapping.nomisEventId) {
        updateNomisEventId(mapping.dpsCourtAppearanceId, mapping.nomisEventId, mapping.bookingId)
        return CourtScheduleMappingUpsertByDpsIdResponse(replacedNomisEventId = existing.nomisEventId)
      }
      // We already know about this mapping, so this is a no-op to maintain idempotency
      else {
        return CourtScheduleMappingUpsertByDpsIdResponse.EVENT_ID_NOT_REPLACED
      }
      // The duplicate is for an existing NOMIS ID but we weren't expecting that, so it appears to be a genuine duplicate
    } else {
      throw DuplicateMappingException(
        messageIn = "Court schedule mapping already exists",
        duplicate = mapping,
        existing = existing,
        cause = dke,
      )
    }
  }
}

fun CourtScheduleMappingDto.toMapping(): CourtScheduleMapping = CourtScheduleMapping(
  dpsCourtAppearanceId,
  nomisEventId,
  prisonerNumber,
  bookingId,
  mappingType = mappingType,
)

fun CourtScheduleMapping.toMappingDto(): CourtScheduleMappingDto = CourtScheduleMappingDto(
  offenderNo,
  bookingId,
  nomisEventId,
  dpsCourtAppearanceId,
  mappingType = mappingType,
)
