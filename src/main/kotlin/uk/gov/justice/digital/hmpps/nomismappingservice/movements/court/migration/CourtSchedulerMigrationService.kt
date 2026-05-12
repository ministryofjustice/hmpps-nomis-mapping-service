package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.migration

import kotlinx.coroutines.flow.count
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.movement.CourtMovementMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.movement.CourtMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule.CourtMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule.CourtScheduleMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule.CourtScheduleRepository

@Service
class CourtSchedulerMigrationService(
  private val scheduleRepository: CourtScheduleRepository,
  private val movementRepository: CourtMovementRepository,
  private val migrationRepository: CourtSchedulerMigrationRepository,
) {

  suspend fun createMigrationMappings(mappings: CourtSchedulerPrisonerMappingsDto) {
    deleteOldMappings(mappings.offenderNo)

    saveCourtScheduleMappings(mappings)
    saveScheduledCourtMovementMappings(mappings)
    saveUnscheduledCourtMovementMappings(mappings)

    migrationRepository.deleteById(mappings.offenderNo)
    migrationRepository.save(CourtSchedulerMigration(mappings.offenderNo, mappings.migrationId))
  }

  private suspend fun saveUnscheduledCourtMovementMappings(mappings: CourtSchedulerPrisonerMappingsDto) {
    mappings.bookings.flatMap { booking ->
      booking.unscheduledMovements.map { movement ->
        movement.toEntity(mappings.offenderNo, booking.bookingId, mappings.migrationId)
      }
    }.also { movementRepository.saveAll(it).count() }
  }

  private suspend fun saveScheduledCourtMovementMappings(mappings: CourtSchedulerPrisonerMappingsDto) {
    mappings.bookings.flatMap { booking ->
      booking.courtSchedules.flatMap { schedule ->
        schedule.movements.map { movement ->
          movement.toEntity(mappings.offenderNo, booking.bookingId, mappings.migrationId)
        }
      }
    }.also { movementRepository.saveAll(it).count() }
  }

  private suspend fun saveCourtScheduleMappings(mappings: CourtSchedulerPrisonerMappingsDto) {
    mappings.bookings.flatMap { booking ->
      booking.courtSchedules.map { schedule ->
        schedule.toEntity(mappings.offenderNo, booking.bookingId, mappings.migrationId)
      }
    }.also { scheduleRepository.saveAll(it).count() }
  }

  private suspend fun deleteOldMappings(offenderNo: String) {
    scheduleRepository.deleteByOffenderNo(offenderNo)
    movementRepository.deleteByOffenderNo(offenderNo)
  }
}

private fun BookingCourtScheduleMappingsDto.toEntity(prisonerNumber: String, bookingId: Long, migrationId: String) = CourtScheduleMapping(
  dpsCourtAppearanceId = this.dpsCourtAppearanceId,
  nomisEventId = this.nomisEventId,
  offenderNo = prisonerNumber,
  bookingId = bookingId,
  label = migrationId,
  mappingType = CourtMappingType.MIGRATED,
)

private fun BookingCourtMovementMappingsDto.toEntity(prisonerNumber: String, bookingId: Long, migrationId: String) = CourtMovementMapping(
  dpsCourtMovementId = this.dpsCourtMovementId,
  nomisBookingId = bookingId,
  nomisMovementSeq = this.nomisMovementSeq,
  offenderNo = prisonerNumber,
  label = migrationId,
  mappingType = CourtMappingType.MIGRATED,
)
