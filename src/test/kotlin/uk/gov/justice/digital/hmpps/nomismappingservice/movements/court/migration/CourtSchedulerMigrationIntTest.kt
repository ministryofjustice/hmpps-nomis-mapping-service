@file:Suppress("ktlint:standard:property-naming")

package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.migration

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
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.movement.CourtMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule.CourtScheduleRepository
import java.util.*

class CourtSchedulerMigrationIntTest(
  @Autowired private val scheduleRepository: CourtScheduleRepository,
  @Autowired private val movementRepository: CourtMovementRepository,
  @Autowired private val migrationRepository: CourtSchedulerMigrationRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("PUT /mapping/court/migrate")
  inner class Migrate {
    private val MIGRATION_ID = "some_migration_id"
    private val NOMIS_OFFENDER_NO = "A1234BC"
    private val NOMIS_BOOKING_ID = 12345L
    private val NOMIS_EVENT_ID = 67890L
    private val DPS_COURT_APPEARANCE_ID = UUID.randomUUID()
    private val NOMIS_SCHEDULED_MOVE_OUT_SEQ = 1
    private val DPS_SCHEDULE_COURT_MOVEMENT_OUT_ID = UUID.randomUUID()
    private val NOMIS_SCHEDULED_MOVE_IN_SEQ = 2
    private val DPS_SCHEDULED_COURT_MOVEMENT_IN_ID = UUID.randomUUID()
    private val NOMIS_UNSCHEDULED_MOVE_OUT_SEQ = 3
    private val DPS_UNSCHEDULED_COURT_MOVEMENT_OUT_ID = UUID.randomUUID()
    private val NOMIS_UNSCHEDULED_MOVE_IN_SEQ = 4
    private val DPS_UNSCHEDULED_COURT_MOVEMENT_IN_ID = UUID.randomUUID()

    @BeforeEach
    fun clearDatabase() = runTest {
      movementRepository.deleteAll()
      scheduleRepository.deleteAll()
      migrationRepository.deleteAll()
    }

    fun mappingsRequest() = CourtSchedulerPrisonerMappingsDto(
      offenderNo = NOMIS_OFFENDER_NO,
      migrationId = MIGRATION_ID,
      bookings = listOf(
        CourtSchedulerBookingMappingsDto(
          bookingId = NOMIS_BOOKING_ID,
          courtSchedules = listOf(
            BookingCourtScheduleMappingsDto(
              nomisEventId = NOMIS_EVENT_ID,
              dpsCourtAppearanceId = DPS_COURT_APPEARANCE_ID,
              movements = listOf(
                BookingCourtMovementMappingsDto(
                  nomisMovementSeq = NOMIS_SCHEDULED_MOVE_OUT_SEQ,
                  dpsCourtMovementId = DPS_SCHEDULE_COURT_MOVEMENT_OUT_ID,
                ),
                BookingCourtMovementMappingsDto(
                  nomisMovementSeq = NOMIS_SCHEDULED_MOVE_IN_SEQ,
                  dpsCourtMovementId = DPS_SCHEDULED_COURT_MOVEMENT_IN_ID,
                ),
              ),
            ),
          ),
          unscheduledMovements = listOf(
            BookingCourtMovementMappingsDto(
              nomisMovementSeq = NOMIS_UNSCHEDULED_MOVE_OUT_SEQ,
              dpsCourtMovementId = DPS_UNSCHEDULED_COURT_MOVEMENT_OUT_ID,
            ),
            BookingCourtMovementMappingsDto(
              nomisMovementSeq = NOMIS_UNSCHEDULED_MOVE_IN_SEQ,
              dpsCourtMovementId = DPS_UNSCHEDULED_COURT_MOVEMENT_IN_ID,
            ),
          ),
        ),
      ),
    )

    fun WebTestClient.saveMappings(mappings: CourtSchedulerPrisonerMappingsDto = mappingsRequest()) {
      put()
        .uri("/mapping/court/migrate")
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
      fun `should save migration mappings`() = runTest {
        with(migrationRepository.findById(NOMIS_OFFENDER_NO)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
        }
      }

      @Test
      fun `should save schedule mapping`() = runTest {
        with(scheduleRepository.findById(DPS_COURT_APPEARANCE_ID)!!) {
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(bookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisEventId).isEqualTo(NOMIS_EVENT_ID)
          assertThat(mappingType.name).isEqualTo("MIGRATED")
        }
      }

      @Test
      fun `should save scheduled movement out mapping`() = runTest {
        with(movementRepository.findById(DPS_SCHEDULE_COURT_MOVEMENT_OUT_ID)!!) {
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_SCHEDULED_MOVE_OUT_SEQ)
        }
      }

      @Test
      fun `should save scheduled movement in mapping`() = runTest {
        with(movementRepository.findById(DPS_SCHEDULED_COURT_MOVEMENT_IN_ID)!!) {
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_SCHEDULED_MOVE_IN_SEQ)
        }
      }

      @Test
      fun `should save unscheduled movement out mapping`() = runTest {
        with(movementRepository.findById(DPS_UNSCHEDULED_COURT_MOVEMENT_OUT_ID)!!) {
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_UNSCHEDULED_MOVE_OUT_SEQ)
        }
      }

      @Test
      fun `should save unscheduled movement in mapping`() = runTest {
        with(movementRepository.findById(DPS_UNSCHEDULED_COURT_MOVEMENT_IN_ID)!!) {
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_UNSCHEDULED_MOVE_IN_SEQ)
        }
      }

      @Test
      fun `should recreate mappings if they already exist`() = runTest {
        val newMigrationId = "new_migration_id"
        val newNomisEventId = 8765L
        val newNomisMovementSeq = 10
        val newDpsCourtAppearanceId = UUID.randomUUID()
        val newDpsCourtMovementId = UUID.randomUUID()

        val mappings = CourtSchedulerPrisonerMappingsDto(
          offenderNo = NOMIS_OFFENDER_NO,
          migrationId = newMigrationId,
          bookings = listOf(
            CourtSchedulerBookingMappingsDto(
              bookingId = NOMIS_BOOKING_ID,
              courtSchedules = listOf(
                BookingCourtScheduleMappingsDto(
                  nomisEventId = newNomisEventId,
                  dpsCourtAppearanceId = newDpsCourtAppearanceId,
                  movements = listOf(
                    BookingCourtMovementMappingsDto(
                      nomisMovementSeq = newNomisMovementSeq,
                      dpsCourtMovementId = newDpsCourtMovementId,
                    ),
                  ),
                ),
              ),
              unscheduledMovements = listOf(),
            ),
          ),
        )

        // We saved the initial mappings in the setup - call the endpoint again
        webTestClient.saveMappings(mappings)

        // The old mappings have disappeared
        assertThat(scheduleRepository.findById(DPS_COURT_APPEARANCE_ID)).isNull()
        assertThat(movementRepository.findById(DPS_SCHEDULE_COURT_MOVEMENT_OUT_ID)).isNull()
        assertThat(movementRepository.findById(DPS_SCHEDULED_COURT_MOVEMENT_IN_ID)).isNull()
        assertThat(movementRepository.findById(DPS_UNSCHEDULED_COURT_MOVEMENT_OUT_ID)).isNull()
        assertThat(movementRepository.findById(DPS_UNSCHEDULED_COURT_MOVEMENT_IN_ID)).isNull()

        // The new mappings are available
        assertThat(scheduleRepository.findById(newDpsCourtAppearanceId)).isNotNull
        assertThat(movementRepository.findById(newDpsCourtMovementId)).isNotNull
      }
    }

    @Nested
    inner class Security {
      val mappings = CourtSchedulerPrisonerMappingsDto("A1234BC", listOf(), "some_migration", "2025-08-13T13:44:55")

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/court/migrate")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/court/migrate")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/court/migrate")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }
}
