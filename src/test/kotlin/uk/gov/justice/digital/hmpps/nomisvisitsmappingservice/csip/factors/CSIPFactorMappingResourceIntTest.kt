package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors

import kotlinx.coroutines.runBlocking
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.CSIPFactorMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class CSIPFactorMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: CSIPFactorMappingRepository

  @Nested
  @DisplayName("POST /mapping/csip/factors")
  inner class CreateMapping {
    private lateinit var existingMapping: CSIPFactorMapping
    private val mapping = CSIPFactorMappingDto(
      dpsCSIPFactorId = "a018f95e-459d-4d0d-9ccd-1fddf4315b2a",
      nomisCSIPFactorId = 54321L,
      label = "2023-01-01T12:45:12",
      mappingType = MIGRATED,
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        CSIPFactorMapping(
          dpsCSIPFactorId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPFactorId = 12345L,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/csip/factors")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/csip/factors")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/csip/factors")
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
          .uri("/mapping/csip/factors")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMapping =
          repository.findOneByNomisCSIPFactorId(nomisCSIPFactorId = mapping.nomisCSIPFactorId)!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.dpsCSIPFactorId).isEqualTo(mapping.dpsCSIPFactorId)
        assertThat(createdMapping.dpsCSIPFactorId).isEqualTo(mapping.dpsCSIPFactorId)
        assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(createdMapping.label).isEqualTo(mapping.label)
      }

      @Test
      fun `can post and then get new and existing mapping`() {
        webTestClient.post()
          .uri("/mapping/csip/factors")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              // language=JSON
              """
                {
                  "nomisCSIPFactorId": 54321,
                  "dpsCSIPFactorId": "018f95e-459d-4d0d-9ccd-1fddf4315b2a"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        webTestClient.get()
          .uri("/mapping/csip/factors/nomis-csip-factor-id/54321")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/csip/factors/nomis-csip-factor-id/${existingMapping.nomisCSIPFactorId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when mapping type is invalid`() {
        webTestClient.post()
          .uri("/mapping/csip/factors")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCSIPFactorId": 54321,
                  "dpsCSIPFactorId": "e52d7268-6e10-41a8-a0b9-2319b32520d6",
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
          .uri("/mapping/csip/factors")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCSIPFactorId": 54321
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when NOMIS id is missing`() {
        webTestClient.post()
          .uri("/mapping/csip/factors")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "dpsCSIPFactorId": "5f70a789-7f36-4bec-87dd-fde1a9a995d8"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest

        webTestClient.post()
          .uri("/mapping/csip/factors")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "dpsCSIPFactorId": "5f70a789-7f36-4bec-87dd-fde1a9a995d8"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 409 if nomis ids already exist`() {
        val dpsCSIPFactorId = UUID.randomUUID().toString()
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/csip/factors")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CSIPFactorMappingDto(
                nomisCSIPFactorId = existingMapping.nomisCSIPFactorId,
                dpsCSIPFactorId = dpsCSIPFactorId,
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
            .containsEntry("nomisCSIPFactorId", existingMapping.nomisCSIPFactorId.toInt())
            .containsEntry("dpsCSIPFactorId", existingMapping.dpsCSIPFactorId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCSIPFactorId", existingMapping.nomisCSIPFactorId.toInt())
            .containsEntry("dpsCSIPFactorId", dpsCSIPFactorId)
        }
      }

      @Test
      fun `returns 409 if dps id already exist`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/csip/factors")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CSIPFactorMappingDto(
                nomisCSIPFactorId = 123123,
                dpsCSIPFactorId = existingMapping.dpsCSIPFactorId,
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
            .containsEntry("nomisCSIPFactorId", existingMapping.nomisCSIPFactorId.toInt())
            .containsEntry("dpsCSIPFactorId", existingMapping.dpsCSIPFactorId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCSIPFactorId", 123123)
            .containsEntry("dpsCSIPFactorId", existingMapping.dpsCSIPFactorId)
        }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/csip/factors/dps-csip-factor-id/{dpsCSIPFactorId}")
  inner class DeleteMapping {
    lateinit var mapping: CSIPFactorMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CSIPFactorMapping(
          dpsCSIPFactorId = "edcd118c-41ba-42ea-b5c4-404b453ad5aa",
          nomisCSIPFactorId = 8912L,
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
        webTestClient.delete()
          .uri("/mapping/csip/factors/dps-csip-factor-id/${mapping.dpsCSIPFactorId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/csip/factors/dps-csip-factor-id/${mapping.dpsCSIPFactorId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/csip/factors/dps-csip-factor-id/${mapping.dpsCSIPFactorId}")
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
          .uri("/mapping/csip/factors/dps-csip-factor-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/csip/factors/dps-csip-factor-id/${mapping.dpsCSIPFactorId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/csip/factors/dps-csip-factor-id/${mapping.dpsCSIPFactorId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/csip/factors/dps-csip-factor-id/${mapping.dpsCSIPFactorId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/csip/factors/nomis-csip-factor-id/{nomisCSIPFactorId}")
  inner class GetMappingByNomisId {
    lateinit var mapping: CSIPFactorMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CSIPFactorMapping(
          dpsCSIPFactorId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisCSIPFactorId = 2345L,
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
          .uri("/mapping/csip/factors/nomis-csip-factor-id/${mapping.nomisCSIPFactorId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/csip/factors/nomis-csip-factor-id/${mapping.nomisCSIPFactorId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/csip/factors/nomis-csip-factor-id/${mapping.nomisCSIPFactorId}")
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
          .uri("/mapping/csip/factors/nomis-csip-factor-id/99999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/csip/factors/nomis-csip-factor-id/${mapping.nomisCSIPFactorId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCSIPFactorId").isEqualTo(mapping.nomisCSIPFactorId)
          .jsonPath("dpsCSIPFactorId").isEqualTo(mapping.dpsCSIPFactorId)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/csip/factors/dps-csip-factor-id/{dpsCSIPFactorId}")
  inner class GetMappingByDpsId {
    lateinit var mapping: CSIPFactorMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CSIPFactorMapping(
          dpsCSIPFactorId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisCSIPFactorId = 54321L,
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
          .uri("/mapping/csip/factors/dps-csip-factor-id/${mapping.dpsCSIPFactorId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/csip/factors/dps-csip-factor-id/${mapping.dpsCSIPFactorId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/csip/factors/dps-csip-factor-id/${mapping.dpsCSIPFactorId}")
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
          .uri("/mapping/csip/factors/dps-csip-factor-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/csip/factors/dps-csip-factor-id/${mapping.dpsCSIPFactorId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCSIPFactorId").isEqualTo(mapping.nomisCSIPFactorId)
          .jsonPath("dpsCSIPFactorId").isEqualTo(mapping.dpsCSIPFactorId)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }
}
