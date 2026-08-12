package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.util.UUID

@Service
class TransferScheduleService(
  private val scheduleRepository: TransferScheduleRepository,
) {

  @Transactional
  suspend fun createScheduleMapping(mappingDto: TransferScheduleMappingDto) {
    scheduleRepository.save(mappingDto.toMapping())
  }

  suspend fun getScheduleMappingByNomisId(nomisEventId: Long) = scheduleRepository.findByNomisEventId(nomisEventId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS event id $nomisEventId not found")

  suspend fun getScheduleMappingByDpsId(dpsTransferScheduleId: UUID) = scheduleRepository.findById(dpsTransferScheduleId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS transfer schedule id $dpsTransferScheduleId not found")

  @Transactional
  suspend fun deleteScheduleMappingByNomisId(nomisEventId: Long) = scheduleRepository.deleteByNomisEventId(nomisEventId)

  suspend fun getExistingTransferScheduleMappingSimilarTo(mapping: TransferScheduleMappingDto) = runCatching {
    getScheduleMappingByNomisId(mapping.nomisEventId)
  }
    .getOrElse {
      getScheduleMappingByDpsId(mapping.dpsTransferScheduleId)
    }
}

fun TransferScheduleMappingDto.toMapping(): TransferScheduleMapping = TransferScheduleMapping(
  dpsTransferScheduleId,
  nomisEventId,
  prisonerNumber,
  bookingId,
  mappingType = mappingType,
)

fun TransferScheduleMapping.toMappingDto(): TransferScheduleMappingDto = TransferScheduleMappingDto(
  offenderNo,
  bookingId,
  nomisEventId,
  dpsTransferScheduleId,
  mappingType = mappingType,
)
