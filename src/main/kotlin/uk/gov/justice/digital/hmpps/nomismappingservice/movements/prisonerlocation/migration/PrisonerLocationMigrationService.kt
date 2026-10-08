package uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.migration

import kotlinx.coroutines.flow.count
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.PrisonerLocationMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.booking.PrisonerLocationBookingMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.booking.PrisonerLocationBookingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.movement.PrisonerLocationMovementMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.movement.PrisonerLocationMovementRepository

@Service
class PrisonerLocationMigrationService(
  private val bookingRepository: PrisonerLocationBookingRepository,
  private val movementRepository: PrisonerLocationMovementRepository,
  private val migrationRepository: PrisonerLocationMigrationRepository,
) {

  @Transactional
  suspend fun createMigrationMappings(mappings: PrisonerLocationsMappingDto) {
    deleteOldMappings(mappings.bookings.map { it.bookingId })

    saveBookingMappings(mappings)
    saveMovementMappings(mappings)

    migrationRepository.deleteById(mappings.offenderNo)
    migrationRepository.save(PrisonerLocationMigration(mappings.offenderNo, mappings.migrationId))
  }

  private suspend fun deleteOldMappings(bookingIds: List<Long>) {
    bookingRepository.deleteByNomisBookingIdIn(bookingIds)
    movementRepository.deleteByNomisBookingIdIn(bookingIds)
  }

  private suspend fun saveBookingMappings(mappings: PrisonerLocationsMappingDto) {
    mappings.bookings
      .map { it.toEntity(mappings.migrationId) }
      .also { bookingRepository.saveAll(it).count() }
  }

  private suspend fun saveMovementMappings(mappings: PrisonerLocationsMappingDto) {
    mappings.bookings.flatMap { booking ->
      booking.movements.map { movement ->
        movement.toEntity(booking.bookingId, mappings.migrationId)
      }
    }.also { movementRepository.saveAll(it).count() }
  }
}

private fun PrisonerLocationBookingMappingDto.toEntity(migrationId: String) = PrisonerLocationBookingMapping(
  dpsCustodialSeriesId = this.dpsCustodialSeriesId,
  nomisBookingId = this.bookingId,
  label = migrationId,
  mappingType = PrisonerLocationMappingType.MIGRATED,
)

private fun PrisonerLocationMovementMappingDto.toEntity(bookingId: Long, migrationId: String) = PrisonerLocationMovementMapping(
  dpsExternalMovementId = this.dpsExternalMovementId,
  nomisBookingId = bookingId,
  nomisMovementSeq = this.nomisMovementSeq,
  label = migrationId,
  mappingType = PrisonerLocationMappingType.MIGRATED,
)
