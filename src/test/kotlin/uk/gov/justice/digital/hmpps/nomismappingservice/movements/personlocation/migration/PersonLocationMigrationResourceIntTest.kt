@file:Suppress("ktlint:standard:property-naming")

package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.migration

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
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.PersonLocationMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.booking.PersonLocationBookingMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.booking.PersonLocationBookingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.movement.PersonLocationMovementMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.movement.PersonLocationMovementRepository
import java.util.*

class PersonLocationMigrationResourceIntTest(
  @Autowired private val bookingRepository: PersonLocationBookingRepository,
  @Autowired private val movementRepository: PersonLocationMovementRepository,
  @Autowired private val migrationRepository: PersonLocationMigrationRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("PUT /mapping/person-location/migrate")
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

    fun mappingsRequest() = PersonLocationsMappingDto(
      offenderNo = NOMIS_OFFENDER_NO,
      migrationId = MIGRATION_ID,
      bookings = listOf(
        PersonLocationBookingMappingDto(
          bookingId = NOMIS_BOOKING_ID,
          dpsCustodialSeriesId = DPS_CUSTODIAL_SERIES_ID,
          movements = listOf(
            PersonLocationMovementMappingDto(
              nomisMovementSeq = NOMIS_MOVEMENT_SEQ_1,
              dpsExternalMovementId = DPS_EXTERNAL_MOVEMENT_ID_1,
            ),
            PersonLocationMovementMappingDto(
              nomisMovementSeq = NOMIS_MOVEMENT_SEQ_2,
              dpsExternalMovementId = DPS_EXTERNAL_MOVEMENT_ID_2,
            ),
          ),
        ),
        PersonLocationBookingMappingDto(
          bookingId = NOMIS_OLD_BOOKING_ID,
          dpsCustodialSeriesId = DPS_OLD_CUSTODIAL_SERIES_ID,
          movements = listOf(
            PersonLocationMovementMappingDto(
              nomisMovementSeq = NOMIS_OLD_BOOKING_MOVEMENT_SEQ,
              dpsExternalMovementId = DPS_OLD_BOOKING_EXTERNAL_MOVEMENT_ID,
            ),
          ),
        ),
      ),
    )

    fun WebTestClient.saveMappings(mappings: PersonLocationsMappingDto = mappingsRequest()) {
      put()
        .uri("/mapping/person-location/migrate")
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
          assertThat(mappingType).isEqualTo(PersonLocationMappingType.MIGRATED)
        }
        with(bookingRepository.findById(DPS_OLD_CUSTODIAL_SERIES_ID)!!) {
          assertThat(nomisBookingId).isEqualTo(NOMIS_OLD_BOOKING_ID)
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(mappingType).isEqualTo(PersonLocationMappingType.MIGRATED)
        }
      }

      @Test
      fun `should save movement mappings`() = runTest {
        with(movementRepository.findById(DPS_EXTERNAL_MOVEMENT_ID_1)!!) {
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_MOVEMENT_SEQ_1)
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(mappingType).isEqualTo(PersonLocationMappingType.MIGRATED)
        }
        with(movementRepository.findById(DPS_EXTERNAL_MOVEMENT_ID_2)!!) {
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_MOVEMENT_SEQ_2)
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(mappingType).isEqualTo(PersonLocationMappingType.MIGRATED)
        }
        with(movementRepository.findById(DPS_OLD_BOOKING_EXTERNAL_MOVEMENT_ID)!!) {
          assertThat(nomisBookingId).isEqualTo(NOMIS_OLD_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_OLD_BOOKING_MOVEMENT_SEQ)
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(mappingType).isEqualTo(PersonLocationMappingType.MIGRATED)
        }
      }

      @Test
      fun `should recreate mappings if they already exist`() = runTest {
        val newMigrationId = "new_migration_id"
        val newDpsCustodialSeriesId = UUID.randomUUID()
        val newDpsExternalMovementId = UUID.randomUUID()

        val mappings = PersonLocationsMappingDto(
          offenderNo = NOMIS_OFFENDER_NO,
          migrationId = newMigrationId,
          bookings = listOf(
            PersonLocationBookingMappingDto(
              bookingId = NOMIS_BOOKING_ID,
              dpsCustodialSeriesId = newDpsCustodialSeriesId,
              movements = listOf(
                PersonLocationMovementMappingDto(
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
          PersonLocationBookingMapping(
            dpsCustodialSeriesId = DPS_CUSTODIAL_SERIES_ID,
            nomisBookingId = NOMIS_BOOKING_ID,
            label = "some_other_label",
            mappingType = PersonLocationMappingType.MIGRATED,
          ),
        )

        webTestClient.saveMappings(
          PersonLocationsMappingDto(
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
      val mappings = PersonLocationsMappingDto(
        offenderNo = "A1234BC",
        bookings = listOf(),
        migrationId = "some_migration_id",
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/person-location/migrate")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/person-location/migrate")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/person-location/migrate")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/person-location/nomis-booking-ids/get-list")
  inner class GetMappingsByBookingIds {
    private val NOMIS_BOOKING_ID = 12345L
    private val DPS_CUSTODIAL_SERIES_ID = UUID.randomUUID()
    private val DPS_EXTERNAL_MOVEMENT_ID_1 = UUID.randomUUID()
    private val DPS_EXTERNAL_MOVEMENT_ID_2 = UUID.randomUUID()
    private val NOMIS_OTHER_BOOKING_ID = 12344L
    private val DPS_OTHER_CUSTODIAL_SERIES_ID = UUID.randomUUID()
    private val DPS_OTHER_BOOKING_EXTERNAL_MOVEMENT_ID = UUID.randomUUID()
    private val NOMIS_NO_MOVEMENTS_BOOKING_ID = 12343L
    private val DPS_NO_MOVEMENTS_CUSTODIAL_SERIES_ID = UUID.randomUUID()
    private val NOMIS_UNREQUESTED_BOOKING_ID = 12342L

    @BeforeEach
    fun setUp() = runTest {
      movementRepository.deleteAll()
      bookingRepository.deleteAll()

      saveBooking(DPS_CUSTODIAL_SERIES_ID, NOMIS_BOOKING_ID)
      saveMovement(DPS_EXTERNAL_MOVEMENT_ID_1, NOMIS_BOOKING_ID, 1)
      saveMovement(DPS_EXTERNAL_MOVEMENT_ID_2, NOMIS_BOOKING_ID, 2)
      saveBooking(DPS_OTHER_CUSTODIAL_SERIES_ID, NOMIS_OTHER_BOOKING_ID)
      saveMovement(DPS_OTHER_BOOKING_EXTERNAL_MOVEMENT_ID, NOMIS_OTHER_BOOKING_ID, 1)
      saveBooking(DPS_NO_MOVEMENTS_CUSTODIAL_SERIES_ID, NOMIS_NO_MOVEMENTS_BOOKING_ID)
      saveBooking(UUID.randomUUID(), NOMIS_UNREQUESTED_BOOKING_ID)
      saveMovement(UUID.randomUUID(), NOMIS_UNREQUESTED_BOOKING_ID, 1)
    }

    private suspend fun saveBooking(dpsId: UUID, bookingId: Long) = bookingRepository.save(
      PersonLocationBookingMapping(
        dpsCustodialSeriesId = dpsId,
        nomisBookingId = bookingId,
        label = "some_label",
        mappingType = PersonLocationMappingType.MIGRATED,
      ),
    )

    private suspend fun saveMovement(dpsId: UUID, bookingId: Long, sequence: Int) = movementRepository.save(
      PersonLocationMovementMapping(
        dpsExternalMovementId = dpsId,
        nomisBookingId = bookingId,
        nomisMovementSeq = sequence,
        label = "some_label",
        mappingType = PersonLocationMappingType.MIGRATED,
      ),
    )

    private fun WebTestClient.getMappings(bookingIds: List<Long>) = post()
      .uri("/mapping/person-location/nomis-booking-ids/get-list")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(bookingIds))
      .exchange()
      .expectStatus().isOk
      .expectBodyList(PersonLocationBookingMappingDto::class.java)
      .returnResult().responseBody!!

    @Nested
    inner class HappyPath {
      @Test
      fun `should return mappings for the requested bookings`() {
        val mappings = webTestClient.getMappings(listOf(NOMIS_BOOKING_ID, NOMIS_OTHER_BOOKING_ID, NOMIS_NO_MOVEMENTS_BOOKING_ID))

        assertThat(mappings).containsExactlyInAnyOrder(
          PersonLocationBookingMappingDto(
            bookingId = NOMIS_BOOKING_ID,
            dpsCustodialSeriesId = DPS_CUSTODIAL_SERIES_ID,
            movements = listOf(
              PersonLocationMovementMappingDto(nomisMovementSeq = 1, dpsExternalMovementId = DPS_EXTERNAL_MOVEMENT_ID_1),
              PersonLocationMovementMappingDto(nomisMovementSeq = 2, dpsExternalMovementId = DPS_EXTERNAL_MOVEMENT_ID_2),
            ),
          ),
          PersonLocationBookingMappingDto(
            bookingId = NOMIS_OTHER_BOOKING_ID,
            dpsCustodialSeriesId = DPS_OTHER_CUSTODIAL_SERIES_ID,
            movements = listOf(
              PersonLocationMovementMappingDto(nomisMovementSeq = 1, dpsExternalMovementId = DPS_OTHER_BOOKING_EXTERNAL_MOVEMENT_ID),
            ),
          ),
          PersonLocationBookingMappingDto(
            bookingId = NOMIS_NO_MOVEMENTS_BOOKING_ID,
            dpsCustodialSeriesId = DPS_NO_MOVEMENTS_CUSTODIAL_SERIES_ID,
            movements = listOf(),
          ),
        )
      }

      @Test
      fun `should omit bookings with no mappings`() {
        val mappings = webTestClient.getMappings(listOf(NOMIS_BOOKING_ID, 99999L))

        assertThat(mappings.map { it.bookingId }).containsExactly(NOMIS_BOOKING_ID)
      }

      @Test
      fun `should return empty list when no bookings requested`() {
        assertThat(webTestClient.getMappings(listOf())).isEmpty()
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/person-location/nomis-booking-ids/get-list")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(listOf(NOMIS_BOOKING_ID)))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/person-location/nomis-booking-ids/get-list")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(listOf(NOMIS_BOOKING_ID)))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/person-location/nomis-booking-ids/get-list")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(listOf(NOMIS_BOOKING_ID)))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }
}
