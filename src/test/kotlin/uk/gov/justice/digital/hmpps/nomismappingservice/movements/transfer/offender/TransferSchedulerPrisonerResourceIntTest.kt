@file:Suppress("ktlint:standard:property-naming")

package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.offender

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.migration.BookingTransferMovementMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.migration.BookingTransferScheduleMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.migration.TransferSchedulerBookingMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.migration.TransferSchedulerPrisonerMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.movement.TransferMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule.TransferScheduleRepository
import java.util.*

class TransferSchedulerPrisonerResourceIntTest(
  @Autowired private val scheduleRepository: TransferScheduleRepository,
  @Autowired private val movementRepository: TransferMovementRepository,
) : IntegrationTestBase() {

  private val MIGRATION_ID = "2025-08-13T13:44:55"
  private val NOMIS_OFFENDER_NO = "A1234BC"
  private val NOMIS_BOOKING_ID = 1L
  private val NOMIS_SCHEDULED_EVENT_ID = 4L
  private val NOMIS_SCHEDULED_MOVEMENT_SEQ = 1
  private val NOMIS_UNSCHEDULED_MOVEMENT_SEQ = 2
  private val DPS_TRANSFER_SCHEDULE_ID = UUID.randomUUID()
  private val DPS_SCHEDULED_MOVEMENT_ID = UUID.randomUUID()
  private val DPS_UNSCHEDULED_MOVEMENT_ID = UUID.randomUUID()

  @AfterEach
  fun clearDatabase() = runTest {
    movementRepository.deleteAll()
    scheduleRepository.deleteAll()
  }

  fun saveMappings(mappings: TransferSchedulerPrisonerMappingsDto = mappingsRequest()) {
    webTestClient.put()
      .uri("/mapping/transfer-scheduler/migrate")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mappings))
      .exchange()
      .expectStatus().isCreated
  }

  fun mappingsRequest(
    dpsTransferScheduleId: UUID = DPS_TRANSFER_SCHEDULE_ID,
    dpsScheduledMovementId: UUID = DPS_SCHEDULED_MOVEMENT_ID,
    dpsUnscheduledMovementId: UUID = DPS_UNSCHEDULED_MOVEMENT_ID,
    migrationId: String = MIGRATION_ID,
  ) = TransferSchedulerPrisonerMappingsDto(
    offenderNo = NOMIS_OFFENDER_NO,
    migrationId = migrationId,
    bookings = listOf(
      TransferSchedulerBookingMappingsDto(
        bookingId = NOMIS_BOOKING_ID,
        schedules = listOf(
          BookingTransferScheduleMappingsDto(
            nomisEventId = NOMIS_SCHEDULED_EVENT_ID,
            dpsTransferScheduleId = dpsTransferScheduleId,
            movement = BookingTransferMovementMappingsDto(
              nomisMovementSeq = NOMIS_SCHEDULED_MOVEMENT_SEQ,
              dpsTransferMovementId = dpsScheduledMovementId,
            ),
          ),
        ),
        unscheduledMovements = listOf(
          BookingTransferMovementMappingsDto(
            nomisMovementSeq = NOMIS_UNSCHEDULED_MOVEMENT_SEQ,
            dpsTransferMovementId = dpsUnscheduledMovementId,
          ),
        ),
      ),
    ),
  )

  @Nested
  @DisplayName("GET /mapping/transfer-scheduler/{prisonerNumber}/ids")
  inner class GetAllPrisonerMappingIds {

    @Nested
    inner class HappyPath {
      private lateinit var allMappings: TransferSchedulerPrisonerMappingIdsDto

      @BeforeEach
      fun setUp() {
        saveMappings()

        allMappings = webTestClient.get()
          .uri("/mapping/transfer-scheduler/$NOMIS_OFFENDER_NO/ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<TransferSchedulerPrisonerMappingIdsDto>()
          .returnResult().responseBody!!
      }

      @Test
      fun `should get schedule mappings`() = runTest {
        assertThat(allMappings.schedules[0].nomisEventId).isEqualTo(NOMIS_SCHEDULED_EVENT_ID)
        assertThat(allMappings.schedules[0].dpsTransferScheduleId).isEqualTo(DPS_TRANSFER_SCHEDULE_ID)
      }

      @Test
      fun `should get movement mappings`() = runTest {
        assertThat(allMappings.movements[0].nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
        assertThat(allMappings.movements[0].nomisMovementSeq).isEqualTo(NOMIS_SCHEDULED_MOVEMENT_SEQ)
        assertThat(allMappings.movements[0].dpsTransferMovementId).isEqualTo(DPS_SCHEDULED_MOVEMENT_ID)
        assertThat(allMappings.movements[1].nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
        assertThat(allMappings.movements[1].nomisMovementSeq).isEqualTo(NOMIS_UNSCHEDULED_MOVEMENT_SEQ)
        assertThat(allMappings.movements[1].dpsTransferMovementId).isEqualTo(DPS_UNSCHEDULED_MOVEMENT_ID)
      }

      @Test
      fun `should return nothing if none found`() = runTest {
        webTestClient.get()
          .uri("/mapping/transfer-scheduler/UNKNOWN/ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<TransferSchedulerPrisonerMappingIdsDto>()
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
          .uri("/mapping/transfer-scheduler/$NOMIS_OFFENDER_NO/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/transfer-scheduler/$NOMIS_OFFENDER_NO/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/transfer-scheduler/$NOMIS_OFFENDER_NO/ids")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/transfer-scheduler/move-booking/{bookingId}")
  inner class GetBookingMappings {

    @Nested
    inner class HappyPath {
      private lateinit var bookingMappings: TransferSchedulerMoveBookingMappingDto

      @BeforeEach
      fun setUp() {
        saveMappings()

        bookingMappings = webTestClient.get()
          .uri("/mapping/transfer-scheduler/move-booking/$NOMIS_BOOKING_ID")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<TransferSchedulerMoveBookingMappingDto>()
          .returnResult().responseBody!!
      }

      @Test
      fun `should get schedule mappings`() = runTest {
        assertThat(bookingMappings.scheduleIds[0].nomisEventId).isEqualTo(NOMIS_SCHEDULED_EVENT_ID)
        assertThat(bookingMappings.scheduleIds[0].dpsTransferScheduleId).isEqualTo(DPS_TRANSFER_SCHEDULE_ID)
      }

      @Test
      fun `should get movement mappings`() = runTest {
        assertThat(bookingMappings.movementIds[0].nomisMovementSeq).isEqualTo(NOMIS_SCHEDULED_MOVEMENT_SEQ)
        assertThat(bookingMappings.movementIds[0].dpsTransferMovementId).isEqualTo(DPS_SCHEDULED_MOVEMENT_ID)
        assertThat(bookingMappings.movementIds[1].nomisMovementSeq).isEqualTo(NOMIS_UNSCHEDULED_MOVEMENT_SEQ)
        assertThat(bookingMappings.movementIds[1].dpsTransferMovementId).isEqualTo(DPS_UNSCHEDULED_MOVEMENT_ID)
      }

      @Test
      fun `should return nothing if none found`() = runTest {
        webTestClient.get()
          .uri("/mapping/transfer-scheduler/move-booking/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<TransferSchedulerMoveBookingMappingDto>()
          .returnResult().responseBody!!
          .apply {
            assertThat(scheduleIds).isEmpty()
            assertThat(movementIds).isEmpty()
          }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/transfer-scheduler/move-booking/$NOMIS_BOOKING_ID")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/transfer-scheduler/move-booking/$NOMIS_BOOKING_ID")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/transfer-scheduler/move-booking/$NOMIS_BOOKING_ID")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  @Nested
  @DisplayName("PUT /mapping/transfer-scheduler/move-booking/{bookingId}/from/{fromOffenderNo}/to/{toOffenderNo}")
  inner class MoveBookingMappings {

    private val NOMIS_TO_OFFENDER_NO = "Z9876YX"
    private val NOMIS_WRONG_OFFENDER_NO = "X1111XX"

    private fun WebTestClient.moveBooking(
      bookingId: Long = NOMIS_BOOKING_ID,
      fromOffenderNo: String = NOMIS_OFFENDER_NO,
      toOffenderNo: String = NOMIS_TO_OFFENDER_NO,
    ) = put()
      .uri("/mapping/transfer-scheduler/move-booking/$bookingId/from/$fromOffenderNo/to/$toOffenderNo")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()

    @Nested
    inner class HappyPath {

      @BeforeEach
      fun setUp() {
        saveMappings()
      }

      @Test
      fun `should move schedule and movement mappings to the new offender`() = runTest {
        webTestClient.moveBooking()
          .expectStatus().isOk

        assertThat(scheduleRepository.findByBookingId(NOMIS_BOOKING_ID)).allSatisfy {
          assertThat(it.offenderNo).isEqualTo(NOMIS_TO_OFFENDER_NO)
        }
        assertThat(movementRepository.findByNomisBookingId(NOMIS_BOOKING_ID)).allSatisfy {
          assertThat(it.offenderNo).isEqualTo(NOMIS_TO_OFFENDER_NO)
        }
      }

      @Test
      fun `should be idempotent when mappings are already on the target offender`() {
        webTestClient.moveBooking().expectStatus().isOk
        webTestClient.moveBooking().expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `should return not found when there are no mappings for the booking`() {
        webTestClient.moveBooking(bookingId = 99999)
          .expectStatus().isNotFound
      }

      @Test
      fun `should return bad request when a mapping is on an unexpected offender`() {
        saveMappings()

        webTestClient.moveBooking(fromOffenderNo = NOMIS_WRONG_OFFENDER_NO)
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/transfer-scheduler/move-booking/$NOMIS_BOOKING_ID/from/$NOMIS_OFFENDER_NO/to/$NOMIS_TO_OFFENDER_NO")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/transfer-scheduler/move-booking/$NOMIS_BOOKING_ID/from/$NOMIS_OFFENDER_NO/to/$NOMIS_TO_OFFENDER_NO")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/transfer-scheduler/move-booking/$NOMIS_BOOKING_ID/from/$NOMIS_OFFENDER_NO/to/$NOMIS_TO_OFFENDER_NO")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }
}
