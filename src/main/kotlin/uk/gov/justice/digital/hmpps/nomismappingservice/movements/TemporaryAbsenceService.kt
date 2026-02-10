package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.time.LocalDate
import java.util.*

@Service
@Transactional(readOnly = true)
class TemporaryAbsenceService(
  private val applicationRepository: TemporaryAbsenceApplicationRepository,
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

  // TODO - this was supposed to perform batch inserts which are needed for performance reasons. Unfortunately Spring R2DBC doesn't support batch inserts/updates so it's not any quicker. We'll have to roll our own with something like this: https://blog.davidvassallo.me/2022/03/01/spring-boot-r2dbc-insert-batching-reactive-sql/.
  @Transactional
  suspend fun createMappings(mappings: TemporaryAbsencesPrisonerMappingDto) {
    // Clear down old mappings
    applicationRepository.deleteByOffenderNo(mappings.prisonerNumber)
    scheduleRepository.deleteByOffenderNo(mappings.prisonerNumber)
    movementRepository.deleteByOffenderNo(mappings.prisonerNumber)

    // Save all application mappings
    mappings.bookings.flatMap { booking ->
      booking.applications.map { application ->
        application.toEntity(mappings.prisonerNumber, booking.bookingId, mappings.migrationId)
      }
    }.also { applicationRepository.saveAll(it).count() }

    // save all schedule mappings
    mappings.bookings.flatMap { booking ->
      booking.applications.flatMap { application ->
        application.schedules.map { schedule ->
          schedule.toEntity(mappings.prisonerNumber, booking.bookingId, mappings.migrationId)
        }
      }
    }.also { scheduleRepository.saveAll(it).count() }

    // save all scheduled movement mappings
    mappings.bookings.flatMap { booking ->
      booking.applications.flatMap { application ->
        application.movements.map { movement ->
          movement.toEntity(mappings.prisonerNumber, booking.bookingId, mappings.migrationId)
        }
      }
    }.also { movementRepository.saveAll(it).count() }

    // save all unscheduled movement mappings
    mappings.bookings.flatMap { booking ->
      booking.unscheduledMovements.map { movement ->
        movement.toEntity(mappings.prisonerNumber, booking.bookingId, mappings.migrationId)
      }
    }.also { movementRepository.saveAll(it).count() }
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
  suspend fun createScheduledMovementMapping(mappingDto: ScheduledMovementSyncMappingDto) = scheduleRepository.save(mappingDto.toMapping())
    .toMappingDto()

  @Transactional
  suspend fun updateScheduledMovementMapping(mappingDto: ScheduledMovementSyncMappingDto, source: String) = scheduleRepository.findById(mappingDto.dpsOccurrenceId)
    ?.let {
      it.nomisAddressId = mappingDto.nomisAddressId
      it.nomisAddressOwnerClass = mappingDto.nomisAddressOwnerClass
      it.dpsUprn = mappingDto.dpsUprn
      it.dpsAddressText = mappingDto.dpsAddressText
      it.dpsDescription = mappingDto.dpsDescription
      it.dpsPostcode = mappingDto.dpsPostcode
      it.eventTime = mappingDto.eventTime
      scheduleRepository.save(it)
    }
    ?.toMappingDto()
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
      it.dpsDescription = mappingDto.dpsDescription
      it.dpsPostcode = mappingDto.dpsPostcode
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

  suspend fun getCountByMigrationId(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<TemporaryAbsenceMigrationDto> = coroutineScope {
    val mappings = async {
      migrationRepository.findAllByLabelOrderByLabelDesc(
        label = migrationId,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      migrationRepository.countAllByLabel(
        migrationId = migrationId,
      )
    }

    PageImpl(
      mappings.await().toList().map { it.toDto() },
      pageRequest,
      count.await(),
    )
  }

  suspend fun getMappingsForMoveBooking(bookingId: Long): TemporaryAbsenceMoveBookingMappingDto {
    val applications = applicationRepository.findByBookingId(bookingId)
    val movements = movementRepository.findByNomisBookingId(bookingId)
    return TemporaryAbsenceMoveBookingMappingDto(
      applicationIds = applications.map { TemporaryAbsenceApplicationIdMapping(it.nomisApplicationId, it.dpsApplicationId) },
      movementIds = movements.map { TemporaryAbsenceMovementIdMapping(it.nomisMovementSeq, it.dpsMovementId) },
    )
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

fun ScheduledMovementSyncMappingDto.toMapping(): TemporaryAbsenceScheduleMapping = TemporaryAbsenceScheduleMapping(
  dpsOccurrenceId,
  nomisEventId,
  prisonerNumber,
  bookingId,
  nomisAddressId,
  nomisAddressOwnerClass,
  dpsAddressText,
  dpsUprn,
  dpsDescription,
  dpsPostcode,
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
  dpsDescription,
  dpsPostcode,
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
  dpsDescription = dpsDescription,
  dpsPostcode = dpsPostcode,
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
  dpsDescription,
  dpsPostcode,
  dpsUprn,
)

fun TemporaryAbsenceMigration.toDto() = TemporaryAbsenceMigrationDto(offenderNo, label, whenCreated)

private fun TemporaryAbsenceApplicationMappingDto.toEntity(offenderNo: String, bookingId: Long, migrationId: String) = TemporaryAbsenceApplicationMapping(
  dpsApplicationId = dpsMovementApplicationId,
  nomisApplicationId = nomisMovementApplicationId,
  offenderNo = offenderNo,
  bookingId = bookingId,
  label = migrationId,
  mappingType = MovementMappingType.MIGRATED,
)

private fun ScheduledMovementMappingDto.toEntity(offenderNo: String, bookingId: Long, migrationId: String) = TemporaryAbsenceScheduleMapping(
  dpsOccurrenceId = dpsOccurrenceId,
  nomisEventId = nomisEventId,
  offenderNo = offenderNo,
  bookingId = bookingId,
  nomisAddressId = nomisAddressId,
  nomisAddressOwnerClass = nomisAddressOwnerClass,
  dpsAddressText = dpsAddressText,
  dpsUprn = null,
  dpsDescription = dpsDescription,
  dpsPostcode = dpsPostcode,
  eventTime = eventTime,
  label = migrationId,
  mappingType = MovementMappingType.MIGRATED,
)

private fun ExternalMovementMappingDto.toEntity(offenderNo: String, bookingId: Long, migrationId: String) = TemporaryAbsenceMovementMapping(
  dpsMovementId = dpsMovementId,
  nomisBookingId = bookingId,
  nomisMovementSeq = nomisMovementSeq,
  offenderNo = offenderNo,
  nomisAddressId = nomisAddressId,
  nomisAddressOwnerClass = nomisAddressOwnerClass,
  dpsAddressText = dpsAddressText,
  label = migrationId,
  dpsUprn = null,
  dpsDescription = dpsDescription,
  dpsPostcode = dpsPostcode,
  mappingType = MovementMappingType.MIGRATED,
)
