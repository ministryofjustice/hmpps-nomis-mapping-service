package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hamcrest.Matchers
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts.AlertMappingType.NOMIS_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class AlertMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: AlertsMappingRepository

  @Autowired
  private lateinit var alertPrisonerRepository: AlertPrisonerMappingRepository

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
          offenderNo = "A1234KT",
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      alertPrisonerRepository.deleteAll()
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
          .jsonPath("offenderNo").isEqualTo(mapping.offenderNo!!)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/alerts/dps-alert-id/{dpsAlertId}")
  inner class GetMappingByDpsId {
    lateinit var mapping: AlertMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        AlertMapping(
          dpsAlertId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisBookingId = 54321L,
          nomisAlertSequence = 2L,
          offenderNo = null,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      alertPrisonerRepository.deleteAll()
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/alerts/dps-alert-id/${mapping.dpsAlertId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/alerts/dps-alert-id/${mapping.dpsAlertId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/alerts/dps-alert-id/${mapping.dpsAlertId}")
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
          .uri("/mapping/alerts/dps-alert-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/alerts/dps-alert-id/${mapping.dpsAlertId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisBookingId").isEqualTo(mapping.nomisBookingId)
          .jsonPath("nomisAlertSequence").isEqualTo(mapping.nomisAlertSequence)
          .jsonPath("dpsAlertId").isEqualTo(mapping.dpsAlertId)
          .jsonPath("offenderNo").doesNotExist()
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/alerts/dps-alert-id/{dpsAlertId}")
  inner class DeleteMappingByDpsId {
    lateinit var mapping: AlertMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        AlertMapping(
          dpsAlertId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisBookingId = 54321L,
          nomisAlertSequence = 2L,
          offenderNo = "A1234KT",
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      alertPrisonerRepository.deleteAll()
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/alerts/dps-alert-id/${mapping.dpsAlertId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/alerts/dps-alert-id/${mapping.dpsAlertId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/alerts/dps-alert-id/${mapping.dpsAlertId}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return 204 even when mapping does not exist`() {
        webTestClient.delete()
          .uri("/mapping/alerts/dps-alert-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/alerts/dps-alert-id/${mapping.dpsAlertId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/alerts/dps-alert-id/${mapping.dpsAlertId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/alerts/dps-alert-id/${mapping.dpsAlertId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isNotFound
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
      offenderNo = "A1234KT",
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
          offenderNo = "A1234KT",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      alertPrisonerRepository.deleteAll()
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
                offenderNo = existingMapping.offenderNo,
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
                offenderNo = existingMapping.offenderNo,
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

  @Nested
  @DisplayName("POST /mapping/alerts/all")
  inner class CreateMappings {
    private lateinit var existingMapping: AlertMapping
    private val mappings = listOf(
      AlertMappingDto(
        dpsAlertId = "e52d7268-6e10-41a8-a0b9-2319b32520d6",
        nomisBookingId = 54321L,
        nomisAlertSequence = 3L,
        label = "2023-01-01T12:45:12",
        mappingType = MIGRATED,
        offenderNo = "A1234KT",
      ),
      AlertMappingDto(
        dpsAlertId = "fd4e55a8-0805-439b-9e27-647583b96e4e",
        nomisBookingId = 54321L,
        nomisAlertSequence = 4L,
        label = "2023-01-01T12:45:12",
        mappingType = MIGRATED,
        offenderNo = "A1234KT",
      ),
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
          offenderNo = "A1234KT",
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      alertPrisonerRepository.deleteAll()
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/alerts/all")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/alerts/all")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/alerts/all")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns 201 when mapping created`() = runTest {
        webTestClient.post()
          .uri("/mapping/alerts/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated

        val createdMapping1 =
          repository.findOneByNomisBookingIdAndNomisAlertSequence(
            bookingId = mappings[0].nomisBookingId,
            alertSequence = mappings[0].nomisAlertSequence,
          )!!

        assertThat(createdMapping1.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping1.nomisBookingId).isEqualTo(mappings[0].nomisBookingId)
        assertThat(createdMapping1.nomisAlertSequence).isEqualTo(mappings[0].nomisAlertSequence)
        assertThat(createdMapping1.dpsAlertId).isEqualTo(mappings[0].dpsAlertId)
        assertThat(createdMapping1.mappingType).isEqualTo(mappings[0].mappingType)
        assertThat(createdMapping1.label).isEqualTo(mappings[0].label)
        val createdMapping2 =
          repository.findOneByNomisBookingIdAndNomisAlertSequence(
            bookingId = mappings[1].nomisBookingId,
            alertSequence = mappings[1].nomisAlertSequence,
          )!!

        assertThat(createdMapping2.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping2.nomisBookingId).isEqualTo(mappings[1].nomisBookingId)
        assertThat(createdMapping2.nomisAlertSequence).isEqualTo(mappings[1].nomisAlertSequence)
        assertThat(createdMapping2.dpsAlertId).isEqualTo(mappings[1].dpsAlertId)
        assertThat(createdMapping2.mappingType).isEqualTo(mappings[1].mappingType)
        assertThat(createdMapping2.label).isEqualTo(mappings[1].label)
      }

      @Test
      fun `can create with minimal data`() = runTest {
        webTestClient.post()
          .uri("/mapping/alerts/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                [
                  {
                    "nomisBookingId": 54321,
                    "nomisAlertSequence": 3,
                    "dpsAlertId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                  }
                ]
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
          .uri("/mapping/alerts/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                [
                  {
                    "nomisBookingId": 54321,
                    "nomisAlertSequence": 3,
                    "dpsAlertId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                  }
                ]
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
          .uri("/mapping/alerts/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                [
                  {
                    "nomisBookingId": 54321,
                    "nomisAlertSequence": 3,
                    "dpsAlertId": "e52d7268-6e10-41a8-a0b9-2319b32520d6",
                    "mappingType": "INVALID_TYPE"
                  }
                ]
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when DPS id is missing`() {
        webTestClient.post()
          .uri("/mapping/alerts/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                [
                  {
                    "nomisBookingId": 54321,
                    "nomisAlertSequence": 3
                  }
                ]
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when NOMIS ids are missing`() {
        webTestClient.post()
          .uri("/mapping/alerts/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                [
                  {
                    "nomisAlertSequence": 3,
                    "dpsAlertId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                  }
                ]
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest

        webTestClient.post()
          .uri("/mapping/alerts/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                [
                  {
                    "nomisBookingId": 54321,
                    "dpsAlertId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                  }
                ]
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
          .uri("/mapping/alerts/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings +
                AlertMappingDto(
                  nomisBookingId = existingMapping.nomisBookingId,
                  nomisAlertSequence = existingMapping.nomisAlertSequence,
                  dpsAlertId = dpsAlertId,
                  offenderNo = existingMapping.offenderNo,
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
            .containsEntry("offenderNo", existingMapping.offenderNo)
            .containsEntry("dpsAlertId", existingMapping.dpsAlertId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisAlertSequence", existingMapping.nomisAlertSequence.toInt())
            .containsEntry("offenderNo", existingMapping.offenderNo)
            .containsEntry("dpsAlertId", dpsAlertId)
        }
      }

      @Test
      fun `returns 409 if dps id already exist`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/alerts/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings + AlertMappingDto(
                nomisBookingId = existingMapping.nomisBookingId,
                nomisAlertSequence = 99,
                dpsAlertId = existingMapping.dpsAlertId,
                offenderNo = existingMapping.offenderNo,
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

  @Nested
  @DisplayName("POST /mapping/alerts/{offenderNo}/all")
  inner class CreateMappingsForPrisoner {
    private lateinit var existingMapping: AlertMapping
    private val prisonerMappings = PrisonerAlertMappingsDto(
      label = "2023-01-01T12:45:12",
      mappingType = MIGRATED,
      mappings = listOf(
        AlertMappingIdDto(
          dpsAlertId = "e52d7268-6e10-41a8-a0b9-2319b32520d6",
          nomisBookingId = 54321L,
          nomisAlertSequence = 3L,
        ),
        AlertMappingIdDto(
          dpsAlertId = "fd4e55a8-0805-439b-9e27-647583b96e4e",
          nomisBookingId = 54321L,
          nomisAlertSequence = 4L,
        ),
      ),
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
          offenderNo = "A1234KT",
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      alertPrisonerRepository.deleteAll()
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/alerts/A1234KT/all")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(prisonerMappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/alerts/A1234KT/all")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(prisonerMappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/alerts/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(prisonerMappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns 201 when mapping created`() = runTest {
        webTestClient.post()
          .uri("/mapping/alerts/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(prisonerMappings))
          .exchange()
          .expectStatus().isCreated

        val createdMapping1 =
          repository.findOneByNomisBookingIdAndNomisAlertSequence(
            bookingId = prisonerMappings.mappings[0].nomisBookingId,
            alertSequence = prisonerMappings.mappings[0].nomisAlertSequence,
          )!!

        assertThat(createdMapping1.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping1.nomisBookingId).isEqualTo(prisonerMappings.mappings[0].nomisBookingId)
        assertThat(createdMapping1.nomisAlertSequence).isEqualTo(prisonerMappings.mappings[0].nomisAlertSequence)
        assertThat(createdMapping1.dpsAlertId).isEqualTo(prisonerMappings.mappings[0].dpsAlertId)
        assertThat(createdMapping1.mappingType).isEqualTo(prisonerMappings.mappingType)
        assertThat(createdMapping1.label).isEqualTo(prisonerMappings.label)
        val createdMapping2 =
          repository.findOneByNomisBookingIdAndNomisAlertSequence(
            bookingId = prisonerMappings.mappings[1].nomisBookingId,
            alertSequence = prisonerMappings.mappings[1].nomisAlertSequence,
          )!!

        assertThat(createdMapping2.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping2.nomisBookingId).isEqualTo(prisonerMappings.mappings[1].nomisBookingId)
        assertThat(createdMapping2.nomisAlertSequence).isEqualTo(prisonerMappings.mappings[1].nomisAlertSequence)
        assertThat(createdMapping2.dpsAlertId).isEqualTo(prisonerMappings.mappings[1].dpsAlertId)
        assertThat(createdMapping2.mappingType).isEqualTo(prisonerMappings.mappingType)
        assertThat(createdMapping2.label).isEqualTo(prisonerMappings.label)
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when mapping type is invalid`() {
        webTestClient.post()
          .uri("/mapping/alerts/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "mappingType": "INVALID_TYPE",
                  "mappings": [
                    {
                      "nomisBookingId": 54321,
                      "nomisAlertSequence": 3,
                      "dpsAlertId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                    }
                  ]
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
          .uri("/mapping/alerts/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "mappingType": "MIGRATED",
                  "mappings": [
                    {
                      "nomisBookingId": 54321,
                      "nomisAlertSequence": 3
                    }
                  ]
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
          .uri("/mapping/alerts/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "mappingType": "MIGRATED",
                  "mappings": [
                    {
                      "dpsAlertId": "e52d7268-6e10-41a8-a0b9-2319b32520d6",
                      "nomisAlertSequence": 3
                    }
                  ]
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest

        webTestClient.post()
          .uri("/mapping/alerts/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "mappingType": "MIGRATED",
                  "mappings": [
                    {
                      "nomisBookingId": 54321,
                      "dpsAlertId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                    }
                  ]
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
          .uri("/mapping/alerts/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              prisonerMappings.copy(
                mappings = prisonerMappings.mappings + AlertMappingIdDto(
                  nomisBookingId = existingMapping.nomisBookingId,
                  nomisAlertSequence = existingMapping.nomisAlertSequence,
                  dpsAlertId = dpsAlertId,
                ),
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
            .containsEntry("offenderNo", existingMapping.offenderNo)
            .containsEntry("dpsAlertId", existingMapping.dpsAlertId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisAlertSequence", existingMapping.nomisAlertSequence.toInt())
            .containsEntry("offenderNo", existingMapping.offenderNo)
            .containsEntry("dpsAlertId", dpsAlertId)
        }
      }

      @Test
      fun `returns 409 if dps id already exist`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/alerts/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              prisonerMappings.copy(
                mappings = prisonerMappings.mappings + AlertMappingIdDto(
                  nomisBookingId = existingMapping.nomisBookingId,
                  nomisAlertSequence = 99,
                  dpsAlertId = existingMapping.dpsAlertId,
                ),
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

  @Nested
  @DisplayName("DELETE /mapping/alerts")
  inner class DeleteAllMappings {
    private lateinit var existingMapping1: AlertMapping
    private lateinit var existingMapping2: AlertMapping

    @BeforeEach
    fun setUp() = runTest {
      existingMapping1 = repository.save(
        AlertMapping(
          dpsAlertId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisBookingId = 54321L,
          nomisAlertSequence = 2L,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
          offenderNo = "A1234KT",
        ),
      )
      existingMapping2 = repository.save(
        AlertMapping(
          dpsAlertId = "4433eb7d-2fa0-4055-99d9-633fefa53288",
          nomisBookingId = 54321L,
          nomisAlertSequence = 3L,
          mappingType = NOMIS_CREATED,
          offenderNo = "A1234KT",
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      alertPrisonerRepository.deleteAll()
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/alerts")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/alerts")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns 204 when all mappings are deleted`() = runTest {
        assertThat(
          repository.findOneByNomisBookingIdAndNomisAlertSequence(
            bookingId = existingMapping1.nomisBookingId,
            alertSequence = existingMapping1.nomisAlertSequence,
          ),
        ).isNotNull
        assertThat(
          repository.findOneByNomisBookingIdAndNomisAlertSequence(
            bookingId = existingMapping2.nomisBookingId,
            alertSequence = existingMapping2.nomisAlertSequence,
          ),
        ).isNotNull

        webTestClient.delete()
          .uri("/mapping/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(
          repository.findOneByNomisBookingIdAndNomisAlertSequence(
            bookingId = existingMapping1.nomisBookingId,
            alertSequence = existingMapping1.nomisAlertSequence,
          ),
        ).isNull()
        assertThat(
          repository.findOneByNomisBookingIdAndNomisAlertSequence(
            bookingId = existingMapping2.nomisBookingId,
            alertSequence = existingMapping2.nomisAlertSequence,
          ),
        ).isNull()
      }
    }
  }

  @DisplayName("GET /mapping/alerts/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationIdTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      alertPrisonerRepository.deleteAll()
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/alerts/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/alerts/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/alerts/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Test
    fun `can retrieve all mappings by migration Id`() = runTest {
      (1L..4L).forEach {
        repository.save(
          AlertMapping(
            dpsAlertId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
            nomisBookingId = 54321L,
            nomisAlertSequence = it,
            label = "2023-01-01T12:45:12",
            mappingType = MIGRATED,
            offenderNo = "A1234KT",
          ),
        )
      }

      repository.save(
        AlertMapping(
          dpsAlertId = "edcd118c-91ba-42ea-b5c4-404b453ad58b",
          nomisBookingId = 54321L,
          nomisAlertSequence = 99,
          label = "2022-01-01T12:43:12",
          mappingType = MIGRATED,
          offenderNo = "A1234KT",
        ),
      )

      webTestClient.get().uri("/mapping/alerts/migration-id/2023-01-01T12:45:12")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..nomisAlertSequence").value(
          Matchers.contains(
            1,
            2,
            3,
            4,
          ),
        )
        .jsonPath("$.content[0].whenCreated").isNotEmpty
    }

    @Test
    fun `200 response even when no mappings are found`() {
      webTestClient.get().uri("/mapping/alerts/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
        .jsonPath("content").isEmpty
    }

    @Test
    fun `can request a different page size`() = runTest {
      (1L..6L).forEach {
        repository.save(
          AlertMapping(
            dpsAlertId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
            nomisBookingId = 54321L,
            nomisAlertSequence = it,
            label = "2023-01-01T12:45:12",
            mappingType = MIGRATED,
            offenderNo = "A1234KT",
          ),
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/alerts/migration-id/2023-01-01T12:45:12")
          .queryParam("size", "2")
          .queryParam("sort", "nomisAlertSequence,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(6)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(3)
        .jsonPath("size").isEqualTo(2)
    }
  }

  @DisplayName("GET /mapping/alerts/migration-id/{migrationId}/grouped-by-prisoner")
  @Nested
  inner class GetMappingByMigrationIdGroupedByPrisoner {

    @AfterEach
    internal fun deleteData() = runBlocking {
      alertPrisonerRepository.deleteAll()
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/alerts/migration-id/2022-01-01T00:00:00/grouped-by-prisoner")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/alerts/migration-id/2022-01-01T00:00:00/grouped-by-prisoner")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/alerts/migration-id/2022-01-01T00:00:00/grouped-by-prisoner")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Test
    fun `can retrieve all mappings by migration Id when created via the 'all' endpoint`() = runTest {
      webTestClient.post()
        .uri("/mapping/alerts/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            (1L..4L).map {
              AlertMappingDto(
                dpsAlertId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
                nomisBookingId = 54321L,
                nomisAlertSequence = it,
                label = "2023-01-01T12:45:12",
                mappingType = MIGRATED,
                offenderNo = "A1111KT",
              )
            },
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post()
        .uri("/mapping/alerts/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            (5L..6L).map {
              AlertMappingDto(
                dpsAlertId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
                nomisBookingId = 54321L,
                nomisAlertSequence = it,
                label = "2023-01-01T12:45:12",
                mappingType = MIGRATED,
                offenderNo = "A2222KT",
              )
            },
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post()
        .uri("/mapping/alerts/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            listOf(
              AlertMappingDto(
                dpsAlertId = "edcd118c-91ba-42ea-b5c4-404b453ad58b",
                nomisBookingId = 54321L,
                nomisAlertSequence = 99,
                label = "2022-01-01T12:43:12",
                mappingType = MIGRATED,
                offenderNo = "A1234KT",
              ),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/alerts/migration-id/2023-01-01T12:45:12/grouped-by-prisoner")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(2)
        .jsonPath("$.content..offenderNo").value(
          Matchers.contains(
            "A1111KT",
            "A2222KT",
          ),
        )
        .jsonPath("$.content..mappingsCount").value(
          Matchers.contains(
            4,
            2,
          ),
        )
    }

    @Test
    fun `can retrieve all mappings by migration Id when created via the 'offenderNo all' endpoint`() = runTest {
      webTestClient.post()
        .uri("/mapping/alerts/A1111KT/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            PrisonerAlertMappingsDto(
              label = "2023-01-01T12:45:12",
              mappingType = MIGRATED,
              mappings = (1L..4L).map {
                AlertMappingIdDto(
                  dpsAlertId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
                  nomisBookingId = 54321L,
                  nomisAlertSequence = it,
                )
              },
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post()
        .uri("/mapping/alerts/A2222KT/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            PrisonerAlertMappingsDto(
              label = "2023-01-01T12:45:12",
              mappingType = MIGRATED,
              mappings = (5L..6L).map {
                AlertMappingIdDto(
                  dpsAlertId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
                  nomisBookingId = 54321L,
                  nomisAlertSequence = it,
                )
              },
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post()
        .uri("/mapping/alerts/A1234KT/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            PrisonerAlertMappingsDto(
              label = "2022-01-01T12:43:12",
              mappingType = MIGRATED,
              mappings = listOf(
                AlertMappingIdDto(
                  dpsAlertId = "edcd118c-91ba-42ea-b5c4-404b453ad58b",
                  nomisBookingId = 54321L,
                  nomisAlertSequence = 99,
                ),
              ),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/alerts/migration-id/2023-01-01T12:45:12/grouped-by-prisoner")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(2)
        .jsonPath("$.content..offenderNo").value(
          Matchers.contains(
            "A1111KT",
            "A2222KT",
          ),
        )
        .jsonPath("$.content..mappingsCount").value(
          Matchers.contains(
            4,
            2,
          ),
        )
    }

    @Test
    fun `200 response even when no mappings are found`() {
      webTestClient.get().uri("/mapping/alerts/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
        .jsonPath("content").isEmpty
    }

    @Test
    fun `can request a different page size`() = runTest {
      (1L..6L).forEach {
        repository.save(
          AlertMapping(
            dpsAlertId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
            nomisBookingId = 54321L,
            nomisAlertSequence = it,
            label = "2023-01-01T12:45:12",
            mappingType = MIGRATED,
            offenderNo = "A1234KT",
          ),
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/alerts/migration-id/2023-01-01T12:45:12")
          .queryParam("size", "2")
          .queryParam("sort", "nomisAlertSequence,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(6)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(3)
        .jsonPath("size").isEqualTo(2)
    }
  }
}
