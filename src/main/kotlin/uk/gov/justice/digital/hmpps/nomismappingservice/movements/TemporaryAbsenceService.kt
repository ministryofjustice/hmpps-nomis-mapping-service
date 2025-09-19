package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
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

  suspend fun deleteApplicationMappingByNomisId(nomisApplicationId: Long) = applicationRepository.deleteByNomisApplicationId(nomisApplicationId)

  suspend fun createOutsideMovementMapping(mappingDto: TemporaryAbsenceOutsideMovementSyncMappingDto) = appMultiRepository.save(mappingDto.toMapping())

  suspend fun getOutsideMovementMappingByNomisId(nomisAppMultiId: Long) = appMultiRepository.findByNomisAppMultiId(nomisAppMultiId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS application multi id $nomisAppMultiId not found")

  suspend fun getOutsideMovementMappingByDpsId(dpsOutsideMovementId: UUID) = appMultiRepository.findById(dpsOutsideMovementId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS outside movement id $dpsOutsideMovementId not found")

  suspend fun deleteOutsideMovementMappingByNomisId(nomisAppMultiId: Long) = appMultiRepository.deleteByNomisAppMultiId(nomisAppMultiId)

  suspend fun createScheduledMovementMapping(mappingDto: ScheduledMovementSyncMappingDto) = scheduleRepository.save(mappingDto.toMapping())

  suspend fun getScheduledMovementMappingByNomisId(nomisEventId: Long) = scheduleRepository.findByNomisScheduleId(nomisEventId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS event id $nomisEventId not found")

  suspend fun getScheduledMovementMappingByDpsId(dpsScheduledMovementId: UUID) = scheduleRepository.findById(dpsScheduledMovementId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS scheduled movement id $dpsScheduledMovementId not found")

  suspend fun deleteScheduledMovementMappingByNomisId(nomisEventId: Long) = scheduleRepository.deleteByNomisScheduleId(nomisEventId)

  suspend fun createExternalMovementMapping(mappingDto: ExternalMovementSyncMappingDto) = movementRepository.save(mappingDto.toMapping())

  suspend fun getExternalMovementMappingByNomisId(bookingId: Long, movementSeq: Int) = movementRepository.findByNomisBookingIdAndNomisMovementSeq(bookingId, movementSeq)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS booking id $bookingId and movement sequence $movementSeq not found")

  suspend fun getExternalMovementMappingByDpsId(dpsExternalMovementId: UUID) = movementRepository.findById(dpsExternalMovementId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS external movement id $dpsExternalMovementId not found")

  suspend fun deleteExternalMovementMappingByNomisId(bookingId: Long, movementSeq: Int) = movementRepository.deleteByNomisBookingIdAndNomisMovementSeq(bookingId, movementSeq)
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

fun TemporaryAbsenceOutsideMovementSyncMappingDto.toMapping(): TemporaryAbsenceAppMultiMapping = TemporaryAbsenceAppMultiMapping(
  dpsOutsideMovementId,
  nomisMovementApplicationMultiId,
  prisonerNumber,
  bookingId,
  mappingType = mappingType,
)

fun TemporaryAbsenceAppMultiMapping.toMappingDto(): TemporaryAbsenceOutsideMovementSyncMappingDto = TemporaryAbsenceOutsideMovementSyncMappingDto(
  offenderNo,
  bookingId,
  nomisAppMultiId,
  dpsAppMultiId,
  mappingType = mappingType,
)

fun ScheduledMovementSyncMappingDto.toMapping(): TemporaryAbsenceScheduleMapping = TemporaryAbsenceScheduleMapping(
  dpsScheduledMovementId,
  nomisEventId,
  prisonerNumber,
  bookingId,
  mappingType = mappingType,
)

fun TemporaryAbsenceScheduleMapping.toMappingDto(): ScheduledMovementSyncMappingDto = ScheduledMovementSyncMappingDto(
  offenderNo,
  bookingId,
  nomisScheduleId,
  dpsScheduleId,
  mappingType = mappingType,
)

fun ExternalMovementSyncMappingDto.toMapping(): TemporaryAbsenceMovementMapping = TemporaryAbsenceMovementMapping(
  dpsExternalMovementId,
  bookingId,
  nomisMovementSeq,
  prisonerNumber,
  mappingType = mappingType,
)

fun TemporaryAbsenceMovementMapping.toMappingDto(): ExternalMovementSyncMappingDto = ExternalMovementSyncMappingDto(
  offenderNo,
  nomisBookingId,
  nomisMovementSeq,
  dpsMovementId,
  mappingType = mappingType,
)
