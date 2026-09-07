package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.offender

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.movement.TransferMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule.TransferScheduleRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

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

  suspend fun getMappingsForMoveBooking(bookingId: Long): TransferSchedulerMoveBookingMappingDto {
    val schedules = scheduleRepository.findByBookingId(bookingId)
    val movements = movementRepository.findByNomisBookingId(bookingId)
    return TransferSchedulerMoveBookingMappingDto(
      scheduleIds = schedules.map { TransferScheduleIdMapping(it.nomisEventId, it.dpsTransferScheduleId) },
      movementIds = movements.map { TransferMovementIdMapping(it.nomisMovementSeq, it.dpsTransferMovementId) },
    )
  }

  @Transactional
  suspend fun moveMappingsForBooking(bookingId: Long, fromOffenderNo: String, toOffenderNo: String) {
    val schedules = scheduleRepository.findByBookingId(bookingId)
    val movements = movementRepository.findByNomisBookingId(bookingId)
    val bookingOffenders = (schedules.map { it.offenderNo } + movements.map { it.offenderNo }).toSet()

    // If we don't hold the booking ID then return not found as we probably shouldn't have made this request
    if (schedules.isEmpty() && movements.isEmpty()) {
      throw NotFoundException("No mappings found for booking $bookingId")
    }

    // The bookings are already on the to offender, so return OK as we are idempotent
    if (bookingOffenders.all { it == toOffenderNo }) {
      return
    }

    // If any mappings are on a different offender then reject the request - we might be in a more complicated merge + move booking scenario that needs to happen in the correct order
    val wrongOffenders = bookingOffenders.filter { it != fromOffenderNo && it != toOffenderNo }
    if (wrongOffenders.isNotEmpty()) {
      throw ValidationException("Mappings exist for booking $bookingId on unexpected offender(s): $wrongOffenders")
    }

    // Move the mappings to the new offender
    schedules.forEach {
      it.offenderNo = toOffenderNo
      scheduleRepository.save(it)
    }
    movements.forEach {
      it.offenderNo = toOffenderNo
      movementRepository.save(it)
    }
  }
}
