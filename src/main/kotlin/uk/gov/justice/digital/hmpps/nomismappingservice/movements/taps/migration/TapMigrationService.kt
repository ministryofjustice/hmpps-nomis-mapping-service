package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.migration

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application.MovementMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application.TapApplicationMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application.TapApplicationRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.movement.TapMovementMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.movement.TapMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapApplicationMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapMovementMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapPrisonerMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapScheduleMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.schedule.TapScheduleMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.schedule.TapScheduleRepository

@Service
class TapMigrationService(
  private val applicationRepository: TapApplicationRepository,
  private val scheduleRepository: TapScheduleRepository,
  private val movementRepository: TapMovementRepository,
  private val migrationRepository: TapMigrationRepository,
) {

  @Transactional
  suspend fun createMigrationMappings(mappings: TapPrisonerMappingsDto) {
    createMappings(mappings).also {
      migrationRepository.deleteById(mappings.prisonerNumber)
      migrationRepository.save(TapMigration(mappings.prisonerNumber, mappings.migrationId))
    }
  }

  @Transactional
  suspend fun createMappings(mappings: TapPrisonerMappingsDto) {
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

  suspend fun getCountByMigrationId(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<TapMigrationDto> = coroutineScope {
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

  fun TapMigration.toDto() = TapMigrationDto(offenderNo, label, whenCreated)
}

private fun TapApplicationMappingsDto.toEntity(offenderNo: String, bookingId: Long, migrationId: String) = TapApplicationMapping(
  dpsAuthorisationId = dpsAuthorisationId,
  nomisApplicationId = nomisApplicationId,
  offenderNo = offenderNo,
  bookingId = bookingId,
  label = migrationId,
  mappingType = MovementMappingType.MIGRATED,
)

private fun TapScheduleMappingsDto.toEntity(offenderNo: String, bookingId: Long, migrationId: String) = TapScheduleMapping(
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

private fun TapMovementMappingsDto.toEntity(offenderNo: String, bookingId: Long, migrationId: String) = TapMovementMapping(
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
