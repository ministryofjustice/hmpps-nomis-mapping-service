package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

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
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val NOMIS_COURT_APPEARANCE_1_ID = 54321L
private const val NOMIS_COURT_APPEARANCE_2_ID = 65432L
private const val NOMIS_COURT_CHARGE_1_ID = 32121L
private const val NOMIS_COURT_CHARGE_2_ID = 87676L
private const val DPS_COURT_APPEARANCE_1_ID = "dpsca1"
private const val DPS_COURT_APPEARANCE_2_ID = "dpsca2"
private const val DPS_COURT_CHARGE_1_ID = "dpscha1"
private const val DPS_COURT_CHARGE_2_ID = "dpscha2"
private const val NOMIS_COURT_CASE_ID = 54321L
private const val DPS_COURT_CASE_ID = "dps123"
private const val EXISTING_DPS_COURT_CASE_ID = "DPS321"
private const val EXISTING_NOMIS_COURT_CASE_ID = 98765L
private const val EXISTING_NOMIS_COURT_APPEARANCE_ID = 98733L

class CourtSentencingCourtCaseResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: CourtCaseMappingRepository

  @Autowired
  private lateinit var courtAppearanceRepository: CourtAppearanceMappingRepository

  @Autowired
  private lateinit var courtChargeRepository: CourtChargeMappingRepository

  @Nested
  @DisplayName("GET /mapping/court-sentencing/court-cases/dps-court-case-id/{courtCaseId}")
  inner class GetMappingByDpsId {
    lateinit var courtCaseMapping: CourtCaseMapping

    @BeforeEach
    fun setUp() = runTest {
      courtCaseMapping = repository.save(
        CourtCaseMapping(
          dpsCourtCaseId = DPS_COURT_CASE_ID,
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
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("DPS Court case Id =DOESNOTEXIST")
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
  @DisplayName("GET /mapping/court-sentencing/court-cases/nomis-court-case-id/{courtCaseId}")
  inner class GetMappingByNomisId {
    lateinit var courtCaseMapping: CourtCaseMapping

    @BeforeEach
    fun setUp() = runTest {
      courtCaseMapping = repository.save(
        CourtCaseMapping(
          dpsCourtCaseId = DPS_COURT_CASE_ID,
          nomisCourtCaseId = NOMIS_COURT_CASE_ID,
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
          .uri("/mapping/court-sentencing/court-cases/nomis-court-case-id/${courtCaseMapping.nomisCourtCaseId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/nomis-court-case-id/${courtCaseMapping.nomisCourtCaseId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/nomis-court-case-id/${courtCaseMapping.nomisCourtCaseId}")
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
          .uri("/mapping/court-sentencing/court-cases/nomis-court-case-id/879687")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Nomis Court case Id =879687")
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/nomis-court-case-id/${courtCaseMapping.nomisCourtCaseId}")
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
    private val mapping = CourtCaseAllMappingDto(
      dpsCourtCaseId = DPS_COURT_CASE_ID,
      nomisCourtCaseId = NOMIS_COURT_APPEARANCE_1_ID,
      label = "2023-01-01T12:45:12",
      mappingType = CourtCaseMappingType.DPS_CREATED,
      courtAppearances = listOf(
        CourtAppearanceMappingDto(
          dpsCourtAppearanceId = DPS_COURT_APPEARANCE_1_ID,
          nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_1_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.DPS_CREATED,
        ),
        CourtAppearanceMappingDto(
          dpsCourtAppearanceId = DPS_COURT_APPEARANCE_2_ID,
          nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_2_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.DPS_CREATED,
        ),
      ),
      courtCharges = listOf(
        CourtChargeMappingDto(
          dpsCourtChargeId = DPS_COURT_CHARGE_1_ID,
          nomisCourtChargeId = NOMIS_COURT_CHARGE_1_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtChargeMappingType.DPS_CREATED,
        ),
        CourtChargeMappingDto(
          dpsCourtChargeId = DPS_COURT_CHARGE_2_ID,
          nomisCourtChargeId = NOMIS_COURT_CHARGE_2_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtChargeMappingType.DPS_CREATED,
        ),
      ),
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        CourtCaseMapping(
          dpsCourtCaseId = EXISTING_DPS_COURT_CASE_ID,
          nomisCourtCaseId = EXISTING_NOMIS_COURT_CASE_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtCaseMappingType.MIGRATED,
        ),
      )
      courtAppearanceRepository.save(
        CourtAppearanceMapping(
          nomisCourtAppearanceId = EXISTING_NOMIS_COURT_APPEARANCE_ID,
          dpsCourtAppearanceId = "dps123",
          mappingType = CourtAppearanceMappingType.NOMIS_CREATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
      courtAppearanceRepository.deleteAll()
      courtChargeRepository.deleteAll()
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
            id = DPS_COURT_APPEARANCE_1_ID,
          )!!

        val createdCourtAppearance2Mapping =
          courtAppearanceRepository.findById(
            id = DPS_COURT_APPEARANCE_2_ID,
          )!!

        val createdCourtCharge1Mapping =
          courtChargeRepository.findById(
            id = DPS_COURT_CHARGE_1_ID,
          )!!

        val createdCourtCharge2Mapping =
          courtChargeRepository.findById(
            id = DPS_COURT_CHARGE_2_ID,
          )!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisCourtCaseId).isEqualTo(mapping.nomisCourtCaseId)
        assertThat(createdMapping.dpsCourtCaseId).isEqualTo(mapping.dpsCourtCaseId)
        assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(createdMapping.label).isEqualTo(mapping.label)
        assertThat(createdCourtAppearance1Mapping.whenCreated).isCloseTo(
          LocalDateTime.now(),
          within(10, ChronoUnit.SECONDS),
        )
        assertThat(createdCourtAppearance1Mapping.nomisCourtAppearanceId).isEqualTo(NOMIS_COURT_APPEARANCE_1_ID)
        assertThat(createdCourtAppearance1Mapping.dpsCourtAppearanceId).isEqualTo(DPS_COURT_APPEARANCE_1_ID)
        assertThat(createdCourtAppearance1Mapping.mappingType).isEqualTo(CourtAppearanceMappingType.DPS_CREATED)
        assertThat(createdCourtAppearance1Mapping.label).isEqualTo(mapping.courtAppearances[0].label)
        assertThat(createdCourtAppearance2Mapping.whenCreated).isCloseTo(
          LocalDateTime.now(),
          within(10, ChronoUnit.SECONDS),
        )
        assertThat(createdCourtAppearance2Mapping.nomisCourtAppearanceId).isEqualTo(NOMIS_COURT_APPEARANCE_2_ID)
        assertThat(createdCourtAppearance2Mapping.dpsCourtAppearanceId).isEqualTo(DPS_COURT_APPEARANCE_2_ID)
        assertThat(createdCourtAppearance2Mapping.mappingType).isEqualTo(CourtAppearanceMappingType.DPS_CREATED)
        assertThat(createdCourtAppearance2Mapping.label).isEqualTo(mapping.courtAppearances[1].label)
        assertThat(createdCourtCharge1Mapping.nomisCourtChargeId).isEqualTo(NOMIS_COURT_CHARGE_1_ID)
        assertThat(createdCourtCharge1Mapping.dpsCourtChargeId).isEqualTo(DPS_COURT_CHARGE_1_ID)
        assertThat(createdCourtCharge1Mapping.mappingType).isEqualTo(CourtChargeMappingType.DPS_CREATED)
        assertThat(createdCourtCharge1Mapping.label).isEqualTo(mapping.courtCharges[0].label)
        assertThat(createdCourtCharge2Mapping.whenCreated).isCloseTo(
          LocalDateTime.now(),
          within(10, ChronoUnit.SECONDS),
        )
        assertThat(createdCourtCharge2Mapping.nomisCourtChargeId).isEqualTo(NOMIS_COURT_CHARGE_2_ID)
        assertThat(createdCourtCharge2Mapping.dpsCourtChargeId).isEqualTo(DPS_COURT_CHARGE_2_ID)
        assertThat(createdCourtCharge2Mapping.mappingType).isEqualTo(CourtChargeMappingType.DPS_CREATED)
        assertThat(createdCourtCharge2Mapping.label).isEqualTo(mapping.courtCharges[1].label)
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
                  "nomisCourtCaseId": $NOMIS_COURT_CASE_ID,
                  "dpsCourtCaseId": "$DPS_COURT_CASE_ID"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/$DPS_COURT_CASE_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/$EXISTING_DPS_COURT_CASE_ID")
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

      @Test
      fun `returns 409 and mapping dto if child is duplicate`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CourtCaseAllMappingDto(
                // case ids are fine
                nomisCourtCaseId = NOMIS_COURT_CASE_ID,
                dpsCourtCaseId = DPS_COURT_CASE_ID,
                courtAppearances = listOf(
                  CourtAppearanceMappingDto(
                    dpsCourtAppearanceId = "1234",
                    nomisCourtAppearanceId = EXISTING_NOMIS_COURT_APPEARANCE_ID,
                  ),
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
          // for a duplicate child  - will return the original mapping with some decent logging to help debug
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisCourtCaseId", NOMIS_COURT_CASE_ID.toInt())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCourtCaseId", NOMIS_COURT_CASE_ID.toInt())
        }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/court-sentencing/court-cases/dps-court-case-id/{courtCaseId}")
  inner class DeleteMappingByDpsId {
    lateinit var mapping: CourtCaseMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CourtCaseMapping(
          dpsCourtCaseId = DPS_COURT_CASE_ID,
          nomisCourtCaseId = NOMIS_COURT_CASE_ID,
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
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${mapping.dpsCourtCaseId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${mapping.dpsCourtCaseId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${mapping.dpsCourtCaseId}")
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
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/NOPE")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${mapping.dpsCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${mapping.dpsCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${mapping.dpsCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/court-sentencing/court-cases/nomis-court-case-id/{courtCaseId}")
  inner class DeleteMappingByNomisId {
    lateinit var mapping: CourtCaseMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CourtCaseMapping(
          dpsCourtCaseId = DPS_COURT_CASE_ID,
          nomisCourtCaseId = NOMIS_COURT_CASE_ID,
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
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-cases/nomis-court-case-id/${mapping.nomisCourtCaseId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-cases/nomis-court-case-id/${mapping.nomisCourtCaseId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-cases/nomis-court-case-id/${mapping.nomisCourtCaseId}")
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
          .uri("/mapping/court-sentencing/court-cases/nomis-court-case-id/13333")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${mapping.dpsCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        // delete using nomis id
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-cases/nomis-court-case-id/${mapping.nomisCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${mapping.dpsCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @DisplayName("GET /mapping/court-sentencing/court-cases/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationIdTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/court-sentencing/court-cases/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/court-sentencing/court-cases/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/court-sentencing/court-cases/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Test
    fun `can retrieve all mappings by migration Id`() = runTest {
      (1L..4L).forEach {
        repository.save(
          CourtCaseMapping(
            dpsCourtCaseId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
            nomisCourtCaseId = it,
            label = "2023-01-01T12:45:12",
            mappingType = CourtCaseMappingType.MIGRATED,
          ),
        )
      }

      repository.save(
        CourtCaseMapping(
          dpsCourtCaseId = "edcd118c-91ba-42ea-b5c4-404b453ad58b",
          nomisCourtCaseId = 99,
          label = "2022-01-01T12:43:12",
          mappingType = CourtCaseMappingType.MIGRATED,
        ),
      )

      webTestClient.get().uri("/mapping/court-sentencing/court-cases/migration-id/2023-01-01T12:45:12")
        .headers(setAuthorisation(roles = listOf("NOMIS_COURT_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..nomisCourtCaseId").value(
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
      webTestClient.get().uri("/mapping/court-sentencing/court-cases/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("NOMIS_COURT_SENTENCING")))
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
          CourtCaseMapping(
            dpsCourtCaseId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
            nomisCourtCaseId = it,
            label = "2023-01-01T12:45:12",
            mappingType = CourtCaseMappingType.MIGRATED,
          ),
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/court-sentencing/court-cases/migration-id/2023-01-01T12:45:12")
          .queryParam("size", "2")
          .queryParam("sort", "nomisCourtCaseId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_COURT_SENTENCING")))
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
