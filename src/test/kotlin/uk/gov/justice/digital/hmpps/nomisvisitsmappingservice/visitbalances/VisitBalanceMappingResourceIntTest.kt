package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.visitbalances

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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.visitbalances.VisitBalanceMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.visitbalances.VisitBalanceMappingType.NOMIS_CREATED
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class VisitBalanceMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: VisitBalanceMappingRepository

  @Nested
  @DisplayName("GET /mapping/visit-balance/nomis-id/{nomisVisitBalanceId}")
  inner class GetMappingByNomisId {
    lateinit var mapping: VisitBalanceMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        VisitBalanceMapping(
          dpsId = "A1234KT",
          nomisVisitBalanceId = 12345,
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
          .uri("/mapping/visit-balance/nomis-id/${mapping.nomisVisitBalanceId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/visit-balance/nomis-id/${mapping.nomisVisitBalanceId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/visit-balance/nomis-id/${mapping.nomisVisitBalanceId}")
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
          .uri("/mapping/visit-balance/nomis-id/9999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/visit-balance/nomis-id/${mapping.nomisVisitBalanceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(mapping.dpsId)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("nomisVisitBalanceId").isEqualTo(mapping.nomisVisitBalanceId)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/visit-balance/dps-id/{dpsId}")
  inner class GetMappingByDpsId {
    lateinit var mapping: VisitBalanceMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        VisitBalanceMapping(
          dpsId = "A1234KT",
          nomisVisitBalanceId = 12345,
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
          .uri("/mapping/visit-balance/dps-id/${mapping.dpsId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/visit-balance/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/visit-balance/dps-id/${mapping.dpsId}")
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
          .uri("/mapping/visit-balance/dps-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/visit-balance/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(mapping.dpsId)
          .jsonPath("nomisVisitBalanceId").isEqualTo(mapping.nomisVisitBalanceId)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/visit-balance/dps-id/{dpsId}")
  inner class DeleteMappingByDpsId {
    lateinit var mapping: VisitBalanceMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        VisitBalanceMapping(
          dpsId = "A1234KT",
          nomisVisitBalanceId = 12345,
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
          .uri("/mapping/visit-balance/dps-id/${mapping.dpsId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/visit-balance/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/visit-balance/dps-id/${mapping.dpsId}")
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
          .uri("/mapping/visit-balance/dps-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/visit-balance/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/visit-balance/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/visit-balance/dps-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/visit-balance")
  inner class CreateMapping {
    private lateinit var existingMapping: VisitBalanceMapping
    private val mapping = VisitBalanceMappingDto(
      dpsId = "A1234KU",
      nomisVisitBalanceId = 12346,
      label = "2023-01-01T12:45:12",
      mappingType = MIGRATED,
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        VisitBalanceMapping(
          dpsId = "A1234KT",
          nomisVisitBalanceId = 12345,
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
          .uri("/mapping/visit-balance")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/visit-balance")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/visit-balance")
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
          .uri("/mapping/visit-balance")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMapping =
          repository.findOneByNomisVisitBalanceId(
            nomisVisitBalanceId = mapping.nomisVisitBalanceId,
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
          .uri("/mapping/visit-balance")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisVisitBalanceId": "A1234BC",
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
          .uri("/mapping/visit-balance")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisVisitBalanceId": "A1234BC"
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
          .uri("/mapping/visit-balance")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
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
          .uri("/mapping/visit-balance")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              VisitBalanceMappingDto(
                dpsId = dpsId,
                nomisVisitBalanceId = existingMapping.nomisVisitBalanceId,
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
            .containsEntry("nomisVisitBalanceId", existingMapping.nomisVisitBalanceId.toInt())
            .containsEntry("dpsId", existingMapping.dpsId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisVisitBalanceId", existingMapping.nomisVisitBalanceId.toInt())
            .containsEntry("dpsId", dpsId)
        }
      }

      @Test
      fun `returns 409 if dps id already exist`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/visit-balance")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              VisitBalanceMappingDto(
                dpsId = existingMapping.dpsId,
                nomisVisitBalanceId = existingMapping.nomisVisitBalanceId,
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
            .containsEntry("nomisVisitBalanceId", existingMapping.nomisVisitBalanceId.toInt())
            .containsEntry("dpsId", existingMapping.dpsId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisVisitBalanceId", existingMapping.nomisVisitBalanceId.toInt())
            .containsEntry("dpsId", existingMapping.dpsId)
        }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/visit-balance")
  inner class DeleteAllMappings {
    private lateinit var existingMapping1: VisitBalanceMapping
    private lateinit var existingMapping2: VisitBalanceMapping

    @BeforeEach
    fun setUp() = runTest {
      existingMapping1 = repository.save(
        VisitBalanceMapping(
          dpsId = "A1234KU",
          nomisVisitBalanceId = 12345,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
      existingMapping2 = repository.save(
        VisitBalanceMapping(
          dpsId = "A1234KT",
          nomisVisitBalanceId = 12346,
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
          .uri("/mapping/visit-balance")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/visit-balance")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/visit-balance")
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
          repository.findOneByNomisVisitBalanceId(
            nomisVisitBalanceId = existingMapping1.nomisVisitBalanceId,
          ),
        ).isNotNull
        assertThat(
          repository.findOneByNomisVisitBalanceId(
            nomisVisitBalanceId = existingMapping2.nomisVisitBalanceId,
          ),
        ).isNotNull

        webTestClient.delete()
          .uri("/mapping/visit-balance")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(
          repository.findOneByNomisVisitBalanceId(
            nomisVisitBalanceId = existingMapping1.nomisVisitBalanceId,
          ),
        ).isNull()
        assertThat(
          repository.findOneByNomisVisitBalanceId(
            nomisVisitBalanceId = existingMapping2.nomisVisitBalanceId,
          ),
        ).isNull()
      }
    }
  }

  @DisplayName("GET /mapping/visit-balance/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationId {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/visit-balance/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/visit-balance/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/visit-balance/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Test
    fun `can retrieve all mappings by migration Id`() = runTest {
      (1L..4L).forEach {
        repository.save(
          VisitBalanceMapping(
            dpsId = "A123${it}KT",
            nomisVisitBalanceId = 1000 + it,
            label = "2023-01-01T12:45:12",
            mappingType = MIGRATED,
          ),
        )
      }

      repository.save(
        VisitBalanceMapping(
          dpsId = "A4321KT",
          nomisVisitBalanceId = 12345,
          label = "2022-01-01T12:43:12",
          mappingType = MIGRATED,
        ),
      )

      webTestClient.get().uri("/mapping/visit-balance/migration-id/2023-01-01T12:45:12")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..nomisVisitBalanceId").value(
          Matchers.contains(1001, 1002, 1003, 1004),
        )
        .jsonPath("$.content[0].whenCreated").isNotEmpty
    }

    @Test
    fun `200 response even when no mappings are found`() {
      webTestClient.get().uri("/mapping/visit-balance/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
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
          VisitBalanceMapping(
            dpsId = "A123${it}KT",
            nomisVisitBalanceId = 1000 + it,
            label = "2023-01-01T12:45:12",
            mappingType = MIGRATED,
          ),
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/visit-balance/migration-id/2023-01-01T12:45:12")
          .queryParam("size", "2")
          .queryParam("sort", "nomisVisitBalanceId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
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
