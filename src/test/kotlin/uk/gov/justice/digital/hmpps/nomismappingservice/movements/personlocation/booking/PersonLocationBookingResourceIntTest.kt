package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.booking

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

class PersonLocationBookingResourceIntTest(
  @Autowired private val bookingRepository: PersonLocationBookingRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("POST /mapping/person-location/booking")
  inner class CreateBookingMapping {
    private val mapping = PersonLocationBookingIdMappingDto(
      nomisBookingId = 12345L,
      dpsCustodialSeriesId = UUID.randomUUID(),
      mappingType = PersonLocationMappingType.NOMIS_CREATED,
    )

    @AfterEach
    fun tearDown() = runTest {
      bookingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/person-location/booking")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/person-location/booking")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/person-location/booking")
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
          .uri("/mapping/person-location/booking")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapOf("nomisBookingId" to 12345L)))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `should reject duplicate DPS ID mapping`() = runTest {
        val duplicateMappingDps = mapping.copy(nomisBookingId = 56789L, mappingType = PersonLocationMappingType.MIGRATED)
        webTestClient.createBookingMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createBookingMapping(duplicateMappingDps)
          .expectStatus().isDuplicateMapping
          .expectBody<TestDuplicateErrorResponse>()
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("nomisBookingId", mapping.nomisBookingId.toInt())
              .containsEntry("dpsCustodialSeriesId", mapping.dpsCustodialSeriesId.toString())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("nomisBookingId", duplicateMappingDps.nomisBookingId.toInt())
              .containsEntry("dpsCustodialSeriesId", duplicateMappingDps.dpsCustodialSeriesId.toString())
              .containsEntry("mappingType", duplicateMappingDps.mappingType.toString())
          }
      }

      @Test
      fun `should reject duplicate NOMIS ID mapping`() = runTest {
        val duplicateMappingNomis = mapping.copy(dpsCustodialSeriesId = UUID.randomUUID(), mappingType = PersonLocationMappingType.MIGRATED)
        webTestClient.createBookingMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createBookingMapping(duplicateMappingNomis)
          .expectStatus().isDuplicateMapping
          .expectBody<TestDuplicateErrorResponse>()
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("nomisBookingId", mapping.nomisBookingId.toInt())
              .containsEntry("dpsCustodialSeriesId", mapping.dpsCustodialSeriesId.toString())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("nomisBookingId", duplicateMappingNomis.nomisBookingId.toInt())
              .containsEntry("dpsCustodialSeriesId", duplicateMappingNomis.dpsCustodialSeriesId.toString())
              .containsEntry("mappingType", duplicateMappingNomis.mappingType.toString())
          }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should create mapping`() = runTest {
        webTestClient.createBookingMapping(mapping)
          .expectStatus().isCreated

        with(bookingRepository.findByNomisBookingId(mapping.nomisBookingId)!!) {
          assertThat(dpsCustodialSeriesId).isEqualTo(mapping.dpsCustodialSeriesId)
          assertThat(mappingType).isEqualTo(PersonLocationMappingType.NOMIS_CREATED)
          assertThat(label).isNull()
        }
      }
    }

    private fun WebTestClient.createBookingMapping(mapping: PersonLocationBookingIdMappingDto) = post()
      .uri("/mapping/person-location/booking")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/person-location/booking/nomis-booking-id/{nomisBookingId}")
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class GetBookingMappingByNomisId {
    private val mapping = PersonLocationBookingMapping(
      dpsCustodialSeriesId = UUID.randomUUID(),
      nomisBookingId = 23456L,
      mappingType = PersonLocationMappingType.NOMIS_CREATED,
    )

    @AfterAll
    fun tearDown() = runTest {
      bookingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/person-location/booking/nomis-booking-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/person-location/booking/nomis-booking-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/person-location/booking/nomis-booking-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() {
        webTestClient.getBookingMapping(99999L)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should get booking mapping by NOMIS booking ID`() = runTest {
        bookingRepository.save(mapping)

        webTestClient.getBookingMapping(mapping.nomisBookingId)
          .expectStatus().isOk
          .expectBody<PersonLocationBookingIdMappingDto>()
          .returnResult().responseBody!!
          .apply {
            assertThat(nomisBookingId).isEqualTo(mapping.nomisBookingId)
            assertThat(dpsCustodialSeriesId).isEqualTo(mapping.dpsCustodialSeriesId)
            assertThat(mappingType).isEqualTo(PersonLocationMappingType.NOMIS_CREATED)
          }
      }
    }

    private fun WebTestClient.getBookingMapping(nomisBookingId: Long) = get()
      .uri("/mapping/person-location/booking/nomis-booking-id/$nomisBookingId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("DELETE /mapping/person-location/booking/nomis-booking-id/{nomisBookingId}")
  inner class DeleteBookingMappingByNomisId {
    private val mapping1 = PersonLocationBookingMapping(
      dpsCustodialSeriesId = UUID.randomUUID(),
      nomisBookingId = 12345L,
      mappingType = PersonLocationMappingType.NOMIS_CREATED,
    )
    private val mapping2 = PersonLocationBookingMapping(
      dpsCustodialSeriesId = UUID.randomUUID(),
      nomisBookingId = 12346L,
      mappingType = PersonLocationMappingType.NOMIS_CREATED,
    )

    @AfterEach
    fun tearDown() = runTest {
      bookingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/person-location/booking/nomis-booking-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/person-location/booking/nomis-booking-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/person-location/booking/nomis-booking-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `delete endpoint should be idempotent`() {
        webTestClient.deleteBookingMapping(99999L)
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should delete only the requested booking mapping`() = runTest {
        bookingRepository.save(mapping1)
        bookingRepository.save(mapping2)

        webTestClient.deleteBookingMapping(mapping1.nomisBookingId)
          .expectStatus().isNoContent

        assertThat(bookingRepository.findByNomisBookingId(mapping1.nomisBookingId)).isNull()
        assertThat(bookingRepository.findByNomisBookingId(mapping2.nomisBookingId)).isNotNull
      }
    }

    private fun WebTestClient.deleteBookingMapping(nomisBookingId: Long) = delete()
      .uri("/mapping/person-location/booking/nomis-booking-id/$nomisBookingId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }
}
