package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.movements

import org.springframework.stereotype.Service

@Service
class TemporaryAbsenceService(
  private val applicationRepository: TemporaryAbsenceApplicationRepository,
  private val appMultiRepository: TemporaryAbsenceAppMultiRepository,
  private val scheduleRepository: TemporaryAbsenceScheduleRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
) {

  suspend fun createMappings(mappings: TemporaryAbsencesPrisonerMappingDto) {
    mappings.bookings.forEach { booking ->
      booking.applications.forEach { application ->
        applicationRepository.save(
          TemporaryAbsenceApplicationMapping(
            application.dpsMovementApplicationId,
            application.nomisMovementApplicationId,
            mappings.prisonerNumber,
            booking.bookingId,
            mappings.migrationId,
            MovementMappingType.MIGRATED,
          ),
        )
        application.outsideMovements.forEach { outside ->
          appMultiRepository.save(
            TemporaryAbsenceAppMultiMapping(
              outside.dpsOutsideMovementId,
              outside.nomisMovementApplicationMultiId,
              mappings.prisonerNumber,
              booking.bookingId,
              mappings.migrationId,
              MovementMappingType.MIGRATED,
            ),
          )
        }
        application.schedules.forEach { schedule ->
          scheduleRepository.save(
            TemporaryAbsenceScheduleMapping(
              schedule.dpsScheduledMovementId,
              schedule.nomisEventId,
              mappings.prisonerNumber,
              booking.bookingId,
              mappings.migrationId,
              MovementMappingType.MIGRATED,
            ),
          )
        }
        application.movements.forEach { movement ->
          movementRepository.save(
            TemporaryAbsenceMovementMapping(
              movement.dpsExternalMovementId,
              booking.bookingId,
              movement.nomisMovementSeq,
              mappings.prisonerNumber,
              mappings.migrationId,
              MovementMappingType.MIGRATED,
            ),
          )
        }
      }
      booking.unscheduledMovements.forEach { unscheduledMovement ->
        movementRepository.save(
          TemporaryAbsenceMovementMapping(
            unscheduledMovement.dpsExternalMovementId,
            booking.bookingId,
            unscheduledMovement.nomisMovementSeq,
            mappings.prisonerNumber,
            mappings.migrationId,
            MovementMappingType.MIGRATED,
          ),
        )
      }
    }
  }
}
