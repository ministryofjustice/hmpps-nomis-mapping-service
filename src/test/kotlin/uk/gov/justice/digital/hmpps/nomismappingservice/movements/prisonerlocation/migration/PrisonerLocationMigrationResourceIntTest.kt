@file:Suppress("ktlint:standard:property-naming")

package uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.migration

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.PrisonerLocationMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.booking.PrisonerLocationBookingMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.booking.PrisonerLocationBookingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.movement.PrisonerLocationMovementRepository
import java.util.*

class PrisonerLocationMigrationResourceIntTest(
  @Autowired private val bookingRepository: PrisonerLocationBookingRepository,
  @Autowired private val movementRepository: PrisonerLocationMovementRepository,
  @Autowired private val migrationRepository: PrisonerLocationMigrationRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("PUT /mapping/prisoner-location/migrate")
  inner class Migrate {
    private val MIGRATION_ID = "some_migration_id"
    private val NOMIS_OFFENDER_NO = "A1234BC"
    private val NOMIS_BOOKING_ID = 12345L
    private val DPS_CUSTODIAL_SERIES_ID = UUID.randomUUID()
    private val NOMIS_MOVEMENT_SEQ_1 = 1
    private val DPS_EXTERNAL_MOVEMENT_ID_1 = UUID.randomUUID()
    private val NOMIS_MOVEMENT_SEQ_2 = 2
    private val DPS_EXTERNAL_MOVEMENT_ID_2 = UUID.randomUUID()
    private val NOMIS_OLD_BOOKING_ID = 12344L
    private val DPS_OLD_CUSTODIAL_SERIES_ID = UUID.randomUUID()
    private val NOMIS_OLD_BOOKING_MOVEMENT_SEQ = 1
    private val DPS_OLD_BOOKING_EXTERNAL_MOVEMENT_ID = UUID.randomUUID()

    @BeforeEach
    fun clearDatabase() = runTest {
      movementRepository.deleteAll()
      bookingRepository.deleteAll()
      migrationRepository.deleteAll()
    }

    fun mappingsRequest() = PrisonerLocationsMappingDto(
      offenderNo = NOMIS_OFFENDER_NO,
      migrationId = MIGRATION_ID,
      bookings = listOf(
        PrisonerLocationBookingMappingDto(
          bookingId = NOMIS_BOOKING_ID,
          dpsCustodialSeriesId = DPS_CUSTODIAL_SERIES_ID,
          movements = listOf(
            PrisonerLocationMovementMappingDto(
              nomisMovementSeq = NOMIS_MOVEMENT_SEQ_1,
              dpsExternalMovementId = DPS_EXTERNAL_MOVEMENT_ID_1,
            ),
            PrisonerLocationMovementMappingDto(
              nomisMovementSeq = NOMIS_MOVEMENT_SEQ_2,
              dpsExternalMovementId = DPS_EXTERNAL_MOVEMENT_ID_2,
            ),
          ),
        ),
        PrisonerLocationBookingMappingDto(
          bookingId = NOMIS_OLD_BOOKING_ID,
          dpsCustodialSeriesId = DPS_OLD_CUSTODIAL_SERIES_ID,
          movements = listOf(
            PrisonerLocationMovementMappingDto(
              nomisMovementSeq = NOMIS_OLD_BOOKING_MOVEMENT_SEQ,
              dpsExternalMovementId = DPS_OLD_BOOKING_EXTERNAL_MOVEMENT_ID,
            ),
          ),
        ),
      ),
    )

    fun WebTestClient.saveMappings(mappings: PrisonerLocationsMappingDto = mappingsRequest()) {
      put()
        .uri("/mapping/prisoner-location/migrate")
        .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(mappings))
        .exchange()
        .expectStatus().isCreated
    }

    @Nested
    inner class HappyPath {
      @BeforeEach
      fun setUp() {
        webTestClient.saveMappings()
      }

      @Test
      fun `should save migration mapping`() = runTest {
        with(migrationRepository.findById(NOMIS_OFFENDER_NO)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
        }
      }

      @Test
      fun `should save booking mappings`() = runTest {
        with(bookingRepository.findById(DPS_CUSTODIAL_SERIES_ID)!!) {
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(mappingType).isEqualTo(PrisonerLocationMappingType.MIGRATED)
        }
        with(bookingRepository.findById(DPS_OLD_CUSTODIAL_SERIES_ID)!!) {
          assertThat(nomisBookingId).isEqualTo(NOMIS_OLD_BOOKING_ID)
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(mappingType).isEqualTo(PrisonerLocationMappingType.MIGRATED)
        }
      }

      @Test
      fun `should save movement mappings`() = runTest {
        with(movementRepository.findById(DPS_EXTERNAL_MOVEMENT_ID_1)!!) {
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_MOVEMENT_SEQ_1)
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(mappingType).isEqualTo(PrisonerLocationMappingType.MIGRATED)
        }
        with(movementRepository.findById(DPS_EXTERNAL_MOVEMENT_ID_2)!!) {
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_MOVEMENT_SEQ_2)
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(mappingType).isEqualTo(PrisonerLocationMappingType.MIGRATED)
        }
        with(movementRepository.findById(DPS_OLD_BOOKING_EXTERNAL_MOVEMENT_ID)!!) {
          assertThat(nomisBookingId).isEqualTo(NOMIS_OLD_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_OLD_BOOKING_MOVEMENT_SEQ)
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(mappingType).isEqualTo(PrisonerLocationMappingType.MIGRATED)
        }
      }

      @Test
      fun `should recreate mappings if they already exist`() = runTest {
        val newMigrationId = "new_migration_id"
        val newDpsCustodialSeriesId = UUID.randomUUID()
        val newDpsExternalMovementId = UUID.randomUUID()

        val mappings = PrisonerLocationsMappingDto(
          offenderNo = NOMIS_OFFENDER_NO,
          migrationId = newMigrationId,
          bookings = listOf(
            PrisonerLocationBookingMappingDto(
              bookingId = NOMIS_BOOKING_ID,
              dpsCustodialSeriesId = newDpsCustodialSeriesId,
              movements = listOf(
                PrisonerLocationMovementMappingDto(
                  nomisMovementSeq = NOMIS_MOVEMENT_SEQ_1,
                  dpsExternalMovementId = newDpsExternalMovementId,
                ),
              ),
            ),
          ),
        )

        // We saved the initial mappings in the setup - call the endpoint again
        webTestClient.saveMappings(mappings)

        // The old mappings for the re-migrated booking have disappeared
        assertThat(bookingRepository.findById(DPS_CUSTODIAL_SERIES_ID)).isNull()
        assertThat(movementRepository.findById(DPS_EXTERNAL_MOVEMENT_ID_1)).isNull()
        assertThat(movementRepository.findById(DPS_EXTERNAL_MOVEMENT_ID_2)).isNull()

        // The new mappings are available
        with(bookingRepository.findById(newDpsCustodialSeriesId)!!) {
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(label).isEqualTo(newMigrationId)
        }
        with(movementRepository.findById(newDpsExternalMovementId)!!) {
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_MOVEMENT_SEQ_1)
          assertThat(label).isEqualTo(newMigrationId)
        }
        assertThat(migrationRepository.findById(NOMIS_OFFENDER_NO)!!.label).isEqualTo(newMigrationId)

        // Mappings for bookings not in the request are left alone
        assertThat(bookingRepository.findById(DPS_OLD_CUSTODIAL_SERIES_ID)).isNotNull
        assertThat(movementRepository.findById(DPS_OLD_BOOKING_EXTERNAL_MOVEMENT_ID)).isNotNull
      }

      @Test
      fun `should recreate mappings with the same DPS ids`() = runTest {
        webTestClient.saveMappings(mappingsRequest())

        assertThat(bookingRepository.findById(DPS_CUSTODIAL_SERIES_ID)).isNotNull
        assertThat(movementRepository.findById(DPS_EXTERNAL_MOVEMENT_ID_1)).isNotNull
        assertThat(movementRepository.findById(DPS_EXTERNAL_MOVEMENT_ID_2)).isNotNull
        assertThat(movementRepository.findById(DPS_OLD_BOOKING_EXTERNAL_MOVEMENT_ID)).isNotNull
      }
    }

    @Nested
    inner class NoBookings {
      @Test
      fun `should save migration mapping when there are no bookings`() = runTest {
        bookingRepository.save(
          PrisonerLocationBookingMapping(
            dpsCustodialSeriesId = DPS_CUSTODIAL_SERIES_ID,
            nomisBookingId = NOMIS_BOOKING_ID,
            label = "some_other_label",
            mappingType = PrisonerLocationMappingType.MIGRATED,
          ),
        )

        webTestClient.saveMappings(
          PrisonerLocationsMappingDto(
            offenderNo = NOMIS_OFFENDER_NO,
            migrationId = MIGRATION_ID,
            bookings = listOf(),
          ),
        )

        assertThat(migrationRepository.findById(NOMIS_OFFENDER_NO)!!.label).isEqualTo(MIGRATION_ID)
        assertThat(bookingRepository.findById(DPS_CUSTODIAL_SERIES_ID)).isNotNull
      }
    }

    @Nested
    inner class Security {
      val mappings = PrisonerLocationsMappingDto(
        offenderNo = "A1234BC",
        bookings = listOf(),
        migrationId = "some_migration_id",
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/prisoner-location/migrate")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/prisoner-location/migrate")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/prisoner-location/migrate")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }
}
