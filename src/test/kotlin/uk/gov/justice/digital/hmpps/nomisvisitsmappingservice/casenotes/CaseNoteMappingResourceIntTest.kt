package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.casenotes

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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class CaseNoteMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: CaseNoteMappingRepository

  @AfterEach
  internal fun deleteData() = runBlocking {
    repository.deleteAll()
  }

  @Nested
  @DisplayName("POST /mapping/casenotes")
  inner class CreateMapping {
    private lateinit var existingMapping: CaseNoteMapping
    private val mapping = CaseNoteMappingDto(
      dpsCaseNoteId = "e52d7268-6e10-41a8-a0b9-2319b32520d6",
      nomisCaseNoteId = 543211L,
      label = "2024-02-01T12:45:12",
      mappingType = CaseNoteMappingType.MIGRATED,
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        CaseNoteMapping(
          dpsCaseNoteId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisCaseNoteId = 543210L,
          label = "2024-01-01T12:45:12",
          mappingType = CaseNoteMappingType.DPS_CREATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/casenotes")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/casenotes")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/casenotes")
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
          .uri("/mapping/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMapping =
          repository.findOneByNomisCaseNoteId(mapping.nomisCaseNoteId)!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisCaseNoteId).isEqualTo(mapping.nomisCaseNoteId)
        assertThat(createdMapping.dpsCaseNoteId).isEqualTo(mapping.dpsCaseNoteId)
        assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(createdMapping.label).isEqualTo(mapping.label)
      }

      @Test
      fun `can create with minimal data`() = runTest {
        webTestClient.post()
          .uri("/mapping/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCaseNoteId": 54321,
                  "dpsCaseNoteId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        val createdMapping =
          repository.findOneByNomisCaseNoteId(nomisCaseNoteId = 54321)!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisCaseNoteId).isEqualTo(54321L)
        assertThat(createdMapping.dpsCaseNoteId).isEqualTo("e52d7268-6e10-41a8-a0b9-2319b32520d6")
        assertThat(createdMapping.mappingType).isEqualTo(CaseNoteMappingType.DPS_CREATED)
        assertThat(createdMapping.label).isNull()
      }

      @Test
      fun `can post and then get mapping`() {
        webTestClient.post()
          .uri("/mapping/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCaseNoteId": 54555,
                  "dpsCaseNoteId": "e52d7268-6e10-41a8-a0b9-2319b3254555"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        webTestClient.get()
          .uri("/mapping/casenotes/nomis-casenote-id/54555")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/e52d7268-6e10-41a8-a0b9-2319b3254555")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when mapping type is invalid`() {
        webTestClient.post()
          .uri("/mapping/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCaseNoteId": 54321,
                  "dpsCaseNoteId": "e52d7268-6e10-41a8-a0b9-2319b32520d6",
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
          .uri("/mapping/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCaseNoteId": 3,
                  "mappingType": "MIGRATED"
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
          .uri("/mapping/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "dpsCaseNoteId": "e52d7268-6e10-41a8-a0b9-2319b32520d6",
                  "mappingType": "MIGRATED"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 409 if nomis ids already exist`() {
        val dpsCaseNoteId = UUID.randomUUID().toString()
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CaseNoteMappingDto(
                nomisCaseNoteId = existingMapping.nomisCaseNoteId,
                dpsCaseNoteId = dpsCaseNoteId,
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
            .containsEntry("nomisCaseNoteId", existingMapping.nomisCaseNoteId.toInt())
            .containsEntry("dpsCaseNoteId", existingMapping.dpsCaseNoteId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCaseNoteId", existingMapping.nomisCaseNoteId.toInt())
            .containsEntry("dpsCaseNoteId", dpsCaseNoteId)
        }
      }

      @Test
      fun `returns 409 if dps id already exist`() {
        val nomisCaseNoteId = 987654L
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CaseNoteMappingDto(
                nomisCaseNoteId = nomisCaseNoteId,
                dpsCaseNoteId = existingMapping.dpsCaseNoteId,
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
            .containsEntry("nomisCaseNoteId", existingMapping.nomisCaseNoteId.toInt())
            .containsEntry("dpsCaseNoteId", existingMapping.dpsCaseNoteId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCaseNoteId", nomisCaseNoteId.toInt())
            .containsEntry("dpsCaseNoteId", existingMapping.dpsCaseNoteId)
        }
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/casenotes/batch")
  inner class CreateMappings {
    private var existingMapping: CaseNoteMapping = CaseNoteMapping(
      dpsCaseNoteId = "edcd118c-41ba-42ea-b5c4-000000000001",
      nomisCaseNoteId = 50001L,
      label = "2023-01-01T12:45:12",
      mappingType = CaseNoteMappingType.MIGRATED,
    )
    private val mappings = listOf(
      CaseNoteMappingDto(
        dpsCaseNoteId = "e52d7268-6e10-41a8-a0b9-000000000002",
        nomisCaseNoteId = 50002L,
        mappingType = CaseNoteMappingType.DPS_CREATED,
      ),
      CaseNoteMappingDto(
        dpsCaseNoteId = "fd4e55a8-0805-439b-9e27-000000000003",
        nomisCaseNoteId = 50003L,
        mappingType = CaseNoteMappingType.NOMIS_CREATED,
      ),
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(existingMapping)
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/casenotes/batch")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/casenotes/batch")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/casenotes/batch")
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
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/casenotes/batch")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated

        val createdMapping1 =
          repository.findOneByNomisCaseNoteId(
            nomisCaseNoteId = mappings[0].nomisCaseNoteId,
          )!!

        assertThat(createdMapping1.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping1.nomisCaseNoteId).isEqualTo(mappings[0].nomisCaseNoteId)
        assertThat(createdMapping1.dpsCaseNoteId).isEqualTo(mappings[0].dpsCaseNoteId)
        assertThat(createdMapping1.mappingType).isEqualTo(mappings[0].mappingType)
        assertThat(createdMapping1.label).isEqualTo(mappings[0].label)

        val createdMapping2 =
          repository.findOneByNomisCaseNoteId(
            nomisCaseNoteId = mappings[1].nomisCaseNoteId,
          )!!

        assertThat(createdMapping2.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping2.nomisCaseNoteId).isEqualTo(mappings[1].nomisCaseNoteId)
        assertThat(createdMapping2.dpsCaseNoteId).isEqualTo(mappings[1].dpsCaseNoteId)
        assertThat(createdMapping2.mappingType).isEqualTo(mappings[1].mappingType)
        assertThat(createdMapping2.label).isEqualTo(mappings[1].label)
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when mapping type is invalid`() {
        webTestClient.post()
          .uri("/mapping/casenotes/batch")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                [
                    {
                      "nomisCaseNoteId": 54321,
                      "dpsCaseNoteId": "e52d7268-6e10-41a8-a0b9-2319b32520d6",
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
      fun `will return 409 if nomis ids already exist`() {
        val dpsCaseNoteId = UUID.randomUUID().toString()
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/casenotes/batch")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings + existingMapping.copy(dpsCaseNoteId = dpsCaseNoteId),
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
            .containsEntry("nomisCaseNoteId", existingMapping.nomisCaseNoteId.toInt())
            .containsEntry("dpsCaseNoteId", existingMapping.dpsCaseNoteId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCaseNoteId", existingMapping.nomisCaseNoteId.toInt())
            .containsEntry("dpsCaseNoteId", dpsCaseNoteId)
        }
      }

      @Test
      fun `will return 409 if dps ids already exist`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/casenotes/batch")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings + existingMapping.copy(nomisCaseNoteId = 99),
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
            .containsEntry("nomisCaseNoteId", existingMapping.nomisCaseNoteId.toInt())
            .containsEntry("dpsCaseNoteId", existingMapping.dpsCaseNoteId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCaseNoteId", 99)
            .containsEntry("dpsCaseNoteId", existingMapping.dpsCaseNoteId)
        }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/casenotes/nomis-casenote-id/{caseNoteId}")
  inner class GetMappingByNomisId {
    lateinit var mapping: CaseNoteMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CaseNoteMapping(
          dpsCaseNoteId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisCaseNoteId = 54321L,
          label = "2023-01-01T12:45:12",
          mappingType = CaseNoteMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/casenotes/nomis-casenote-id/${mapping.nomisCaseNoteId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/casenotes/nomis-casenote-id/${mapping.nomisCaseNoteId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/casenotes/nomis-casenote-id/${mapping.nomisCaseNoteId}")
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
          .uri("/mapping/casenotes/nomis-casenote-id/9999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/casenotes/nomis-casenote-id/${mapping.nomisCaseNoteId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCaseNoteId").isEqualTo(mapping.nomisCaseNoteId)
          .jsonPath("dpsCaseNoteId").isEqualTo(mapping.dpsCaseNoteId)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/nomis-casenote-id")
  inner class GetMappingsByNomisId {
    lateinit var mapping1: CaseNoteMapping
    lateinit var mapping2: CaseNoteMapping
    val caseNoteIds: List<Long> = listOf(54321L, 54322L)

    @BeforeEach
    fun setUp() = runTest {
      mapping1 = repository.save(
        CaseNoteMapping(
          dpsCaseNoteId = "edcd118c-41ba-42ea-b5c4-000000000001",
          nomisCaseNoteId = 54321L,
          label = "2023-01-01T12:45:12",
          mappingType = CaseNoteMappingType.MIGRATED,
        ),
      )
      mapping2 = repository.save(
        CaseNoteMapping(
          dpsCaseNoteId = "edcd118c-41ba-42ea-b5c4-000000000002",
          nomisCaseNoteId = 54322L,
          label = "2023-06-01T12:45:12",
          mappingType = CaseNoteMappingType.DPS_CREATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/casenotes/nomis-casenote-id")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(caseNoteIds))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/casenotes/nomis-casenote-id")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(caseNoteIds))
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/casenotes/nomis-casenote-id")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(caseNoteIds))
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return 200 when no mappings exist`() {
        webTestClient.post()
          .uri("/mapping/casenotes/nomis-casenote-id")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(listOf(99999)))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("[]")
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.post()
          .uri("/mapping/casenotes/nomis-casenote-id")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(caseNoteIds))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$[0].nomisCaseNoteId").isEqualTo(mapping1.nomisCaseNoteId)
          .jsonPath("$[0].dpsCaseNoteId").isEqualTo(mapping1.dpsCaseNoteId)
          .jsonPath("$[0].mappingType").isEqualTo(mapping1.mappingType.name)
          .jsonPath("$[0].label").isEqualTo(mapping1.label!!)
          .jsonPath("$[0].whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
          .jsonPath("$[1].nomisCaseNoteId").isEqualTo(mapping2.nomisCaseNoteId)
          .jsonPath("$[1].dpsCaseNoteId").isEqualTo(mapping2.dpsCaseNoteId)
          .jsonPath("$[1].mappingType").isEqualTo(mapping2.mappingType.name)
          .jsonPath("$[1].label").isEqualTo(mapping2.label!!)
          .jsonPath("$[1].whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/casenotes/dps-casenote-id/{dpsCaseNoteId}")
  inner class GetMappingByDpsId {
    lateinit var mapping: CaseNoteMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CaseNoteMapping(
          dpsCaseNoteId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisCaseNoteId = 54321L,
          label = "2023-01-01T12:45:12",
          mappingType = CaseNoteMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/${mapping.dpsCaseNoteId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/${mapping.dpsCaseNoteId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/${mapping.dpsCaseNoteId}")
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
          .uri("/mapping/casenotes/dps-casenote-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/${mapping.dpsCaseNoteId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCaseNoteId").isEqualTo(mapping.nomisCaseNoteId)
          .jsonPath("dpsCaseNoteId").isEqualTo(mapping.dpsCaseNoteId)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/casenotes/nomis-casenote-id/{nomisCaseNoteId}")
  inner class DeleteMappingByNomisId {
    lateinit var mapping: CaseNoteMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CaseNoteMapping(
          dpsCaseNoteId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisCaseNoteId = 54321L,
          label = "2023-01-01T12:45:12",
          mappingType = CaseNoteMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/casenotes/nomis-casenote-id/${mapping.nomisCaseNoteId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/casenotes/nomis-casenote-id/${mapping.nomisCaseNoteId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/casenotes/nomis-casenote-id/${mapping.nomisCaseNoteId}")
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
          .uri("/mapping/casenotes/nomis-casenote-id/99999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/casenotes/nomis-casenote-id/${mapping.nomisCaseNoteId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/casenotes/nomis-casenote-id/${mapping.nomisCaseNoteId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/casenotes/nomis-casenote-id/${mapping.nomisCaseNoteId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/casenotes/dps-casenote-id/{dpsCaseNoteId}")
  inner class DeleteMappingByDpsId {
    lateinit var mapping: CaseNoteMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CaseNoteMapping(
          dpsCaseNoteId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisCaseNoteId = 54321L,
          label = "2023-01-01T12:45:12",
          mappingType = CaseNoteMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/casenotes/dps-casenote-id/${mapping.dpsCaseNoteId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/casenotes/dps-casenote-id/${mapping.dpsCaseNoteId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/casenotes/dps-casenote-id/${mapping.dpsCaseNoteId}")
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
          .uri("/mapping/casenotes/dps-casenote-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/${mapping.dpsCaseNoteId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/casenotes/dps-casenote-id/${mapping.dpsCaseNoteId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/${mapping.dpsCaseNoteId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/casenotes")
  inner class DeleteAllMappings {
    private lateinit var existingMapping1: CaseNoteMapping
    private lateinit var existingMapping2: CaseNoteMapping

    @BeforeEach
    fun setUp() = runTest {
      existingMapping1 = repository.save(
        CaseNoteMapping(
          dpsCaseNoteId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisCaseNoteId = 54321L,
          label = "2023-01-01T12:45:12",
          mappingType = CaseNoteMappingType.MIGRATED,
        ),
      )
      existingMapping2 = repository.save(
        CaseNoteMapping(
          dpsCaseNoteId = "4433eb7d-2fa0-4055-99d9-633fefa53288",
          nomisCaseNoteId = 54322L,
          mappingType = CaseNoteMappingType.NOMIS_CREATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/casenotes")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/casenotes")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/casenotes")
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
          repository.findOneByNomisCaseNoteId(
            nomisCaseNoteId = existingMapping1.nomisCaseNoteId,
          ),
        ).isNotNull
        assertThat(
          repository.findOneByNomisCaseNoteId(
            nomisCaseNoteId = existingMapping2.nomisCaseNoteId,
          ),
        ).isNotNull

        webTestClient.delete()
          .uri("/mapping/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(
          repository.findOneByNomisCaseNoteId(
            nomisCaseNoteId = existingMapping1.nomisCaseNoteId,
          ),
        ).isNull()
        assertThat(
          repository.findOneByNomisCaseNoteId(
            nomisCaseNoteId = existingMapping2.nomisCaseNoteId,
          ),
        ).isNull()
      }
    }
  }
}
