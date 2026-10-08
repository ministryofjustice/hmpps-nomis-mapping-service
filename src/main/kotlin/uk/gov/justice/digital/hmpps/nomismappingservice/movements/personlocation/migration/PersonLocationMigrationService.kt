package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.migration

import kotlinx.coroutines.flow.count
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.PersonLocationMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.booking.PersonLocationBookingMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.booking.PersonLocationBookingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.movement.PersonLocationMovementMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.movement.PersonLocationMovementRepository

@Service
class PersonLocationMigrationService(
  private val bookingRepository: PersonLocationBookingRepository,
  private val movementRepository: PersonLocationMovementRepository,
  private val migrationRepository: PersonLocationMigrationRepository,
) {

  @Transactional
  suspend fun createMigrationMappings(mappings: PersonLocationsMappingDto) {
    deleteOldMappings(mappings.bookings.map { it.bookingId })

    saveBookingMappings(mappings)
    saveMovementMappings(mappings)

    migrationRepository.deleteById(mappings.offenderNo)
    migrationRepository.save(PersonLocationMigration(mappings.offenderNo, mappings.migrationId))
  }

  private suspend fun deleteOldMappings(bookingIds: List<Long>) {
    bookingRepository.deleteByNomisBookingIdIn(bookingIds)
    movementRepository.deleteByNomisBookingIdIn(bookingIds)
  }

  private suspend fun saveBookingMappings(mappings: PersonLocationsMappingDto) {
    mappings.bookings
      .map { it.toEntity(mappings.migrationId) }
      .also { bookingRepository.saveAll(it).count() }
  }

  private suspend fun saveMovementMappings(mappings: PersonLocationsMappingDto) {
    mappings.bookings.flatMap { booking ->
      booking.movements.map { movement ->
        movement.toEntity(booking.bookingId, mappings.migrationId)
      }
    }.also { movementRepository.saveAll(it).count() }
  }
}

private fun PersonLocationBookingMappingDto.toEntity(migrationId: String) = PersonLocationBookingMapping(
  dpsCustodialSeriesId = this.dpsCustodialSeriesId,
  nomisBookingId = this.bookingId,
  label = migrationId,
  mappingType = PersonLocationMappingType.MIGRATED,
)

private fun PersonLocationMovementMappingDto.toEntity(bookingId: Long, migrationId: String) = PersonLocationMovementMapping(
  dpsExternalMovementId = this.dpsExternalMovementId,
  nomisBookingId = bookingId,
  nomisMovementSeq = this.nomisMovementSeq,
  label = migrationId,
  mappingType = PersonLocationMappingType.MIGRATED,
)
