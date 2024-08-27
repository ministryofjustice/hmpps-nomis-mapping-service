package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans

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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class CSIPPlanMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: CSIPPlanMappingRepository

  @Autowired
  private lateinit var csipReportRepository: CSIPMappingRepository

  @Nested
  @DisplayName("POST /mapping/csip/plans")
  inner class CreateMapping {
    private lateinit var existingMapping: CSIPPlanMapping

    private val mapping = CSIPPlanMappingDto(
      dpsCSIPPlanId = "a018f95e-459d-4d0d-9ccd-1fddf4315b2a",
      nomisCSIPPlanId = 54321L,
      label = "2023-01-01T12:45:12",
      mappingType = MIGRATED,
    )

    @BeforeEach
    fun setUp() = runTest {
      csipReportRepository.save(
        CSIPMapping(
          dpsCSIPId = "987",
          nomisCSIPId = 654,
          label = "TIMESTAMP",
          mappingType = CSIPMappingType.MIGRATED,
        ),
      )

      existingMapping = repository.save(
        CSIPPlanMapping(
          dpsCSIPPlanId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPPlanId = 12345L,
          dpsCSIPReportId = "987",
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
      csipReportRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/csip/plans")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/csip/plans")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/csip/plans")
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
          .uri("/mapping/csip/plans")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMapping =
          repository.findOneByNomisCSIPPlanId(nomisCSIPPlanId = mapping.nomisCSIPPlanId)!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.dpsCSIPPlanId).isEqualTo(mapping.dpsCSIPPlanId)
        assertThat(createdMapping.nomisCSIPPlanId).isEqualTo(mapping.nomisCSIPPlanId)
        assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(createdMapping.label).isEqualTo(mapping.label)
      }

      @Test
      fun `can post and then get new and existing mapping`() {
        webTestClient.post()
          .uri("/mapping/csip/plans")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              // language=JSON
              """
                {
                  "nomisCSIPPlanId": 54321,
                  "dpsCSIPPlanId": "018f95e-459d-4d0d-9ccd-1fddf4315b2a",
                  "dpsCSIPReportId": "987"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        webTestClient.get()
          .uri("/mapping/csip/plans/nomis-csip-plan-id/54321")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/csip/plans/nomis-csip-plan-id/${existingMapping.nomisCSIPPlanId}")
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
          .uri("/mapping/csip/plans")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCSIPPlanId": 54321,
                  "dpsCSIPPlanId": "e52d7268-6e10-41a8-a0b9-2319b32520d6",
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
          .uri("/mapping/csip/plans")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCSIPPlanId": 54321
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
          .uri("/mapping/csip/plans")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "dpsCSIPPlanId": "5f70a789-7f36-4bec-87dd-fde1a9a995d8"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest

        webTestClient.post()
          .uri("/mapping/csip/plans")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "dpsCSIPPlanId": "5f70a789-7f36-4bec-87dd-fde1a9a995d8"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 409 if nomis ids already exist`() {
        val dpsCSIPPlanId = UUID.randomUUID().toString()
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/csip/plans")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CSIPPlanMappingDto(
                nomisCSIPPlanId = existingMapping.nomisCSIPPlanId,
                dpsCSIPPlanId = dpsCSIPPlanId,
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
            .containsEntry("nomisCSIPPlanId", existingMapping.nomisCSIPPlanId.toInt())
            .containsEntry("dpsCSIPPlanId", existingMapping.dpsCSIPPlanId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCSIPPlanId", existingMapping.nomisCSIPPlanId.toInt())
            .containsEntry("dpsCSIPPlanId", dpsCSIPPlanId)
        }
      }

      @Test
      fun `returns 409 if dps id already exist`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/csip/plans")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CSIPPlanMappingDto(
                nomisCSIPPlanId = 123123,
                dpsCSIPPlanId = existingMapping.dpsCSIPPlanId,
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
            .containsEntry("nomisCSIPPlanId", existingMapping.nomisCSIPPlanId.toInt())
            .containsEntry("dpsCSIPPlanId", existingMapping.dpsCSIPPlanId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCSIPPlanId", 123123)
            .containsEntry("dpsCSIPPlanId", existingMapping.dpsCSIPPlanId)
        }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/csip/plans/dps-csip-plan-id/{dpsCSIPPlanId}")
  inner class DeleteMapping {
    lateinit var mapping: CSIPPlanMapping
    private lateinit var dpsCsipReportId: String

    @BeforeEach
    fun setUp() = runTest {
      dpsCsipReportId = csipReportRepository.save(
        CSIPMapping(
          dpsCSIPId = "987",
          nomisCSIPId = 654,
          label = "TIMESTAMP",
          mappingType = CSIPMappingType.MIGRATED,
        ),
      ).dpsCSIPId
      mapping = repository.save(
        CSIPPlanMapping(
          dpsCSIPPlanId = "edcd118c-41ba-42ea-b5c4-404b453ad5aa",
          nomisCSIPPlanId = 8912L,
          dpsCSIPReportId = dpsCsipReportId,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
      csipReportRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/csip/plans/dps-csip-plan-id/${mapping.dpsCSIPPlanId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/csip/plans/dps-csip-plan-id/${mapping.dpsCSIPPlanId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/csip/plans/dps-csip-plan-id/${mapping.dpsCSIPPlanId}")
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
          .uri("/mapping/csip/plans/dps-csip-plan-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/csip/plans/dps-csip-plan-id/${mapping.dpsCSIPPlanId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/csip/plans/dps-csip-plan-id/${mapping.dpsCSIPPlanId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/csip/plans/dps-csip-plan-id/${mapping.dpsCSIPPlanId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/csip/plans/nomis-csip-plan-id/{nomisCSIPPlanId}")
  inner class GetMappingByNomisId {
    lateinit var mapping: CSIPPlanMapping

    @BeforeEach
    fun setUp() = runTest {
      csipReportRepository.save(
        CSIPMapping(
          dpsCSIPId = "987",
          nomisCSIPId = 654,
          label = "TIMESTAMP",
          mappingType = CSIPMappingType.MIGRATED,
        ),
      )
      mapping = repository.save(
        CSIPPlanMapping(
          dpsCSIPPlanId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisCSIPPlanId = 2345L,
          dpsCSIPReportId = "987",
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
      csipReportRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/csip/plans/nomis-csip-plan-id/${mapping.nomisCSIPPlanId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/csip/plans/nomis-csip-plan-id/${mapping.nomisCSIPPlanId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/csip/plans/nomis-csip-plan-id/${mapping.nomisCSIPPlanId}")
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
          .uri("/mapping/csip/plans/nomis-csip-plan-id/99999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/csip/plans/nomis-csip-plan-id/${mapping.nomisCSIPPlanId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCSIPPlanId").isEqualTo(mapping.nomisCSIPPlanId)
          .jsonPath("dpsCSIPPlanId").isEqualTo(mapping.dpsCSIPPlanId)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/csip/plans/dps-csip-plan-id/{dpsCSIPPlanId}")
  inner class GetMappingByDpsId {
    lateinit var mapping: CSIPPlanMapping

    @BeforeEach
    fun setUp() = runTest {
      csipReportRepository.save(
        CSIPMapping(
          dpsCSIPId = "765",
          nomisCSIPId = 654,
          label = "TIMESTAMP",
          mappingType = CSIPMappingType.MIGRATED,
        ),
      )
      mapping = repository.save(
        CSIPPlanMapping(
          dpsCSIPPlanId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisCSIPPlanId = 54321L,
          dpsCSIPReportId = "765",
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
      csipReportRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/csip/plans/dps-csip-plan-id/${mapping.dpsCSIPPlanId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/csip/plans/dps-csip-plan-id/${mapping.dpsCSIPPlanId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/csip/plans/dps-csip-plan-id/${mapping.dpsCSIPPlanId}")
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
          .uri("/mapping/csip/plans/dps-csip-plan-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/csip/plans/dps-csip-plan-id/${mapping.dpsCSIPPlanId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCSIPPlanId").isEqualTo(mapping.nomisCSIPPlanId)
          .jsonPath("dpsCSIPPlanId").isEqualTo(mapping.dpsCSIPPlanId)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }
}
