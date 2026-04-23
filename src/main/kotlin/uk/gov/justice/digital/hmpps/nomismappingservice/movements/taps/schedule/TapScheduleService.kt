package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.schedule

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.time.LocalDate
import java.util.UUID

@Service
class TapScheduleService(
  @Autowired val scheduleRepository: TapScheduleRepository,
) {

  @Transactional
  suspend fun createScheduleMapping(mappingDto: TapScheduleMappingDto) {
    scheduleRepository.save(mappingDto.toMapping())
  }

  @Transactional
  suspend fun updateScheduleMapping(mappingDto: TapScheduleMappingDto) {
    scheduleRepository.findById(mappingDto.dpsOccurrenceId)
      ?.let {
        it.nomisAddressId = mappingDto.nomisAddressId
        it.nomisAddressOwnerClass = mappingDto.nomisAddressOwnerClass
        it.dpsUprn = mappingDto.dpsUprn
        it.dpsAddressText = mappingDto.dpsAddressText
        it.dpsDescription = mappingDto.dpsDescription
        it.dpsPostcode = mappingDto.dpsPostcode
        it.eventTime = mappingDto.eventTime
        scheduleRepository.save(it)
      }
      ?: throw NotFoundException("Mapping for DPS occurrence id ${mappingDto.dpsOccurrenceId} not found")
  }

  suspend fun getScheduleMappingByNomisId(nomisEventId: Long) = scheduleRepository.findByNomisEventId(nomisEventId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS event id $nomisEventId not found")

  suspend fun getScheduleMappingByDpsId(dpsScheduledMovementId: UUID) = scheduleRepository.findById(dpsScheduledMovementId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS scheduled movement id $dpsScheduledMovementId not found")

  @Transactional
  suspend fun deleteScheduleMappingByNomisId(nomisEventId: Long) = scheduleRepository.deleteByNomisEventId(nomisEventId)

  suspend fun findTapScheduleMappingsByNomisAddressId(nomisAddressId: Long, fromDate: LocalDate) = scheduleRepository
    .findByNomisAddressIdAndEventTimeIsGreaterThanEqual(nomisAddressId, fromDate.atStartOfDay())
    .map { it.toMappingDto() }
    .let { FindTapScheduleMappingsForAddressResponse(it) }
}

fun TapScheduleMappingDto.toMapping(): TapScheduleMapping = TapScheduleMapping(
  dpsOccurrenceId,
  nomisEventId,
  prisonerNumber,
  bookingId,
  nomisAddressId,
  nomisAddressOwnerClass,
  dpsAddressText,
  dpsUprn,
  dpsDescription,
  dpsPostcode,
  eventTime,
  mappingType = mappingType,
)

fun TapScheduleMapping.toMappingDto(): TapScheduleMappingDto = TapScheduleMappingDto(
  offenderNo,
  bookingId,
  nomisEventId,
  dpsOccurrenceId,
  mappingType = mappingType,
  nomisAddressId,
  nomisAddressOwnerClass,
  dpsAddressText,
  dpsUprn,
  dpsDescription,
  dpsPostcode,
  eventTime,
)
