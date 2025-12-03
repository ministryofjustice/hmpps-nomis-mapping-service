package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class TemporaryAbsenceService(
  private val applicationRepository: TemporaryAbsenceApplicationRepository,
  private val appMultiRepository: TemporaryAbsenceAppMultiRepository,
  private val scheduleRepository: TemporaryAbsenceScheduleRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
  private val migrationRepository: TemporaryAbsenceMigrationRepository,
  private val addressRepository: TemporaryAbsenceAddressRepository,
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
              schedule.dpsOccurrenceId,
              schedule.nomisEventId,
              mappings.prisonerNumber,
              booking.bookingId,
              schedule.nomisAddressId,
              schedule.nomisAddressOwnerClass,
              schedule.dpsAddressText,
              null,
              schedule.eventTime,
              mappings.migrationId,
              MovementMappingType.MIGRATED,
            ),
          )
          if (schedule.nomisAddressOwnerClass != null && schedule.nomisAddressId != null) {
            upsertMigrationAddressMapping(schedule.nomisAddressOwnerClass, schedule.nomisAddressId, mappings.prisonerNumber, schedule.dpsAddressText)
          }
        }
        application.movements.forEach { movement ->
          movementRepository.save(
            TemporaryAbsenceMovementMapping(
              movement.dpsMovementId,
              booking.bookingId,
              movement.nomisMovementSeq,
              mappings.prisonerNumber,
              movement.nomisAddressId,
              movement.nomisAddressOwnerClass,
              movement.dpsAddressText,
              mappings.migrationId,
              null,
              MovementMappingType.MIGRATED,
            ),
          )
          if (movement.nomisAddressOwnerClass != null && movement.nomisAddressId != null) {
            upsertMigrationAddressMapping(movement.nomisAddressOwnerClass, movement.nomisAddressId, mappings.prisonerNumber, movement.dpsAddressText)
          }
        }
      }
      booking.unscheduledMovements.forEach { unscheduledMovement ->
        movementRepository.save(
          TemporaryAbsenceMovementMapping(
            unscheduledMovement.dpsMovementId,
            booking.bookingId,
            unscheduledMovement.nomisMovementSeq,
            mappings.prisonerNumber,
            unscheduledMovement.nomisAddressId,
            unscheduledMovement.nomisAddressOwnerClass,
            unscheduledMovement.dpsAddressText,
            mappings.migrationId,
            null,
            MovementMappingType.MIGRATED,
          ),
        )
        if (unscheduledMovement.nomisAddressOwnerClass != null && unscheduledMovement.nomisAddressId != null) {
          upsertMigrationAddressMapping(unscheduledMovement.nomisAddressOwnerClass, unscheduledMovement.nomisAddressId, mappings.prisonerNumber, unscheduledMovement.dpsAddressText)
        }
      }
    }
  }

  @Transactional
  suspend fun upsertMigrationAddressMapping(nomisOwnerClass: String, nomisAddressId: Long, nomisOffenderNo: String, dpsAddressText: String) {
    val offenderNo = if (nomisOwnerClass == "OFF") nomisOffenderNo else null
    addressRepository.findByNomisAddressIdAndNomisAddressOwnerClassAndNomisOffenderNo(nomisAddressId, nomisOwnerClass, offenderNo)
      ?.also {
        it.dpsAddressText = dpsAddressText
        addressRepository.save(it)
      }
      ?: addressRepository.save(
        TemporaryAbsenceAddressMapping(
          nomisAddressId = nomisAddressId,
          nomisAddressOwnerClass = nomisOwnerClass,
          nomisOffenderNo = offenderNo,
          dpsAddressText = dpsAddressText,
        ),
      )
  }

  @Transactional
  suspend fun createApplicationMapping(mappingDto: TemporaryAbsenceApplicationSyncMappingDto) = applicationRepository.save(mappingDto.toMapping()).toMappingDto()

  suspend fun getApplicationMappingByNomisId(nomisApplicationId: Long) = applicationRepository.findByNomisApplicationId(nomisApplicationId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS application id $nomisApplicationId not found")

  suspend fun getApplicationMappingByDpsId(dpsApplicationId: UUID) = applicationRepository.findById(dpsApplicationId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS application id $dpsApplicationId not found")

  @Transactional
  suspend fun deleteApplicationMappingByNomisId(nomisApplicationId: Long) = applicationRepository.deleteByNomisApplicationId(nomisApplicationId)

  @Transactional
  suspend fun createOutsideMovementMapping(mappingDto: TemporaryAbsenceOutsideMovementSyncMappingDto) = appMultiRepository.save(mappingDto.toMapping()).toMappingDto()

  suspend fun getOutsideMovementMappingByNomisId(nomisAppMultiId: Long) = appMultiRepository.findByNomisAppMultiId(nomisAppMultiId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS application multi id $nomisAppMultiId not found")

  suspend fun getOutsideMovementMappingByDpsId(dpsOutsideMovementId: UUID) = appMultiRepository.findById(dpsOutsideMovementId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS outside movement id $dpsOutsideMovementId not found")

  @Transactional
  suspend fun deleteOutsideMovementMappingByNomisId(nomisAppMultiId: Long) = appMultiRepository.deleteByNomisAppMultiId(nomisAppMultiId)

  @Transactional
  suspend fun createScheduledMovementMapping(mappingDto: ScheduledMovementSyncMappingDto) = scheduleRepository.save(mappingDto.toMapping()).toMappingDto()

  @Transactional
  suspend fun updateScheduledMovementMapping(mappingDto: ScheduledMovementSyncMappingDto) = scheduleRepository.findById(mappingDto.dpsOccurrenceId)
    ?.let {
      it.nomisAddressId = mappingDto.nomisAddressId
      it.nomisAddressOwnerClass = mappingDto.nomisAddressOwnerClass
      it.dpsAddressText = mappingDto.dpsAddressText
      it.eventTime = mappingDto.eventTime
      scheduleRepository.save(it).toMappingDto()
    }
    ?: throw NotFoundException("Mapping for DPS occurrence id ${mappingDto.dpsOccurrenceId} not found")

  suspend fun getScheduledMovementMappingByNomisId(nomisEventId: Long) = scheduleRepository.findByNomisEventId(nomisEventId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS event id $nomisEventId not found")

  suspend fun getScheduledMovementMappingByDpsId(dpsScheduledMovementId: UUID) = scheduleRepository.findById(dpsScheduledMovementId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS scheduled movement id $dpsScheduledMovementId not found")

  @Transactional
  suspend fun deleteScheduledMovementMappingByNomisId(nomisEventId: Long) = scheduleRepository.deleteByNomisEventId(nomisEventId)

  @Transactional
  suspend fun createExternalMovementMapping(mappingDto: ExternalMovementSyncMappingDto) = movementRepository.save(mappingDto.toMapping()).toMappingDto()

  @Transactional
  suspend fun updateExternalMovementMapping(mappingDto: ExternalMovementSyncMappingDto) = movementRepository.findById(mappingDto.dpsMovementId)
    ?.let {
      it.nomisAddressId = mappingDto.nomisAddressId
      it.nomisAddressOwnerClass = mappingDto.nomisAddressOwnerClass
      it.dpsAddressText = mappingDto.dpsAddressText
      movementRepository.save(it).toMappingDto()
    }
    ?: throw NotFoundException("Mapping for DPS movement id ${mappingDto.dpsMovementId} not found")

  suspend fun getExternalMovementMappingByNomisId(bookingId: Long, movementSeq: Int) = movementRepository.findByNomisBookingIdAndNomisMovementSeq(bookingId, movementSeq)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS booking id $bookingId and movement sequence $movementSeq not found")

  suspend fun getExternalMovementMappingByDpsId(dpsExternalMovementId: UUID) = movementRepository.findById(dpsExternalMovementId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS external movement id $dpsExternalMovementId not found")

  @Transactional
  suspend fun deleteExternalMovementMappingByNomisId(bookingId: Long, movementSeq: Int) = movementRepository.deleteByNomisBookingIdAndNomisMovementSeq(bookingId, movementSeq)

  suspend fun findScheduledMovementsByNomisAddressId(nomisAddressId: Long, fromDate: LocalDate) = scheduleRepository
    .findByNomisAddressIdAndEventTimeIsGreaterThanEqual(nomisAddressId, fromDate.atStartOfDay())
    .map { it.toMappingDto() }
    .let { FindScheduledMovementsForAddressResponse(it) }

  suspend fun findAddress(request: FindTemporaryAbsenceAddressByDpsIdRequest): TemporaryAbsenceAddressMappingResponse = with(request) {
    when (ownerClass) {
      "OFF" -> addressRepository.findByNomisOffenderNoAndDpsUprnAndDpsAddressText(offenderNo, dpsUprn, dpsAddressText)
      else -> addressRepository.findByNomisAddressOwnerClassAndDpsUprnAndDpsAddressText(ownerClass, dpsUprn, dpsAddressText)
    }
      ?.toMappingDto()
      ?: throw NotFoundException("No address found for address owner class $ownerClass and offender $offenderNo with dpsUprn $dpsUprn and dpsAddressText $dpsAddressText")
  }
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
  dpsOccurrenceId,
  nomisEventId,
  prisonerNumber,
  bookingId,
  nomisAddressId,
  nomisAddressOwnerClass,
  dpsAddressText,
  dpsUprn,
  eventTime,
  mappingType = mappingType,
)

fun TemporaryAbsenceScheduleMapping.toMappingDto(): ScheduledMovementSyncMappingDto = ScheduledMovementSyncMappingDto(
  offenderNo,
  bookingId,
  nomisEventId,
  dpsOccurrenceId,
  mappingType = mappingType,
  nomisAddressId,
  nomisAddressOwnerClass,
  dpsAddressText,
  dpsUprn,
  eventTime,
)

fun ExternalMovementSyncMappingDto.toMapping(): TemporaryAbsenceMovementMapping = TemporaryAbsenceMovementMapping(
  dpsMovementId,
  bookingId,
  nomisMovementSeq,
  prisonerNumber,
  nomisAddressId,
  nomisAddressOwnerClass,
  dpsAddressText,
  dpsUprn = dpsUprn,
  mappingType = mappingType,
)

fun TemporaryAbsenceMovementMapping.toMappingDto(): ExternalMovementSyncMappingDto = ExternalMovementSyncMappingDto(
  offenderNo,
  nomisBookingId,
  nomisMovementSeq,
  dpsMovementId,
  mappingType = mappingType,
  nomisAddressId,
  nomisAddressOwnerClass,
  dpsAddressText,
  dpsUprn,
)

fun TemporaryAbsenceAddressMapping.toMappingDto() = TemporaryAbsenceAddressMappingResponse(
  nomisOffenderNo,
  nomisAddressOwnerClass,
  nomisAddressId,
  dpsUprn,
  dpsAddressText,
)
