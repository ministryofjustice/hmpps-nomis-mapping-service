package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.util.UUID

@Service
class CourtScheduleService(
  @Autowired val scheduleRepository: CourtScheduleRepository,
) {

  @Transactional
  suspend fun createScheduleMapping(mappingDto: CourtScheduleMappingDto) {
    scheduleRepository.save(mappingDto.toMapping())
  }

  suspend fun getScheduleMappingByNomisId(nomisEventId: Long) = scheduleRepository.findByNomisEventId(nomisEventId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS event id $nomisEventId not found")

  suspend fun getScheduleMappingByDpsId(dpsScheduledMovementId: UUID) = scheduleRepository.findById(dpsScheduledMovementId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS scheduled movement id $dpsScheduledMovementId not found")
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
