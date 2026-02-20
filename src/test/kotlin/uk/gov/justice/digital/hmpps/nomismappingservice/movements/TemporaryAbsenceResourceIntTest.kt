package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class TemporaryAbsenceResourceIntTest(
  @Autowired private val applicationRepository: TemporaryAbsenceApplicationRepository,
  @Autowired private val scheduleRepository: TemporaryAbsenceScheduleRepository,
  @Autowired private val movementRepository: TemporaryAbsenceMovementRepository,
  @Autowired private val migrationRepository: TemporaryAbsenceMigrationRepository,
  @Autowired private val addressRepository: TemporaryAbsenceAddressRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("PUT /mapping/temporary-absence/migrate")
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
      addressRepository.deleteAll()
    }

    fun saveMappings(mappings: TemporaryAbsencesPrisonerMappingDto = mappingsRequest()) {
      webTestClient.put()
        .uri("/mapping/temporary-absence/migrate")
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
    ) = TemporaryAbsencesPrisonerMappingDto(
      prisonerNumber = NOMIS_OFFENDER_NO,
      migrationId = migrationId,
      bookings = listOf(
        TemporaryAbsenceBookingMappingDto(
          bookingId = NOMIS_BOOKING_ID,
          applications = listOf(
            TemporaryAbsenceApplicationMappingDto(
              nomisMovementApplicationId = NOMIS_APPLICATION_ID,
              dpsMovementApplicationId = dpsApplicationId,
              schedules = listOf(
                ScheduledMovementMappingDto(
                  nomisEventId = NOMIS_SCHEDULED_OUT_EVENT_ID,
                  dpsOccurrenceId = dpsScheduledOutId,
                  nomisAddressId = nomisAddressId,
                  nomisAddressOwnerClass = nomisAddressOwnerClass,
                  dpsAddressText = dpsAddressText,
                  dpsDescription = dpsDescription,
                  dpsPostcode = dpsPostcode,
                  eventTime = EVENT_TIME,
                ),
                ScheduledMovementMappingDto(
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
                ExternalMovementMappingDto(
                  nomisMovementSeq = NOMIS_MOVEMENT_OUT_SEQ,
                  dpsMovementId = dpsMovementOutId,
                  nomisAddressId = nomisAddressId,
                  nomisAddressOwnerClass = nomisAddressOwnerClass,
                  dpsAddressText = dpsAddressText,
                  dpsDescription = dpsDescription,
                  dpsPostcode = dpsPostcode,
                ),
                ExternalMovementMappingDto(
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
            ExternalMovementMappingDto(
              nomisMovementSeq = NOMIS_UNSCHEDULED_MOVEMENT_OUT_SEQ,
              dpsMovementId = dpsUnscheduledMovementOutId,
              nomisAddressId = nomisAddressId,
              nomisAddressOwnerClass = nomisAddressOwnerClass,
              dpsAddressText = dpsAddressText,
              dpsDescription = dpsDescription,
              dpsPostcode = dpsPostcode,
            ),
            ExternalMovementMappingDto(
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
          assertThat(dpsApplicationId).isEqualTo(DPS_APPLICATION_ID)
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
      val mappings = TemporaryAbsencesPrisonerMappingDto(
        "A1234BC",
        listOf(),
        "some_migration",
        "${LocalDateTime.now()}",
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/temporary-absence/migrate")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/temporary-absence/migrate")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/temporary-absence/migrate")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/temporary-absence/{prisonerNumber}/ids")
  @Suppress("ktlint:standard:property-naming")
  inner class GetAllPrisonerMappingIds {

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
    private val EVENT_TIME = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
    private val DPS_APPLICATION_ID = UUID.randomUUID()
    private val DPS_SCHEDULED_OUT_ID = UUID.randomUUID()
    private val DPS_SCHEDULED_IN_ID = UUID.randomUUID()
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
      applicationRepository.deleteAll()
      addressRepository.deleteAll()
    }

    fun saveMappings(mappings: TemporaryAbsencesPrisonerMappingDto = mappingsRequest()) {
      webTestClient.put()
        .uri("/mapping/temporary-absence/migrate")
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
    ) = TemporaryAbsencesPrisonerMappingDto(
      prisonerNumber = NOMIS_OFFENDER_NO,
      migrationId = migrationId,
      bookings = listOf(
        TemporaryAbsenceBookingMappingDto(
          bookingId = NOMIS_BOOKING_ID,
          applications = listOf(
            TemporaryAbsenceApplicationMappingDto(
              nomisMovementApplicationId = NOMIS_APPLICATION_ID,
              dpsMovementApplicationId = dpsApplicationId,
              schedules = listOf(
                ScheduledMovementMappingDto(
                  nomisEventId = NOMIS_SCHEDULED_OUT_EVENT_ID,
                  dpsOccurrenceId = dpsScheduledOutId,
                  nomisAddressId = null,
                  nomisAddressOwnerClass = null,
                  dpsAddressText = "",
                  dpsDescription = null,
                  dpsPostcode = null,
                  eventTime = EVENT_TIME,
                ),
                ScheduledMovementMappingDto(
                  nomisEventId = NOMIS_SCHEDULED_IN_EVENT_ID,
                  dpsOccurrenceId = dpsScheduledInId,
                  nomisAddressId = null,
                  nomisAddressOwnerClass = null,
                  dpsAddressText = "",
                  dpsDescription = null,
                  dpsPostcode = null,
                  eventTime = EVENT_TIME,
                ),
              ),
              movements = listOf(
                ExternalMovementMappingDto(
                  nomisMovementSeq = NOMIS_MOVEMENT_OUT_SEQ,
                  dpsMovementId = dpsMovementOutId,
                  nomisAddressId = null,
                  nomisAddressOwnerClass = null,
                  dpsAddressText = "",
                  dpsDescription = null,
                  dpsPostcode = null,
                ),
                ExternalMovementMappingDto(
                  nomisMovementSeq = NOMIS_MOVEMENT_IN_SEQ,
                  dpsMovementId = dpsMovementInId,
                  nomisAddressId = null,
                  nomisAddressOwnerClass = null,
                  dpsAddressText = "",
                  dpsDescription = null,
                  dpsPostcode = null,
                ),
              ),
            ),
          ),
          unscheduledMovements = listOf(
            ExternalMovementMappingDto(
              nomisMovementSeq = NOMIS_UNSCHEDULED_MOVEMENT_OUT_SEQ,
              dpsMovementId = dpsUnscheduledMovementOutId,
              nomisAddressId = null,
              nomisAddressOwnerClass = null,
              dpsAddressText = "",
              dpsDescription = null,
              dpsPostcode = null,
            ),
            ExternalMovementMappingDto(
              nomisMovementSeq = NOMIS_UNSCHEDULED_MOVEMENT_IN_SEQ,
              dpsMovementId = dpsUnscheduledMovementInId,
              nomisAddressId = null,
              nomisAddressOwnerClass = null,
              dpsAddressText = "",
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
      private lateinit var allMappings: TemporaryAbsencesPrisonerMappingIdsDto

      @BeforeEach
      fun setUp() {
        saveMappings()

        allMappings = webTestClient.get()
          .uri("/mapping/temporary-absence/$NOMIS_OFFENDER_NO/ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<TemporaryAbsencesPrisonerMappingIdsDto>()
          .returnResult().responseBody!!
      }

      @Test
      fun `should get application mappings`() = runTest {
        assertThat(allMappings.applications[0].nomisMovementApplicationId).isEqualTo(NOMIS_APPLICATION_ID)
        assertThat(allMappings.applications[0].dpsMovementApplicationId).isEqualTo(DPS_APPLICATION_ID)
      }

      @Test
      fun `should get schedule mappings`() = runTest {
        assertThat(allMappings.schedules[0].nomisEventId).isEqualTo(NOMIS_SCHEDULED_OUT_EVENT_ID)
        assertThat(allMappings.schedules[0].dpsOccurrenceId).isEqualTo(DPS_SCHEDULED_OUT_ID)
        assertThat(allMappings.schedules[1].nomisEventId).isEqualTo(NOMIS_SCHEDULED_IN_EVENT_ID)
        assertThat(allMappings.schedules[1].dpsOccurrenceId).isEqualTo(DPS_SCHEDULED_IN_ID)
      }

      @Test
      fun `should get application movement mappings`() = runTest {
        assertThat(allMappings.movements[0].nomisMovementSeq).isEqualTo(NOMIS_MOVEMENT_OUT_SEQ)
        assertThat(allMappings.movements[0].dpsMovementId).isEqualTo(DPS_MOVEMENT_OUT_ID)
        assertThat(allMappings.movements[1].nomisMovementSeq).isEqualTo(NOMIS_MOVEMENT_IN_SEQ)
        assertThat(allMappings.movements[1].dpsMovementId).isEqualTo(DPS_MOVEMENT_IN_ID)
        assertThat(allMappings.movements[2].nomisMovementSeq).isEqualTo(NOMIS_UNSCHEDULED_MOVEMENT_OUT_SEQ)
        assertThat(allMappings.movements[2].dpsMovementId).isEqualTo(DPS_UNSCHEDULED_MOVEMENT_OUT_ID)
      }

      @Test
      fun `should return nothing if none found`() = runTest {
        webTestClient.get()
          .uri("/mapping/temporary-absence/UNKNOWN/ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<TemporaryAbsencesPrisonerMappingIdsDto>()
          .returnResult().responseBody!!
          .apply {
            assertThat(applications).isEmpty()
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
          .uri("/mapping/temporary-absence/$NOMIS_OFFENDER_NO/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/$NOMIS_OFFENDER_NO/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/$NOMIS_OFFENDER_NO/ids")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/temporary-absence/application")
  inner class CreateApplicationMapping {

    @AfterEach
    fun tearDown() = runTest {
      applicationRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TemporaryAbsenceApplicationSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should create mapping`() = runTest {
        webTestClient.createApplicationSyncMapping(mapping)
          .expectStatus().isCreated

        with(applicationRepository.findByNomisApplicationId(23456L)!!) {
          assertThat(offenderNo).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(12345L)
          assertThat(dpsApplicationId).isEqualTo(mapping.dpsMovementApplicationId)
          assertThat(mappingType).isEqualTo(MovementMappingType.NOMIS_CREATED)
        }
      }
    }

    @Nested
    inner class Validation {
      val mapping = TemporaryAbsenceApplicationSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
      )
      val duplicateMappingDps = TemporaryAbsenceApplicationSyncMappingDto(
        "B2345CD",
        56789L,
        34567L,
        mapping.dpsMovementApplicationId,
        MovementMappingType.MIGRATED,
      )
      val duplicateMappingNomis = TemporaryAbsenceApplicationSyncMappingDto(
        "C3456DE",
        9101112L,
        mapping.nomisMovementApplicationId,
        UUID.randomUUID(),
        MovementMappingType.MIGRATED,
      )

      @Test
      fun `should reject duplicate DPS ID mapping`() = runTest {
        webTestClient.createApplicationSyncMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createApplicationSyncMapping(duplicateMappingDps)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsMovementApplicationId", mapping.dpsMovementApplicationId.toString())
              .containsEntry("nomisMovementApplicationId", mapping.nomisMovementApplicationId.toInt())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingDps.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingDps.bookingId.toInt())
              .containsEntry("dpsMovementApplicationId", duplicateMappingDps.dpsMovementApplicationId.toString())
              .containsEntry("nomisMovementApplicationId", duplicateMappingDps.nomisMovementApplicationId.toInt())
              .containsEntry("mappingType", duplicateMappingDps.mappingType.toString())
          }
      }

      @Test
      fun `should reject duplicate NOMIS ID mapping`() = runTest {
        webTestClient.createApplicationSyncMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createApplicationSyncMapping(duplicateMappingNomis)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsMovementApplicationId", mapping.dpsMovementApplicationId.toString())
              .containsEntry("nomisMovementApplicationId", mapping.nomisMovementApplicationId.toInt())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingNomis.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingNomis.bookingId.toInt())
              .containsEntry("dpsMovementApplicationId", duplicateMappingNomis.dpsMovementApplicationId.toString())
              .containsEntry("nomisMovementApplicationId", duplicateMappingNomis.nomisMovementApplicationId.toInt())
              .containsEntry("mappingType", duplicateMappingNomis.mappingType.toString())
          }
      }
    }

    @Nested
    inner class Security {
      val mapping = TemporaryAbsenceApplicationSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/application")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/application")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/application")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.createApplicationSyncMapping(mapping: TemporaryAbsenceApplicationSyncMappingDto) = post()
      .uri("/mapping/temporary-absence/application")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/temporary-absence/application/nomis-application-id/{nomisApplicationId}")
  inner class GetNomisApplicationMapping {

    @AfterEach
    fun tearDown() = runTest {
      applicationRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TemporaryAbsenceApplicationMapping(
        UUID.randomUUID(),
        23456L,
        "A1234BC",
        12345L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should get application mapping by NOMIS ID`() = runTest {
        applicationRepository.save(mapping)

        webTestClient.getApplicationSyncMapping(mapping.nomisApplicationId)
          .expectStatus().isOk
          .expectBody(object : ParameterizedTypeReference<TemporaryAbsenceApplicationSyncMappingDto>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(nomisMovementApplicationId).isEqualTo(mapping.nomisApplicationId)
            assertThat(dpsMovementApplicationId).isEqualTo(mapping.dpsApplicationId)
            assertThat(prisonerNumber).isEqualTo(mapping.offenderNo)
            assertThat(bookingId).isEqualTo(mapping.bookingId)
            assertThat(mappingType).isEqualTo(mapping.mappingType)
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() = runTest {
        webTestClient.getApplicationSyncMapping(12345L)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      val mapping = TemporaryAbsenceApplicationSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/application/nomis-application-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/application/nomis-application-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/application/nomis-application-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getApplicationSyncMapping(nomisApplicationId: Long) = get()
      .uri("/mapping/temporary-absence/application/nomis-application-id/$nomisApplicationId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/temporary-absence/application/dps-id/{dpsId}")
  inner class GetDpsApplicationMapping {

    @AfterEach
    fun tearDown() = runTest {
      applicationRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TemporaryAbsenceApplicationMapping(
        UUID.randomUUID(),
        23456L,
        "A1234BC",
        12345L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should get application mapping by DPS ID`() = runTest {
        applicationRepository.save(mapping)

        webTestClient.getApplicationSyncMapping(mapping.dpsApplicationId)
          .expectStatus().isOk
          .expectBody(object : ParameterizedTypeReference<TemporaryAbsenceApplicationSyncMappingDto>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(nomisMovementApplicationId).isEqualTo(mapping.nomisApplicationId)
            assertThat(dpsMovementApplicationId).isEqualTo(mapping.dpsApplicationId)
            assertThat(prisonerNumber).isEqualTo(mapping.offenderNo)
            assertThat(bookingId).isEqualTo(mapping.bookingId)
            assertThat(mappingType).isEqualTo(mapping.mappingType)
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() = runTest {
        webTestClient.getApplicationSyncMapping(UUID.randomUUID())
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      val mapping = TemporaryAbsenceApplicationSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        mappingType = MovementMappingType.NOMIS_CREATED,
      )
      val dpsId = UUID.randomUUID()

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/application/dps-id/$dpsId")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/application/dps-id/$dpsId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/application/dps-id/$dpsId")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getApplicationSyncMapping(dpsApplicationId: UUID) = get()
      .uri("/mapping/temporary-absence/application/dps-id/$dpsApplicationId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("DELETE /mapping/temporary-absence/application/nomis-application-id/{nomisApplicationId}")
  inner class DeleteNomisApplicationMapping {

    @AfterEach
    fun tearDown() = runTest {
      applicationRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping1 = TemporaryAbsenceApplicationMapping(
        UUID.randomUUID(),
        23456L,
        "A1234BC",
        12345L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )
      val mapping2 = TemporaryAbsenceApplicationMapping(
        UUID.randomUUID(),
        65432L,
        "A1234BC",
        12345L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should delete application mapping by NOMIS ID`() = runTest {
        applicationRepository.save(mapping1)
        applicationRepository.save(mapping2)

        webTestClient.deleteApplicationSyncMapping(mapping1.nomisApplicationId)
          .expectStatus().isNoContent

        assertThat(applicationRepository.findByNomisApplicationId(mapping1.nomisApplicationId)).isNull()
        assertThat(applicationRepository.findByNomisApplicationId(mapping2.nomisApplicationId)).isNotNull
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `delete endpoint should be idempotent`() = runTest {
        webTestClient.deleteApplicationSyncMapping(12345L)
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/temporary-absence/application/nomis-application-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/temporary-absence/application/nomis-application-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/temporary-absence/application/nomis-application-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.deleteApplicationSyncMapping(nomisApplicationId: Long) = delete()
      .uri("/mapping/temporary-absence/application/nomis-application-id/$nomisApplicationId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("POST /mapping/temporary-absence/scheduled-movement")
  inner class CreateScheduledMovementSyncMapping {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
      addressRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = ScheduledMovementSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        77L,
        "some description",
        "S1 1AA",
        LocalDateTime.now(),
      )

      @Test
      fun `should create mapping`() = runTest {
        webTestClient.createScheduledMovementSyncMapping(mapping)
          .expectStatus().isCreated

        with(scheduleRepository.findByNomisEventId(mapping.nomisEventId)!!) {
          assertThat(offenderNo).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(12345L)
          assertThat(dpsOccurrenceId).isEqualTo(mapping.dpsOccurrenceId)
          assertThat(mappingType).isEqualTo(MovementMappingType.NOMIS_CREATED)
          assertThat(nomisAddressId).isEqualTo(34567L)
          assertThat(nomisAddressOwnerClass).isEqualTo("CORP")
          assertThat(dpsAddressText).isEqualTo("some address")
          assertThat(dpsDescription).isEqualTo("some description")
          assertThat(dpsPostcode).isEqualTo("S1 1AA")
          assertThat(eventTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `should create mapping with null address`() = runTest {
        webTestClient.createScheduledMovementSyncMapping(mapping.copy(nomisAddressId = null, nomisAddressOwnerClass = null))
          .expectStatus().isCreated

        with(scheduleRepository.findByNomisEventId(mapping.nomisEventId)!!) {
          assertThat(nomisAddressId).isNull()
          assertThat(nomisAddressOwnerClass).isNull()
        }
      }
    }

    @Nested
    inner class Validation {
      val mapping = ScheduledMovementSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        77L,
        "some description",
        "S1 1AA",
        LocalDateTime.now(),
      )
      val duplicateMappingDps = ScheduledMovementSyncMappingDto(
        "B2345CD",
        56789L,
        34567L,
        mapping.dpsOccurrenceId,
        MovementMappingType.MIGRATED,
        34567L,
        "CORP",
        "some address",
        77L,
        "some description",
        "S1 1AA",
        LocalDateTime.now(),
      )
      val duplicateMappingNomis = ScheduledMovementSyncMappingDto(
        "C3456DE",
        9101112L,
        mapping.nomisEventId,
        UUID.randomUUID(),
        MovementMappingType.MIGRATED,
        34567L,
        "CORP",
        "some address",
        77L,
        "some description",
        "S1 1AA",
        LocalDateTime.now(),
      )

      @Test
      fun `should reject duplicate DPS ID mapping`() = runTest {
        webTestClient.createScheduledMovementSyncMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createScheduledMovementSyncMapping(duplicateMappingDps)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsOccurrenceId", mapping.dpsOccurrenceId.toString())
              .containsEntry("nomisEventId", mapping.nomisEventId.toInt())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingDps.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingDps.bookingId.toInt())
              .containsEntry("dpsOccurrenceId", duplicateMappingDps.dpsOccurrenceId.toString())
              .containsEntry("nomisEventId", duplicateMappingDps.nomisEventId.toInt())
              .containsEntry("mappingType", duplicateMappingDps.mappingType.toString())
          }
      }

      @Test
      fun `should reject duplicate NOMIS ID mapping`() = runTest {
        webTestClient.createScheduledMovementSyncMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createScheduledMovementSyncMapping(duplicateMappingNomis)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsOccurrenceId", mapping.dpsOccurrenceId.toString())
              .containsEntry("nomisEventId", mapping.nomisEventId.toInt())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingNomis.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingNomis.bookingId.toInt())
              .containsEntry("dpsOccurrenceId", duplicateMappingNomis.dpsOccurrenceId.toString())
              .containsEntry("nomisEventId", duplicateMappingNomis.nomisEventId.toInt())
              .containsEntry("mappingType", duplicateMappingNomis.mappingType.toString())
          }
      }
    }

    @Nested
    inner class Security {
      val mapping = ScheduledMovementSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        mappingType = MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        77L,
        "some description",
        "S1 1AA",
        LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/scheduled-movement")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/scheduled-movement")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/scheduled-movement")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.createScheduledMovementSyncMapping(mapping: ScheduledMovementSyncMappingDto) = post()
      .uri("/mapping/temporary-absence/scheduled-movement")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
  @DisplayName("PUT /mapping/temporary-absence/scheduled-movement")
  inner class UpdateScheduledMovementSyncMapping {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
      addressRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = ScheduledMovementSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        77L,
        "some description",
        "S1 1AA",
        LocalDateTime.now(),
      )

      @Test
      fun `should update mapping`() = runTest {
        webTestClient.createScheduledMovementSyncMapping(mapping)
          .expectStatus().isCreated

        webTestClient.updateScheduledMovementSyncMapping(
          mapping.copy(
            nomisAddressId = 77777L,
            nomisAddressOwnerClass = "OFF",
            dpsUprn = 777L,
            dpsAddressText = "a different address",
            dpsDescription = "a different description",
            dpsPostcode = "S1 2BB",
            eventTime = LocalDateTime.now().plusDays(1),
          ),
        ).expectStatus().isOk

        with(scheduleRepository.findByNomisEventId(mapping.nomisEventId)!!) {
          assertThat(offenderNo).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(12345L)
          assertThat(dpsOccurrenceId).isEqualTo(mapping.dpsOccurrenceId)
          assertThat(mappingType).isEqualTo(MovementMappingType.NOMIS_CREATED)
          assertThat(nomisAddressId).isEqualTo(77777L)
          assertThat(nomisAddressOwnerClass).isEqualTo("OFF")
          assertThat(dpsUprn).isEqualTo(777L)
          assertThat(dpsAddressText).isEqualTo("a different address")
          assertThat(dpsDescription).isEqualTo("a different description")
          assertThat(dpsPostcode).isEqualTo("S1 2BB")
          assertThat(eventTime).isCloseTo(LocalDateTime.now().plusDays(1), within(1, ChronoUnit.SECONDS))
        }
      }
    }

    @Nested
    inner class Validation {
      val mapping = ScheduledMovementSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        77L,
        "some description",
        "S1 1AA",
        LocalDateTime.now(),
      )

      @Test
      fun `should return not found if mapping does not exist`() = runTest {
        webTestClient.updateScheduledMovementSyncMapping(mapping)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      val mapping = ScheduledMovementSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        mappingType = MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        77L,
        "some description",
        "S1 1AA",
        LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        webTestClient.createScheduledMovementSyncMapping(mapping)
          .expectStatus().isCreated
      }

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/temporary-absence/scheduled-movement")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/temporary-absence/scheduled-movement")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/temporary-absence/scheduled-movement")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.createScheduledMovementSyncMapping(mapping: ScheduledMovementSyncMappingDto) = post()
      .uri("/mapping/temporary-absence/scheduled-movement")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()

    private fun WebTestClient.updateScheduledMovementSyncMapping(mapping: ScheduledMovementSyncMappingDto, source: String = "NOMIS") = put()
      .uri("/mapping/temporary-absence/scheduled-movement?source=$source")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/temporary-absence/scheduled-movement/nomis-event-id/{nomisEventId}")
  inner class GetNomisScheduledMovementSyncMapping {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TemporaryAbsenceScheduleMapping(
        UUID.randomUUID(),
        23456L,
        "A1234BC",
        12345L,
        34567L,
        "CORP",
        "some address",
        77L,
        "some name",
        "S1 1AA",
        LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should get scheduled temporary absence mapping by NOMIS ID`() = runTest {
        scheduleRepository.save(mapping)

        webTestClient.getScheduledMovementSyncMapping(mapping.nomisEventId)
          .expectStatus().isOk
          .expectBody(object : ParameterizedTypeReference<ScheduledMovementSyncMappingDto>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(nomisEventId).isEqualTo(mapping.nomisEventId)
            assertThat(dpsOccurrenceId).isEqualTo(mapping.dpsOccurrenceId)
            assertThat(prisonerNumber).isEqualTo(mapping.offenderNo)
            assertThat(bookingId).isEqualTo(mapping.bookingId)
            assertThat(nomisAddressId).isEqualTo(mapping.nomisAddressId)
            assertThat(nomisAddressOwnerClass).isEqualTo(mapping.nomisAddressOwnerClass)
            assertThat(dpsAddressText).isEqualTo(mapping.dpsAddressText)
            assertThat(dpsDescription).isEqualTo(mapping.dpsDescription)
            assertThat(dpsPostcode).isEqualTo(mapping.dpsPostcode)
            assertThat(eventTime).isEqualTo(mapping.eventTime)
            assertThat(mappingType).isEqualTo(mapping.mappingType)
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() = runTest {
        webTestClient.getScheduledMovementSyncMapping(12345L)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      val mapping = ScheduledMovementSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        77L,
        "some description",
        "S1 1AA",
        LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/scheduled-movement/nomis-event-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/scheduled-movement/nomis-event-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/scheduled-movement/nomis-event-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getScheduledMovementSyncMapping(nomisEventId: Long) = get()
      .uri("/mapping/temporary-absence/scheduled-movement/nomis-event-id/$nomisEventId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/temporary-absence/scheduled-movement/dps-id/{dpsId}")
  inner class GetDpsScheduledMovementSyncMapping {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TemporaryAbsenceScheduleMapping(
        UUID.randomUUID(),
        23456L,
        "A1234BC",
        12345L,
        34567L,
        "CORP",
        "some address",
        77L,
        "some description",
        "S1 1AA",
        LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should get scheduled temporary absence mapping by DPS ID`() = runTest {
        scheduleRepository.save(mapping)

        webTestClient.getScheduledMovementSyncMapping(mapping.dpsOccurrenceId)
          .expectStatus().isOk
          .expectBody(object : ParameterizedTypeReference<ScheduledMovementSyncMappingDto>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(nomisEventId).isEqualTo(mapping.nomisEventId)
            assertThat(dpsOccurrenceId).isEqualTo(mapping.dpsOccurrenceId)
            assertThat(prisonerNumber).isEqualTo(mapping.offenderNo)
            assertThat(bookingId).isEqualTo(mapping.bookingId)
            assertThat(nomisAddressId).isEqualTo(mapping.nomisAddressId)
            assertThat(nomisAddressOwnerClass).isEqualTo(mapping.nomisAddressOwnerClass)
            assertThat(dpsAddressText).isEqualTo(mapping.dpsAddressText)
            assertThat(dpsDescription).isEqualTo(mapping.dpsDescription)
            assertThat(dpsPostcode).isEqualTo(mapping.dpsPostcode)
            assertThat(eventTime).isEqualTo(mapping.eventTime)
            assertThat(mappingType).isEqualTo(mapping.mappingType)
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() = runTest {
        webTestClient.getScheduledMovementSyncMapping(UUID.randomUUID())
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      val mapping = ScheduledMovementSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        77L,
        "some description",
        "S1 1AA",
        LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/scheduled-movement/dps-id/${mapping.dpsOccurrenceId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/scheduled-movement/dps-id/${mapping.dpsOccurrenceId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/scheduled-movement/dps-id/${mapping.dpsOccurrenceId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getScheduledMovementSyncMapping(dpsId: UUID) = get()
      .uri("/mapping/temporary-absence/scheduled-movement/dps-id/$dpsId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("DELETE /mapping/temporary-absence/scheduled-movement/nomis-event-id/{nomisEventId}")
  inner class DeleteNomisScheduledMovementMapping {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping1 = TemporaryAbsenceScheduleMapping(
        UUID.randomUUID(),
        23456L,
        "A1234BC",
        12345L,
        34567L,
        "CORP",
        "some address",
        77L,
        "some description",
        "S1 1AA",
        LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
        mappingType = MovementMappingType.NOMIS_CREATED,
      )
      val mapping2 = TemporaryAbsenceScheduleMapping(
        UUID.randomUUID(),
        65432L,
        "A1234BC",
        12345L,
        34567L,
        "CORP",
        "some address",
        77L,
        "some description",
        "S1 1AA",
        LocalDateTime.now(),
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should delete scheduled movement mapping by NOMIS ID`() = runTest {
        scheduleRepository.save(mapping1)
        scheduleRepository.save(mapping2)

        webTestClient.deleteScheduledMovementSyncMapping(mapping1.nomisEventId)
          .expectStatus().isNoContent

        assertThat(scheduleRepository.findByNomisEventId(mapping1.nomisEventId)).isNull()
        assertThat(scheduleRepository.findByNomisEventId(mapping2.nomisEventId)).isNotNull
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `delete endpoint should be idempotent`() = runTest {
        webTestClient.deleteScheduledMovementSyncMapping(12345L)
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/temporary-absence/scheduled-movement/nomis-event-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/temporary-absence/scheduled-movement/nomis-event-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/temporary-absence/scheduled-movement/nomis-event-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.deleteScheduledMovementSyncMapping(nomisEventId: Long) = delete()
      .uri("/mapping/temporary-absence/scheduled-movement/nomis-event-id/$nomisEventId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("POST /mapping/temporary-absence/external-movement")
  inner class CreateExternalMovementMapping {

    @AfterEach
    fun tearDown() = runTest {
      movementRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = ExternalMovementSyncMappingDto(
        "A1234BC",
        12345L,
        12,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        "some description",
        "S1 1AA",
        77L,
      )

      @Test
      fun `should create mapping`() = runTest {
        webTestClient.createExternalMovementSyncMapping(mapping)
          .expectStatus().isCreated

        with(movementRepository.findByNomisBookingIdAndNomisMovementSeq(12345L, 12)!!) {
          assertThat(offenderNo).isEqualTo("A1234BC")
          assertThat(nomisBookingId).isEqualTo(12345L)
          assertThat(nomisMovementSeq).isEqualTo(12)
          assertThat(dpsMovementId).isEqualTo(mapping.dpsMovementId)
          assertThat(mappingType).isEqualTo(MovementMappingType.NOMIS_CREATED)
          assertThat(nomisAddressId).isEqualTo(mapping.nomisAddressId)
          assertThat(nomisAddressOwnerClass).isEqualTo(mapping.nomisAddressOwnerClass)
          assertThat(dpsAddressText).isEqualTo(dpsAddressText)
        }
      }

      @Test
      fun `should create mapping with null address`() = runTest {
        webTestClient.createExternalMovementSyncMapping(mapping.copy(nomisAddressId = null, nomisAddressOwnerClass = null))
          .expectStatus().isCreated

        with(movementRepository.findByNomisBookingIdAndNomisMovementSeq(12345L, 12)!!) {
          assertThat(nomisAddressId).isNull()
          assertThat(nomisAddressOwnerClass).isNull()
        }
      }
    }

    @Nested
    inner class Validation {
      val mapping = ExternalMovementSyncMappingDto(
        "A1234BC",
        12345L,
        12,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        "some description",
        "S1 1AA",
        77L,
      )
      val duplicateMappingDps = ExternalMovementSyncMappingDto(
        "B2345CD",
        56789L,
        13,
        mapping.dpsMovementId,
        MovementMappingType.MIGRATED,
        34567L,
        "CORP",
        "some address",
        "some description",
        "S1 1AA",
        77L,
      )
      val duplicateMappingNomis = ExternalMovementSyncMappingDto(
        "C3456DE",
        mapping.bookingId,
        mapping.nomisMovementSeq,
        UUID.randomUUID(),
        MovementMappingType.MIGRATED,
        // checking these don't affect the duplicate check
        1,
        "A",
        "a",
        "some description",
        "S1 1AA",
        77L,
      )

      @Test
      fun `should reject duplicate DPS ID mapping`() = runTest {
        webTestClient.createExternalMovementSyncMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createExternalMovementSyncMapping(duplicateMappingDps)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsMovementId", mapping.dpsMovementId.toString())
              .containsEntry("nomisMovementSeq", mapping.nomisMovementSeq)
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingDps.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingDps.bookingId.toInt())
              .containsEntry("dpsMovementId", duplicateMappingDps.dpsMovementId.toString())
              .containsEntry("nomisMovementSeq", duplicateMappingDps.nomisMovementSeq)
              .containsEntry("mappingType", duplicateMappingDps.mappingType.toString())
          }
      }

      @Test
      fun `should reject duplicate NOMIS ID mapping`() = runTest {
        webTestClient.createExternalMovementSyncMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createExternalMovementSyncMapping(duplicateMappingNomis)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsMovementId", mapping.dpsMovementId.toString())
              .containsEntry("nomisMovementSeq", mapping.nomisMovementSeq)
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingNomis.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingNomis.bookingId.toInt())
              .containsEntry("dpsMovementId", duplicateMappingNomis.dpsMovementId.toString())
              .containsEntry("nomisMovementSeq", duplicateMappingNomis.nomisMovementSeq)
              .containsEntry("mappingType", duplicateMappingNomis.mappingType.toString())
          }
      }
    }

    @Nested
    inner class Security {
      val mapping = ExternalMovementSyncMappingDto(
        "A1234BC",
        12345L,
        12,
        UUID.randomUUID(),
        mappingType = MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        "some description",
        "S1 1AA",
        77L,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/external-movement")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/external-movement")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/external-movement")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.createExternalMovementSyncMapping(mapping: ExternalMovementSyncMappingDto) = post()
      .uri("/mapping/temporary-absence/external-movement")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
  @DisplayName("PUT /mapping/temporary-absence/external-movement")
  inner class UpdateExternalMovementMapping {

    @AfterEach
    fun tearDown() = runTest {
      movementRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = ExternalMovementSyncMappingDto(
        "A1234BC",
        12345L,
        12,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        "some description",
        "S1 1AA",
        77L,
      )

      @Test
      fun `should update mapping`() = runTest {
        webTestClient.createExternalMovementSyncMapping(mapping)
          .expectStatus().isCreated

        webTestClient.updateExternalMovementSyncMapping(
          mapping.copy(
            nomisAddressId = 77777L,
            nomisAddressOwnerClass = "OFF",
            dpsAddressText = "a different address",
          ),
        ).expectStatus().isOk

        with(movementRepository.findByNomisBookingIdAndNomisMovementSeq(12345L, 12)!!) {
          assertThat(offenderNo).isEqualTo("A1234BC")
          assertThat(nomisBookingId).isEqualTo(12345L)
          assertThat(nomisMovementSeq).isEqualTo(12)
          assertThat(dpsMovementId).isEqualTo(mapping.dpsMovementId)
          assertThat(mappingType).isEqualTo(MovementMappingType.NOMIS_CREATED)
          assertThat(nomisAddressId).isEqualTo(77777L)
          assertThat(nomisAddressOwnerClass).isEqualTo("OFF")
          assertThat(dpsAddressText).isEqualTo("a different address")
        }
      }
    }

    @Nested
    inner class Validation {
      val mapping = ExternalMovementSyncMappingDto(
        "A1234BC",
        12345L,
        12,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        "some description",
        "S1 1AA",
        77L,
      )

      @Test
      fun `should return not found if mapping does not exist`() = runTest {
        webTestClient.updateExternalMovementSyncMapping(mapping)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      val mapping = ExternalMovementSyncMappingDto(
        "A1234BC",
        12345L,
        12,
        UUID.randomUUID(),
        mappingType = MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        "some description",
        "S1 1AA",
        77L,
      )

      @BeforeEach
      fun setUp() = runTest {
        webTestClient.createExternalMovementSyncMapping(mapping)
          .expectStatus().isCreated
      }

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/temporary-absence/external-movement")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/temporary-absence/external-movement")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/temporary-absence/external-movement")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.createExternalMovementSyncMapping(mapping: ExternalMovementSyncMappingDto) = post()
      .uri("/mapping/temporary-absence/external-movement")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()

    private fun WebTestClient.updateExternalMovementSyncMapping(mapping: ExternalMovementSyncMappingDto) = put()
      .uri("/mapping/temporary-absence/external-movement")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/temporary-absence/external-movement/nomis-movement-id/{bookingId}/{movementSeq}")
  inner class GetNomisExternalMovementMapping {

    @AfterEach
    fun tearDown() = runTest {
      movementRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TemporaryAbsenceMovementMapping(
        UUID.randomUUID(),
        12345L,
        12,
        "A1234BC",
        34567L,
        "CORP",
        "some address",
        dpsUprn = 77L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should get external movement mapping by NOMIS booking ID and movement sequence`() = runTest {
        movementRepository.save(mapping)

        webTestClient.getExternalMovementSyncMapping(mapping.nomisBookingId, mapping.nomisMovementSeq)
          .expectStatus().isOk
          .expectBody(object : ParameterizedTypeReference<ExternalMovementSyncMappingDto>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(bookingId).isEqualTo(mapping.nomisBookingId)
            assertThat(nomisMovementSeq).isEqualTo(mapping.nomisMovementSeq)
            assertThat(dpsMovementId).isEqualTo(mapping.dpsMovementId)
            assertThat(prisonerNumber).isEqualTo(mapping.offenderNo)
            assertThat(nomisAddressId).isEqualTo(mapping.nomisAddressId)
            assertThat(nomisAddressOwnerClass).isEqualTo(mapping.nomisAddressOwnerClass)
            assertThat(dpsAddressText).isEqualTo(mapping.dpsAddressText)
            assertThat(mappingType).isEqualTo(mapping.mappingType)
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() = runTest {
        webTestClient.getExternalMovementSyncMapping(12345L, 23456)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/external-movement/nomis-movement-id/12345/23456")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/external-movement/nomis-movement-id/12345/23456")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/external-movement/nomis-movement-id/12345/23456")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getExternalMovementSyncMapping(bookingId: Long, movementSeq: Int) = get()
      .uri("/mapping/temporary-absence/external-movement/nomis-movement-id/$bookingId/$movementSeq")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/temporary-absence/external-movement/dps-id/{dpsId}")
  inner class GetDpsExternalMovementMapping {

    @AfterEach
    fun tearDown() = runTest {
      movementRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TemporaryAbsenceMovementMapping(
        UUID.randomUUID(),
        12345L,
        12,
        "A1234BC",
        34567L,
        "CORP",
        "some address",
        dpsUprn = 77L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should get external movement mapping by DPS ID`() = runTest {
        movementRepository.save(mapping)

        webTestClient.getExternalMovementSyncMapping(mapping.dpsMovementId)
          .expectStatus().isOk
          .expectBody(object : ParameterizedTypeReference<ExternalMovementSyncMappingDto>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(bookingId).isEqualTo(mapping.nomisBookingId)
            assertThat(nomisMovementSeq).isEqualTo(mapping.nomisMovementSeq)
            assertThat(dpsMovementId).isEqualTo(mapping.dpsMovementId)
            assertThat(prisonerNumber).isEqualTo(mapping.offenderNo)
            assertThat(nomisAddressId).isEqualTo(mapping.nomisAddressId)
            assertThat(nomisAddressOwnerClass).isEqualTo(mapping.nomisAddressOwnerClass)
            assertThat(dpsAddressText).isEqualTo(mapping.dpsAddressText)
            assertThat(mappingType).isEqualTo(mapping.mappingType)
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() = runTest {
        webTestClient.getExternalMovementSyncMapping(UUID.randomUUID())
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/external-movement/dps-id/${UUID.randomUUID()}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/external-movement/dps-id/${UUID.randomUUID()}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/external-movement/dps-id/${UUID.randomUUID()}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getExternalMovementSyncMapping(dpsId: UUID) = get()
      .uri("/mapping/temporary-absence/external-movement/dps-id/$dpsId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("DELETE /mapping/temporary-absence/external-movement/nomis-movement-id/{bookingId}/{movementSeq}")
  inner class DeleteNomisExternalMovementMapping {

    @AfterEach
    fun tearDown() = runTest {
      movementRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping1 = TemporaryAbsenceMovementMapping(
        UUID.randomUUID(),
        12345L,
        12,
        "A1234BC",
        34567L,
        "CORP",
        "some address",
        dpsUprn = 77L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )
      val mapping2 = TemporaryAbsenceMovementMapping(
        UUID.randomUUID(),
        12345L,
        13,
        "A1234BC",
        34567L,
        "CORP",
        "some address",
        dpsUprn = 77L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should delete external movement mapping by NOMIS booking ID and movement sequence`() = runTest {
        movementRepository.save(mapping1)
        movementRepository.save(mapping2)

        webTestClient.deleteExternalMovementSyncMapping(mapping1.nomisBookingId, mapping1.nomisMovementSeq)
          .expectStatus().isNoContent

        assertThat(movementRepository.findByNomisBookingIdAndNomisMovementSeq(mapping1.nomisBookingId, mapping1.nomisMovementSeq)).isNull()
        assertThat(movementRepository.findByNomisBookingIdAndNomisMovementSeq(mapping2.nomisBookingId, mapping2.nomisMovementSeq)).isNotNull
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `delete endpoint should be idempotent`() = runTest {
        webTestClient.deleteExternalMovementSyncMapping(12345L, 12)
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/temporary-absence/external-movement/nomis-movement-id/12345/12")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/temporary-absence/external-movement/nomis-movement-id/12345/12")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/temporary-absence/external-movement/nomis-movement-id/12345/12")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.deleteExternalMovementSyncMapping(bookingId: Long, movementSeq: Int) = delete()
      .uri("/mapping/temporary-absence/external-movement/nomis-movement-id/$bookingId/$movementSeq")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/temporary-absence/scheduled-movements/nomis-address-id/{nomisAddressId}")
  inner class FindFutureSchedulesByAddressId {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @BeforeEach
    fun setUp() = runTest {
      scheduleRepository.save(aMapping(eventIdYesterday, addressIdUpdated, yesterday))
      scheduleRepository.save(aMapping(eventIdEarlierToday, addressIdUpdated, startOfToday))
      scheduleRepository.save(aMapping(eventIdLaterToday, addressIdUpdated, now.plusHours(1)))
      scheduleRepository.save(aMapping(eventIdTomorrow, addressIdUpdated, tomorrow))
      scheduleRepository.save(aMapping(eventIdWrongAddress, addressIdNotUpdated, tomorrow))
      scheduleRepository.save(aMapping(eventIdMissingAddress, null, tomorrow))
    }

    private val eventIdYesterday = 1L
    private val eventIdEarlierToday = 2L
    private val eventIdLaterToday = 3L
    private val eventIdTomorrow = 4L
    private val eventIdWrongAddress = 5L
    private val eventIdMissingAddress = 6L
    private val addressIdUpdated = 11L
    private val addressIdNotUpdated = 12L
    private val now = LocalDateTime.now()
    private val yesterday = now.minusDays(1)
    private val startOfToday = now.truncatedTo(ChronoUnit.DAYS)
    private val tomorrow = now.plusDays(1)

    private fun aMapping(eventId: Long, addressId: Long?, eventTime: LocalDateTime) = TemporaryAbsenceScheduleMapping(
      dpsOccurrenceId = UUID.randomUUID(),
      nomisEventId = eventId,
      offenderNo = "A1234AA",
      bookingId = 12345L,
      nomisAddressId = addressId,
      nomisAddressOwnerClass = "CORP",
      dpsAddressText = "some address",
      eventTime = eventTime,
      dpsUprn = 77L,
      mappingType = MovementMappingType.NOMIS_CREATED,
    )

    @Nested
    inner class EventSelection {
      @Test
      fun `should find events from later today`() = runTest {
        webTestClient.findScheduledMovementsForAddress(addressIdUpdated)
          .apply {
            assertThat(scheduleMappings.map { it.nomisEventId }).contains(eventIdLaterToday)
          }
      }

      @Test
      fun `should find events from earlier today`() = runTest {
        webTestClient.findScheduledMovementsForAddress(addressIdUpdated)
          .apply {
            assertThat(scheduleMappings.map { it.nomisEventId }).contains(eventIdEarlierToday)
          }
      }

      @Test
      fun `should find events from tomorrow`() = runTest {
        webTestClient.findScheduledMovementsForAddress(addressIdUpdated)
          .apply {
            assertThat(scheduleMappings.map { it.nomisEventId }).contains(eventIdTomorrow)
          }
      }

      @Test
      fun `should find handle different date`() = runTest {
        webTestClient.findScheduledMovementsForAddress(addressIdUpdated)
          .apply {
            assertThat(scheduleMappings.map { it.nomisEventId }).contains(eventIdTomorrow)
          }
      }

      @Test
      fun `should not find events from yesterday`() = runTest {
        webTestClient.findScheduledMovementsForAddress(addressIdUpdated)
          .apply {
            assertThat(scheduleMappings.map { it.nomisEventId }).doesNotContain(eventIdYesterday)
          }
      }

      @Test
      fun `should not find events with a different address`() = runTest {
        webTestClient.findScheduledMovementsForAddress(addressIdUpdated)
          .apply {
            assertThat(scheduleMappings.map { it.nomisEventId }).doesNotContain(eventIdWrongAddress)
          }
      }

      @Test
      fun `should not find events without an address`() = runTest {
        webTestClient.findScheduledMovementsForAddress(addressIdUpdated)
          .apply {
            assertThat(scheduleMappings.map { it.nomisEventId }).doesNotContain(eventIdMissingAddress)
          }
      }

      @Test
      fun `should find events from a different date`() = runTest {
        webTestClient.findScheduledMovementsForAddress(addressIdUpdated, fromDate = yesterday.toLocalDate())
          .apply {
            assertThat(scheduleMappings.map { it.nomisEventId }).contains(eventIdYesterday)
          }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/scheduled-movements/nomis-address-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/scheduled-movements/nomis-address-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/scheduled-movements/nomis-address-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.findScheduledMovementsForAddress(nomisAddressId: Long, fromDate: LocalDate? = null) = get()
      .uri {
        it.path("/mapping/temporary-absence/scheduled-movements/nomis-address-id/{nomisAddressId}")
          .apply { fromDate?.let { queryParam("fromDate", fromDate) } }
          .build(nomisAddressId)
      }
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
      .expectStatus().isOk
      .expectBody<FindScheduledMovementsForAddressResponse>()
      .returnResult().responseBody!!
  }

  @DisplayName("GET /mapping/temporary-absence/migration-id/{migrationId}")
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
        webTestClient.get().uri("/mapping/temporary-absence/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/temporary-absence/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/temporary-absence/migration-id/2022-01-01T00:00:00")
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
            TemporaryAbsenceMigration(
              offenderNo = "any$it",
              label = "2023-01-01T12:45:12",
            ),
          )
        }

        migrationRepository.save(
          TemporaryAbsenceMigration(
            offenderNo = "different",
            label = "2022-02-02T12:45:12",
          ),
        )

        webTestClient.get().uri("/mapping/temporary-absence/migration-id/2023-01-01T12:45:12")
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
          TemporaryAbsenceMigration(
            offenderNo = "any",
            label = "2023-01-01T12:45:12",
            whenCreated = now,
          ),
        )

        webTestClient.get().uri("/mapping/temporary-absence/migration-id/2023-01-01T12:45:12")
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

  @Nested
  @DisplayName("GET /mapping/temporary-absence/move-booking/{bookingId}")
  inner class GetMappingsForMoveBooking {
    private val bookingId1 = 1L
    private val bookingId2 = 2L
    private val book1app1 = 3L
    private val book1app2 = 4L
    private val book1seq1 = 5
    private val book1seq2 = 6
    private val book2app1 = 7L
    private val book2seq1 = 8
    private val book1auth1 = UUID.randomUUID()
    private val book1auth2 = UUID.randomUUID()
    private val book1move1 = UUID.randomUUID()
    private val book1move2 = UUID.randomUUID()
    private val book2auth1 = UUID.randomUUID()
    private val book2move1 = UUID.randomUUID()

    @AfterEach
    fun tearDown() = runTest {
      applicationRepository.deleteAll()
      movementRepository.deleteAll()
    }

    @BeforeEach
    fun setUp() = runTest {
      applicationRepository.save(anApplicationMapping(bookingId1, book1app1, book1auth1))
      applicationRepository.save(anApplicationMapping(bookingId1, book1app2, book1auth2))
      applicationRepository.save(anApplicationMapping(bookingId2, book2app1, book2auth1))

      movementRepository.save(aMovementMapping(bookingId1, book1seq1, book1move1))
      movementRepository.save(aMovementMapping(bookingId1, book1seq2, book1move2))
      movementRepository.save(aMovementMapping(bookingId2, book2seq1, book2move1))
    }

    private fun anApplicationMapping(bookingId: Long, applicationId: Long, authorisationId: UUID) = TemporaryAbsenceApplicationMapping(
      nomisApplicationId = applicationId,
      dpsApplicationId = authorisationId,
      offenderNo = "A1234AB",
      bookingId = bookingId,
      mappingType = MovementMappingType.NOMIS_CREATED,
    )

    private fun aMovementMapping(bookingId: Long, movementSeq: Int, movementId: UUID) = TemporaryAbsenceMovementMapping(
      dpsMovementId = movementId,
      nomisBookingId = bookingId,
      nomisMovementSeq = movementSeq,
      offenderNo = "A1234AB",
      mappingType = MovementMappingType.NOMIS_CREATED,
      nomisAddressId = null,
      nomisAddressOwnerClass = null,
      dpsAddressText = "some address",
      dpsUprn = null,
    )

    @Nested
    inner class HappyPath {
      @Test
      fun `should find booking IDs`() = runTest {
        webTestClient.getMoveBookingIds(bookingId1)
          .apply {
            assertThat(applicationIds).containsExactlyInAnyOrder(
              TemporaryAbsenceApplicationIdMapping(book1app1, book1auth1),
              TemporaryAbsenceApplicationIdMapping(book1app2, book1auth2),
            )
            assertThat(movementIds).containsExactlyInAnyOrder(
              TemporaryAbsenceMovementIdMapping(book1seq1, book1move1),
              TemporaryAbsenceMovementIdMapping(book1seq2, book1move2),
            )
          }
      }

      @Test
      fun `should return empty lists if there are no mappings`() = runTest {
        webTestClient.getMoveBookingIds(99)
          .apply {
            assertThat(applicationIds).isEmpty()
            assertThat(movementIds).isEmpty()
          }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/move-booking/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/move-booking/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/move-booking/1")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getMoveBookingIds(bookingId: Long = 1L) = get()
      .uri("/mapping/temporary-absence/move-booking/$bookingId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
      .expectStatus().isOk
      .expectBody<TemporaryAbsenceMoveBookingMappingDto>()
      .returnResult().responseBody!!
  }

  @Nested
  @DisplayName("PUT /mapping/temporary-absence/move-booking/{bookingId}/from/{fromOffenderNo}/to/{toOffenderNo}")
  inner class UpdateMappingsForMoveBooking {
    private val fromOffenderNo = "A1234AB"
    private val toOffenderNo = "A9876BA"

    private val bookingId1 = 1L
    private val bookingId2 = 2L
    private val book1app1 = 3L
    private val book1app2 = 4L
    private val book1event1 = 5L
    private val book1event2 = 6L
    private val book1seq1 = 7
    private val book1seq2 = 8
    private val book2app1 = 9L
    private val book2seq1 = 10
    private val book2event1 = 11L
    private val book1auth1 = UUID.randomUUID()
    private val book1auth2 = UUID.randomUUID()
    private val book1occ1 = UUID.randomUUID()
    private val book1occ2 = UUID.randomUUID()
    private val book1move1 = UUID.randomUUID()
    private val book1move2 = UUID.randomUUID()
    private val book2auth1 = UUID.randomUUID()
    private val book2occ1 = UUID.randomUUID()
    private val book2move1 = UUID.randomUUID()

    @AfterEach
    fun tearDown() = runTest {
      applicationRepository.deleteAll()
      scheduleRepository.deleteAll()
      movementRepository.deleteAll()
    }

    @BeforeEach
    fun setUp() = runTest {
      applicationRepository.save(anApplicationMapping(bookingId1, book1app1, book1auth1))
      applicationRepository.save(anApplicationMapping(bookingId1, book1app2, book1auth2))
      applicationRepository.save(anApplicationMapping(bookingId2, book2app1, book2auth1))

      scheduleRepository.save(aScheduleMapping(bookingId1, book1event1, book1occ1))
      scheduleRepository.save(aScheduleMapping(bookingId1, book1event2, book1occ2))
      scheduleRepository.save(aScheduleMapping(bookingId2, book2event1, book2occ1))

      movementRepository.save(aMovementMapping(bookingId1, book1seq1, book1move1))
      movementRepository.save(aMovementMapping(bookingId1, book1seq2, book1move2))
      movementRepository.save(aMovementMapping(bookingId2, book2seq1, book2move1))
    }

    private fun anApplicationMapping(bookingId: Long, applicationId: Long, authorisationId: UUID) = TemporaryAbsenceApplicationMapping(
      nomisApplicationId = applicationId,
      dpsApplicationId = authorisationId,
      offenderNo = "A1234AB",
      bookingId = bookingId,
      mappingType = MovementMappingType.NOMIS_CREATED,
    )

    private fun aScheduleMapping(bookingId: Long, eventId: Long, occurrenceId: UUID) = TemporaryAbsenceScheduleMapping(
      dpsOccurrenceId = occurrenceId,
      nomisEventId = eventId,
      offenderNo = "A1234AB",
      bookingId = bookingId,
      mappingType = MovementMappingType.NOMIS_CREATED,
      nomisAddressId = null,
      nomisAddressOwnerClass = null,
      dpsAddressText = "some address",
      dpsUprn = null,
      eventTime = LocalDateTime.now(),
    )

    private fun aMovementMapping(bookingId: Long, movementSeq: Int, movementId: UUID) = TemporaryAbsenceMovementMapping(
      dpsMovementId = movementId,
      nomisBookingId = bookingId,
      nomisMovementSeq = movementSeq,
      offenderNo = "A1234AB",
      mappingType = MovementMappingType.NOMIS_CREATED,
      nomisAddressId = null,
      nomisAddressOwnerClass = null,
      dpsAddressText = "some address",
      dpsUprn = null,
    )

    @Nested
    inner class HappyPath {
      @Test
      fun `should move all mappings to new booking`() = runTest {
        webTestClient.moveBookingIdsOk(bookingId1, fromOffenderNo, toOffenderNo)

        with(applicationRepository.findByBookingId(1)) {
          forEach { assertThat(it.offenderNo).isEqualTo(toOffenderNo) }
        }
        with(scheduleRepository.findByBookingId(1)) {
          forEach { assertThat(it.offenderNo).isEqualTo(toOffenderNo) }
        }
        with(movementRepository.findByNomisBookingId(1)) {
          forEach { assertThat(it.offenderNo).isEqualTo(toOffenderNo) }
        }
      }

      @Test
      fun `should leave other bookings alone`() = runTest {
        webTestClient.moveBookingIdsOk(bookingId1, fromOffenderNo, toOffenderNo)

        with(applicationRepository.findByBookingId(2)) {
          forEach { assertThat(it.offenderNo).isEqualTo(fromOffenderNo) }
        }
        with(scheduleRepository.findByBookingId(2)) {
          forEach { assertThat(it.offenderNo).isEqualTo(fromOffenderNo) }
        }
        with(movementRepository.findByNomisBookingId(2)) {
          forEach { assertThat(it.offenderNo).isEqualTo(fromOffenderNo) }
        }
      }

      @Test
      fun `should return OK if all bookings already on the to offender`() = runTest {
        webTestClient.moveBookingIdsOk(bookingId1, "any", fromOffenderNo)
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `should return not found if booking not mapped`() {
        webTestClient.moveBookingIds(9999, fromOffenderNo, toOffenderNo)
          .expectStatus().isNotFound
      }

      @Test
      fun `should return bad request if bookings exist for a different offender`() {
        webTestClient.moveBookingIds(bookingId1, "A8888AA", toOffenderNo)
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/temporary-absence/move-booking/1/from/A1234AB/to/A9876BA")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/temporary-absence/move-booking/1/from/A1234AB/to/A9876BA")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/temporary-absence/move-booking/1/from/A1234AB/to/A9876BA")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.moveBookingIds(bookingId: Long = 1L, from: String = "A1234AB", to: String = "A9876BA") = put()
      .uri("/mapping/temporary-absence/move-booking/$bookingId/from/$from/to/$to")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()

    private fun WebTestClient.moveBookingIdsOk(bookingId: Long = 1L, from: String = "A1234AB", to: String = "A9876BA") = moveBookingIds(bookingId, from, to)
      .expectStatus().isOk
  }
}
