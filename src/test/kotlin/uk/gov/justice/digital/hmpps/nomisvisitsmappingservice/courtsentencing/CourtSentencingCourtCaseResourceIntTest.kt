package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

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

private const val nomisCourtAppearance1Id = 54321L
private const val nomisCourtAppearance2Id = 65432L
private const val dpsCourtAppearance1Id = "dpsca1"
private const val dpsCourtAppearance2Id = "dpsca2"
private const val nomisCourtCase1Id = 54321L
private const val dpsCourtCaseId = "dps123"
private const val existingDpsCourtCaseId = "DPS321"
private const val existingNomisCourtCaseId = 98765L

class CourtSentencingCourtCaseResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: CourtCaseMappingRepository

  @Autowired
  private lateinit var courtAppearanceRepository: CourtAppearanceMappingRepository

  @Nested
  @DisplayName("GET /mapping/court-sentencing/court-cases/dps-court-case-id/{courtCaseId}")
  inner class GetMappingByDpsId {
    lateinit var courtCaseMapping: CourtCaseMapping

    @BeforeEach
    fun setUp() = runTest {
      courtCaseMapping = repository.save(
        CourtCaseMapping(
          dpsCourtCaseId = dpsCourtCaseId,
          nomisCourtCaseId = 4321L,
          label = "2023-01-01T12:45:12",
          mappingType = CourtCaseMappingType.MIGRATED,
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
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${courtCaseMapping.dpsCourtCaseId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${courtCaseMapping.dpsCourtCaseId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${courtCaseMapping.dpsCourtCaseId}")
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
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${courtCaseMapping.dpsCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCourtCaseId").isEqualTo(courtCaseMapping.nomisCourtCaseId)
          .jsonPath("dpsCourtCaseId").isEqualTo(courtCaseMapping.dpsCourtCaseId)
          .jsonPath("mappingType").isEqualTo(courtCaseMapping.mappingType.name)
          .jsonPath("label").isEqualTo(courtCaseMapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/court-sentencing/court-cases")
  inner class CreateMapping {
    private lateinit var existingMapping: CourtCaseMapping
    private val mapping = CourtCaseMappingDto(
      dpsCourtCaseId = dpsCourtCaseId,
      nomisCourtCaseId = nomisCourtAppearance1Id,
      label = "2023-01-01T12:45:12",
      mappingType = CourtCaseMappingType.DPS_CREATED,
      courtAppearances = listOf(
        CourtAppearanceMappingDto(
          dpsCourtAppearanceId = dpsCourtAppearance1Id,
          nomisCourtAppearanceId = nomisCourtAppearance1Id,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.DPS_CREATED,
        ),
        CourtAppearanceMappingDto(
          dpsCourtAppearanceId = dpsCourtAppearance2Id,
          nomisCourtAppearanceId = nomisCourtAppearance2Id,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.DPS_CREATED,
        ),
      ),
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        CourtCaseMapping(
          dpsCourtCaseId = existingDpsCourtCaseId,
          nomisCourtCaseId = existingNomisCourtCaseId,
          label = "2023-01-01T12:45:12",
          mappingType = CourtCaseMappingType.MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
      courtAppearanceRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases")
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
          .uri("/mapping/court-sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMapping =
          repository.findById(
            id = mapping.dpsCourtCaseId,
          )!!

        val createdCourtAppearance1Mapping =
          courtAppearanceRepository.findById(
            id = dpsCourtAppearance1Id,
          )!!

        val createdCourtAppearance2Mapping =
          courtAppearanceRepository.findById(
            id = dpsCourtAppearance2Id,
          )!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisCourtCaseId).isEqualTo(mapping.nomisCourtCaseId)
        assertThat(createdMapping.dpsCourtCaseId).isEqualTo(mapping.dpsCourtCaseId)
        assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(createdMapping.label).isEqualTo(mapping.label)
        assertThat(createdCourtAppearance1Mapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdCourtAppearance1Mapping.nomisCourtAppearanceId).isEqualTo(nomisCourtAppearance1Id)
        assertThat(createdCourtAppearance1Mapping.dpsCourtAppearanceId).isEqualTo(dpsCourtAppearance1Id)
        assertThat(createdCourtAppearance1Mapping.mappingType).isEqualTo(CourtAppearanceMappingType.DPS_CREATED)
        assertThat(createdCourtAppearance1Mapping.label).isEqualTo(mapping.courtAppearances[0].label)
        assertThat(createdCourtAppearance2Mapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdCourtAppearance2Mapping.nomisCourtAppearanceId).isEqualTo(nomisCourtAppearance2Id)
        assertThat(createdCourtAppearance2Mapping.dpsCourtAppearanceId).isEqualTo(dpsCourtAppearance2Id)
        assertThat(createdCourtAppearance2Mapping.mappingType).isEqualTo(CourtAppearanceMappingType.DPS_CREATED)
        assertThat(createdCourtAppearance2Mapping.label).isEqualTo(mapping.courtAppearances[1].label)
      }

      @Test
      fun `can post and then get new and existing mapping`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                 {
                  "nomisCourtCaseId": $nomisCourtCase1Id,
                  "dpsCourtCaseId": "$dpsCourtCaseId"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/$dpsCourtCaseId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/$existingDpsCourtCaseId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when mapping type is invalid`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCourtCaseId": 54321,
                  "dpsCourtCaseId": "e52d7268-6e10-41a8-a0b9-2319b32520d6",
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
          .uri("/mapping/court-sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCourtCaseId": 54321
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
          .uri("/mapping/court-sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "dpsCourtCaseId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 409 if nomis id already exist`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CourtCaseMappingDto(
                nomisCourtCaseId = existingMapping.nomisCourtCaseId,
                dpsCourtCaseId = "DPS888",
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
            .containsEntry("nomisCourtCaseId", existingMapping.nomisCourtCaseId.toInt())
            .containsEntry("dpsCourtCaseId", existingMapping.dpsCourtCaseId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCourtCaseId", existingMapping.nomisCourtCaseId.toInt())
            .containsEntry("dpsCourtCaseId", "DPS888")
        }
      }

      @Test
      fun `returns 409 if dps id already exists`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CourtCaseMappingDto(
                nomisCourtCaseId = 8877,
                dpsCourtCaseId = existingMapping.dpsCourtCaseId,
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
            .containsEntry("nomisCourtCaseId", existingMapping.nomisCourtCaseId.toInt())
            .containsEntry("dpsCourtCaseId", existingMapping.dpsCourtCaseId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCourtCaseId", 8877)
            .containsEntry("dpsCourtCaseId", existingMapping.dpsCourtCaseId)
        }
      }
    }
  }
}
