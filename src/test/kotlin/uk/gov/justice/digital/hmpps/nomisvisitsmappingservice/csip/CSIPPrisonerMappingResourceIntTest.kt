package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip

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
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingType.DPS_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class CSIPPrisonerMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: CSIPMappingRepository

  @Autowired
  private lateinit var csipPrisonerRepository: CSIPPrisonerMappingRepository

  @Nested
  @DisplayName("GET /mapping/csip/{offenderNo}/all")
  inner class GetMappingByPrisoner {
    private var mapping1: CSIPMapping = CSIPMapping(
      dpsCSIPId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
      nomisCSIPId = 54321L,
      offenderNo = "A1234KT",
      mappingType = DPS_CREATED,
    )
    private var mapping2: CSIPMapping = CSIPMapping(
      dpsCSIPId = "85665bb9-ab28-458a-8386-b8cc91b311f7",
      nomisCSIPId = 11111L,
      offenderNo = "A1234KT",
      mappingType = DPS_CREATED,
    )
    private val prisonerMappings = PrisonerCSIPMappingsDto(
      label = "2023-01-01T12:45:12",
      mappingType = MIGRATED,
      mappings = listOf(
        CSIPMappingIdDto(
          dpsCSIPId = "e52d7268-6e10-41a8-a0b9-2319b32520d6",
          nomisCSIPId = 23232L,
        ),
        CSIPMappingIdDto(
          dpsCSIPId = "fd4e55a8-0805-439b-9e27-647583b96e4e",
          nomisCSIPId = 43434L,
        ),
      ),
    )

    @BeforeEach
    fun setUp() = runTest {
      webTestClient.post()
        .uri("/mapping/csip/A1234KT/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(prisonerMappings))
        .exchange()
        .expectStatus().isCreated
      mapping1 = repository.save(mapping1)
      mapping2 = repository.save(mapping2)
      repository.save(
        CSIPMapping(
          dpsCSIPId = "fd4e55a8-41ba-42ea-b5c4-404b453ad99b",
          nomisCSIPId = 9999L,
          offenderNo = "A1111KT",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      csipPrisonerRepository.deleteAll()
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/csip/A1234KT/all")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/csip/A1234KT/all")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/csip/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return 200 when no mappings found for prisoner`() {
        webTestClient.get()
          .uri("/mapping/csip/A9999KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("mappings.size()").isEqualTo(0)
      }

      @Test
      fun `will return all mappings for prisoner`() {
        webTestClient.get()
          .uri("/mapping/csip/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("mappings.size()").isEqualTo(4)
          .jsonPath("mappings[0].nomisCSIPId").isEqualTo(11111)
          .jsonPath("mappings[1].nomisCSIPId").isEqualTo(23232L)
          .jsonPath("mappings[2].nomisCSIPId").isEqualTo(43434L)
          .jsonPath("mappings[3].nomisCSIPId").isEqualTo(54321L)
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/csip/{offenderNo}/all")
  inner class CreateMappingsForPrisoner {
    private lateinit var existingMapping: CSIPMapping
    private val prisonerMappings = PrisonerCSIPMappingsDto(
      label = "2023-01-01T12:45:12",
      mappingType = MIGRATED,
      mappings = listOf(
        CSIPMappingIdDto(
          dpsCSIPId = "e52d7268-6e10-41a8-a0b9-2319b32520d6",
          nomisCSIPId = 11221L,
        ),
        CSIPMappingIdDto(
          dpsCSIPId = "fd4e55a8-0805-439b-9e27-647583b96e4e",
          nomisCSIPId = 98989L,
        ),
      ),
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        CSIPMapping(
          dpsCSIPId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisCSIPId = 99889L,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
          offenderNo = "A1234KT",
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      csipPrisonerRepository.deleteAll()
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/csip/A1234KT/all")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(prisonerMappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/csip/A1234KT/all")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(prisonerMappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/csip/A1234KT/all")
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
          .uri("/mapping/csip/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(prisonerMappings))
          .exchange()
          .expectStatus().isCreated

        val createdMapping1 =
          repository.findOneByNomisCSIPId(
            nomisCSIPId = prisonerMappings.mappings[0].nomisCSIPId,
          )!!

        assertThat(createdMapping1.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping1.nomisCSIPId).isEqualTo(prisonerMappings.mappings[0].nomisCSIPId)
        assertThat(createdMapping1.dpsCSIPId).isEqualTo(prisonerMappings.mappings[0].dpsCSIPId)
        assertThat(createdMapping1.mappingType).isEqualTo(prisonerMappings.mappingType)
        assertThat(createdMapping1.label).isEqualTo(prisonerMappings.label)
        val createdMapping2 =
          repository.findOneByNomisCSIPId(
            nomisCSIPId = prisonerMappings.mappings[1].nomisCSIPId,
          )!!

        assertThat(createdMapping2.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping2.nomisCSIPId).isEqualTo(prisonerMappings.mappings[1].nomisCSIPId)
        assertThat(createdMapping2.dpsCSIPId).isEqualTo(prisonerMappings.mappings[1].dpsCSIPId)
        assertThat(createdMapping2.mappingType).isEqualTo(prisonerMappings.mappingType)
        assertThat(createdMapping2.label).isEqualTo(prisonerMappings.label)
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when mapping type is invalid`() {
        webTestClient.post()
          .uri("/mapping/csip/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "mappingType": "INVALID_TYPE",
                  "mappings": [
                    {
                      "nomisCSIPId": 54321,
                      "dpsCSIPId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
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
          .uri("/mapping/csip/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "mappingType": "MIGRATED",
                  "mappings": [
                    {
                      "nomisCSIPId": 54321
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
      fun `returns 400 when NOMIS id is missing`() {
        webTestClient.post()
          .uri("/mapping/csip/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "mappingType": "MIGRATED",
                  "mappings": [
                    {
                      "dpsCSIPId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
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
      fun `will not return 409 if nomis ids already exist since it will be deleted`() {
        val dpsCSIPId = UUID.randomUUID().toString()
        webTestClient.post()
          .uri("/mapping/csip/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              prisonerMappings.copy(
                mappings = prisonerMappings.mappings + CSIPMappingIdDto(
                  nomisCSIPId = existingMapping.nomisCSIPId,
                  dpsCSIPId = dpsCSIPId,
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(201)
      }

      @Test
      fun `will not return 409 if dps id already exist since it will be deleted`() {
        webTestClient.post()
          .uri("/mapping/csip/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              prisonerMappings.copy(
                mappings = prisonerMappings.mappings + CSIPMappingIdDto(
                  nomisCSIPId = existingMapping.nomisCSIPId,
                  dpsCSIPId = existingMapping.dpsCSIPId,
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(201)
      }
    }
  }

  @DisplayName("GET /mapping/csip/migration-id/{migrationId}/grouped-by-prisoner")
  @Nested
  inner class GetMappingByMigrationIdGroupedByPrisoner {

    @AfterEach
    internal fun deleteData() = runBlocking {
      csipPrisonerRepository.deleteAll()
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/csip/migration-id/2022-01-01T00:00:00/grouped-by-prisoner")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/csip/migration-id/2022-01-01T00:00:00/grouped-by-prisoner")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/csip/migration-id/2022-01-01T00:00:00/grouped-by-prisoner")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Test
    fun `can retrieve all mappings by migration Id`() = runTest {
      webTestClient.post()
        .uri("/mapping/csip/A1111KT/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            PrisonerCSIPMappingsDto(
              label = "2023-01-01T12:45:12",
              mappingType = MIGRATED,
              mappings = (1L..4L).map {
                CSIPMappingIdDto(
                  dpsCSIPId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
                  nomisCSIPId = 54320 + it,
                )
              },
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post()
        .uri("/mapping/csip/A2222KT/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            PrisonerCSIPMappingsDto(
              label = "2023-01-01T12:45:12",
              mappingType = MIGRATED,
              mappings = (5L..6L).map {
                CSIPMappingIdDto(
                  dpsCSIPId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
                  nomisCSIPId = 54320 + it,
                )
              },
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post()
        .uri("/mapping/csip/A1234KT/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            PrisonerCSIPMappingsDto(
              label = "2022-01-01T12:43:12",
              mappingType = MIGRATED,
              mappings = listOf(
                CSIPMappingIdDto(
                  dpsCSIPId = "edcd118c-91ba-42ea-b5c4-404b453ad58b",
                  nomisCSIPId = 54329L,
                ),
              ),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/csip/migration-id/2023-01-01T12:45:12/grouped-by-prisoner")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(2)
        .jsonPath("$.content..offenderNo").value<List<String>> {
          assertThat(it).contains("A1111KT", "A2222KT")
        }
        .jsonPath("$.content..mappingsCount").value<List<Int>> {
          assertThat(it).contains(4, 2)
        }

      webTestClient.get().uri("/mapping/csip/migration-id/2023-01-01T12:45:12/grouped-by-prisoner?size=1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(2)
        .jsonPath("numberOfElements").isEqualTo(1)
        .jsonPath("$.content[0].offenderNo").isEqualTo("A1111KT")
        .jsonPath("$.content[0].whenCreated").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.MINUTES),
          )
        }
    }

    @Test
    fun `200 response even when no mappings are found`() {
      webTestClient.get().uri("/mapping/csip/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
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
          CSIPMapping(
            dpsCSIPId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
            nomisCSIPId = 54320 + it,
            label = "2023-01-01T12:45:12",
            mappingType = MIGRATED,
            offenderNo = "A1234KT",
          ),
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/csip/migration-id/2023-01-01T12:45:12")
          .queryParam("size", "2")
          .queryParam("sort", "nomisCSIPId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
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
