package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.migration

import kotlinx.coroutines.flow.count
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.movement.TransferMovementMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.movement.TransferMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule.TransferMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule.TransferScheduleMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule.TransferScheduleRepository

@Service
class TransferSchedulerMigrationService(
  private val scheduleRepository: TransferScheduleRepository,
  private val movementRepository: TransferMovementRepository,
  private val migrationRepository: TransferSchedulerMigrationRepository,
) {

  @Transactional
  suspend fun createMigrationMappings(mappings: TransferSchedulerPrisonerMappingsDto) {
    deleteOldMappings(mappings.offenderNo)

    saveTransferScheduleMappings(mappings)
    saveScheduledTransferMovementMappings(mappings)
    saveUnscheduledTransferMovementMappings(mappings)

    migrationRepository.deleteById(mappings.offenderNo)
    migrationRepository.save(TransferSchedulerMigration(mappings.offenderNo, mappings.migrationId))
  }

  private suspend fun deleteOldMappings(offenderNo: String) {
    scheduleRepository.deleteByOffenderNo(offenderNo)
    movementRepository.deleteByOffenderNo(offenderNo)
  }

  private suspend fun saveTransferScheduleMappings(mappings: TransferSchedulerPrisonerMappingsDto) {
    mappings.bookings.flatMap { booking ->
      booking.schedules.map { schedule ->
        schedule.toEntity(mappings.offenderNo, booking.bookingId, mappings.migrationId)
      }
    }.also { scheduleRepository.saveAll(it).count() }
  }

  private suspend fun saveScheduledTransferMovementMappings(mappings: TransferSchedulerPrisonerMappingsDto) {
    mappings.bookings.flatMap { booking ->
      booking.schedules.mapNotNull { schedule ->
        schedule.movement?.toEntity(mappings.offenderNo, booking.bookingId, mappings.migrationId)
      }
    }.also { movementRepository.saveAll(it).count() }
  }

  private suspend fun saveUnscheduledTransferMovementMappings(mappings: TransferSchedulerPrisonerMappingsDto) {
    mappings.bookings.flatMap { booking ->
      booking.unscheduledMovements.map { movement ->
        movement.toEntity(mappings.offenderNo, booking.bookingId, mappings.migrationId)
      }
    }.also { movementRepository.saveAll(it).count() }
  }
}

private fun BookingTransferScheduleMappingsDto.toEntity(offenderNo: String, bookingId: Long, migrationId: String) = TransferScheduleMapping(
  dpsTransferScheduleId = this.dpsTransferScheduleId,
  nomisEventId = this.nomisEventId,
  offenderNo = offenderNo,
  bookingId = bookingId,
  label = migrationId,
  mappingType = TransferMappingType.MIGRATED,
)

private fun BookingTransferMovementMappingsDto.toEntity(offenderNo: String, bookingId: Long, migrationId: String) = TransferMovementMapping(
  dpsTransferMovementId = this.dpsTransferMovementId,
  nomisBookingId = bookingId,
  nomisMovementSeq = this.nomisMovementSeq,
  offenderNo = offenderNo,
  label = migrationId,
  mappingType = TransferMappingType.MIGRATED,
)
