package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.offender

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.movement.TransferMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule.TransferScheduleRepository

@Service
class TransferSchedulerPrisonerService(
  private val scheduleRepository: TransferScheduleRepository,
  private val movementRepository: TransferMovementRepository,
) {

  suspend fun getAllMappingIds(prisonerNumber: String) = TransferSchedulerPrisonerMappingIdsDto(
    prisonerNumber = prisonerNumber,
    schedules = scheduleRepository.findByOffenderNo(prisonerNumber)
      .map { TransferScheduleMappingIdsDto(it.nomisEventId, it.dpsTransferScheduleId) },
    movements = movementRepository.findByOffenderNo(prisonerNumber)
      .map { TransferMovementMappingIdsDto(it.nomisBookingId, it.nomisMovementSeq, it.dpsTransferMovementId) },
  )
}
