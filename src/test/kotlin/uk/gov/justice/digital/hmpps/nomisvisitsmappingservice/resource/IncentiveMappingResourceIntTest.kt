package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.byLessThan
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.IncentiveMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders.IncentiveRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.IncentiveMappingType.INCENTIVE_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.IncentiveMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.IncentiveMappingType.NOMIS_CREATED
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class IncentiveMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var repository: IncentiveRepository

  private val bookingId = 1234L
  private val sequence = 1L
  private val incentiveId = 4444L

  private fun createIncentiveMapping(
    nomisBookingId: Long = bookingId,
    nomisIncentiveSequence: Long = sequence,
    incentiveServiceId: Long = incentiveId,
    label: String = "2022-01-01",
    mappingType: String = NOMIS_CREATED.name
  ): IncentiveMappingDto = IncentiveMappingDto(
    nomisBookingId = nomisBookingId,
    nomisIncentiveSequence = nomisIncentiveSequence,
    incentiveId = incentiveServiceId,
    label = label,
    mappingType = mappingType
  )

  private fun postCreateIncentiveMappingRequest(
    nomisBookingId: Long = 1234L,
    nomisIncentiveSequence: Long = 1,
    incentiveId: Long = 4444L,
    label: String = "2022-01-01",
    mappingType: String = NOMIS_CREATED.name
  ) {
    webTestClient.post().uri("/mapping/incentives")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createIncentiveMapping(
            nomisBookingId = nomisBookingId,
            nomisIncentiveSequence = nomisIncentiveSequence,
            incentiveServiceId = incentiveId,
            label = label,
            mappingType = mappingType
          )
        )
      )
      .exchange()
      .expectStatus().isCreated
  }

  @DisplayName("POST /mapping/incentives")
  @Nested
  inner class CreateIncentiveMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/incentives")
        .body(BodyInserters.fromValue(createIncentiveMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createIncentiveMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createIncentiveMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create when mapping for incentive id already exists for another IEP`() {
      postCreateIncentiveMappingRequest()

      assertThat(
        webTestClient.post().uri("/mapping/incentives")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createIncentiveMapping().copy(nomisBookingId = 21)))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Validation failure: Incentive mapping id = 4444 already exists")
    }

    @Test
    internal fun `create mapping succeeds when the same mapping already exists for the same IEP`() {
      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisBookingId"     : $bookingId,
            "nomisIncentiveSequence"     : $sequence,
            "incentiveId"      : $incentiveId,
            "label"       : "2022-01-01",
            "mappingType" : "INCENTIVE_CREATED"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisBookingId"     : $bookingId,
            "nomisIncentiveSequence"     : $sequence,
            "incentiveId"      : $incentiveId,
            "label"       : "2022-01-01",
            "mappingType" : "INCENTIVE_CREATED"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    fun `create when mapping for nomis ids already exists`() {
      postCreateIncentiveMappingRequest()

      assertThat(
        webTestClient.post().uri("/mapping/incentives")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createIncentiveMapping().copy(incentiveId = 99)))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Validation failure: Incentive with bookingId=1234 and incentiveSequence=1 already exists")
    }

    @Test
    fun `create mapping success`() {
      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisBookingId"     : $bookingId,
            "nomisIncentiveSequence"     : $sequence,
            "incentiveId"      : $incentiveId,
            "label"       : "2022-01-01",
            "mappingType" : "INCENTIVE_CREATED"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated

      val mapping1 =
        webTestClient.get().uri("/mapping/incentives/nomis-booking-id/$bookingId/nomis-incentive-sequence/$sequence")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isOk
          .expectBody(IncentiveMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping1.nomisBookingId).isEqualTo(bookingId)
      assertThat(mapping1.nomisIncentiveSequence).isEqualTo(sequence)
      assertThat(mapping1.incentiveId).isEqualTo(incentiveId)
      assertThat(mapping1.label).isEqualTo("2022-01-01")
      assertThat(mapping1.mappingType).isEqualTo("INCENTIVE_CREATED")

      val mapping2 = webTestClient.get().uri("/mapping/incentives/incentive-id/$incentiveId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody(IncentiveMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.nomisBookingId).isEqualTo(bookingId)
      assertThat(mapping2.nomisIncentiveSequence).isEqualTo(sequence)
      assertThat(mapping2.incentiveId).isEqualTo(incentiveId)
      assertThat(mapping2.label).isEqualTo("2022-01-01")
      assertThat(mapping2.mappingType).isEqualTo("INCENTIVE_CREATED")
    }
  }

  @DisplayName("GET /mapping/incentives/nomis-booking-id/{bookingId}/nomis-incentive-sequence/{incentiveId}")
  @Nested
  inner class GetNomisMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/incentives/nomis-booking-id/$bookingId/nomis-incentive-sequence/$sequence")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/incentives/nomis-booking-id/$bookingId/nomis-incentive-sequence/$sequence")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/incentives/nomis-booking-id/$bookingId/nomis-incentive-sequence/$sequence")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {

      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncentiveMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping =
        webTestClient.get().uri("/mapping/incentives/nomis-booking-id/$bookingId/nomis-incentive-sequence/$sequence")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isOk
          .expectBody(IncentiveMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping.nomisBookingId).isEqualTo(bookingId)
      assertThat(mapping.nomisIncentiveSequence).isEqualTo(sequence)
      assertThat(mapping.incentiveId).isEqualTo(incentiveId)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED.name)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/incentives/incentive-id/99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: Incentive id=99999")
    }

    @Test
    fun `get mapping success with update role`() {

      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncentiveMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/incentives/nomis-booking-id/$bookingId/nomis-incentive-sequence/$sequence")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /mapping/incentives/migrated/latest")
  @Nested
  inner class GeMappingMigratedLatestTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/incentives/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/incentives/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/incentives/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {

      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createIncentiveMapping(
              nomisBookingId = 10,
              nomisIncentiveSequence = 2,
              incentiveServiceId = 10,
              label = "2022-01-01T00:00:00",
              mappingType = "MIGRATED"
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createIncentiveMapping(
              nomisBookingId = 20,
              nomisIncentiveSequence = 2,
              incentiveServiceId = 20,
              label = "2022-01-02T00:00:00",
              mappingType = "MIGRATED"
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createIncentiveMapping(
              nomisBookingId = 1,
              nomisIncentiveSequence = 1,
              incentiveServiceId = 1,
              label = "2022-01-02T10:00:00",
              mappingType = MIGRATED.name
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createIncentiveMapping(
              nomisBookingId = 99,
              nomisIncentiveSequence = 2,
              incentiveServiceId = 199,
              label = "whatever",
              mappingType = INCENTIVE_CREATED.name
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/incentives/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody(IncentiveMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisBookingId).isEqualTo(1)
      assertThat(mapping.nomisIncentiveSequence).isEqualTo(1)
      assertThat(mapping.incentiveId).isEqualTo(1)
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo("MIGRATED")
      assertThat(mapping.whenCreated)
        .isCloseTo(LocalDateTime.now(), byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createIncentiveMapping(
              nomisBookingId = 77,
              nomisIncentiveSequence = 7,
              incentiveServiceId = 77,
              label = "whatever",
              mappingType = INCENTIVE_CREATED.name
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/incentives/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }

  @DisplayName("GET /mapping/incentives/incentive/{incentiveId}")
  @Nested
  inner class GetIncentiveMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/incentives/incentive-id/$incentiveId")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/incentives/incentive-id/$incentiveId")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/incentives/incentive-id/$incentiveId")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {

      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncentiveMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/incentives/incentive-id/$incentiveId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody(IncentiveMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisBookingId).isEqualTo(bookingId)
      assertThat(mapping.nomisIncentiveSequence).isEqualTo(sequence)
      assertThat(mapping.incentiveId).isEqualTo(incentiveId)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED.name)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/incentives/incentive-id/765")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: Incentive id=765")
    }

    @Test
    fun `get mapping success with update role`() {

      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncentiveMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/incentives/incentive-id/$incentiveId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("DELETE /mapping/incentives")
  @Nested
  inner class DeleteMappingsTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/incentives")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete mapping success`() {
      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncentiveMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/incentives/nomis-booking-id/$bookingId/nomis-incentive-sequence/$sequence")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/mapping/incentives/nomis-booking-id/$bookingId/nomis-incentive-sequence/$sequence")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `delete incentive mappings - migrated mappings only`() {
      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncentiveMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createIncentiveMapping(
              nomisBookingId = 333,
              nomisIncentiveSequence = 2,
              incentiveServiceId = 222,
              mappingType = MIGRATED.name
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.delete().uri("/mapping/incentives?onlyMigrated=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/mapping/incentives/nomis-booking-id/$bookingId/nomis-incentive-sequence/$sequence")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk

      webTestClient.get().uri("/mapping/incentives/incentive-id/222")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @DisplayName("DELETE /mapping/incentives/incentive-id/{incentiveId}")
  @Nested
  inner class DeleteMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/incentives/incentive-id/999")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/incentives/incentive-id/999")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/incentives/incentive-id/999")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete specific mapping success`() {
      // create mapping
      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncentiveMapping()))
        .exchange()
        .expectStatus().isCreated

      // it is present after creation by incentive id
      webTestClient.get().uri("/mapping/incentives/incentive-id/$incentiveId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
      // it is also present after creation by nomis id
      webTestClient.get().uri("/mapping/incentives/nomis-booking-id/$bookingId/nomis-incentive-sequence/$sequence")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk

      // delete mapping
      webTestClient.delete().uri("/mapping/incentives/incentive-id/$incentiveId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNoContent

      // no longer present by incentive id
      webTestClient.get().uri("/mapping/incentives/incentive-id/$incentiveId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNotFound
      // and also no longer present by nomis id
      webTestClient.get().uri("/mapping/incentives/nomis-booking-id/$bookingId/nomis-incentive-sequence/$sequence")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    internal fun `delete is idempotent`() {
      // create mapping
      webTestClient.post().uri("/mapping/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncentiveMapping()))
        .exchange()
        .expectStatus().isCreated

      // delete mapping
      webTestClient.delete().uri("/mapping/incentives/incentive-id/$incentiveId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNoContent
      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/incentives/incentive-id/$incentiveId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @DisplayName("GET /mapping/incentives/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationIdTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/migration-id/2022-01-01T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/incentives/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get incentive mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/incentives/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get incentive mappings by migration id success`() {

      (1L..4L).forEach {
        postCreateIncentiveMappingRequest(
          nomisBookingId = it,
          incentiveId = it,
          label = "2022-01-01",
          mappingType = "MIGRATED"
        )
      }
      (5L..9L).forEach {
        postCreateIncentiveMappingRequest(
          nomisBookingId = it,
          incentiveId = it,
          label = "2099-01-01",
          mappingType = "MIGRATED"
        )
      }
      postCreateIncentiveMappingRequest(
        nomisBookingId = 12,
        nomisIncentiveSequence = 1,
        incentiveId = 12, mappingType = INCENTIVE_CREATED.name
      )

      webTestClient.get().uri("/mapping/incentives/migration-id/2022-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..nomisBookingId").value(
          Matchers.contains(
            1, 2, 3, 4
          )
        )
        .jsonPath("$.content[0].whenCreated").isNotEmpty
    }

    @Test
    fun `get incentive mappings by migration id - no records exist`() {

      (1L..4L).forEach {
        postCreateIncentiveMappingRequest(
          nomisBookingId = it,
          incentiveId = it,
          label = "2022-01-01",
          mappingType = "MIGRATED"
        )
      }

      webTestClient.get().uri("/mapping/incentives/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
        .jsonPath("content").isEmpty
    }

    @Test
    fun `can request a different page size`() {

      (1L..6L).forEach {
        postCreateIncentiveMappingRequest(
          nomisBookingId = it,
          incentiveId = it,
          label = "2022-01-01",
          mappingType = "MIGRATED"
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/incentives/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("sort", "nomisBookingId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(6)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(3)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    fun `can request a different page`() {
      (1L..3L).forEach {
        postCreateIncentiveMappingRequest(
          nomisBookingId = it,
          incentiveId = it,
          label = "2022-01-01",
          mappingType = "MIGRATED"
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/incentives/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .queryParam("sort", "nomisBookingId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(1)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
    }
  }
}
