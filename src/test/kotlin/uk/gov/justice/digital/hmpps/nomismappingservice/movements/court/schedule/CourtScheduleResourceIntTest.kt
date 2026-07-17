package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.movement.CourtMovementMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.movement.CourtMovementRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.offender.CourtMovementIdMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.offender.CourtScheduleIdMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.offender.CourtSchedulerMoveBookingMappingDto
import java.util.UUID

class CourtScheduleResourceIntTest(
  @Autowired private val scheduleRepository: CourtScheduleRepository,
  @Autowired private val movementRepository: CourtMovementRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("POST /mapping/court-scheduler/schedule")
  inner class CreateCourtScheduleMapping {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = CourtScheduleMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        CourtMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should create mapping`() = runTest {
        webTestClient.createCourtScheduleMapping(mapping)
          .expectStatus().isCreated

        with(scheduleRepository.findByNomisEventId(mapping.nomisEventId)!!) {
          assertThat(offenderNo).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(12345L)
          assertThat(dpsCourtAppearanceId).isEqualTo(mapping.dpsCourtAppearanceId)
          assertThat(mappingType).isEqualTo(CourtMappingType.NOMIS_CREATED)
        }
      }
    }

    @Nested
    inner class Validation {
      val mapping = CourtScheduleMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        CourtMappingType.NOMIS_CREATED,
      )
      val duplicateMappingDps = CourtScheduleMappingDto(
        "B2345CD",
        56789L,
        34567L,
        mapping.dpsCourtAppearanceId,
        CourtMappingType.MIGRATED,
      )
      val duplicateMappingNomis = CourtScheduleMappingDto(
        "C3456DE",
        9101112L,
        mapping.nomisEventId,
        UUID.randomUUID(),
        CourtMappingType.MIGRATED,
      )

      @Test
      fun `should reject duplicate DPS ID mapping`() = runTest {
        webTestClient.createCourtScheduleMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createCourtScheduleMapping(duplicateMappingDps)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsCourtAppearanceId", mapping.dpsCourtAppearanceId.toString())
              .containsEntry("nomisEventId", mapping.nomisEventId.toInt())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingDps.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingDps.bookingId.toInt())
              .containsEntry("dpsCourtAppearanceId", duplicateMappingDps.dpsCourtAppearanceId.toString())
              .containsEntry("nomisEventId", duplicateMappingDps.nomisEventId.toInt())
              .containsEntry("mappingType", duplicateMappingDps.mappingType.toString())
          }
      }

      @Test
      fun `should reject duplicate NOMIS ID mapping`() = runTest {
        webTestClient.createCourtScheduleMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createCourtScheduleMapping(duplicateMappingNomis)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsCourtAppearanceId", mapping.dpsCourtAppearanceId.toString())
              .containsEntry("nomisEventId", mapping.nomisEventId.toInt())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingNomis.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingNomis.bookingId.toInt())
              .containsEntry("dpsCourtAppearanceId", duplicateMappingNomis.dpsCourtAppearanceId.toString())
              .containsEntry("nomisEventId", duplicateMappingNomis.nomisEventId.toInt())
              .containsEntry("mappingType", duplicateMappingNomis.mappingType.toString())
          }
      }
    }

    @Nested
    inner class Security {
      val mapping = CourtScheduleMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        mappingType = CourtMappingType.NOMIS_CREATED,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/court-scheduler/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/court-scheduler/schedule")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/court-scheduler/schedule")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  @Nested
  @DisplayName("PUT /mapping/court-scheduler/schedule/dps-id")
  inner class UpsertCourtScheduleMappingByDpsId {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = CourtScheduleMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        CourtMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should create mapping`() = runTest {
        webTestClient.upsertCourtScheduleMappingByDpsId(mapping)
          .expectStatus().isOk
          .expectBody<CourtScheduleMappingUpsertByDpsIdResponse>()
          .returnResult().responseBody!!
          .apply {
            // We didn't replace the event ID so returns null
            assertThat(replacedNomisEventId).isNull()
          }

        with(scheduleRepository.findByNomisEventId(mapping.nomisEventId)!!) {
          assertThat(offenderNo).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(12345L)
          assertThat(dpsCourtAppearanceId).isEqualTo(mapping.dpsCourtAppearanceId)
          assertThat(mappingType).isEqualTo(CourtMappingType.NOMIS_CREATED)
        }
      }

      @Test
      fun `should do nothing if we receive the same create request twice (endpoint is idempotent)`() = runTest {
        webTestClient.upsertCourtScheduleMappingByDpsId(mapping)
          .expectStatus().isOk

        webTestClient.upsertCourtScheduleMappingByDpsId(mapping)
          .expectStatus().isOk
          .expectBody<CourtScheduleMappingUpsertByDpsIdResponse>()
          .returnResult().responseBody!!
          .apply {
            // We didn't replace the event ID so returns null
            assertThat(replacedNomisEventId).isNull()
          }

        with(scheduleRepository.findByNomisEventId(mapping.nomisEventId)!!) {
          assertThat(offenderNo).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(12345L)
          assertThat(dpsCourtAppearanceId).isEqualTo(mapping.dpsCourtAppearanceId)
          assertThat(mappingType).isEqualTo(CourtMappingType.NOMIS_CREATED)
        }
      }
    }

    @Nested
    inner class Upsert {
      val mapping = CourtScheduleMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        CourtMappingType.NOMIS_CREATED,
      )
      val duplicateMappingDps = CourtScheduleMappingDto(
        "A1234BC",
        56789L,
        34567L,
        mapping.dpsCourtAppearanceId,
        CourtMappingType.MIGRATED,
      )
      val duplicateMappingNomis = CourtScheduleMappingDto(
        "A1234BC",
        9101112L,
        mapping.nomisEventId,
        UUID.randomUUID(),
        CourtMappingType.MIGRATED,
      )

      @Test
      fun `should replace duplicate DPS ID mapping and return old nomis ID`() = runTest {
        webTestClient.upsertCourtScheduleMappingByDpsId(mapping)
          .expectStatus().isOk

        webTestClient.upsertCourtScheduleMappingByDpsId(duplicateMappingDps)
          .expectStatus().isOk
          .expectBody<CourtScheduleMappingUpsertByDpsIdResponse>()
          .returnResult().responseBody!!
          .apply {
            assertThat(replacedNomisEventId).isEqualTo(mapping.nomisEventId)
          }

        assertThat(scheduleRepository.findByNomisEventId(mapping.nomisEventId)).isNull()
        scheduleRepository.findByNomisEventId(duplicateMappingDps.nomisEventId)!!
          .apply {
            assertThat(dpsCourtAppearanceId).isEqualTo(duplicateMappingDps.dpsCourtAppearanceId)
            assertThat(bookingId).isEqualTo(duplicateMappingDps.bookingId)
          }
      }

      @Test
      fun `should reject duplicate NOMIS ID mapping`() = runTest {
        webTestClient.upsertCourtScheduleMappingByDpsId(mapping)
          .expectStatus().isOk

        webTestClient.upsertCourtScheduleMappingByDpsId(duplicateMappingNomis)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsCourtAppearanceId", mapping.dpsCourtAppearanceId.toString())
              .containsEntry("nomisEventId", mapping.nomisEventId.toInt())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingNomis.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingNomis.bookingId.toInt())
              .containsEntry("dpsCourtAppearanceId", duplicateMappingNomis.dpsCourtAppearanceId.toString())
              .containsEntry("nomisEventId", duplicateMappingNomis.nomisEventId.toInt())
              .containsEntry("mappingType", duplicateMappingNomis.mappingType.toString())
          }
      }
    }

    @Nested
    inner class Security {
      val mapping = CourtScheduleMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        mappingType = CourtMappingType.NOMIS_CREATED,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/court-scheduler/schedule/dps-id")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/court-scheduler/schedule/dps-id")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/court-scheduler/schedule/dps-id")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.upsertCourtScheduleMappingByDpsId(mapping: CourtScheduleMappingDto) = put()
      .uri("/mapping/court-scheduler/schedule/dps-id")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/court-scheduler/schedule/nomis-id/{nomisEventId}")
  inner class GetCourtScheduleMappingByNomisId {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = CourtScheduleMapping(
        UUID.randomUUID(),
        23456L,
        "A1234BC",
        12345L,
        mappingType = CourtMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should get court schedule mapping by NOMIS ID`() = runTest {
        scheduleRepository.save(mapping)

        webTestClient.getCourtScheduleMapping(mapping.nomisEventId)
          .expectStatus().isOk
          .expectBody(object : ParameterizedTypeReference<CourtScheduleMappingDto>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(nomisEventId).isEqualTo(mapping.nomisEventId)
            assertThat(dpsCourtAppearanceId).isEqualTo(mapping.dpsCourtAppearanceId)
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
        webTestClient.getCourtScheduleMapping(12345L)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      val mapping = CourtScheduleMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        CourtMappingType.NOMIS_CREATED,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/court-scheduler/schedule/nomis-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-scheduler/schedule/nomis-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-scheduler/schedule/nomis-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getCourtScheduleMapping(nomisEventId: Long) = get()
      .uri("/mapping/court-scheduler/schedule/nomis-id/$nomisEventId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("DELETE /mapping/court-scheduler/schedule/nomis-id/{nomisEventId}")
  inner class DeleteCourtScheduledMovementMapping {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping1 = CourtScheduleMapping(
        UUID.randomUUID(),
        23456L,
        "A1234BC",
        12345L,
        mappingType = CourtMappingType.NOMIS_CREATED,
      )
      val mapping2 = CourtScheduleMapping(
        UUID.randomUUID(),
        65432L,
        "A1234BC",
        12345L,
        mappingType = CourtMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should delete court schedule mapping by NOMIS ID`() = runTest {
        scheduleRepository.save(mapping1)
        scheduleRepository.save(mapping2)

        webTestClient.deleteCourtScheduleMapping(mapping1.nomisEventId)
          .expectStatus().isNoContent

        assertThat(scheduleRepository.findByNomisEventId(mapping1.nomisEventId)).isNull()
        assertThat(scheduleRepository.findByNomisEventId(mapping2.nomisEventId)).isNotNull
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `delete endpoint should be idempotent`() = runTest {
        webTestClient.deleteCourtScheduleMapping(12345L)
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/court-scheduler/schedule/nomis-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/court-scheduler/schedule/nomis-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/court-scheduler/schedule/nomis-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.deleteCourtScheduleMapping(nomisEventId: Long) = delete()
      .uri("/mapping/court-scheduler/schedule/nomis-id/$nomisEventId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("DELETE /mapping/court-scheduler/schedule/dps-id/{dpdsEventId}")
  inner class DeleteCourtScheduleMappingByDpsId {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping1 = CourtScheduleMapping(
        UUID.randomUUID(),
        23456L,
        "A1234BC",
        12345L,
        mappingType = CourtMappingType.NOMIS_CREATED,
      )
      val mapping2 = CourtScheduleMapping(
        UUID.randomUUID(),
        65432L,
        "A1234BC",
        12345L,
        mappingType = CourtMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should delete court schedule mapping by NOMIS ID`() = runTest {
        scheduleRepository.save(mapping1)
        scheduleRepository.save(mapping2)

        webTestClient.deleteCourtScheduleMapping(mapping1.dpsCourtAppearanceId)
          .expectStatus().isNoContent

        assertThat(scheduleRepository.findByDpsCourtAppearanceId(mapping1.dpsCourtAppearanceId)).isNull()
        assertThat(scheduleRepository.findByDpsCourtAppearanceId(mapping2.dpsCourtAppearanceId)).isNotNull
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `delete endpoint should be idempotent`() = runTest {
        webTestClient.deleteCourtScheduleMapping(UUID.randomUUID())
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/court-scheduler/schedule/dps-id/${UUID.randomUUID()}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/court-scheduler/schedule/dps-id/${UUID.randomUUID()}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/court-scheduler/schedule/dps-id/${UUID.randomUUID()}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.deleteCourtScheduleMapping(dpsId: UUID) = delete()
      .uri("/mapping/court-scheduler/schedule/dps-id/$dpsId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/court-scheduler/schedule/dps-id/{dpsId}")
  inner class GetDpsCourtScheduleSyncMapping {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = CourtScheduleMapping(
        dpsCourtAppearanceId = UUID.randomUUID(),
        nomisEventId = 23456L,
        offenderNo = "A1234BC",
        bookingId = 12345L,
        mappingType = CourtMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should get court schedule mapping by DPS ID`() = runTest {
        scheduleRepository.save(mapping)

        webTestClient.getCourtScheduleSyncMapping(mapping.dpsCourtAppearanceId)
          .expectStatus().isOk
          .expectBody(object : ParameterizedTypeReference<CourtScheduleMappingDto>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(nomisEventId).isEqualTo(mapping.nomisEventId)
            assertThat(dpsCourtAppearanceId).isEqualTo(mapping.dpsCourtAppearanceId)
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
        webTestClient.getCourtScheduleSyncMapping(UUID.randomUUID())
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      val mapping = CourtScheduleMappingDto(
        prisonerNumber = "A1234BC",
        bookingId = 12345L,
        nomisEventId = 23456L,
        dpsCourtAppearanceId = UUID.randomUUID(),
        mappingType = CourtMappingType.NOMIS_CREATED,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/court-scheduler/schedule/dps-id/${mapping.dpsCourtAppearanceId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-scheduler/schedule/dps-id/${mapping.dpsCourtAppearanceId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-scheduler/schedule/dps-id/${mapping.dpsCourtAppearanceId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getCourtScheduleSyncMapping(dpsId: UUID) = get()
      .uri("/mapping/court-scheduler/schedule/dps-id/$dpsId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/court-scheduler/move-booking/{bookingId}")
  inner class GetMappingsForMoveBooking {
    private val bookingId1 = 1L
    private val bookingId2 = 2L
    private val book1event1 = 3L
    private val book1event2 = 4L
    private val book1seq1 = 5
    private val book1seq2 = 6
    private val book2event1 = 7L
    private val book2seq1 = 8
    private val book1sched1 = UUID.randomUUID()
    private val book1sched2 = UUID.randomUUID()
    private val book1move1 = UUID.randomUUID()
    private val book1move2 = UUID.randomUUID()
    private val book2sched1 = UUID.randomUUID()
    private val book2move1 = UUID.randomUUID()

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
      movementRepository.deleteAll()
    }

    @BeforeEach
    fun setUp() = runTest {
      scheduleRepository.save(aScheduleMapping(bookingId1, book1event1, book1sched1))
      scheduleRepository.save(aScheduleMapping(bookingId1, book1event2, book1sched2))
      scheduleRepository.save(aScheduleMapping(bookingId2, book2event1, book2sched1))

      movementRepository.save(aMovementMapping(bookingId1, book1seq1, book1move1))
      movementRepository.save(aMovementMapping(bookingId1, book1seq2, book1move2))
      movementRepository.save(aMovementMapping(bookingId2, book2seq1, book2move1))
    }

    private fun aScheduleMapping(bookingId: Long, nomisEventId: Long, dpsScheduleId: UUID) = CourtScheduleMapping(
      nomisEventId = nomisEventId,
      dpsCourtAppearanceId = dpsScheduleId,
      offenderNo = "A1234AB",
      bookingId = bookingId,
      mappingType = CourtMappingType.NOMIS_CREATED,
    )

    private fun aMovementMapping(bookingId: Long, movementSeq: Int, movementId: UUID) = CourtMovementMapping(
      dpsCourtMovementId = movementId,
      nomisBookingId = bookingId,
      nomisMovementSeq = movementSeq,
      offenderNo = "A1234AB",
      mappingType = CourtMappingType.NOMIS_CREATED,
    )

    @Nested
    inner class HappyPath {
      @Test
      fun `should find booking IDs`() = runTest {
        webTestClient.getMoveBookingIds(bookingId1)
          .apply {
            assertThat(scheduleIds).containsExactlyInAnyOrder(
              CourtScheduleIdMapping(book1event1, book1sched1),
              CourtScheduleIdMapping(book1event2, book1sched2),
            )
            assertThat(movementIds).containsExactlyInAnyOrder(
              CourtMovementIdMapping(book1seq1, book1move1),
              CourtMovementIdMapping(book1seq2, book1move2),
            )
          }
      }

      @Test
      fun `should return empty lists if there are no mappings`() = runTest {
        webTestClient.getMoveBookingIds(99)
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
          .uri("/mapping/court-scheduler/move-booking/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-scheduler/move-booking/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-scheduler/move-booking/1")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getMoveBookingIds(bookingId: Long = 1L) = get()
      .uri("/mapping/court-scheduler/move-booking/$bookingId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
      .expectStatus().isOk
      .expectBody<CourtSchedulerMoveBookingMappingDto>()
      .returnResult().responseBody!!
  }

  @Nested
  @DisplayName("PUT /mapping/court-scheduler/move-booking/{bookingId}/from/{fromOffenderNo}/to/{toOffenderNo}")
  inner class UpdateMappingsForMoveBooking {
    private val fromOffenderNo = "A1234AB"
    private val toOffenderNo = "A9876BA"

    private val bookingId1 = 1L
    private val bookingId2 = 2L
    private val book1event1 = 3L
    private val book1event2 = 4L
    private val book1seq1 = 5
    private val book1seq2 = 6
    private val book2event1 = 7L
    private val book2seq1 = 8
    private val book1sched1 = UUID.randomUUID()
    private val book1sched2 = UUID.randomUUID()
    private val book1move1 = UUID.randomUUID()
    private val book1move2 = UUID.randomUUID()
    private val book2sched1 = UUID.randomUUID()
    private val book2move1 = UUID.randomUUID()

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
      movementRepository.deleteAll()
    }

    @BeforeEach
    fun setUp() = runTest {
      scheduleRepository.save(aScheduleMapping(bookingId1, book1event1, book1sched1))
      scheduleRepository.save(aScheduleMapping(bookingId1, book1event2, book1sched2))
      scheduleRepository.save(aScheduleMapping(bookingId2, book2event1, book2sched1))

      movementRepository.save(aMovementMapping(bookingId1, book1seq1, book1move1))
      movementRepository.save(aMovementMapping(bookingId1, book1seq2, book1move2))
      movementRepository.save(aMovementMapping(bookingId2, book2seq1, book2move1))
    }

    private fun aScheduleMapping(bookingId: Long, nomisEventId: Long, dpsScheduleId: UUID) = CourtScheduleMapping(
      nomisEventId = nomisEventId,
      dpsCourtAppearanceId = dpsScheduleId,
      offenderNo = "A1234AB",
      bookingId = bookingId,
      mappingType = CourtMappingType.NOMIS_CREATED,
    )

    private fun aMovementMapping(bookingId: Long, movementSeq: Int, movementId: UUID) = CourtMovementMapping(
      dpsCourtMovementId = movementId,
      nomisBookingId = bookingId,
      nomisMovementSeq = movementSeq,
      offenderNo = "A1234AB",
      mappingType = CourtMappingType.NOMIS_CREATED,
    )

    @Nested
    inner class HappyPath {
      @Test
      fun `should move all mappings to new booking`() = runTest {
        webTestClient.moveBookingIdsOk(bookingId1, fromOffenderNo, toOffenderNo)

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
          .uri("/mapping/court-scheduler/move-booking/1/from/A1234AB/to/A9876BA")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/court-scheduler/move-booking/1/from/A1234AB/to/A9876BA")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/court-scheduler/move-booking/1/from/A1234AB/to/A9876BA")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.moveBookingIds(bookingId: Long = 1L, from: String = "A1234AB", to: String = "A9876BA") = put()
      .uri("/mapping/court-scheduler/move-booking/$bookingId/from/$from/to/$to")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()

    private fun WebTestClient.moveBookingIdsOk(bookingId: Long = 1L, from: String = "A1234AB", to: String = "A9876BA") = moveBookingIds(bookingId, from, to)
      .expectStatus().isOk
  }

  @Nested
  @DisplayName("PUT /mapping/court-scheduler/schedule/prisoner")
  inner class UpdateMappingPrisoner {
    private val prisonerNumber = "A1234AB"
    private val newPrisonerNumber = "B1234BC"
    private val bookingId = 12345L
    private val newBooking = 54321L
    private val nomisEventId = 123L
    private val dpsScheduleId = UUID.randomUUID()

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
      movementRepository.deleteAll()
    }

    @BeforeEach
    fun setUp() = runTest {
      scheduleRepository.save(aScheduleMapping(prisonerNumber, bookingId, nomisEventId, dpsScheduleId))
    }

    @Nested
    @Disabled("WIP - Need to implement service")
    inner class HappyPath {
      @Test
      fun `should update prisoner and booking`() {}

      @Test
      fun `should return success if nothing changes (idempotent)`() {}
    }

    @Nested
    @Disabled("WIP - Need to implement service")
    inner class Validation {
      @Test
      fun `should return not found if no existing mapping`() {
      }

      @Test
      fun `should return bad request if DPS id is different`() {
      }

      @Test
      fun `should return bad request if old prisoner number is different`() {
      }

      @Test
      fun `should return bad request if old booking id is different`() {
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/court-scheduler/schedule/update-prisoner/nomis-id/123")
          .bodyValue(anUpdateMappingPrisonerRequest())
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/court-scheduler/schedule/update-prisoner/nomis-id/123")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(anUpdateMappingPrisonerRequest())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/court-scheduler/schedule/update-prisoner/nomis-id/123")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(anUpdateMappingPrisonerRequest())
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun aScheduleMapping(
      offenderNo: String = prisonerNumber,
      booking: Long = bookingId,
      eventId: Long = nomisEventId,
      dpsId: UUID = dpsScheduleId,
    ) = CourtScheduleMapping(
      nomisEventId = eventId,
      dpsCourtAppearanceId = dpsId,
      offenderNo = offenderNo,
      bookingId = booking,
      mappingType = CourtMappingType.NOMIS_CREATED,
    )

    private fun anUpdateMappingPrisonerRequest(
      oldOffenderNo: String = prisonerNumber,
      oldBookingId: Long = bookingId,
      newOffenderNo: String = newPrisonerNumber,
      newBookingId: Long = newBooking,
      dpsId: UUID = dpsScheduleId,
    ) = UpdateScheduleMappingPrisonerRequest(dpsId, oldOffenderNo, oldBookingId, newOffenderNo, newBookingId)

    private fun WebTestClient.updateMappingPrisoner(eventId: Long, request: UpdateScheduleMappingPrisonerRequest) = put()
      .uri("/mapping/court-scheduler/schedule/update-prisoner/nomis-id/$eventId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .bodyValue(request)
      .exchange()

    private fun WebTestClient.updateMappingPrisonerOk(eventId: Long, request: UpdateScheduleMappingPrisonerRequest) = updateMappingPrisoner(eventId, request)
      .expectStatus().isOk
  }

  private fun WebTestClient.createCourtScheduleMapping(mapping: CourtScheduleMappingDto) = post()
    .uri("/mapping/court-scheduler/schedule")
    .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
    .contentType(MediaType.APPLICATION_JSON)
    .body(BodyInserters.fromValue(mapping))
    .exchange()
}
