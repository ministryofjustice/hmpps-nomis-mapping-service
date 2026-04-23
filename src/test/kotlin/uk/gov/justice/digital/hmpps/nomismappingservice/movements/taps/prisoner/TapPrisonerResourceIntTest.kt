package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.prisoner

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
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application.MovementMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application.TapApplicationMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application.TapApplicationRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.movement.TapMovementMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.movement.TapMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapApplicationIdMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapApplicationMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapBookingMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapMoveBookingMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapMovementIdMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapMovementMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapPrisonerMappingIdsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapPrisonerMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender.TapScheduleMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.schedule.TapScheduleMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.schedule.TapScheduleRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class TapPrisonerResourceIntTest(
  @Autowired private val applicationRepository: TapApplicationRepository,
  @Autowired private val scheduleRepository: TapScheduleRepository,
  @Autowired private val movementRepository: TapMovementRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /mapping/taps/{prisonerNumber}/ids")
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
                  nomisAddressId = null,
                  nomisAddressOwnerClass = null,
                  dpsAddressText = "",
                  dpsDescription = null,
                  dpsPostcode = null,
                  eventTime = EVENT_TIME,
                ),
                TapScheduleMappingsDto(
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
                TapMovementMappingsDto(
                  nomisMovementSeq = NOMIS_MOVEMENT_OUT_SEQ,
                  dpsMovementId = dpsMovementOutId,
                  nomisAddressId = null,
                  nomisAddressOwnerClass = null,
                  dpsAddressText = "",
                  dpsDescription = null,
                  dpsPostcode = null,
                ),
                TapMovementMappingsDto(
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
            TapMovementMappingsDto(
              nomisMovementSeq = NOMIS_UNSCHEDULED_MOVEMENT_OUT_SEQ,
              dpsMovementId = dpsUnscheduledMovementOutId,
              nomisAddressId = null,
              nomisAddressOwnerClass = null,
              dpsAddressText = "",
              dpsDescription = null,
              dpsPostcode = null,
            ),
            TapMovementMappingsDto(
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
      private lateinit var allMappings: TapPrisonerMappingIdsDto

      @BeforeEach
      fun setUp() {
        saveMappings()

        allMappings = webTestClient.get()
          .uri("/mapping/taps/$NOMIS_OFFENDER_NO/ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<TapPrisonerMappingIdsDto>()
          .returnResult().responseBody!!
      }

      @Test
      fun `should get application mappings`() = runTest {
        assertThat(allMappings.applications[0].nomisApplicationId).isEqualTo(NOMIS_APPLICATION_ID)
        assertThat(allMappings.applications[0].dpsAuthorisationId).isEqualTo(DPS_APPLICATION_ID)
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
          .uri("/mapping/taps/UNKNOWN/ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<TapPrisonerMappingIdsDto>()
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
          .uri("/mapping/taps/$NOMIS_OFFENDER_NO/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/taps/$NOMIS_OFFENDER_NO/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/taps/$NOMIS_OFFENDER_NO/ids")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/taps/move-booking/{bookingId}")
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

    private fun anApplicationMapping(bookingId: Long, applicationId: Long, authorisationId: UUID) = TapApplicationMapping(
      nomisApplicationId = applicationId,
      dpsAuthorisationId = authorisationId,
      offenderNo = "A1234AB",
      bookingId = bookingId,
      mappingType = MovementMappingType.NOMIS_CREATED,
    )

    private fun aMovementMapping(bookingId: Long, movementSeq: Int, movementId: UUID) = TapMovementMapping(
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
              TapApplicationIdMapping(book1app1, book1auth1),
              TapApplicationIdMapping(book1app2, book1auth2),
            )
            assertThat(movementIds).containsExactlyInAnyOrder(
              TapMovementIdMapping(book1seq1, book1move1),
              TapMovementIdMapping(book1seq2, book1move2),
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
          .uri("/mapping/taps/move-booking/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/taps/move-booking/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/taps/move-booking/1")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getMoveBookingIds(bookingId: Long = 1L) = get()
      .uri("/mapping/taps/move-booking/$bookingId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
      .expectStatus().isOk
      .expectBody<TapMoveBookingMappingDto>()
      .returnResult().responseBody!!
  }

  @Nested
  @DisplayName("PUT /mapping/taps/move-booking/{bookingId}/from/{fromOffenderNo}/to/{toOffenderNo}")
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

    private fun anApplicationMapping(bookingId: Long, applicationId: Long, authorisationId: UUID) = TapApplicationMapping(
      nomisApplicationId = applicationId,
      dpsAuthorisationId = authorisationId,
      offenderNo = "A1234AB",
      bookingId = bookingId,
      mappingType = MovementMappingType.NOMIS_CREATED,
    )

    private fun aScheduleMapping(bookingId: Long, eventId: Long, occurrenceId: UUID) = TapScheduleMapping(
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

    private fun aMovementMapping(bookingId: Long, movementSeq: Int, movementId: UUID) = TapMovementMapping(
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
          .uri("/mapping/taps/move-booking/1/from/A1234AB/to/A9876BA")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/taps/move-booking/1/from/A1234AB/to/A9876BA")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/taps/move-booking/1/from/A1234AB/to/A9876BA")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.moveBookingIds(bookingId: Long = 1L, from: String = "A1234AB", to: String = "A9876BA") = put()
      .uri("/mapping/taps/move-booking/$bookingId/from/$from/to/$to")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()

    private fun WebTestClient.moveBookingIdsOk(bookingId: Long = 1L, from: String = "A1234AB", to: String = "A9876BA") = moveBookingIds(bookingId, from, to)
      .expectStatus().isOk
  }
}
