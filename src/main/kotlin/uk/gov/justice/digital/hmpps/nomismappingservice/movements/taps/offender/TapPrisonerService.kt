package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application.TapApplicationRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.movement.TapMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.schedule.TapScheduleRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
class TapPrisonerService(
  private val applicationRepository: TapApplicationRepository,
  private val scheduleRepository: TapScheduleRepository,
  private val movementRepository: TapMovementRepository,
) {

  suspend fun getAllMappingIds(prisonerNumber: String) = TapPrisonerMappingIdsDto(
    prisonerNumber = prisonerNumber,
    applications = applicationRepository.findByOffenderNo(prisonerNumber)
      .map { TapApplicationMappingIdsDto(it.nomisApplicationId, it.dpsAuthorisationId) },
    schedules = scheduleRepository.findByOffenderNo(prisonerNumber)
      .map { TapScheduleMappingIdsDto(it.nomisEventId, it.dpsOccurrenceId) },
    movements = movementRepository.findByOffenderNo(prisonerNumber)
      .map { TapMovementMappingIdsDto(it.nomisBookingId, it.nomisMovementSeq, it.dpsMovementId) },
  )

  suspend fun getMappingsForMoveBooking(bookingId: Long): TapMoveBookingMappingDto {
    val applications = applicationRepository.findByBookingId(bookingId)
    val movements = movementRepository.findByNomisBookingId(bookingId)
    return TapMoveBookingMappingDto(
      applicationIds = applications.map {
        TapApplicationIdMapping(
          it.nomisApplicationId,
          it.dpsAuthorisationId,
        )
      },
      movementIds = movements.map { TapMovementIdMapping(it.nomisMovementSeq, it.dpsMovementId) },
    )
  }

  @Transactional
  suspend fun moveMappingsForBooking(bookingId: Long, fromOffenderNo: String, toOffenderNo: String) {
    val applications = applicationRepository.findByBookingId(bookingId)
    val schedules = scheduleRepository.findByBookingId(bookingId)
    val movements = movementRepository.findByNomisBookingId(bookingId)
    val bookingOffenders = (applications.map { it.offenderNo } + schedules.map { it.offenderNo } + movements.map { it.offenderNo }).toSet()

    // If we don't hold the booking ID then return not found as we probably shouldn't have made this request
    if (applications.isEmpty() && schedules.isEmpty() && movements.isEmpty()) {
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
    applications.forEach {
      it.offenderNo = toOffenderNo
      applicationRepository.save(it)
    }
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
