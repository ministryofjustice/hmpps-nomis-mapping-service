package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.movement

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.PersonLocationMappingType
import java.util.UUID

class PersonLocationMovementResourceIntTest(
  @Autowired private val movementRepository: PersonLocationMovementRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("POST /mapping/person-location/movement")
  inner class CreateMovementMapping {
    private val mapping = PersonLocationMovementIdMappingDto(
      nomisBookingId = 12345L,
      nomisMovementSeq = 3,
      dpsExternalMovementId = UUID.randomUUID(),
      mappingType = PersonLocationMappingType.NOMIS_CREATED,
    )

    @AfterEach
    fun tearDown() = runTest {
      movementRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/person-location/movement")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/person-location/movement")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/person-location/movement")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should reject mapping with missing required fields`() {
        webTestClient.post()
          .uri("/mapping/person-location/movement")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapOf("nomisBookingId" to 12345L)))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `should reject duplicate DPS ID mapping`() = runTest {
        val duplicateMappingDps = mapping.copy(nomisBookingId = 56789L, nomisMovementSeq = 4, mappingType = PersonLocationMappingType.MIGRATED)
        webTestClient.createMovementMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createMovementMapping(duplicateMappingDps)
          .expectStatus().isDuplicateMapping
          .expectBody<TestDuplicateErrorResponse>()
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("nomisBookingId", mapping.nomisBookingId.toInt())
              .containsEntry("nomisMovementSeq", mapping.nomisMovementSeq)
              .containsEntry("dpsExternalMovementId", mapping.dpsExternalMovementId.toString())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("nomisBookingId", duplicateMappingDps.nomisBookingId.toInt())
              .containsEntry("nomisMovementSeq", duplicateMappingDps.nomisMovementSeq)
              .containsEntry("dpsExternalMovementId", duplicateMappingDps.dpsExternalMovementId.toString())
              .containsEntry("mappingType", duplicateMappingDps.mappingType.toString())
          }
      }

      @Test
      fun `should reject duplicate NOMIS ID mapping`() = runTest {
        val duplicateMappingNomis = mapping.copy(dpsExternalMovementId = UUID.randomUUID(), mappingType = PersonLocationMappingType.MIGRATED)
        webTestClient.createMovementMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createMovementMapping(duplicateMappingNomis)
          .expectStatus().isDuplicateMapping
          .expectBody<TestDuplicateErrorResponse>()
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("nomisBookingId", mapping.nomisBookingId.toInt())
              .containsEntry("nomisMovementSeq", mapping.nomisMovementSeq)
              .containsEntry("dpsExternalMovementId", mapping.dpsExternalMovementId.toString())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("nomisBookingId", duplicateMappingNomis.nomisBookingId.toInt())
              .containsEntry("nomisMovementSeq", duplicateMappingNomis.nomisMovementSeq)
              .containsEntry("dpsExternalMovementId", duplicateMappingNomis.dpsExternalMovementId.toString())
              .containsEntry("mappingType", duplicateMappingNomis.mappingType.toString())
          }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should create mapping`() = runTest {
        webTestClient.createMovementMapping(mapping)
          .expectStatus().isCreated

        with(movementRepository.findByNomisBookingIdAndNomisMovementSeq(mapping.nomisBookingId, mapping.nomisMovementSeq)!!) {
          assertThat(dpsExternalMovementId).isEqualTo(mapping.dpsExternalMovementId)
          assertThat(mappingType).isEqualTo(PersonLocationMappingType.NOMIS_CREATED)
          assertThat(label).isNull()
        }
      }
    }

    private fun WebTestClient.createMovementMapping(mapping: PersonLocationMovementIdMappingDto) = post()
      .uri("/mapping/person-location/movement")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/person-location/movement/nomis-booking-id/{nomisBookingId}/nomis-movement-seq/{nomisMovementSeq}")
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class GetMovementMappingByNomisId {
    private val mapping = PersonLocationMovementMapping(
      dpsExternalMovementId = UUID.randomUUID(),
      nomisBookingId = 23456L,
      nomisMovementSeq = 3,
      mappingType = PersonLocationMappingType.NOMIS_CREATED,
    )

    @AfterAll
    fun tearDown() = runTest {
      movementRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/person-location/movement/nomis-booking-id/12345/nomis-movement-seq/3")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/person-location/movement/nomis-booking-id/12345/nomis-movement-seq/3")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/person-location/movement/nomis-booking-id/12345/nomis-movement-seq/3")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() {
        webTestClient.getMovementMapping(99999L, 1)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should get movement mapping by NOMIS booking ID and movement sequence`() = runTest {
        movementRepository.save(mapping)

        webTestClient.getMovementMapping(mapping.nomisBookingId, mapping.nomisMovementSeq)
          .expectStatus().isOk
          .expectBody<PersonLocationMovementIdMappingDto>()
          .returnResult().responseBody!!
          .apply {
            assertThat(nomisBookingId).isEqualTo(mapping.nomisBookingId)
            assertThat(nomisMovementSeq).isEqualTo(mapping.nomisMovementSeq)
            assertThat(dpsExternalMovementId).isEqualTo(mapping.dpsExternalMovementId)
            assertThat(mappingType).isEqualTo(PersonLocationMappingType.NOMIS_CREATED)
          }
      }
    }

    private fun WebTestClient.getMovementMapping(nomisBookingId: Long, nomisMovementSeq: Int) = get()
      .uri("/mapping/person-location/movement/nomis-booking-id/$nomisBookingId/nomis-movement-seq/$nomisMovementSeq")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }
}
