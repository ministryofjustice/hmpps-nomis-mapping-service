package uk.gov.justice.digital.hmpps.nomismappingservice.visitbalances

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
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.visitbalances.VisitBalanceAdjustmentMappingType.DPS_CREATED
import uk.gov.justice.digital.hmpps.nomismappingservice.visitbalances.VisitBalanceAdjustmentMappingType.NOMIS_CREATED
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class VisitBalanceAdjustmentMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: VisitBalanceAdjustmentMappingRepository

  @Nested
  @DisplayName("GET /mapping/visit-balance-adjustment/nomis-id/{nomisVisitBalanceAdjustmentId}")
  inner class GetMappingByNomisId {
    lateinit var mapping: VisitBalanceAdjustmentMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        VisitBalanceAdjustmentMapping(
          dpsId = "A1234KT",
          nomisId = 12345,
          label = "2023-01-01T12:45:12",
          mappingType = DPS_CREATED,
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
          .uri("/mapping/visit-balance-adjustment/nomis-id/${mapping.nomisId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/visit-balance-adjustment/nomis-id/${mapping.nomisId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/visit-balance-adjustment/nomis-id/${mapping.nomisId}")
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
          .uri("/mapping/visit-balance-adjustment/nomis-id/9999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/visit-balance-adjustment/nomis-id/${mapping.nomisId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(mapping.dpsId)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("nomisVisitBalanceAdjustmentId").isEqualTo(mapping.nomisId)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/visit-balance-adjustment/dps-id/{dpsId}")
  inner class GetMappingByDpsId {
    lateinit var mapping: VisitBalanceAdjustmentMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        VisitBalanceAdjustmentMapping(
          dpsId = "A1234KT",
          nomisId = 12345,
          label = "2023-01-01T12:45:12",
          mappingType = DPS_CREATED,
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
          .uri("/mapping/visit-balance-adjustment/dps-id/${mapping.dpsId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/visit-balance-adjustment/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/visit-balance-adjustment/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return empty list when mapping does not exist`() {
        webTestClient.get()
          .uri("/mapping/visit-balance-adjustment/dps-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(0)
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/visit-balance-adjustment/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("[0].dpsId").isEqualTo(mapping.dpsId)
          .jsonPath("[0].nomisVisitBalanceAdjustmentId").isEqualTo(mapping.nomisId)
          .jsonPath("[0].mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("[0].label").isEqualTo(mapping.label!!)
          .jsonPath("[0].whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/visit-balance-adjustment/dps-id/{dpsId}")
  inner class DeleteMappingsByDpsId {
    lateinit var mapping: VisitBalanceAdjustmentMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        VisitBalanceAdjustmentMapping(
          dpsId = "A1234KT",
          nomisId = 12345,
          label = "2023-01-01T12:45:12",
          mappingType = DPS_CREATED,
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
          .uri("/mapping/visit-balance-adjustment/dps-id/${mapping.dpsId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/visit-balance-adjustment/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/visit-balance-adjustment/dps-id/${mapping.dpsId}")
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
          .uri("/mapping/visit-balance-adjustment/dps-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/visit-balance-adjustment/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(1)

        webTestClient.delete()
          .uri("/mapping/visit-balance-adjustment/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/visit-balance-adjustment/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(0)
      }

      @Test
      fun `will delete multiple mappings by dpsId`() {
        runTest {
          mapping = repository.save(
            VisitBalanceAdjustmentMapping(
              dpsId = "A1234KT",
              nomisId = 6543,
              label = "2023-01-01T12:45:12",
              mappingType = DPS_CREATED,
            ),
          )
        }

        webTestClient.get()
          .uri("/mapping/visit-balance-adjustment/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(2)

        webTestClient.delete()
          .uri("/mapping/visit-balance-adjustment/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/visit-balance-adjustment/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(0)
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/visit-balance-adjustment")
  inner class CreateMapping {
    private lateinit var existingMapping: VisitBalanceAdjustmentMapping
    private val mapping = VisitBalanceAdjustmentMappingDto(
      dpsId = "A1234KU",
      nomisVisitBalanceAdjustmentId = 12346,
      label = "2023-01-01T12:45:12",
      mappingType = DPS_CREATED,
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        VisitBalanceAdjustmentMapping(
          dpsId = "A1234KT",
          nomisId = 12345,
          label = "2023-01-01T12:45:12",
          mappingType = DPS_CREATED,
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
          .uri("/mapping/visit-balance-adjustment")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/visit-balance-adjustment")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/visit-balance-adjustment")
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
          .uri("/mapping/visit-balance-adjustment")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMapping =
          repository.findOneByNomisId(
            nomisVisitBalanceAdjustmentId = mapping.nomisVisitBalanceAdjustmentId,
          )!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(createdMapping.label).isEqualTo(mapping.label)
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when mapping type is invalid`() {
        webTestClient.post()
          .uri("/mapping/visit-balance-adjustment")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisVisitBalanceAdjustmentId": "A1234BC",
                  "dpsId": "e52d7268-6e10-41a8-a0b9-2319b32520d6",
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
          .uri("/mapping/visit-balance-adjustment")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisVisitBalanceAdjustmentId": "A1234BC"
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
          .uri("/mapping/visit-balance-adjustment")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "dpsId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 409 if nomis id already exists`() {
        val dpsId = UUID.randomUUID().toString()
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/visit-balance-adjustment")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              VisitBalanceAdjustmentMappingDto(
                dpsId = dpsId,
                nomisVisitBalanceAdjustmentId = existingMapping.nomisId,
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
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisVisitBalanceAdjustmentId", existingMapping.nomisId.toInt())
            .containsEntry("dpsId", existingMapping.dpsId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisVisitBalanceAdjustmentId", existingMapping.nomisId.toInt())
            .containsEntry("dpsId", dpsId)
        }
      }

      @Test
      fun `Does not return 409 if dps id already exists`() {
        val nomisVisitBalanceAdjustmentId = -1L
        webTestClient.post()
          .uri("/mapping/visit-balance-adjustment")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              VisitBalanceAdjustmentMappingDto(
                dpsId = existingMapping.dpsId,
                nomisVisitBalanceAdjustmentId = nomisVisitBalanceAdjustmentId,
              ),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(201)
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/visit-balance-adjustment")
  inner class DeleteAllMappings {
    private lateinit var existingMapping1: VisitBalanceAdjustmentMapping
    private lateinit var existingMapping2: VisitBalanceAdjustmentMapping

    @BeforeEach
    fun setUp() = runTest {
      existingMapping1 = repository.save(
        VisitBalanceAdjustmentMapping(
          dpsId = "A1234KU",
          nomisId = 12345,
          label = "2023-01-01T12:45:12",
          mappingType = DPS_CREATED,
        ),
      )
      existingMapping2 = repository.save(
        VisitBalanceAdjustmentMapping(
          dpsId = "A1234KT",
          nomisId = 12346,
          mappingType = NOMIS_CREATED,
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
          .uri("/mapping/visit-balance-adjustment")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/visit-balance-adjustment")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/visit-balance-adjustment")
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
          repository.findOneByNomisId(
            nomisVisitBalanceAdjustmentId = existingMapping1.nomisId,
          ),
        ).isNotNull
        assertThat(
          repository.findOneByNomisId(
            nomisVisitBalanceAdjustmentId = existingMapping2.nomisId,
          ),
        ).isNotNull

        webTestClient.delete()
          .uri("/mapping/visit-balance-adjustment")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(
          repository.findOneByNomisId(
            nomisVisitBalanceAdjustmentId = existingMapping1.nomisId,
          ),
        ).isNull()
        assertThat(
          repository.findOneByNomisId(
            nomisVisitBalanceAdjustmentId = existingMapping2.nomisId,
          ),
        ).isNull()
      }
    }
  }
}
