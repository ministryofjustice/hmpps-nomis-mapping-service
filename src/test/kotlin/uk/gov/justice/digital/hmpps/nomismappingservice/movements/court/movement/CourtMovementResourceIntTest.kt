package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.movement

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
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule.CourtMappingType
import java.util.UUID

class CourtMovementResourceIntTest(
  @Autowired private val movementRepository: CourtMovementRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("POST /mapping/court/movement")
  inner class CreateCourtMovementMapping {

    @AfterEach
    fun tearDown() = runTest {
      movementRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = CourtMovementMappingDto(
        "A1234BC",
        12345L,
        3,
        UUID.randomUUID(),
        CourtMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should create mapping`() = runTest {
        webTestClient.createCourtMovementMapping(mapping)
          .expectStatus().isCreated

        with(movementRepository.findByNomisBookingIdAndNomisMovementSeq(mapping.nomisBookingId, mapping.nomisMovementSeq)!!) {
          assertThat(offenderNo).isEqualTo("A1234BC")
          assertThat(dpsCourtMovementId).isEqualTo(mapping.dpsCourtMovementId)
          assertThat(mappingType).isEqualTo(CourtMappingType.NOMIS_CREATED)
        }
      }
    }

    @Nested
    inner class Validation {
      val mapping = CourtMovementMappingDto(
        "A1234BC",
        12345L,
        3,
        UUID.randomUUID(),
        CourtMappingType.NOMIS_CREATED,
      )
      val duplicateMappingDps = CourtMovementMappingDto(
        "B2345CD",
        56789L,
        4,
        mapping.dpsCourtMovementId,
        CourtMappingType.MIGRATED,
      )
      val duplicateMappingNomis = CourtMovementMappingDto(
        "C3456DE",
        12345L,
        3,
        UUID.randomUUID(),
        CourtMappingType.MIGRATED,
      )

      @Test
      fun `should reject duplicate DPS ID mapping`() = runTest {
        webTestClient.createCourtMovementMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createCourtMovementMapping(duplicateMappingDps)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("nomisBookingId", mapping.nomisBookingId.toInt())
              .containsEntry("nomisMovementSeq", mapping.nomisMovementSeq)
              .containsEntry("dpsCourtMovementId", mapping.dpsCourtMovementId.toString())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingDps.prisonerNumber)
              .containsEntry("nomisBookingId", duplicateMappingDps.nomisBookingId.toInt())
              .containsEntry("nomisMovementSeq", duplicateMappingDps.nomisMovementSeq)
              .containsEntry("dpsCourtMovementId", duplicateMappingDps.dpsCourtMovementId.toString())
              .containsEntry("mappingType", duplicateMappingDps.mappingType.toString())
          }
      }

      @Test
      fun `should reject duplicate NOMIS ID mapping`() = runTest {
        webTestClient.createCourtMovementMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createCourtMovementMapping(duplicateMappingNomis)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("nomisBookingId", mapping.nomisBookingId.toInt())
              .containsEntry("nomisMovementSeq", mapping.nomisMovementSeq)
              .containsEntry("dpsCourtMovementId", mapping.dpsCourtMovementId.toString())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingNomis.prisonerNumber)
              .containsEntry("nomisBookingId", duplicateMappingNomis.nomisBookingId.toInt())
              .containsEntry("nomisMovementSeq", duplicateMappingNomis.nomisMovementSeq)
              .containsEntry("dpsCourtMovementId", duplicateMappingNomis.dpsCourtMovementId.toString())
              .containsEntry("mappingType", duplicateMappingNomis.mappingType.toString())
          }
      }
    }

    @Nested
    inner class Security {
      val mapping = CourtMovementMappingDto(
        "A1234BC",
        12345L,
        3,
        UUID.randomUUID(),
        mappingType = CourtMappingType.NOMIS_CREATED,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/court/movement")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/court/movement")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/court/movement")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.createCourtMovementMapping(mapping: CourtMovementMappingDto) = post()
      .uri("/mapping/court/movement")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/court/movement/nomis-id/{nomisBookingId}/{nomisMovementSeq}")
  inner class GetCourtMovementMappingByNomisId {

    @AfterEach
    fun tearDown() = runTest {
      movementRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = CourtMovementMapping(
        UUID.randomUUID(),
        23456L,
        3,
        "A1234BC",
        mappingType = CourtMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should get court movement mapping by NOMIS ID`() = runTest {
        movementRepository.save(mapping)

        webTestClient.getCourtMovementMapping(mapping.nomisBookingId, mapping.nomisMovementSeq)
          .expectStatus().isOk
          .expectBody(object : ParameterizedTypeReference<CourtMovementMappingDto>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(nomisBookingId).isEqualTo(mapping.nomisBookingId)
            assertThat(nomisMovementSeq).isEqualTo(mapping.nomisMovementSeq)
            assertThat(dpsCourtMovementId).isEqualTo(mapping.dpsCourtMovementId)
            assertThat(prisonerNumber).isEqualTo(mapping.offenderNo)
            assertThat(mappingType).isEqualTo(mapping.mappingType)
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() = runTest {
        webTestClient.getCourtMovementMapping(12345L, 3)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      val mapping = CourtMovementMappingDto(
        "A1234BC",
        12345L,
        3,
        UUID.randomUUID(),
        CourtMappingType.NOMIS_CREATED,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/court/movement/nomis-id/12345/3")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court/movement/nomis-id/12345/3")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court/movement/nomis-id/12345/3")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getCourtMovementMapping(nomisBookingId: Long, nomisMovementSeq: Int) = get()
      .uri("/mapping/court/movement/nomis-id/$nomisBookingId/$nomisMovementSeq")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }
}
