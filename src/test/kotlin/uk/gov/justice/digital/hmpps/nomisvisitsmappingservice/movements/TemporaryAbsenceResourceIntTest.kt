package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.movements

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TemporaryAbsenceResourceIntTest(
  @Autowired private val applicationRepository: TemporaryAbsenceApplicationRepository,
  @Autowired private val appMultiRepository: TemporaryAbsenceAppMultiRepository,
  @Autowired private val scheduleRepository: TemporaryAbsenceScheduleRepository,
  @Autowired private val movementRepository: TemporaryAbsenceMovementRepository,
) : IntegrationTestBase() {

  @Nested
  inner class Migrate {

    @Nested
    @Suppress("ktlint:standard:property-naming")
    inner class HappyPath {
      private val MIGRATION_ID = "2025-08-13T13:44:55"
      private val NOMIS_OFFENDER_NO = "A1234BC"
      private val NOMIS_BOOKING_ID = 1L
      private val NOMIS_APPLICATION_ID = 2L
      private val NOMIS_APPLICATION_MULTI_ID = 3L
      private val NOMIS_SCHEDULED_OUT_EVENT_ID = 4L
      private val NOMIS_SCHEDULED_IN_EVENT_ID = 5L
      private val NOMIS_MOVEMENT_OUT_SEQ = 1
      private val NOMIS_MOVEMENT_IN_SEQ = 2
      private val NOMIS_UNSCHEDULED_MOVEMENT_OUT_SEQ = 3
      private val NOMIS_UNSCHEDULED_MOVEMENT_IN_SEQ = 4
      private val DPS_APPLICATION_ID = UUID.randomUUID()
      private val DPS_OUTSIDE_MOVEMENT_ID = UUID.randomUUID()
      private val DPS_SCHEDULED_OUT_ID = UUID.randomUUID()
      private val DPS_SCHEDULED_IN_ID = UUID.randomUUID()
      private val DPS_MOVEMENT_OUT_ID = UUID.randomUUID()
      private val DPS_MOVEMENT_IN_ID = UUID.randomUUID()
      private val DPS_UNSCHEDULED_MOVEMENT_OUT_ID = UUID.randomUUID()
      private val DPS_UNSCHEDULED_MOVEMENT_IN_ID = UUID.randomUUID()

      @AfterEach
      fun clearDatabase() = runTest {
        movementRepository.deleteAll()
        scheduleRepository.deleteAll()
        appMultiRepository.deleteAll()
        applicationRepository.deleteAll()
      }

      @BeforeEach
      fun saveMappings() {
        val mappings = TemporaryAbsencesPrisonerMappingDto(
          prisonerNumber = NOMIS_OFFENDER_NO,
          migrationId = MIGRATION_ID,
          bookings = listOf(
            TemporaryAbsenceBookingMappingDto(
              bookingId = NOMIS_BOOKING_ID,
              applications = listOf(
                TemporaryAbsenceApplicationMappingDto(
                  nomisMovementApplicationId = NOMIS_APPLICATION_ID,
                  dpsMovementApplicationId = DPS_APPLICATION_ID,
                  outsideMovements = listOf(
                    TemporaryAbsencesOutsideMovementMappingDto(
                      nomisMovementApplicationMultiId = NOMIS_APPLICATION_MULTI_ID,
                      dpsOutsideMovementId = DPS_OUTSIDE_MOVEMENT_ID,
                    ),
                  ),
                  schedules = listOf(
                    ScheduledMovementMappingDto(
                      nomisEventId = NOMIS_SCHEDULED_OUT_EVENT_ID,
                      dpsScheduledMovementId = DPS_SCHEDULED_OUT_ID,
                    ),
                    ScheduledMovementMappingDto(
                      nomisEventId = NOMIS_SCHEDULED_IN_EVENT_ID,
                      dpsScheduledMovementId = DPS_SCHEDULED_IN_ID,
                    ),
                  ),
                  movements = listOf(
                    ExternalMovementMappingDto(
                      nomisMovementSeq = NOMIS_MOVEMENT_OUT_SEQ,
                      dpsExternalMovementId = DPS_MOVEMENT_OUT_ID,
                    ),
                    ExternalMovementMappingDto(
                      nomisMovementSeq = NOMIS_MOVEMENT_IN_SEQ,
                      dpsExternalMovementId = DPS_MOVEMENT_IN_ID,
                    ),
                  ),
                ),
              ),
              unscheduledMovements = listOf(
                ExternalMovementMappingDto(
                  nomisMovementSeq = NOMIS_UNSCHEDULED_MOVEMENT_OUT_SEQ,
                  dpsExternalMovementId = DPS_UNSCHEDULED_MOVEMENT_OUT_ID,
                ),
                ExternalMovementMappingDto(
                  nomisMovementSeq = NOMIS_UNSCHEDULED_MOVEMENT_IN_SEQ,
                  dpsExternalMovementId = DPS_UNSCHEDULED_MOVEMENT_IN_ID,
                ),
              ),
            ),
          ),
        )

        webTestClient.post()
          .uri("/mapping/temporary-absence/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_MOVEMENTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `should save application mappings`() = runTest {
        with(applicationRepository.findById(DPS_APPLICATION_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(bookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisApplicationId).isEqualTo(NOMIS_APPLICATION_ID)
          assertThat(dpsApplicationId).isEqualTo(DPS_APPLICATION_ID)
        }
      }

      @Test
      fun `should save application outside movement mappings`() = runTest {
        with(appMultiRepository.findById(DPS_OUTSIDE_MOVEMENT_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(bookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisAppMultiId).isEqualTo(NOMIS_APPLICATION_MULTI_ID)
          assertThat(dpsAppMultiId).isEqualTo(DPS_OUTSIDE_MOVEMENT_ID)
        }
      }

      @Test
      fun `should save application schedule mappings`() = runTest {
        with(scheduleRepository.findById(DPS_SCHEDULED_OUT_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(bookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisScheduleId).isEqualTo(NOMIS_SCHEDULED_OUT_EVENT_ID)
          assertThat(dpsScheduleId).isEqualTo(DPS_SCHEDULED_OUT_ID)
        }
        with(scheduleRepository.findById(DPS_SCHEDULED_IN_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(bookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisScheduleId).isEqualTo(NOMIS_SCHEDULED_IN_EVENT_ID)
          assertThat(dpsScheduleId).isEqualTo(DPS_SCHEDULED_IN_ID)
        }
      }

      @Test
      fun `should save application movement mappings`() = runTest {
        with(movementRepository.findById(DPS_MOVEMENT_OUT_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_MOVEMENT_OUT_SEQ)
          assertThat(dpsMovementId).isEqualTo(DPS_MOVEMENT_OUT_ID)
        }
        with(movementRepository.findById(DPS_MOVEMENT_IN_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_MOVEMENT_IN_SEQ)
          assertThat(dpsMovementId).isEqualTo(DPS_MOVEMENT_IN_ID)
        }
      }

      @Test
      fun `should save unscheduled movement mappings`() = runTest {
        with(movementRepository.findById(DPS_UNSCHEDULED_MOVEMENT_OUT_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_UNSCHEDULED_MOVEMENT_OUT_SEQ)
          assertThat(dpsMovementId).isEqualTo(DPS_UNSCHEDULED_MOVEMENT_OUT_ID)
        }
        with(movementRepository.findById(DPS_UNSCHEDULED_MOVEMENT_IN_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_UNSCHEDULED_MOVEMENT_IN_SEQ)
          assertThat(dpsMovementId).isEqualTo(DPS_UNSCHEDULED_MOVEMENT_IN_ID)
        }
      }
    }

    @Nested
    inner class Security {
      val mappings = TemporaryAbsencesPrisonerMappingDto(
        "A1234BC",
        listOf(),
        "some_migration",
        "${LocalDateTime.now()}",
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/migrate")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/migrate")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/migrate")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }
}
