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

  @Nested
  @DisplayName("GET /mapping/transfer-scheduler/{prisonerNumber}/ids")
  inner class GetAllPrisonerMappingIds {

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
}
