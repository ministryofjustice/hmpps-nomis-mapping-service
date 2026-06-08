package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.offender

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.movement.CourtMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule.CourtScheduleRepository

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
}
