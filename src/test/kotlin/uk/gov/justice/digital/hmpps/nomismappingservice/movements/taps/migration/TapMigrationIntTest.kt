package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.migration

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application.MovementMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application.TapApplicationRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.movement.TapMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapApplicationMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapBookingMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapMovementMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapPrisonerMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapScheduleMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.schedule.TapScheduleRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class TapMigrationIntTest(
  @Autowired private val applicationRepository: TapApplicationRepository,
  @Autowired private val scheduleRepository: TapScheduleRepository,
  @Autowired private val movementRepository: TapMovementRepository,
  @Autowired private val migrationRepository: TapMigrationRepository,
) : IntegrationTestBase() {
  @Nested
  @DisplayName("PUT /mapping/taps/migrate")
  @Suppress("ktlint:standard:property-naming")
  inner class Migrate {

    private val MIGRATION_ID = "2025-08-13T13:44:55"
    private val NOMIS_OFFENDER_NO = "A1234BC"
    private val NOMIS_BOOKING_ID = 1L
    private val NOMIS_APPLICATION_ID = 2L
    private val NOMIS_SCHEDULED_OUT_EVENT_ID = 4L
    private val NOMIS_SCHEDULED_IN_EVENT_ID = 5L
    private val NOMIS_MOVEMENT_OUT_SEQ = 1
    private val NOMIS_MOVEMENT_IN_SEQ = 2
    private val NOMIS_UNSCHEDULED_MOVEMENT_OUT_SEQ = 3
    private val NOMIS_UNSCHEDULED_MOVEMENT_IN_SEQ = 4
    private val NOMIS_ADDESS_ID = 6L
    private val NOMIS_ADDRESS_OWNER_CLASS = "CORP"
    private val DPS_ADDRESS_TEXT = "some address"
    private val DPS_DESCRIPTION = "corp name"
    private val DPS_POSTCODE = "S1 1AA"
    private val EVENT_TIME = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
    private val DPS_APPLICATION_ID = UUID.randomUUID()
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
      applicationRepository.deleteAll()
    }

    fun saveMappings(mappings: TapPrisonerMappingsDto = mappingsRequest()) {
      webTestClient.put()
        .uri("/mapping/taps/migrate")
        .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(mappings))
        .exchange()
        .expectStatus().isCreated
    }

    fun mappingsRequest(
      dpsApplicationId: UUID = DPS_APPLICATION_ID,
      dpsScheduledOutId: UUID = DPS_SCHEDULED_OUT_ID,
      dpsScheduledInId: UUID = DPS_SCHEDULED_IN_ID,
      dpsMovementOutId: UUID = DPS_MOVEMENT_OUT_ID,
      dpsMovementInId: UUID = DPS_MOVEMENT_IN_ID,
      dpsUnscheduledMovementOutId: UUID = DPS_UNSCHEDULED_MOVEMENT_OUT_ID,
      dpsUnscheduledMovementInId: UUID = DPS_UNSCHEDULED_MOVEMENT_IN_ID,
      migrationId: String = MIGRATION_ID,
      nomisAddressId: Long = NOMIS_ADDESS_ID,
      nomisAddressOwnerClass: String = NOMIS_ADDRESS_OWNER_CLASS,
      dpsAddressText: String = DPS_ADDRESS_TEXT,
      dpsDescription: String = DPS_DESCRIPTION,
      dpsPostcode: String = DPS_POSTCODE,
    ) = TapPrisonerMappingsDto(
      prisonerNumber = NOMIS_OFFENDER_NO,
      migrationId = migrationId,
      bookings = listOf(
        TapBookingMappingsDto(
          bookingId = NOMIS_BOOKING_ID,
          applications = listOf(
            TapApplicationMappingsDto(
              nomisApplicationId = NOMIS_APPLICATION_ID,
              dpsAuthorisationId = dpsApplicationId,
              schedules = listOf(
                TapScheduleMappingsDto(
                  nomisEventId = NOMIS_SCHEDULED_OUT_EVENT_ID,
                  dpsOccurrenceId = dpsScheduledOutId,
                  nomisAddressId = nomisAddressId,
                  nomisAddressOwnerClass = nomisAddressOwnerClass,
                  dpsAddressText = dpsAddressText,
                  dpsDescription = dpsDescription,
                  dpsPostcode = dpsPostcode,
                  eventTime = EVENT_TIME,
                ),
                TapScheduleMappingsDto(
                  nomisEventId = NOMIS_SCHEDULED_IN_EVENT_ID,
                  dpsOccurrenceId = dpsScheduledInId,
                  nomisAddressId = null,
                  nomisAddressOwnerClass = null,
                  dpsAddressText = DPS_ADDRESS_TEXT,
                  dpsDescription = null,
                  dpsPostcode = null,
                  eventTime = EVENT_TIME,
                ),
              ),
              movements = listOf(
                TapMovementMappingsDto(
                  nomisMovementSeq = NOMIS_MOVEMENT_OUT_SEQ,
                  dpsMovementId = dpsMovementOutId,
                  nomisAddressId = nomisAddressId,
                  nomisAddressOwnerClass = nomisAddressOwnerClass,
                  dpsAddressText = dpsAddressText,
                  dpsDescription = dpsDescription,
                  dpsPostcode = dpsPostcode,
                ),
                TapMovementMappingsDto(
                  nomisMovementSeq = NOMIS_MOVEMENT_IN_SEQ,
                  dpsMovementId = dpsMovementInId,
                  nomisAddressId = null,
                  nomisAddressOwnerClass = null,
                  dpsAddressText = dpsAddressText,
                  dpsDescription = null,
                  dpsPostcode = null,
                ),
              ),
            ),
          ),
          unscheduledMovements = listOf(
            TapMovementMappingsDto(
              nomisMovementSeq = NOMIS_UNSCHEDULED_MOVEMENT_OUT_SEQ,
              dpsMovementId = dpsUnscheduledMovementOutId,
              nomisAddressId = nomisAddressId,
              nomisAddressOwnerClass = nomisAddressOwnerClass,
              dpsAddressText = dpsAddressText,
              dpsDescription = dpsDescription,
              dpsPostcode = dpsPostcode,
            ),
            TapMovementMappingsDto(
              nomisMovementSeq = NOMIS_UNSCHEDULED_MOVEMENT_IN_SEQ,
              dpsMovementId = dpsUnscheduledMovementInId,
              nomisAddressId = null,
              nomisAddressOwnerClass = null,
              dpsAddressText = dpsAddressText,
              dpsDescription = null,
              dpsPostcode = null,
            ),
          ),
        ),
      ),
    )

    @Nested
    @Suppress("ktlint:standard:property-naming")
    inner class HappyPath {

      @BeforeEach
      fun setUp() {
        saveMappings()
      }

      @Test
      fun `should save migration mapping`() = runTest {
        with(migrationRepository.findById(NOMIS_OFFENDER_NO)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
        }
      }

      @Test
      fun `should save application mappings`() = runTest {
        with(applicationRepository.findById(DPS_APPLICATION_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(bookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisApplicationId).isEqualTo(NOMIS_APPLICATION_ID)
          assertThat(dpsAuthorisationId).isEqualTo(DPS_APPLICATION_ID)
          assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
        }
      }

      @Test
      fun `should save application schedule mappings`() = runTest {
        with(scheduleRepository.findById(DPS_SCHEDULED_OUT_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(bookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisEventId).isEqualTo(NOMIS_SCHEDULED_OUT_EVENT_ID)
          assertThat(dpsOccurrenceId).isEqualTo(DPS_SCHEDULED_OUT_ID)
          assertThat(nomisAddressId).isEqualTo(NOMIS_ADDESS_ID)
          assertThat(nomisAddressOwnerClass).isEqualTo(NOMIS_ADDRESS_OWNER_CLASS)
          assertThat(dpsAddressText).isEqualTo(DPS_ADDRESS_TEXT)
          assertThat(dpsDescription).isEqualTo(DPS_DESCRIPTION)
          assertThat(dpsPostcode).isEqualTo(DPS_POSTCODE)
          assertThat(eventTime).isEqualTo(EVENT_TIME)
          assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
        }
        with(scheduleRepository.findById(DPS_SCHEDULED_IN_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(bookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisEventId).isEqualTo(NOMIS_SCHEDULED_IN_EVENT_ID)
          assertThat(dpsOccurrenceId).isEqualTo(DPS_SCHEDULED_IN_ID)
          assertThat(nomisAddressId).isNull()
          assertThat(nomisAddressOwnerClass).isNull()
          assertThat(dpsAddressText).isEqualTo(DPS_ADDRESS_TEXT)
          assertThat(dpsDescription).isNull()
          assertThat(dpsPostcode).isNull()
          assertThat(eventTime).isEqualTo(EVENT_TIME)
          assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
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
          assertThat(nomisAddressId).isEqualTo(NOMIS_ADDESS_ID)
          assertThat(nomisAddressOwnerClass).isEqualTo(NOMIS_ADDRESS_OWNER_CLASS)
          assertThat(dpsAddressText).isEqualTo(DPS_ADDRESS_TEXT)
          assertThat(dpsDescription).isEqualTo(DPS_DESCRIPTION)
          assertThat(dpsPostcode).isEqualTo(DPS_POSTCODE)
          assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
        }
        with(movementRepository.findById(DPS_MOVEMENT_IN_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_MOVEMENT_IN_SEQ)
          assertThat(dpsMovementId).isEqualTo(DPS_MOVEMENT_IN_ID)
          assertThat(nomisAddressId).isNull()
          assertThat(nomisAddressOwnerClass).isNull()
          assertThat(dpsAddressText).isEqualTo(DPS_ADDRESS_TEXT)
          assertThat(dpsDescription).isNull()
          assertThat(dpsPostcode).isNull()
          assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
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
          assertThat(nomisAddressId).isEqualTo(NOMIS_ADDESS_ID)
          assertThat(nomisAddressOwnerClass).isEqualTo(NOMIS_ADDRESS_OWNER_CLASS)
          assertThat(dpsAddressText).isEqualTo(DPS_ADDRESS_TEXT)
          assertThat(dpsDescription).isEqualTo(DPS_DESCRIPTION)
          assertThat(dpsPostcode).isEqualTo(DPS_POSTCODE)
          assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
        }
        with(movementRepository.findById(DPS_UNSCHEDULED_MOVEMENT_IN_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_UNSCHEDULED_MOVEMENT_IN_SEQ)
          assertThat(dpsMovementId).isEqualTo(DPS_UNSCHEDULED_MOVEMENT_IN_ID)
          assertThat(nomisAddressId).isNull()
          assertThat(nomisAddressOwnerClass).isNull()
          assertThat(dpsAddressText).isEqualTo(DPS_ADDRESS_TEXT)
          assertThat(dpsDescription).isNull()
          assertThat(dpsPostcode).isNull()
          assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
        }
      }

      @Test
      fun `should recreate mappings if they already exist`() = runTest {
        val dpsApplicationId = UUID.randomUUID()
        val dpsScheduledOutId = UUID.randomUUID()
        val dpsScheduledInId = UUID.randomUUID()
        val dpsMovementOutId = UUID.randomUUID()
        val dpsMovementInId = UUID.randomUUID()
        val dpsUnscheduledMovementOutId = UUID.randomUUID()
        val dpsUnscheduledMovementInId = UUID.randomUUID()
        val secondMigration = "second_migration"

        // We call the post mappings endpoint once in the BeforeEach - let's do it again for the same prisoner with different DPS ids
        saveMappings(
          mappingsRequest(
            dpsApplicationId,
            dpsScheduledOutId,
            dpsScheduledInId,
            dpsMovementOutId,
            dpsMovementInId,
            dpsUnscheduledMovementOutId,
            dpsUnscheduledMovementInId,
            secondMigration,
          ),
        )

        // And spot check some of the new DPS ids are saved to the DB
        assertThat(applicationRepository.findById(DPS_APPLICATION_ID)).isNull()
        assertThat(applicationRepository.findById(dpsApplicationId)).isNotNull
        assertThat(scheduleRepository.findById(DPS_SCHEDULED_OUT_ID)).isNull()
        assertThat(scheduleRepository.findById(dpsScheduledOutId)).isNotNull
        assertThat(movementRepository.findById(DPS_MOVEMENT_IN_ID)).isNull()
        assertThat(movementRepository.findById(dpsMovementInId)).isNotNull
        assertThat(movementRepository.findById(DPS_UNSCHEDULED_MOVEMENT_OUT_ID)).isNull()
        assertThat(movementRepository.findById(dpsUnscheduledMovementOutId)).isNotNull
        assertThat(migrationRepository.findById(NOMIS_OFFENDER_NO)!!.label).isEqualTo("second_migration")
      }
    }

    @Nested
    inner class Security {
      val mappings = TapPrisonerMappingsDto(
        "A1234BC",
        listOf(),
        "some_migration",
        "${LocalDateTime.now()}",
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/taps/migrate")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/taps/migrate")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/taps/migrate")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  @DisplayName("GET /mapping/taps/migration-id/{migrationId}")
  @Nested
  inner class GetMappingsCountByMigrationId {

    @BeforeEach
    fun setUp() = runTest {
      migrationRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/taps/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/taps/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/taps/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `can retrieve mappings count by migration Id`() = runTest {
        (1L..4).forEach {
          migrationRepository.save(
            TapMigration(
              offenderNo = "any$it",
              label = "2023-01-01T12:45:12",
            ),
          )
        }

        migrationRepository.save(
          TapMigration(
            offenderNo = "different",
            label = "2022-02-02T12:45:12",
          ),
        )

        webTestClient.get().uri("/mapping/taps/migration-id/2023-01-01T12:45:12")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(4)
      }

      @Test
      fun `should return when created as mandatory in migration service`() = runTest {
        val now = LocalDateTime.now().withNano(0)
        migrationRepository.save(
          TapMigration(
            offenderNo = "any",
            label = "2023-01-01T12:45:12",
            whenCreated = now,
          ),
        )

        webTestClient.get().uri("/mapping/taps/migration-id/2023-01-01T12:45:12")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("content[0].whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isEqualTo(now)
          }
      }
    }
  }
}
