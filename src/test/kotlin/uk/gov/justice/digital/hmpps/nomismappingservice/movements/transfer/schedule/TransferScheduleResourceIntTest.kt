package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
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
import java.util.UUID

class TransferScheduleResourceIntTest(
  @Autowired private val scheduleRepository: TransferScheduleRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("POST /mapping/transfer-scheduler/schedule")
  inner class CreateTransferScheduleMapping {

    @AfterEach
    fun tearDown() = runTest {
      scheduleRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TransferScheduleMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        TransferMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should create mapping`() = runTest {
        webTestClient.createTransferScheduleMapping(mapping)
          .expectStatus().isCreated

        with(scheduleRepository.findByNomisEventId(mapping.nomisEventId)!!) {
          assertThat(offenderNo).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(12345L)
          assertThat(dpsTransferScheduleId).isEqualTo(mapping.dpsTransferScheduleId)
          assertThat(mappingType).isEqualTo(TransferMappingType.NOMIS_CREATED)
        }
      }
    }

    @Nested
    inner class Validation {
      val mapping = TransferScheduleMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        TransferMappingType.NOMIS_CREATED,
      )
      val duplicateMappingDps = TransferScheduleMappingDto(
        "B2345CD",
        56789L,
        34567L,
        mapping.dpsTransferScheduleId,
        TransferMappingType.MIGRATED,
      )
      val duplicateMappingNomis = TransferScheduleMappingDto(
        "C3456DE",
        9101112L,
        mapping.nomisEventId,
        UUID.randomUUID(),
        TransferMappingType.MIGRATED,
      )

      @Test
      fun `should reject duplicate DPS ID mapping`() = runTest {
        webTestClient.createTransferScheduleMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createTransferScheduleMapping(duplicateMappingDps)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsTransferScheduleId", mapping.dpsTransferScheduleId.toString())
              .containsEntry("nomisEventId", mapping.nomisEventId.toInt())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingDps.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingDps.bookingId.toInt())
              .containsEntry("dpsTransferScheduleId", duplicateMappingDps.dpsTransferScheduleId.toString())
              .containsEntry("nomisEventId", duplicateMappingDps.nomisEventId.toInt())
              .containsEntry("mappingType", duplicateMappingDps.mappingType.toString())
          }
      }

      @Test
      fun `should reject duplicate NOMIS ID mapping`() = runTest {
        webTestClient.createTransferScheduleMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createTransferScheduleMapping(duplicateMappingNomis)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsTransferScheduleId", mapping.dpsTransferScheduleId.toString())
              .containsEntry("nomisEventId", mapping.nomisEventId.toInt())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingNomis.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingNomis.bookingId.toInt())
              .containsEntry("dpsTransferScheduleId", duplicateMappingNomis.dpsTransferScheduleId.toString())
              .containsEntry("nomisEventId", duplicateMappingNomis.nomisEventId.toInt())
              .containsEntry("mappingType", duplicateMappingNomis.mappingType.toString())
          }
      }
    }

    @Nested
    inner class Security {
      val mapping = TransferScheduleMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        mappingType = TransferMappingType.NOMIS_CREATED,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/transfer-scheduler/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/transfer-scheduler/schedule")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/transfer-scheduler/schedule")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  private fun WebTestClient.createTransferScheduleMapping(mapping: TransferScheduleMappingDto) = post()
    .uri("/mapping/transfer-scheduler/schedule")
    .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
    .contentType(MediaType.APPLICATION_JSON)
    .body(BodyInserters.fromValue(mapping))
    .exchange()
}
