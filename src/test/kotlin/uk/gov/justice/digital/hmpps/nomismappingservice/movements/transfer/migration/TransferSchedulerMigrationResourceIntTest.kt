@file:Suppress("ktlint:standard:property-naming")

package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.migration

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
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.movement.TransferMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule.TransferScheduleRepository
import java.util.*

class TransferSchedulerMigrationResourceIntTest(
  @Autowired private val scheduleRepository: TransferScheduleRepository,
  @Autowired private val movementRepository: TransferMovementRepository,
  @Autowired private val migrationRepository: TransferSchedulerMigrationRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("PUT /mapping/transfer-scheduler/migrate")
  inner class Migrate {
    private val MIGRATION_ID = "some_migration_id"
    private val NOMIS_OFFENDER_NO = "A1234BC"
    private val NOMIS_BOOKING_ID = 12345L
    private val NOMIS_EVENT_ID = 67890L
    private val DPS_TRANSFER_SCHEDULE_ID = UUID.randomUUID()
    private val NOMIS_SCHEDULED_MOVE_SEQ = 1
    private val DPS_SCHEDULED_TRANSFER_MOVEMENT_ID = UUID.randomUUID()
    private val NOMIS_UNSCHEDULED_MOVE_SEQ = 2
    private val DPS_UNSCHEDULED_TRANSFER_MOVEMENT_ID = UUID.randomUUID()

    @BeforeEach
    fun clearDatabase() = runTest {
      movementRepository.deleteAll()
      scheduleRepository.deleteAll()
      migrationRepository.deleteAll()
    }

    fun mappingsRequest() = TransferSchedulerPrisonerMappingsDto(
      offenderNo = NOMIS_OFFENDER_NO,
      migrationId = MIGRATION_ID,
      bookings = listOf(
        TransferSchedulerBookingMappingsDto(
          bookingId = NOMIS_BOOKING_ID,
          schedules = listOf(
            BookingTransferScheduleMappingsDto(
              nomisEventId = NOMIS_EVENT_ID,
              dpsTransferScheduleId = DPS_TRANSFER_SCHEDULE_ID,
              movement = BookingTransferMovementMappingsDto(
                nomisMovementSeq = NOMIS_SCHEDULED_MOVE_SEQ,
                dpsTransferMovementId = DPS_SCHEDULED_TRANSFER_MOVEMENT_ID,
              ),
            ),
          ),
          unscheduledMovements = listOf(
            BookingTransferMovementMappingsDto(
              nomisMovementSeq = NOMIS_UNSCHEDULED_MOVE_SEQ,
              dpsTransferMovementId = DPS_UNSCHEDULED_TRANSFER_MOVEMENT_ID,
            ),
          ),
        ),
      ),
    )

    fun WebTestClient.saveMappings(mappings: TransferSchedulerPrisonerMappingsDto = mappingsRequest()) {
      put()
        .uri("/mapping/transfer-scheduler/migrate")
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
      fun `should save schedule mapping`() = runTest {
        with(scheduleRepository.findById(DPS_TRANSFER_SCHEDULE_ID)!!) {
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(bookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisEventId).isEqualTo(NOMIS_EVENT_ID)
          assertThat(mappingType.name).isEqualTo("MIGRATED")
        }
      }

      @Test
      fun `should save scheduled movement mapping`() = runTest {
        with(movementRepository.findById(DPS_SCHEDULED_TRANSFER_MOVEMENT_ID)!!) {
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_SCHEDULED_MOVE_SEQ)
          assertThat(mappingType.name).isEqualTo("MIGRATED")
        }
      }

      @Test
      fun `should save unscheduled movement mapping`() = runTest {
        with(movementRepository.findById(DPS_UNSCHEDULED_TRANSFER_MOVEMENT_ID)!!) {
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_UNSCHEDULED_MOVE_SEQ)
          assertThat(mappingType.name).isEqualTo("MIGRATED")
        }
      }

      @Test
      fun `should recreate mappings if they already exist`() = runTest {
        val newMigrationId = "new_migration_id"
        val newNomisEventId = 8765L
        val newNomisMovementSeq = 10
        val newDpsTransferScheduleId = UUID.randomUUID()
        val newDpsTransferMovementId = UUID.randomUUID()

        val mappings = TransferSchedulerPrisonerMappingsDto(
          offenderNo = NOMIS_OFFENDER_NO,
          migrationId = newMigrationId,
          bookings = listOf(
            TransferSchedulerBookingMappingsDto(
              bookingId = NOMIS_BOOKING_ID,
              schedules = listOf(
                BookingTransferScheduleMappingsDto(
                  nomisEventId = newNomisEventId,
                  dpsTransferScheduleId = newDpsTransferScheduleId,
                  movement = BookingTransferMovementMappingsDto(
                    nomisMovementSeq = newNomisMovementSeq,
                    dpsTransferMovementId = newDpsTransferMovementId,
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
        assertThat(scheduleRepository.findById(DPS_TRANSFER_SCHEDULE_ID)).isNull()
        assertThat(movementRepository.findById(DPS_SCHEDULED_TRANSFER_MOVEMENT_ID)).isNull()
        assertThat(movementRepository.findById(DPS_UNSCHEDULED_TRANSFER_MOVEMENT_ID)).isNull()

        // The new mappings are available
        assertThat(scheduleRepository.findById(newDpsTransferScheduleId)).isNotNull
        assertThat(movementRepository.findById(newDpsTransferMovementId)).isNotNull
      }
    }

    @Nested
    inner class Security {
      val mappings = TransferSchedulerPrisonerMappingsDto(
        offenderNo = "A1234BC",
        bookings = listOf(),
        migrationId = "some_migration_id",
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/transfer-scheduler/migrate")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/transfer-scheduler/migrate")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/transfer-scheduler/migrate")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }
}
