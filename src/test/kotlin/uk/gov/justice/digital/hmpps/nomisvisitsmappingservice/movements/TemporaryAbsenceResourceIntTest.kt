package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.movements

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.isDuplicateMapping
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TemporaryAbsenceResourceIntTest(
  @Autowired private val applicationRepository: TemporaryAbsenceApplicationRepository,
  @Autowired private val appMultiRepository: TemporaryAbsenceAppMultiRepository,
  @Autowired private val scheduleRepository: TemporaryAbsenceScheduleRepository,
  @Autowired private val movementRepository: TemporaryAbsenceMovementRepository,
  @Autowired private val migrationRepository: TemporaryAbsenceMigrationRepository,
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
      fun setUp() {
        saveMappings()
      }

      fun saveMappings(mappings: TemporaryAbsencesPrisonerMappingDto = mappingsRequest()) {
        webTestClient.put()
          .uri("/mapping/temporary-absence/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_MOVEMENTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated
      }

      fun mappingsRequest(
        dpsApplicationId: UUID = DPS_APPLICATION_ID,
        dpsOutsideMovementId: UUID = DPS_OUTSIDE_MOVEMENT_ID,
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
                outsideMovements = listOf(
                  TemporaryAbsencesOutsideMovementMappingDto(
                    nomisMovementApplicationMultiId = NOMIS_APPLICATION_MULTI_ID,
                    dpsOutsideMovementId = dpsOutsideMovementId,
                  ),
                ),
                schedules = listOf(
                  ScheduledMovementMappingDto(
                    nomisEventId = NOMIS_SCHEDULED_OUT_EVENT_ID,
                    dpsScheduledMovementId = dpsScheduledOutId,
                  ),
                  ScheduledMovementMappingDto(
                    nomisEventId = NOMIS_SCHEDULED_IN_EVENT_ID,
                    dpsScheduledMovementId = dpsScheduledInId,
                  ),
                ),
                movements = listOf(
                  ExternalMovementMappingDto(
                    nomisMovementSeq = NOMIS_MOVEMENT_OUT_SEQ,
                    dpsExternalMovementId = dpsMovementOutId,
                  ),
                  ExternalMovementMappingDto(
                    nomisMovementSeq = NOMIS_MOVEMENT_IN_SEQ,
                    dpsExternalMovementId = dpsMovementInId,
                  ),
                ),
              ),
            ),
            unscheduledMovements = listOf(
              ExternalMovementMappingDto(
                nomisMovementSeq = NOMIS_UNSCHEDULED_MOVEMENT_OUT_SEQ,
                dpsExternalMovementId = dpsUnscheduledMovementOutId,
              ),
              ExternalMovementMappingDto(
                nomisMovementSeq = NOMIS_UNSCHEDULED_MOVEMENT_IN_SEQ,
                dpsExternalMovementId = dpsUnscheduledMovementInId,
              ),
            ),
          ),
        ),
      )

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
      fun `should save application outside movement mappings`() = runTest {
        with(appMultiRepository.findById(DPS_OUTSIDE_MOVEMENT_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(bookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisAppMultiId).isEqualTo(NOMIS_APPLICATION_MULTI_ID)
          assertThat(dpsAppMultiId).isEqualTo(DPS_OUTSIDE_MOVEMENT_ID)
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
          assertThat(nomisScheduleId).isEqualTo(NOMIS_SCHEDULED_OUT_EVENT_ID)
          assertThat(dpsScheduleId).isEqualTo(DPS_SCHEDULED_OUT_ID)
          assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
        }
        with(scheduleRepository.findById(DPS_SCHEDULED_IN_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(bookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisScheduleId).isEqualTo(NOMIS_SCHEDULED_IN_EVENT_ID)
          assertThat(dpsScheduleId).isEqualTo(DPS_SCHEDULED_IN_ID)
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
          assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
        }
        with(movementRepository.findById(DPS_MOVEMENT_IN_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_MOVEMENT_IN_SEQ)
          assertThat(dpsMovementId).isEqualTo(DPS_MOVEMENT_IN_ID)
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
          assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
        }
        with(movementRepository.findById(DPS_UNSCHEDULED_MOVEMENT_IN_ID)!!) {
          assertThat(label).isEqualTo(MIGRATION_ID)
          assertThat(offenderNo).isEqualTo(NOMIS_OFFENDER_NO)
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisMovementSeq).isEqualTo(NOMIS_UNSCHEDULED_MOVEMENT_IN_SEQ)
          assertThat(dpsMovementId).isEqualTo(DPS_UNSCHEDULED_MOVEMENT_IN_ID)
          assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
        }
      }

      @Test
      fun `should recreate mappings if they already exist`() = runTest {
        val dpsApplicationId = UUID.randomUUID()
        val dpsOutsideMovementId = UUID.randomUUID()
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
            dpsOutsideMovementId,
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
      .headers(setAuthorisation(roles = listOf("NOMIS_MOVEMENTS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
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
      .headers(setAuthorisation(roles = listOf("NOMIS_MOVEMENTS")))
      .exchange()
  }

  @Nested
  inner class CreateOutsideMovementMapping {

    @AfterEach
    fun tearDown() = runTest {
      appMultiRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TemporaryAbsenceOutsideMovementSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should create mapping`() = runTest {
        webTestClient.createOutsideMovementSyncMapping(mapping)
          .expectStatus().isCreated

        with(appMultiRepository.findByNomisAppMultiId(23456L)!!) {
          assertThat(offenderNo).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(12345L)
          assertThat(dpsAppMultiId).isEqualTo(mapping.dpsOutsideMovementId)
          assertThat(mappingType).isEqualTo(MovementMappingType.NOMIS_CREATED)
        }
      }
    }

    @Nested
    inner class Validation {
      val mapping = TemporaryAbsenceOutsideMovementSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
      )
      val duplicateMappingDps = TemporaryAbsenceOutsideMovementSyncMappingDto(
        "B2345CD",
        56789L,
        34567L,
        mapping.dpsOutsideMovementId,
        MovementMappingType.MIGRATED,
      )
      val duplicateMappingNomis = TemporaryAbsenceOutsideMovementSyncMappingDto(
        "C3456DE",
        9101112L,
        mapping.nomisMovementApplicationMultiId,
        UUID.randomUUID(),
        MovementMappingType.MIGRATED,
      )

      @Test
      fun `should reject duplicate DPS ID mapping`() = runTest {
        webTestClient.createOutsideMovementSyncMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createOutsideMovementSyncMapping(duplicateMappingDps)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsOutsideMovementId", mapping.dpsOutsideMovementId.toString())
              .containsEntry("nomisMovementApplicationMultiId", mapping.nomisMovementApplicationMultiId.toInt())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingDps.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingDps.bookingId.toInt())
              .containsEntry("dpsOutsideMovementId", duplicateMappingDps.dpsOutsideMovementId.toString())
              .containsEntry("nomisMovementApplicationMultiId", duplicateMappingDps.nomisMovementApplicationMultiId.toInt())
              .containsEntry("mappingType", duplicateMappingDps.mappingType.toString())
          }
      }

      @Test
      fun `should reject duplicate NOMIS ID mapping`() = runTest {
        webTestClient.createOutsideMovementSyncMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createOutsideMovementSyncMapping(duplicateMappingNomis)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsOutsideMovementId", mapping.dpsOutsideMovementId.toString())
              .containsEntry("nomisMovementApplicationMultiId", mapping.nomisMovementApplicationMultiId.toInt())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingNomis.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingNomis.bookingId.toInt())
              .containsEntry("dpsOutsideMovementId", duplicateMappingNomis.dpsOutsideMovementId.toString())
              .containsEntry("nomisMovementApplicationMultiId", duplicateMappingNomis.nomisMovementApplicationMultiId.toInt())
              .containsEntry("mappingType", duplicateMappingNomis.mappingType.toString())
          }
      }
    }

    @Nested
    inner class Security {
      val mapping = TemporaryAbsenceOutsideMovementSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/outside-movement")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/outside-movement")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/outside-movement")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.createOutsideMovementSyncMapping(mapping: TemporaryAbsenceOutsideMovementSyncMappingDto) = post()
      .uri("/mapping/temporary-absence/outside-movement")
      .headers(setAuthorisation(roles = listOf("NOMIS_MOVEMENTS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
  inner class GetNomisOutsideMovementMapping {

    @AfterEach
    fun tearDown() = runTest {
      appMultiRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TemporaryAbsenceAppMultiMapping(
        UUID.randomUUID(),
        23456L,
        "A1234BC",
        12345L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should get outside movement mapping by NOMIS ID`() = runTest {
        appMultiRepository.save(mapping)

        webTestClient.getOutsideMovementSyncMapping(mapping.nomisAppMultiId)
          .expectStatus().isOk
          .expectBody(object : ParameterizedTypeReference<TemporaryAbsenceOutsideMovementSyncMappingDto>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(nomisMovementApplicationMultiId).isEqualTo(mapping.nomisAppMultiId)
            assertThat(dpsOutsideMovementId).isEqualTo(mapping.dpsAppMultiId)
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
        webTestClient.getOutsideMovementSyncMapping(12345L)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      val mapping = TemporaryAbsenceOutsideMovementSyncMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/outside-movement/nomis-application-multi-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/outside-movement/nomis-application-multi-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/temporary-absence/outside-movement/nomis-application-multi-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getOutsideMovementSyncMapping(nomisApplicationMultiId: Long) = get()
      .uri("/mapping/temporary-absence/outside-movement/nomis-application-multi-id/$nomisApplicationMultiId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MOVEMENTS")))
      .exchange()
  }
}
