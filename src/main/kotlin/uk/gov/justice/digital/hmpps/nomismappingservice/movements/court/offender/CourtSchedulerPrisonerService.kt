package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.offender

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.movement.CourtMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule.CourtScheduleRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import kotlin.collections.plus
import kotlin.collections.toSet

@Service
class CourtSchedulerPrisonerService(
  private val scheduleRepository: CourtScheduleRepository,
  private val movementRepository: CourtMovementRepository,
) {

  suspend fun getAllMappingIds(prisonerNumber: String) = CourtSchedulerPrisonerMappingIdsDto(
    prisonerNumber = prisonerNumber,
    schedules = scheduleRepository.findByOffenderNo(prisonerNumber)
      .map { CourtScheduleMappingIdsDto(it.nomisEventId, it.dpsCourtAppearanceId) },
    movements = movementRepository.findByOffenderNo(prisonerNumber)
      .map { CourtMovementMappingIdsDto(it.nomisBookingId, it.nomisMovementSeq, it.dpsCourtMovementId) },
  )

  suspend fun getMappingsForMoveBooking(bookingId: Long): CourtSchedulerMoveBookingMappingDto {
    val courtAppearances = scheduleRepository.findByBookingId(bookingId)
    val movements = movementRepository.findByNomisBookingId(bookingId)
    return CourtSchedulerMoveBookingMappingDto(
      scheduleIds = courtAppearances.map {
        CourtScheduleIdMapping(
          it.nomisEventId,
          it.dpsCourtAppearanceId,
        )
      },
      movementIds = movements.map { CourtMovementIdMapping(it.nomisMovementSeq, it.dpsCourtMovementId) },
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
