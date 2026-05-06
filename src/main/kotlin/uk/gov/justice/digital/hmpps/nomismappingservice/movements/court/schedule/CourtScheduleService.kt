package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

  suspend fun getScheduleMappingByNomisId(nomisEventId: Long) = scheduleRepository.findByNomisEventId(nomisEventId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS event id $nomisEventId not found")

  suspend fun getScheduleMappingByDpsId(dpsCourtScheduleId: UUID) = scheduleRepository.findById(dpsCourtScheduleId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS court schedule id $dpsCourtScheduleId not found")

  @Transactional
  suspend fun deleteScheduleMappingByNomisId(nomisEventId: Long) = scheduleRepository.deleteByNomisEventId(nomisEventId)
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
