package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.movements

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException
import java.util.UUID

@Service
class TemporaryAbsenceService(
  private val applicationRepository: TemporaryAbsenceApplicationRepository,
  private val appMultiRepository: TemporaryAbsenceAppMultiRepository,
  private val scheduleRepository: TemporaryAbsenceScheduleRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
  private val migrationRepository: TemporaryAbsenceMigrationRepository,
) {

  @Transactional
  suspend fun createMigrationMappings(mappings: TemporaryAbsencesPrisonerMappingDto) {
    createMappings(mappings).also {
      migrationRepository.deleteById(mappings.prisonerNumber)
      migrationRepository.save(TemporaryAbsenceMigration(mappings.prisonerNumber, mappings.migrationId))
    }
  }

  @Transactional
  suspend fun createMappings(mappings: TemporaryAbsencesPrisonerMappingDto) {
    applicationRepository.deleteByOffenderNo(mappings.prisonerNumber)
    appMultiRepository.deleteByOffenderNo(mappings.prisonerNumber)
    scheduleRepository.deleteByOffenderNo(mappings.prisonerNumber)
    movementRepository.deleteByOffenderNo(mappings.prisonerNumber)
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

  suspend fun createApplicationMapping(mappingDto: TemporaryAbsenceApplicationSyncMappingDto) = applicationRepository.save(mappingDto.toMapping())

  suspend fun getApplicationMappingByNomisId(nomisApplicationId: Long) = applicationRepository.findByNomisApplicationId(nomisApplicationId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS application id $nomisApplicationId not found")

  suspend fun getApplicationMappingByDpsId(dpsApplicationId: UUID) = applicationRepository.findById(dpsApplicationId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS application id $dpsApplicationId not found")
}

fun TemporaryAbsenceApplicationSyncMappingDto.toMapping(): TemporaryAbsenceApplicationMapping = TemporaryAbsenceApplicationMapping(
  dpsMovementApplicationId,
  nomisMovementApplicationId,
  prisonerNumber,
  bookingId,
  mappingType = mappingType,
)

fun TemporaryAbsenceApplicationMapping.toMappingDto(): TemporaryAbsenceApplicationSyncMappingDto = TemporaryAbsenceApplicationSyncMappingDto(
  offenderNo,
  bookingId,
  nomisApplicationId,
  dpsApplicationId,
  mappingType = mappingType,
)
