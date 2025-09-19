package uk.gov.justice.digital.hmpps.nomismappingservice.courtsentencing

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
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val NOMIS_COURT_APPEARANCE_ID = 4321L
private const val NOMIS_COURT_APPEARANCE_2_ID = 9876L
private const val DPS_COURT_APPEARANCE_ID = "DPS123"

@Suppress("JsonStandardCompliance")
private const val DPS_COURT_APPEARANCE_2_ID = "DPS321"
class CourtSentencingCourtAppearanceResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: CourtAppearanceMappingRepository

  @Autowired
  private lateinit var courtChargeRepository: CourtChargeMappingRepository

  @Autowired
  private lateinit var courtAppearanceRecallRepository: CourtAppearanceRecallMappingRepository

  @Autowired
  private lateinit var courtCaseMappingRepository: CourtCaseMappingRepository

  @Nested
  @DisplayName("GET /mapping/court-sentencing/court-appearances/dps-court-appearance-id/{courtAppearanceId}")
  inner class GetCourtAppearanceMappingByDpsId {
    lateinit var courtAppearanceMapping: CourtAppearanceMapping

    @BeforeEach
    fun setUp() = runTest {
      courtAppearanceMapping = repository.save(
        CourtAppearanceMapping(
          dpsCourtAppearanceId = "DPS123",
          nomisCourtAppearanceId = 4321L,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.MIGRATED,
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
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/${courtAppearanceMapping.dpsCourtAppearanceId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/${courtAppearanceMapping.dpsCourtAppearanceId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/${courtAppearanceMapping.dpsCourtAppearanceId}")
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
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("DPS Court appearance Id =DOESNOTEXIST")
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/${courtAppearanceMapping.dpsCourtAppearanceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCourtAppearanceId").isEqualTo(courtAppearanceMapping.nomisCourtAppearanceId)
          .jsonPath("dpsCourtAppearanceId").isEqualTo(courtAppearanceMapping.dpsCourtAppearanceId)
          .jsonPath("mappingType").isEqualTo(courtAppearanceMapping.mappingType.name)
          .jsonPath("label").isEqualTo(courtAppearanceMapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/court-sentencing/court-appearances/nomis-court-appearance-id/{courtAppearanceId}")
  inner class GetCourtAppearanceMappingByNomisId {
    lateinit var courtAppearanceMapping: CourtAppearanceMapping

    @BeforeEach
    fun setUp() = runTest {
      courtAppearanceMapping = repository.save(
        CourtAppearanceMapping(
          dpsCourtAppearanceId = DPS_COURT_APPEARANCE_ID,
          nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.MIGRATED,
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
          .uri("/mapping/court-sentencing/court-appearances/nomis-court-appearance-id/${courtAppearanceMapping.nomisCourtAppearanceId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/nomis-court-appearance-id/${courtAppearanceMapping.nomisCourtAppearanceId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/nomis-court-appearance-id/${courtAppearanceMapping.nomisCourtAppearanceId}")
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
          .uri("/mapping/court-sentencing/court-appearances/nomis-court-appearance-id/9878987")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Nomis Court appearance Id =9878987")
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/nomis-court-appearance-id/${courtAppearanceMapping.nomisCourtAppearanceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCourtAppearanceId").isEqualTo(courtAppearanceMapping.nomisCourtAppearanceId)
          .jsonPath("dpsCourtAppearanceId").isEqualTo(courtAppearanceMapping.dpsCourtAppearanceId)
          .jsonPath("mappingType").isEqualTo(courtAppearanceMapping.mappingType.name)
          .jsonPath("label").isEqualTo(courtAppearanceMapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/court-sentencing/court-appearances")
  inner class CreateCourtAppearanceMapping {
    private lateinit var existingMapping: CourtAppearanceMapping
    private val mapping = CourtAppearanceMappingDto(
      dpsCourtAppearanceId = DPS_COURT_APPEARANCE_2_ID,
      nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_2_ID,
      label = "2023-01-01T12:45:12",
      mappingType = CourtAppearanceMappingType.DPS_CREATED,
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        CourtAppearanceMapping(
          dpsCourtAppearanceId = DPS_COURT_APPEARANCE_ID,
          nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
      courtChargeRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-appearances")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-appearances")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-appearances")
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
          .uri("/mapping/court-sentencing/court-appearances")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMapping =
          repository.findById(
            id = mapping.dpsCourtAppearanceId,
          )!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisCourtAppearanceId).isEqualTo(mapping.nomisCourtAppearanceId)
        assertThat(createdMapping.dpsCourtAppearanceId).isEqualTo(mapping.dpsCourtAppearanceId)
        assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(createdMapping.label).isEqualTo(mapping.label)
      }

      @Test
      fun `can create with minimal data`() = runTest {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-appearances")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCourtAppearanceId": "$NOMIS_COURT_APPEARANCE_2_ID",
                  "dpsCourtAppearanceId": "$DPS_COURT_APPEARANCE_2_ID"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        val createdMapping =
          repository.findById(
            id = DPS_COURT_APPEARANCE_2_ID,
          )!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisCourtAppearanceId).isEqualTo(NOMIS_COURT_APPEARANCE_2_ID)
        assertThat(createdMapping.dpsCourtAppearanceId).isEqualTo(DPS_COURT_APPEARANCE_2_ID)
        assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(createdMapping.label).isNull()
      }

      @Test
      fun `can post and then get new and existing mapping`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-appearances")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                 {
                  "nomisCourtAppearanceId": "$NOMIS_COURT_APPEARANCE_2_ID",
                  "dpsCourtAppearanceId": "$DPS_COURT_APPEARANCE_2_ID"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/$DPS_COURT_APPEARANCE_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/${DPS_COURT_APPEARANCE_2_ID}")
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
          .uri("/mapping/court-sentencing/court-appearances")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCourtAppearanceId": 54321,
                  "dpsCourtAppearanceId": "e52d7268-6e10-41a8-a0b9-2319b32520d6",
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
          .uri("/mapping/court-sentencing/court-appearances")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCourtAppearanceId": 54321
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
          .uri("/mapping/court-sentencing/court-appearances")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "dpsCourtAppearanceId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
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
          .uri("/mapping/court-sentencing/court-appearances")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CourtAppearanceAllMappingDto(
                nomisCourtAppearanceId = existingMapping.nomisCourtAppearanceId,
                dpsCourtAppearanceId = "DPS888",
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
            .containsEntry("nomisCourtAppearanceId", existingMapping.nomisCourtAppearanceId.toInt())
            .containsEntry("dpsCourtAppearanceId", existingMapping.dpsCourtAppearanceId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCourtAppearanceId", existingMapping.nomisCourtAppearanceId.toInt())
            .containsEntry("dpsCourtAppearanceId", "DPS888")
        }
      }

      @Test
      fun `returns 409 if dps id already exists`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/court-sentencing/court-appearances")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CourtAppearanceAllMappingDto(
                nomisCourtAppearanceId = 8877,
                dpsCourtAppearanceId = existingMapping.dpsCourtAppearanceId,
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
            .containsEntry("nomisCourtAppearanceId", existingMapping.nomisCourtAppearanceId.toInt())
            .containsEntry("dpsCourtAppearanceId", existingMapping.dpsCourtAppearanceId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCourtAppearanceId", 8877)
            .containsEntry("dpsCourtAppearanceId", existingMapping.dpsCourtAppearanceId)
        }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/court-sentencing/court-appearances/dps-court-appearance-id/{courtAppearanceId}")
  inner class DeleteMappingByDpsId {
    lateinit var mapping: CourtAppearanceMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CourtAppearanceMapping(
          dpsCourtAppearanceId = DPS_COURT_APPEARANCE_ID,
          nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.MIGRATED,
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
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/${mapping.dpsCourtAppearanceId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/${mapping.dpsCourtAppearanceId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/${mapping.dpsCourtAppearanceId}")
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
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/NOPE")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/${mapping.dpsCourtAppearanceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/${mapping.dpsCourtAppearanceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/${mapping.dpsCourtAppearanceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/court-sentencing/court-appearances/nomis-court-appearance-id/{courtAppearanceId}")
  inner class DeleteMappingByNomisId {
    lateinit var mapping: CourtAppearanceMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CourtAppearanceMapping(
          dpsCourtAppearanceId = DPS_COURT_APPEARANCE_ID,
          nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.MIGRATED,
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
          .uri("/mapping/court-sentencing/court-appearances/nomis-court-appearance-id/${mapping.nomisCourtAppearanceId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-appearances/nomis-court-appearance-id/${mapping.nomisCourtAppearanceId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-appearances/nomis-court-appearance-id/${mapping.nomisCourtAppearanceId}")
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
          .uri("/mapping/court-sentencing/court-appearances/nomis-court-appearance-id/13333")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/${mapping.dpsCourtAppearanceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        // delete using nomis id
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-appearances/nomis-court-appearance-id/${mapping.nomisCourtAppearanceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-court-appearance-id/${mapping.dpsCourtAppearanceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/court-sentencing/court-appearances/dps-recall-id/{recallId}")
  inner class GetCourtAppearanceRecallMappingsByDpsId {
    private val dpsRecallId = "f6ec6d17-a062-4272-9c21-1017b06d556c"
    lateinit var courtAppearanceRecallMappings: List<CourtAppearanceRecallMapping>

    @BeforeEach
    fun setUp() = runTest {
      courtAppearanceRecallMappings = listOf(
        courtAppearanceRecallRepository.save(
          CourtAppearanceRecallMapping(
            nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_ID,
            dpsRecallId = dpsRecallId,
            label = "2023-01-01T12:45:12",
            mappingType = CourtAppearanceRecallMappingType.MIGRATED,
          ),
        ),
        courtAppearanceRecallRepository.save(
          CourtAppearanceRecallMapping(
            nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_2_ID,
            dpsRecallId = dpsRecallId,
            label = "2023-01-01T12:45:12",
            mappingType = CourtAppearanceRecallMappingType.MIGRATED,
          ),
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      courtAppearanceRecallRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-recall-id/$dpsRecallId")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-recall-id/$dpsRecallId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-recall-id/$dpsRecallId")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return empty list when no mappings exist`() {
        // Delete all mappings first
        runTest {
          courtAppearanceRecallRepository.deleteAll()
        }

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-recall-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("[]")
      }

      @Test
      fun `will return mappings when they exist`() {
        val responseType = object : ParameterizedTypeReference<List<CourtAppearanceRecallMappingDto>>() {}

        val response = webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-recall-id/$dpsRecallId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody(responseType)
          .returnResult()
          .responseBody

        assertThat(response).hasSize(2)

        val firstMapping = response!!.find { it.nomisCourtAppearanceId == NOMIS_COURT_APPEARANCE_ID }!!
        assertThat(firstMapping.dpsRecallId).isEqualTo(dpsRecallId)
        assertThat(firstMapping.mappingType!!.name).isEqualTo("MIGRATED")
        assertThat(firstMapping.label).isEqualTo("2023-01-01T12:45:12")
        assertThat(LocalDateTime.parse(firstMapping.whenCreated.toString())).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))

        val secondMapping = response.find { it.nomisCourtAppearanceId == NOMIS_COURT_APPEARANCE_2_ID }!!
        assertThat(secondMapping.dpsRecallId).isEqualTo(dpsRecallId)
        assertThat(secondMapping.mappingType!!.name).isEqualTo("MIGRATED")
        assertThat(secondMapping.label).isEqualTo("2023-01-01T12:45:12")
        assertThat(LocalDateTime.parse(secondMapping.whenCreated.toString())).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/court-sentencing/court-appearances/dps-recall-id/{recallId}")
  inner class DeleteCourtAppearanceRecallMappingByDpsId {
    private val dpsRecallId = "f6ec6d17-a062-4272-9c21-1017b06d556c"
    lateinit var courtAppearanceRecallMappings: List<CourtAppearanceRecallMapping>

    @BeforeEach
    fun setUp() = runTest {
      courtAppearanceRecallMappings = listOf(
        courtAppearanceRecallRepository.save(
          CourtAppearanceRecallMapping(
            nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_ID,
            dpsRecallId = dpsRecallId,
            label = "2023-01-01T12:45:12",
            mappingType = CourtAppearanceRecallMappingType.MIGRATED,
          ),
        ),
        courtAppearanceRecallRepository.save(
          CourtAppearanceRecallMapping(
            nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_2_ID,
            dpsRecallId = dpsRecallId,
            label = "2023-01-01T12:45:12",
            mappingType = CourtAppearanceRecallMappingType.MIGRATED,
          ),
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      courtAppearanceRecallRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-appearances/dps-recall-id/$dpsRecallId")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-appearances/dps-recall-id/$dpsRecallId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-appearances/dps-recall-id/$dpsRecallId")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return 204 even when mappings do not exist`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-appearances/dps-recall-id/non-existent-id")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mappings exist and are deleted`() {
        // Verify mappings exist
        val responseType = object : ParameterizedTypeReference<List<CourtAppearanceRecallMappingDto>>() {}
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-recall-id/$dpsRecallId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody(responseType)
          .returnResult()
          .responseBody!!.also {
          assertThat(it).hasSize(2)
        }

        // Delete mappings
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-appearances/dps-recall-id/$dpsRecallId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent

        // Verify mappings are deleted
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-appearances/dps-recall-id/$dpsRecallId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("[]")
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/court-sentencing/court-appearances/recall")
  inner class CreateCourtAppearanceRecallMapping {
    val fromNomisId = 99L
    val toNomisId = 100L
    val dpsId = UUID.randomUUID().toString()

    private val mapping = CourtAppearanceRecallMappingsDto(
      nomisCourtAppearanceIds = listOf(NOMIS_COURT_APPEARANCE_ID, NOMIS_COURT_APPEARANCE_2_ID),
      dpsRecallId = "f6ec6d17-a062-4272-9c21-1017b06d556c",
      label = "2023-01-01T12:45:12",
      mappingType = CourtAppearanceRecallMappingType.DPS_CREATED,
      mappingsToUpdate = CourtCaseBatchUpdateMappingDto(
        courtCases = listOf(SimpleCourtSentencingIdPair(fromNomisId, toNomisId)),
        courtAppearances = listOf(SimpleCourtSentencingIdPair(fromNomisId, toNomisId)),
      ),
    )

    @BeforeEach
    fun setUp() = runTest {
      courtCaseMappingRepository.save(
        CourtCaseMapping(
          dpsCourtCaseId = dpsId,
          nomisCourtCaseId = fromNomisId,
          mappingType = CourtCaseMappingType.NOMIS_CREATED,
        ),
      )
      repository.save(
        CourtAppearanceMapping(
          nomisCourtAppearanceId = fromNomisId,
          dpsCourtAppearanceId = dpsId,
          mappingType = CourtAppearanceMappingType.NOMIS_CREATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      courtCaseMappingRepository.deleteAll()
      repository.deleteAll()
      courtAppearanceRecallRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-appearances/recall")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-appearances/recall")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-appearances/recall")
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
          .uri("/mapping/court-sentencing/court-appearances/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMappings = courtAppearanceRecallRepository.findAllByDpsRecallId(mapping.dpsRecallId)
        assertThat(createdMappings).hasSize(2)

        val firstMapping = createdMappings.find { it.nomisCourtAppearanceId == NOMIS_COURT_APPEARANCE_ID }!!
        assertThat(firstMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(firstMapping.nomisCourtAppearanceId).isEqualTo(NOMIS_COURT_APPEARANCE_ID)
        assertThat(firstMapping.dpsRecallId).isEqualTo(mapping.dpsRecallId)
        assertThat(firstMapping.mappingType).isEqualTo(mapping.mappingType)

        val secondMapping = createdMappings.find { it.nomisCourtAppearanceId == NOMIS_COURT_APPEARANCE_2_ID }!!
        assertThat(secondMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(secondMapping.nomisCourtAppearanceId).isEqualTo(NOMIS_COURT_APPEARANCE_2_ID)
        assertThat(secondMapping.dpsRecallId).isEqualTo(mapping.dpsRecallId)
        assertThat(secondMapping.mappingType).isEqualTo(mapping.mappingType)
      }

      @Test
      fun `will also update any of the other supplied mappings`() = runTest {
        assertThat(repository.findById(dpsId)?.nomisCourtAppearanceId).isEqualTo(fromNomisId)
        assertThat(courtCaseMappingRepository.findById(dpsId)?.nomisCourtCaseId).isEqualTo(fromNomisId)

        webTestClient.post()
          .uri("/mapping/court-sentencing/court-appearances/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        assertThat(repository.findById(dpsId)?.nomisCourtAppearanceId).isEqualTo(toNomisId)
        assertThat(courtCaseMappingRepository.findById(dpsId)?.nomisCourtCaseId).isEqualTo(toNomisId)
      }

      @Test
      fun `can create with minimal data`() = runTest {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-appearances/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCourtAppearanceIds": [${NOMIS_COURT_APPEARANCE_ID}],
                  "dpsRecallId": "f6ec6d17-a062-4272-9c21-1017b06d556c"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        val createdMappings = courtAppearanceRecallRepository.findAllByDpsRecallId("f6ec6d17-a062-4272-9c21-1017b06d556c")
        assertThat(createdMappings).hasSize(1)

        val createdMapping = createdMappings.first()
        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisCourtAppearanceId).isEqualTo(NOMIS_COURT_APPEARANCE_ID)
        assertThat(createdMapping.dpsRecallId).isEqualTo("f6ec6d17-a062-4272-9c21-1017b06d556c")
        assertThat(createdMapping.mappingType).isEqualTo(CourtAppearanceRecallMappingType.DPS_CREATED)
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when nomisCourtAppearanceIds not provided`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-appearances/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "dpsRecallId": "f6ec6d17-a062-4272-9c21-1017b06d556c"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when dpsRecallId not provided`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-appearances/recall")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCourtAppearanceIds": [${NOMIS_COURT_APPEARANCE_ID}]
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }
    }
  }
}
