package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.schedule

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
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application.MovementMappingType
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class TapScheduleResourceIntTest(
  @Autowired private val scheduleRepository: TapScheduleRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("POST /mapping/taps/schedule")
  inner class CreateScheduledMovementSyncMapping {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TapScheduleMappingDto(
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
      val mapping = TapScheduleMappingDto(
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
      val duplicateMappingDps = TapScheduleMappingDto(
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
      val duplicateMappingNomis = TapScheduleMappingDto(
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
      val mapping = TapScheduleMappingDto(
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
          .uri("/mapping/taps/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/taps/schedule")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/taps/schedule")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.createScheduledMovementSyncMapping(mapping: TapScheduleMappingDto) = post()
      .uri("/mapping/taps/schedule")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
  @DisplayName("PUT /mapping/taps/schedule")
  inner class UpdateScheduledMovementSyncMapping {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TapScheduleMappingDto(
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
      val mapping = TapScheduleMappingDto(
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
      val mapping = TapScheduleMappingDto(
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
          .uri("/mapping/taps/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/taps/schedule")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/taps/schedule")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.createScheduledMovementSyncMapping(mapping: TapScheduleMappingDto) = post()
      .uri("/mapping/taps/schedule")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()

    private fun WebTestClient.updateScheduledMovementSyncMapping(mapping: TapScheduleMappingDto, source: String = "NOMIS") = put()
      .uri("/mapping/taps/schedule?source=$source")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/taps/schedule/nomis-id/{nomisEventId}")
  inner class GetNomisScheduledMovementSyncMapping {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TapScheduleMapping(
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
          .expectBody(object : ParameterizedTypeReference<TapScheduleMappingDto>() {})
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
      val mapping = TapScheduleMappingDto(
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
          .uri("/mapping/taps/schedule/nomis-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/taps/schedule/nomis-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/taps/schedule/nomis-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getScheduledMovementSyncMapping(nomisEventId: Long) = get()
      .uri("/mapping/taps/schedule/nomis-id/$nomisEventId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/taps/schedule/dps-id/{dpsId}")
  inner class GetDpsScheduledMovementSyncMapping {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TapScheduleMapping(
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
          .expectBody(object : ParameterizedTypeReference<TapScheduleMappingDto>() {})
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
      val mapping = TapScheduleMappingDto(
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
          .uri("/mapping/taps/schedule/dps-id/${mapping.dpsOccurrenceId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/taps/schedule/dps-id/${mapping.dpsOccurrenceId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/taps/schedule/dps-id/${mapping.dpsOccurrenceId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getScheduledMovementSyncMapping(dpsId: UUID) = get()
      .uri("/mapping/taps/schedule/dps-id/$dpsId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("DELETE /mapping/taps/schedule/nomis-id/{nomisEventId}")
  inner class DeleteNomisScheduledMovementMapping {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping1 = TapScheduleMapping(
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
      val mapping2 = TapScheduleMapping(
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
          .uri("/mapping/taps/schedule/nomis-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/taps/schedule/nomis-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/taps/schedule/nomis-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.deleteScheduledMovementSyncMapping(nomisEventId: Long) = delete()
      .uri("/mapping/taps/schedule/nomis-id/$nomisEventId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }
}
