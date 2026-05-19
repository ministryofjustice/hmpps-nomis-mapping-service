package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.offender

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.migration.BookingCourtMovementMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.migration.BookingCourtScheduleMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.migration.CourtSchedulerBookingMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.migration.CourtSchedulerPrisonerMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.movement.CourtMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule.CourtScheduleRepository
import java.util.*

class CourtSchedulerPrisonerResourceIntTest(
  @Autowired private val scheduleRepository: CourtScheduleRepository,
  @Autowired private val movementRepository: CourtMovementRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /mapping/court-scheduler/{prisonerNumber}/ids")
  @Suppress("ktlint:standard:property-naming")
  inner class GetAllPrisonerMappingIds {

    private val MIGRATION_ID = "2025-08-13T13:44:55"
    private val NOMIS_OFFENDER_NO = "A1234BC"
    private val NOMIS_BOOKING_ID = 1L
    private val NOMIS_SCHEDULED_OUT_EVENT_ID = 4L
    private val NOMIS_MOVEMENT_OUT_SEQ = 1
    private val NOMIS_MOVEMENT_IN_SEQ = 2
    private val NOMIS_UNSCHEDULED_MOVEMENT_OUT_SEQ = 3
    private val NOMIS_UNSCHEDULED_MOVEMENT_IN_SEQ = 4
    private val DPS_COURT_APPEARANCE_ID = UUID.randomUUID()
    private val DPS_MOVEMENT_OUT_ID = UUID.randomUUID()
    private val DPS_MOVEMENT_IN_ID = UUID.randomUUID()
    private val DPS_UNSCHEDULED_MOVEMENT_OUT_ID = UUID.randomUUID()
    private val DPS_UNSCHEDULED_MOVEMENT_IN_ID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
      saveMappings()
    }

    @AfterEach
    fun clearDatabase() = runTest {
      movementRepository.deleteAll()
      scheduleRepository.deleteAll()
    }

    fun saveMappings(mappings: CourtSchedulerPrisonerMappingsDto = mappingsRequest()) {
      webTestClient.put()
        .uri("/mapping/court-scheduler/migrate")
        .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(mappings))
        .exchange()
        .expectStatus().isCreated
    }

    fun mappingsRequest(
      dpsCourtAppearanceId: UUID = DPS_COURT_APPEARANCE_ID,
      dpsMovementOutId: UUID = DPS_MOVEMENT_OUT_ID,
      dpsMovementInId: UUID = DPS_MOVEMENT_IN_ID,
      dpsUnscheduledMovementOutId: UUID = DPS_UNSCHEDULED_MOVEMENT_OUT_ID,
      dpsUnscheduledMovementInId: UUID = DPS_UNSCHEDULED_MOVEMENT_IN_ID,
      migrationId: String = MIGRATION_ID,
    ) = CourtSchedulerPrisonerMappingsDto(
      offenderNo = NOMIS_OFFENDER_NO,
      migrationId = migrationId,
      bookings = listOf(
        CourtSchedulerBookingMappingsDto(
          bookingId = NOMIS_BOOKING_ID,
          courtSchedules = listOf(
            BookingCourtScheduleMappingsDto(
              nomisEventId = NOMIS_SCHEDULED_OUT_EVENT_ID,
              dpsCourtAppearanceId = dpsCourtAppearanceId,
              movements = listOf(
                BookingCourtMovementMappingsDto(
                  nomisMovementSeq = NOMIS_MOVEMENT_OUT_SEQ,
                  dpsCourtMovementId = dpsMovementOutId,
                ),
                BookingCourtMovementMappingsDto(
                  nomisMovementSeq = NOMIS_MOVEMENT_IN_SEQ,
                  dpsCourtMovementId = dpsMovementInId,
                ),
              ),
            ),
          ),
          unscheduledMovements = listOf(
            BookingCourtMovementMappingsDto(
              nomisMovementSeq = NOMIS_UNSCHEDULED_MOVEMENT_OUT_SEQ,
              dpsCourtMovementId = dpsUnscheduledMovementOutId,
            ),
            BookingCourtMovementMappingsDto(
              nomisMovementSeq = NOMIS_UNSCHEDULED_MOVEMENT_IN_SEQ,
              dpsCourtMovementId = dpsUnscheduledMovementInId,
            ),
          ),
        ),
      ),
    )

    @Nested
    @Suppress("ktlint:standard:property-naming")
    inner class HappyPath {
      private lateinit var allMappings: CourtSchedulerPrisonerMappingIdsDto

      @BeforeEach
      fun setUp() {
        saveMappings()

        allMappings = webTestClient.get()
          .uri("/mapping/court-scheduler/$NOMIS_OFFENDER_NO/ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<CourtSchedulerPrisonerMappingIdsDto>()
          .returnResult().responseBody!!
      }

      @Test
      fun `should get schedule mappings`() = runTest {
        assertThat(allMappings.schedules[0].nomisEventId).isEqualTo(NOMIS_SCHEDULED_OUT_EVENT_ID)
        assertThat(allMappings.schedules[0].dpsCourtAppearanceId).isEqualTo(DPS_COURT_APPEARANCE_ID)
      }

      @Test
      fun `should get movement mappings`() = runTest {
        assertThat(allMappings.movements[0].nomisMovementSeq).isEqualTo(NOMIS_MOVEMENT_OUT_SEQ)
        assertThat(allMappings.movements[0].dpsCourtMovementId).isEqualTo(DPS_MOVEMENT_OUT_ID)
        assertThat(allMappings.movements[1].nomisMovementSeq).isEqualTo(NOMIS_MOVEMENT_IN_SEQ)
        assertThat(allMappings.movements[1].dpsCourtMovementId).isEqualTo(DPS_MOVEMENT_IN_ID)
        assertThat(allMappings.movements[2].nomisMovementSeq).isEqualTo(NOMIS_UNSCHEDULED_MOVEMENT_OUT_SEQ)
        assertThat(allMappings.movements[2].dpsCourtMovementId).isEqualTo(DPS_UNSCHEDULED_MOVEMENT_OUT_ID)
        assertThat(allMappings.movements[3].nomisMovementSeq).isEqualTo(NOMIS_UNSCHEDULED_MOVEMENT_IN_SEQ)
        assertThat(allMappings.movements[3].dpsCourtMovementId).isEqualTo(DPS_UNSCHEDULED_MOVEMENT_IN_ID)
      }

      @Test
      fun `should return nothing if none found`() = runTest {
        webTestClient.get()
          .uri("/mapping/court-scheduler/UNKNOWN/ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<CourtSchedulerPrisonerMappingIdsDto>()
          .returnResult().responseBody!!
          .apply {
            assertThat(schedules).isEmpty()
            assertThat(movements).isEmpty()
          }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/court-scheduler/$NOMIS_OFFENDER_NO/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-scheduler/$NOMIS_OFFENDER_NO/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-scheduler/$NOMIS_OFFENDER_NO/ids")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }
}
