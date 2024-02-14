package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

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
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts.AlertMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class AlertMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: AlertsMappingRepository

  @Nested
  @DisplayName("GET /mapping/alerts/nomis-booking-id/{bookingId}/nomis-alert-sequence/{alertSequence}")
  inner class GetMappingByNomisId {
    lateinit var mapping: AlertMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        AlertMapping(
          dpsAlertId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisBookingId = 54321L,
          nomisAlertSequence = 2L,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/alerts/nomis-booking-id/${mapping.nomisBookingId}/nomis-alert-sequence/${mapping.nomisAlertSequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/alerts/nomis-booking-id/${mapping.nomisBookingId}/nomis-alert-sequence/${mapping.nomisAlertSequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/alerts/nomis-booking-id/${mapping.nomisBookingId}/nomis-alert-sequence/${mapping.nomisAlertSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return 404 when mapping does not exist`() {
        webTestClient.get()
          .uri("/mapping/alerts/nomis-booking-id/9999/nomis-alert-sequence/${mapping.nomisAlertSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/alerts/nomis-booking-id/${mapping.nomisBookingId}/nomis-alert-sequence/${mapping.nomisAlertSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisBookingId").isEqualTo(mapping.nomisBookingId)
          .jsonPath("nomisAlertSequence").isEqualTo(mapping.nomisAlertSequence)
          .jsonPath("dpsAlertId").isEqualTo(mapping.dpsAlertId)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/alerts")
  inner class CreateMapping {
    private lateinit var existingMapping: AlertMapping
    private val mapping = AlertMappingDto(
      dpsAlertId = "e52d7268-6e10-41a8-a0b9-2319b32520d6",
      nomisBookingId = 54321L,
      nomisAlertSequence = 3L,
      label = "2023-01-01T12:45:12",
      mappingType = MIGRATED,
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        AlertMapping(
          dpsAlertId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisBookingId = 54321L,
          nomisAlertSequence = 2L,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/alerts")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/alerts")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns 201 when mapping created`() = runTest {
        webTestClient.post()
          .uri("/mapping/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMapping =
          repository.findOneByNomisBookingIdAndNomisAlertSequence(
            bookingId = mapping.nomisBookingId,
            alertSequence = mapping.nomisAlertSequence,
          )!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisBookingId).isEqualTo(mapping.nomisBookingId)
        assertThat(createdMapping.nomisAlertSequence).isEqualTo(mapping.nomisAlertSequence)
        assertThat(createdMapping.dpsAlertId).isEqualTo(mapping.dpsAlertId)
        assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(createdMapping.label).isEqualTo(mapping.label)
      }

      @Test
      fun `can create with minimal data`() = runTest {
        webTestClient.post()
          .uri("/mapping/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisBookingId": 54321,
                  "nomisAlertSequence": 3,
                  "dpsAlertId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        val createdMapping =
          repository.findOneByNomisBookingIdAndNomisAlertSequence(
            bookingId = 54321,
            alertSequence = 3,
          )!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisBookingId).isEqualTo(54321L)
        assertThat(createdMapping.nomisAlertSequence).isEqualTo(3L)
        assertThat(createdMapping.dpsAlertId).isEqualTo("e52d7268-6e10-41a8-a0b9-2319b32520d6")
        assertThat(createdMapping.mappingType).isEqualTo(AlertMappingType.DPS_CREATED)
        assertThat(createdMapping.label).isNull()
      }

      @Test
      fun `can post and then get new and existing mapping`() {
        webTestClient.post()
          .uri("/mapping/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisBookingId": 54321,
                  "nomisAlertSequence": 3,
                  "dpsAlertId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        webTestClient.get()
          .uri("/mapping/alerts/nomis-booking-id/54321/nomis-alert-sequence/3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/alerts/nomis-booking-id/${existingMapping.nomisBookingId}/nomis-alert-sequence/${existingMapping.nomisAlertSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when mapping type is invalid`() {
        webTestClient.post()
          .uri("/mapping/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisBookingId": 54321,
                  "nomisAlertSequence": 3,
                  "dpsAlertId": "e52d7268-6e10-41a8-a0b9-2319b32520d6",
                  "mappingType": "INVALID_TYPE"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when DPS id is missing`() {
        webTestClient.post()
          .uri("/mapping/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisBookingId": 54321,
                  "nomisAlertSequence": 3
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when NOMIS ids are missing`() {
        webTestClient.post()
          .uri("/mapping/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisAlertSequence": 3,
                  "dpsAlertId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest

        webTestClient.post()
          .uri("/mapping/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisBookingId": 54321,
                  "dpsAlertId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 409 if nomis ids already exist`() {
        val dpsAlertId = UUID.randomUUID().toString()
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              AlertMappingDto(
                nomisBookingId = existingMapping.nomisBookingId,
                nomisAlertSequence = existingMapping.nomisAlertSequence,
                dpsAlertId = dpsAlertId,
              ),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          // since this is an untyped map an int will be assumed for such small numbers
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisAlertSequence", existingMapping.nomisAlertSequence.toInt())
            .containsEntry("dpsAlertId", existingMapping.dpsAlertId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisAlertSequence", existingMapping.nomisAlertSequence.toInt())
            .containsEntry("dpsAlertId", dpsAlertId)
        }
      }

      @Test
      fun `returns 409 if dps id already exist`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              AlertMappingDto(
                nomisBookingId = existingMapping.nomisBookingId,
                nomisAlertSequence = 99,
                dpsAlertId = existingMapping.dpsAlertId,
              ),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          // since this is an untyped map an int will be assumed for such small numbers
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisAlertSequence", existingMapping.nomisAlertSequence.toInt())
            .containsEntry("dpsAlertId", existingMapping.dpsAlertId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisAlertSequence", 99)
            .containsEntry("dpsAlertId", existingMapping.dpsAlertId)
        }
      }
    }
  }
}
