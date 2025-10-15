package uk.gov.justice.digital.hmpps.nomismappingservice.courtsentencing

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
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val OFFENDER_NO = "AA123456"
private const val EXISTING_OFFENDER_NO = "BB123456"
private const val NOMIS_COURT_APPEARANCE_1_ID = 54321L
private const val NOMIS_COURT_APPEARANCE_2_ID = 65432L
private const val NOMIS_COURT_APPEARANCE_3_ID = 7654L
private const val NOMIS_COURT_APPEARANCE_4_ID = 8765L
private const val NOMIS_COURT_CHARGE_1_ID = 32121L
private const val NOMIS_COURT_CHARGE_2_ID = 87676L
private const val NOMIS_SENTENCE_SEQ = 1
private const val NOMIS_SENTENCE_TERM_SEQ = 2
private const val NOMIS_BOOKING_ID = 32456L
private const val DPS_SENTENCE_ID = "dpss1"
private const val DPS_SENTENCE_ID_2 = "dpss2"
private const val DPS_TERM_ID = "dpss3"
private const val DPS_TERM_ID_2 = "dpss4"
private const val DPS_COURT_APPEARANCE_1_ID = "dpsca1"
private const val DPS_COURT_APPEARANCE_2_ID = "dpsca2"
private const val DPS_COURT_APPEARANCE_3_ID = "dpsca3"
private const val DPS_COURT_APPEARANCE_4_ID = "dpsca4"
private const val DPS_COURT_CHARGE_1_ID = "dpscha1"
private const val DPS_COURT_CHARGE_2_ID = "dpscha2"
private const val NOMIS_COURT_CASE_ID = 54321L
private const val NOMIS_COURT_CASE_2_ID = 65431L
private const val NOMIS_COURT_CASE_3_ID = 87878L
private const val DPS_COURT_CASE_ID = "dps444"
private const val DPS_COURT_CASE_2_ID = "dps123"
private const val DPS_COURT_CASE_3_ID = "dps321"
private const val EXISTING_DPS_COURT_CASE_ID = "DPS321"
private const val EXISTING_NOMIS_COURT_CASE_ID = 98765L
private const val EXISTING_NOMIS_COURT_APPEARANCE_ID = 98733L
private const val EXISTING_NOMIS_SENTENCE_SEQ = 4
private const val EXISTING_NOMIS_SENTENCE_TERM_SEQ = 5

class CourtSentencingCourtCaseResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: CourtCaseMappingRepository

  @Autowired
  private lateinit var prisonerCourtCaseRepository: CourtCasePrisonerMigrationRepository

  @Autowired
  private lateinit var courtAppearanceRepository: CourtAppearanceMappingRepository

  @Autowired
  private lateinit var courtChargeRepository: CourtChargeMappingRepository

  @Autowired
  private lateinit var sentenceRepository: SentenceMappingRepository

  @Autowired
  private lateinit var sentenceTermRepository: SentenceTermMappingRepository

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
      clearDown()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${courtCaseMapping.dpsCourtCaseId}")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(listOf(NOMIS_COURT_CASE_ID, NOMIS_COURT_CASE_2_ID)))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("DPS Court case Id =DOESNOTEXIST")
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${courtCaseMapping.dpsCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
      clearDown()
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Nomis Court case Id =879687")
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/nomis-court-case-id/${courtCaseMapping.nomisCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
  @DisplayName("GET /mapping/court-sentencing/prisoner/{offenderNo}/migration-summary")
  inner class GetMigrationSummaryByOffenderNo {
    lateinit var courtCaseMapping: CourtCasePrisonerMigration

    @BeforeEach
    fun setUp() = runTest {
      courtCaseMapping = prisonerCourtCaseRepository.save(
        CourtCasePrisonerMigration(
          offenderNo = OFFENDER_NO,
          label = "2023-01-01T12:45:12",
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      clearDown()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/migration-summary")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/migration-summary")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/migration-summary")
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
          .uri("/mapping/court-sentencing/prisoner/123/migration-summary")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Court sentencing offender migration summary not found. offenderNo=123")
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/migration-summary")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(OFFENDER_NO)
          .jsonPath("mappingsCount").isEqualTo(0)
          .jsonPath("migrationId").isEqualTo(courtCaseMapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/court-sentencing/prisoner/{offenderNo}/migration-summary")
  inner class DeleteMigrationSummaryByOffenderNo {
    lateinit var courtCaseMapping: CourtCasePrisonerMigration

    @BeforeEach
    fun setUp() = runTest {
      courtCaseMapping = prisonerCourtCaseRepository.save(
        CourtCasePrisonerMigration(
          offenderNo = OFFENDER_NO,
          label = "2023-01-01T12:45:12",
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      clearDown()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/migration-summary")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/migration-summary")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/migration-summary")
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
          .uri("/mapping/court-sentencing/prisoner/NOPE/migration-summary")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/migration-summary")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/migration-summary")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/migration-summary")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
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
      clearDown()
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/$EXISTING_DPS_COURT_CASE_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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

  private suspend fun clearDown() {
    repository.deleteAll()
    courtAppearanceRepository.deleteAll()
    courtChargeRepository.deleteAll()
    sentenceRepository.deleteAll()
    sentenceTermRepository.deleteAll()
    prisonerCourtCaseRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST /mapping/court-sentencing/offender/{offenderNo}court-cases")
  inner class CreateMigrationMapping {
    private lateinit var existingMapping: CourtCaseMapping
    private val mapping = CourtCaseBatchMappingDto(
      courtCases = listOf(
        CourtCaseMappingDto(
          dpsCourtCaseId = DPS_COURT_CASE_ID,
          nomisCourtCaseId = NOMIS_COURT_APPEARANCE_1_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtCaseMappingType.MIGRATED,
        ),
        CourtCaseMappingDto(
          dpsCourtCaseId = DPS_COURT_CASE_2_ID,
          nomisCourtCaseId = NOMIS_COURT_CASE_2_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtCaseMappingType.MIGRATED,
        ),
      ),

      courtAppearances = listOf(
        CourtAppearanceMappingDto(
          dpsCourtAppearanceId = DPS_COURT_APPEARANCE_1_ID,
          nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_1_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.MIGRATED,
        ),
        CourtAppearanceMappingDto(
          dpsCourtAppearanceId = DPS_COURT_APPEARANCE_2_ID,
          nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_2_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.MIGRATED,
        ),
        CourtAppearanceMappingDto(
          dpsCourtAppearanceId = DPS_COURT_APPEARANCE_3_ID,
          nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_3_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.MIGRATED,
        ),
        CourtAppearanceMappingDto(
          dpsCourtAppearanceId = DPS_COURT_APPEARANCE_4_ID,
          nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_4_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.MIGRATED,
        ),
      ),

      courtCharges = listOf(
        CourtChargeMappingDto(
          dpsCourtChargeId = DPS_COURT_CHARGE_1_ID,
          nomisCourtChargeId = NOMIS_COURT_CHARGE_1_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtChargeMappingType.MIGRATED,
        ),
        CourtChargeMappingDto(
          dpsCourtChargeId = DPS_COURT_CHARGE_2_ID,
          nomisCourtChargeId = NOMIS_COURT_CHARGE_2_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtChargeMappingType.MIGRATED,
        ),

      ),
      sentences = listOf(
        SentenceMappingDto(
          dpsSentenceId = DPS_SENTENCE_ID,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQ,
          nomisBookingId = NOMIS_BOOKING_ID,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceMappingType.MIGRATED,
        ),
        SentenceMappingDto(
          dpsSentenceId = DPS_SENTENCE_ID_2,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQ + 1,
          nomisBookingId = NOMIS_BOOKING_ID,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceMappingType.MIGRATED,
        ),
      ),
      sentenceTerms = listOf(
        SentenceTermMappingDto(
          dpsTermId = DPS_TERM_ID,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQ,
          nomisTermSequence = NOMIS_SENTENCE_TERM_SEQ,
          nomisBookingId = NOMIS_BOOKING_ID,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceTermMappingType.MIGRATED,
        ),
        SentenceTermMappingDto(
          dpsTermId = DPS_TERM_ID_2,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQ,
          nomisTermSequence = NOMIS_SENTENCE_TERM_SEQ + 1,
          nomisBookingId = NOMIS_BOOKING_ID,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceTermMappingType.MIGRATED,
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
      sentenceRepository.save(
        SentenceMapping(
          dpsSentenceId = "dps321",
          nomisSentenceSequence = EXISTING_NOMIS_SENTENCE_SEQ,
          nomisBookingId = NOMIS_BOOKING_ID,
          mappingType = SentenceMappingType.NOMIS_CREATED,
        ),
      )
      sentenceTermRepository.save(
        SentenceTermMapping(
          dpsTermId = "dps321",
          nomisSentenceSequence = EXISTING_NOMIS_SENTENCE_SEQ,
          nomisTermSequence = EXISTING_NOMIS_SENTENCE_TERM_SEQ,
          nomisBookingId = NOMIS_BOOKING_ID,
          mappingType = SentenceTermMappingType.NOMIS_CREATED,
        ),
      )
      prisonerCourtCaseRepository.save(
        CourtCasePrisonerMigration(
          offenderNo = EXISTING_OFFENDER_NO,
          mappingType = CourtCaseMappingType.MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      clearDown()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/court-cases")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/court-cases")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/court-cases")
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
      fun `returns 201 when mappings for prisoner created`() = runTest {
        webTestClient.post()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdCourtCase1 =
          repository.findById(
            id = mapping.courtCases[0].dpsCourtCaseId,
          )!!

        val createdCourtCase2 =
          repository.findById(
            id = mapping.courtCases[1].dpsCourtCaseId,
          )!!

        val createdSentenceMapping =
          sentenceRepository.findById(
            id = DPS_SENTENCE_ID,
          )!!

        val createdSentenceTermMapping =
          sentenceTermRepository.findById(
            id = DPS_TERM_ID,
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

        assertThat(createdCourtCase1.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdCourtCase1.nomisCourtCaseId).isEqualTo(mapping.courtCases[0].nomisCourtCaseId)
        assertThat(createdCourtCase1.dpsCourtCaseId).isEqualTo(mapping.courtCases[0].dpsCourtCaseId)
        assertThat(createdCourtCase1.mappingType).isEqualTo(mapping.courtCases[0].mappingType)
        assertThat(createdCourtCase1.label).isEqualTo(mapping.courtCases[0].label)
        assertThat(createdCourtAppearance1Mapping.whenCreated).isCloseTo(
          LocalDateTime.now(),
          within(10, ChronoUnit.SECONDS),
        )
        assertThat(createdCourtCase2.nomisCourtCaseId).isEqualTo(mapping.courtCases[1].nomisCourtCaseId)
        assertThat(createdCourtCase2.dpsCourtCaseId).isEqualTo(mapping.courtCases[1].dpsCourtCaseId)

        assertThat(createdCourtAppearance1Mapping.nomisCourtAppearanceId).isEqualTo(NOMIS_COURT_APPEARANCE_1_ID)
        assertThat(createdCourtAppearance1Mapping.dpsCourtAppearanceId).isEqualTo(DPS_COURT_APPEARANCE_1_ID)
        assertThat(createdCourtAppearance1Mapping.mappingType).isEqualTo(CourtAppearanceMappingType.MIGRATED)
        assertThat(createdCourtAppearance1Mapping.label).isEqualTo(mapping.courtAppearances[0].label)
        assertThat(createdCourtAppearance1Mapping.whenCreated).isCloseTo(
          LocalDateTime.now(),
          within(10, ChronoUnit.SECONDS),
        )
        assertThat(createdCourtAppearance2Mapping.nomisCourtAppearanceId).isEqualTo(NOMIS_COURT_APPEARANCE_2_ID)
        assertThat(createdCourtAppearance2Mapping.dpsCourtAppearanceId).isEqualTo(DPS_COURT_APPEARANCE_2_ID)
        assertThat(createdCourtAppearance2Mapping.mappingType).isEqualTo(CourtAppearanceMappingType.MIGRATED)
        assertThat(createdCourtAppearance2Mapping.label).isEqualTo(mapping.courtAppearances[1].label)
        assertThat(createdCourtCharge1Mapping.nomisCourtChargeId).isEqualTo(NOMIS_COURT_CHARGE_1_ID)
        assertThat(createdCourtCharge1Mapping.dpsCourtChargeId).isEqualTo(DPS_COURT_CHARGE_1_ID)
        assertThat(createdCourtCharge1Mapping.mappingType).isEqualTo(CourtChargeMappingType.MIGRATED)
        assertThat(createdCourtCharge1Mapping.label).isEqualTo(mapping.courtCharges[0].label)
        assertThat(createdCourtCharge2Mapping.whenCreated).isCloseTo(
          LocalDateTime.now(),
          within(10, ChronoUnit.SECONDS),
        )
        assertThat(createdCourtCharge2Mapping.nomisCourtChargeId).isEqualTo(NOMIS_COURT_CHARGE_2_ID)
        assertThat(createdCourtCharge2Mapping.dpsCourtChargeId).isEqualTo(DPS_COURT_CHARGE_2_ID)
        assertThat(createdCourtCharge2Mapping.mappingType).isEqualTo(CourtChargeMappingType.MIGRATED)
        assertThat(createdCourtCharge2Mapping.label).isEqualTo(mapping.courtCharges[1].label)

        assertThat(createdSentenceMapping.nomisSentenceSequence).isEqualTo(NOMIS_SENTENCE_SEQ)
        assertThat(createdSentenceMapping.nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
        assertThat(createdSentenceMapping.dpsSentenceId).isEqualTo(DPS_SENTENCE_ID)
        assertThat(createdSentenceMapping.mappingType).isEqualTo(SentenceMappingType.MIGRATED)
        assertThat(createdSentenceMapping.label).isEqualTo(mapping.sentences[0].label)

        assertThat(createdSentenceTermMapping.nomisSentenceSequence).isEqualTo(NOMIS_SENTENCE_SEQ)
        assertThat(createdSentenceTermMapping.nomisTermSequence).isEqualTo(NOMIS_SENTENCE_TERM_SEQ)
        assertThat(createdSentenceTermMapping.nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
        assertThat(createdSentenceTermMapping.dpsTermId).isEqualTo(DPS_TERM_ID)
        assertThat(createdSentenceTermMapping.mappingType).isEqualTo(SentenceTermMappingType.MIGRATED)
        assertThat(createdSentenceTermMapping.label).isEqualTo(mapping.sentenceTerms[0].label)

        // the prisoner tracking table should have an entry for this offender
        assertThat(prisonerCourtCaseRepository.findById(OFFENDER_NO)).isNotNull
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when mapping type is invalid`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/prisoner/$OFFENDER_NO/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
          .uri("/mapping/court-sentencing/prisoner/${OFFENDER_NO}/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                "offenderNo": "A1234A",
                "courtCases": [{
                  "nomisCourtCaseId": 54321
                }]
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `if court case already exists, will recreate mappings`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/prisoner/${OFFENDER_NO}/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CourtCaseBatchMappingDto(
                courtCases = listOf(
                  CourtCaseMappingDto(
                    nomisCourtCaseId = existingMapping.nomisCourtCaseId,
                    dpsCourtCaseId = "DPS888",
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(201)
      }

      @Test
      fun `if offender already migrated, will recreate mappings`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/prisoner/${EXISTING_OFFENDER_NO}/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CourtCaseBatchMappingDto(
                courtCases = mapping.courtCases,
              ),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(201)
      }

      @Test
      fun `if court appearance already migrated, will recreate mappings`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/prisoner/${OFFENDER_NO}/court-cases")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CourtCaseBatchMappingDto(
                courtCases = mapping.courtCases,
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
          .expectStatus().isEqualTo(201)
      }
    }

    @Test
    fun `if sentence already migrated, will recreate mappings`() {
      webTestClient.post()
        .uri("/mapping/court-sentencing/prisoner/${OFFENDER_NO}/court-cases")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CourtCaseBatchMappingDto(
              courtCases = mapping.courtCases,
              sentences = listOf(
                SentenceMappingDto(
                  dpsSentenceId = "1234",
                  nomisSentenceSequence = EXISTING_NOMIS_SENTENCE_SEQ,
                  nomisBookingId = NOMIS_BOOKING_ID,
                ),
              ),
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(201)
    }

    @Test
    fun `if sentence term already migrated, will recreate mappings`() {
      webTestClient.post()
        .uri("/mapping/court-sentencing/prisoner/${OFFENDER_NO}/court-cases")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CourtCaseBatchMappingDto(
              courtCases = mapping.courtCases,
              sentenceTerms = listOf(
                SentenceTermMappingDto(
                  dpsTermId = "1234",
                  nomisSentenceSequence = EXISTING_NOMIS_SENTENCE_SEQ,
                  nomisTermSequence = EXISTING_NOMIS_SENTENCE_TERM_SEQ,
                  nomisBookingId = NOMIS_BOOKING_ID,
                ),
              ),
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(201)
    }
  }

  @Nested
  @DisplayName("PUT /mapping/court-sentencing/court-cases/replace")
  inner class ReplaceOrCreateMappingByDpsId {
    private lateinit var existingMapping: CourtCaseMapping
    private val mapping = CourtCaseBatchMappingDto(
      courtCases = listOf(
        CourtCaseMappingDto(
          dpsCourtCaseId = DPS_COURT_CASE_ID,
          nomisCourtCaseId = NOMIS_COURT_APPEARANCE_1_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtCaseMappingType.MIGRATED,
        ),
        CourtCaseMappingDto(
          dpsCourtCaseId = DPS_COURT_CASE_2_ID,
          nomisCourtCaseId = NOMIS_COURT_CASE_2_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtCaseMappingType.MIGRATED,
        ),
      ),

      courtAppearances = listOf(
        CourtAppearanceMappingDto(
          dpsCourtAppearanceId = DPS_COURT_APPEARANCE_1_ID,
          nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_1_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.MIGRATED,
        ),
        CourtAppearanceMappingDto(
          dpsCourtAppearanceId = DPS_COURT_APPEARANCE_2_ID,
          nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_2_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.MIGRATED,
        ),
        CourtAppearanceMappingDto(
          dpsCourtAppearanceId = DPS_COURT_APPEARANCE_3_ID,
          nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_3_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.MIGRATED,
        ),
        CourtAppearanceMappingDto(
          dpsCourtAppearanceId = DPS_COURT_APPEARANCE_4_ID,
          nomisCourtAppearanceId = NOMIS_COURT_APPEARANCE_4_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.MIGRATED,
        ),
      ),

      courtCharges = listOf(
        CourtChargeMappingDto(
          dpsCourtChargeId = DPS_COURT_CHARGE_1_ID,
          nomisCourtChargeId = NOMIS_COURT_CHARGE_1_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtChargeMappingType.MIGRATED,
        ),
        CourtChargeMappingDto(
          dpsCourtChargeId = DPS_COURT_CHARGE_2_ID,
          nomisCourtChargeId = NOMIS_COURT_CHARGE_2_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtChargeMappingType.MIGRATED,
        ),

      ),
      sentences = listOf(
        SentenceMappingDto(
          dpsSentenceId = DPS_SENTENCE_ID,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQ,
          nomisBookingId = NOMIS_BOOKING_ID,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceMappingType.MIGRATED,
        ),
        SentenceMappingDto(
          dpsSentenceId = DPS_SENTENCE_ID_2,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQ + 1,
          nomisBookingId = NOMIS_BOOKING_ID,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceMappingType.MIGRATED,
        ),
      ),
      sentenceTerms = listOf(
        SentenceTermMappingDto(
          dpsTermId = DPS_TERM_ID,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQ,
          nomisTermSequence = NOMIS_SENTENCE_TERM_SEQ,
          nomisBookingId = NOMIS_BOOKING_ID,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceTermMappingType.MIGRATED,
        ),
        SentenceTermMappingDto(
          dpsTermId = DPS_TERM_ID_2,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQ,
          nomisTermSequence = NOMIS_SENTENCE_TERM_SEQ + 1,
          nomisBookingId = NOMIS_BOOKING_ID,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceTermMappingType.MIGRATED,
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
      sentenceRepository.save(
        SentenceMapping(
          dpsSentenceId = "dps321",
          nomisSentenceSequence = EXISTING_NOMIS_SENTENCE_SEQ,
          nomisBookingId = NOMIS_BOOKING_ID,
          mappingType = SentenceMappingType.NOMIS_CREATED,
        ),
      )
      sentenceTermRepository.save(
        SentenceTermMapping(
          dpsTermId = "dps321",
          nomisSentenceSequence = EXISTING_NOMIS_SENTENCE_SEQ,
          nomisTermSequence = EXISTING_NOMIS_SENTENCE_TERM_SEQ,
          nomisBookingId = NOMIS_BOOKING_ID,
          mappingType = SentenceTermMappingType.NOMIS_CREATED,
        ),
      )
      prisonerCourtCaseRepository.save(
        CourtCasePrisonerMigration(
          offenderNo = EXISTING_OFFENDER_NO,
          mappingType = CourtCaseMappingType.MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      clearDown()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/court-sentencing/court-cases/replace")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/court-sentencing/court-cases/replace")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/court-sentencing/court-cases/replace")
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
      fun `returns 200 when batch mappings are replaced`() = runTest {
        webTestClient.put()
          .uri("/mapping/court-sentencing/court-cases/replace")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk

        val createdCourtCase1 =
          repository.findById(
            id = mapping.courtCases[0].dpsCourtCaseId,
          )!!

        val createdCourtCase2 =
          repository.findById(
            id = mapping.courtCases[1].dpsCourtCaseId,
          )!!

        val createdSentenceMapping =
          sentenceRepository.findById(
            id = DPS_SENTENCE_ID,
          )!!

        val createdSentenceTermMapping =
          sentenceTermRepository.findById(
            id = DPS_TERM_ID,
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

        assertThat(createdCourtCase1.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdCourtCase1.nomisCourtCaseId).isEqualTo(mapping.courtCases[0].nomisCourtCaseId)
        assertThat(createdCourtCase1.dpsCourtCaseId).isEqualTo(mapping.courtCases[0].dpsCourtCaseId)
        assertThat(createdCourtCase1.mappingType).isEqualTo(mapping.courtCases[0].mappingType)
        assertThat(createdCourtCase1.label).isEqualTo(mapping.courtCases[0].label)
        assertThat(createdCourtAppearance1Mapping.whenCreated).isCloseTo(
          LocalDateTime.now(),
          within(10, ChronoUnit.SECONDS),
        )
        assertThat(createdCourtCase2.nomisCourtCaseId).isEqualTo(mapping.courtCases[1].nomisCourtCaseId)
        assertThat(createdCourtCase2.dpsCourtCaseId).isEqualTo(mapping.courtCases[1].dpsCourtCaseId)

        assertThat(createdCourtAppearance1Mapping.nomisCourtAppearanceId).isEqualTo(NOMIS_COURT_APPEARANCE_1_ID)
        assertThat(createdCourtAppearance1Mapping.dpsCourtAppearanceId).isEqualTo(DPS_COURT_APPEARANCE_1_ID)
        assertThat(createdCourtAppearance1Mapping.mappingType).isEqualTo(CourtAppearanceMappingType.MIGRATED)
        assertThat(createdCourtAppearance1Mapping.label).isEqualTo(mapping.courtAppearances[0].label)
        assertThat(createdCourtAppearance1Mapping.whenCreated).isCloseTo(
          LocalDateTime.now(),
          within(10, ChronoUnit.SECONDS),
        )
        assertThat(createdCourtAppearance2Mapping.nomisCourtAppearanceId).isEqualTo(NOMIS_COURT_APPEARANCE_2_ID)
        assertThat(createdCourtAppearance2Mapping.dpsCourtAppearanceId).isEqualTo(DPS_COURT_APPEARANCE_2_ID)
        assertThat(createdCourtAppearance2Mapping.mappingType).isEqualTo(CourtAppearanceMappingType.MIGRATED)
        assertThat(createdCourtAppearance2Mapping.label).isEqualTo(mapping.courtAppearances[1].label)
        assertThat(createdCourtCharge1Mapping.nomisCourtChargeId).isEqualTo(NOMIS_COURT_CHARGE_1_ID)
        assertThat(createdCourtCharge1Mapping.dpsCourtChargeId).isEqualTo(DPS_COURT_CHARGE_1_ID)
        assertThat(createdCourtCharge1Mapping.mappingType).isEqualTo(CourtChargeMappingType.MIGRATED)
        assertThat(createdCourtCharge1Mapping.label).isEqualTo(mapping.courtCharges[0].label)
        assertThat(createdCourtCharge2Mapping.whenCreated).isCloseTo(
          LocalDateTime.now(),
          within(10, ChronoUnit.SECONDS),
        )
        assertThat(createdCourtCharge2Mapping.nomisCourtChargeId).isEqualTo(NOMIS_COURT_CHARGE_2_ID)
        assertThat(createdCourtCharge2Mapping.dpsCourtChargeId).isEqualTo(DPS_COURT_CHARGE_2_ID)
        assertThat(createdCourtCharge2Mapping.mappingType).isEqualTo(CourtChargeMappingType.MIGRATED)
        assertThat(createdCourtCharge2Mapping.label).isEqualTo(mapping.courtCharges[1].label)

        assertThat(createdSentenceMapping.nomisSentenceSequence).isEqualTo(NOMIS_SENTENCE_SEQ)
        assertThat(createdSentenceMapping.nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
        assertThat(createdSentenceMapping.dpsSentenceId).isEqualTo(DPS_SENTENCE_ID)
        assertThat(createdSentenceMapping.mappingType).isEqualTo(SentenceMappingType.MIGRATED)
        assertThat(createdSentenceMapping.label).isEqualTo(mapping.sentences[0].label)

        assertThat(createdSentenceTermMapping.nomisSentenceSequence).isEqualTo(NOMIS_SENTENCE_SEQ)
        assertThat(createdSentenceTermMapping.nomisTermSequence).isEqualTo(NOMIS_SENTENCE_TERM_SEQ)
        assertThat(createdSentenceTermMapping.nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
        assertThat(createdSentenceTermMapping.dpsTermId).isEqualTo(DPS_TERM_ID)
        assertThat(createdSentenceTermMapping.mappingType).isEqualTo(SentenceTermMappingType.MIGRATED)
        assertThat(createdSentenceTermMapping.label).isEqualTo(mapping.sentenceTerms[0].label)
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when mapping type is invalid`() {
        webTestClient.put()
          .uri("/mapping/court-sentencing/court-cases/replace")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
        webTestClient.put()
          .uri("/mapping/court-sentencing/court-cases/replace")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                "offenderNo": "A1234A",
                "courtCases": [{
                  "nomisCourtCaseId": 54321
                }]
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `if court case already exists, will replace mappings`() {
        webTestClient.put()
          .uri("/mapping/court-sentencing/court-cases/replace")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CourtCaseBatchMappingDto(
                courtCases = listOf(
                  CourtCaseMappingDto(
                    nomisCourtCaseId = existingMapping.nomisCourtCaseId,
                    dpsCourtCaseId = "DPS888",
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(200)
      }

      @Test
      fun `if court appearance already exists, will replace mappings`() {
        webTestClient.put()
          .uri("/mapping/court-sentencing/court-cases/replace")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CourtCaseBatchMappingDto(
                courtCases = mapping.courtCases,
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
          .expectStatus().isEqualTo(200)
      }
    }

    @Test
    fun `if sentence already exists, will replace mappings`() {
      webTestClient.put()
        .uri("/mapping/court-sentencing/court-cases/replace")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CourtCaseBatchMappingDto(
              courtCases = mapping.courtCases,
              sentences = listOf(
                SentenceMappingDto(
                  dpsSentenceId = "1234",
                  nomisSentenceSequence = EXISTING_NOMIS_SENTENCE_SEQ,
                  nomisBookingId = NOMIS_BOOKING_ID,
                ),
              ),
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(200)
    }

    @Test
    fun `if sentence term already exists, will replace mappings`() {
      webTestClient.put()
        .uri("/mapping/court-sentencing/court-cases/replace")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CourtCaseBatchMappingDto(
              courtCases = mapping.courtCases,
              sentenceTerms = listOf(
                SentenceTermMappingDto(
                  dpsTermId = "1234",
                  nomisSentenceSequence = EXISTING_NOMIS_SENTENCE_SEQ,
                  nomisTermSequence = EXISTING_NOMIS_SENTENCE_TERM_SEQ,
                  nomisBookingId = NOMIS_BOOKING_ID,
                ),
              ),
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(200)
    }
  }

  @Nested
  @DisplayName("PUT /mapping/court-sentencing/court-cases/update-create")
  inner class UpdateAndCreateMappingByNomisId {
    @AfterEach
    fun tearDown() = runTest {
      clearDown()
    }

    @Nested
    inner class Security {
      val request = CourtCaseBatchUpdateAndCreateMappingDto(
        mappingsToCreate = CourtCaseBatchMappingDto(),
        mappingsToUpdate = CourtCaseBatchUpdateMappingDto(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/court-sentencing/court-cases/update-create")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/court-sentencing/court-cases/update-create")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/court-sentencing/court-cases/update-create")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      val fromNomisBookingId = 10L
      val toNomisBookingId = 20L
      val fromNomisSentenceSequence = 10
      val toNomisSentenceSequence = 20
      val fromNomisSequence = 1
      val toNomisSequence = 2
      val dpsId = UUID.randomUUID().toString()
      val newDpsId = UUID.randomUUID().toString()
      val fromNomisId = 99L
      val toNomisId = 100L

      @BeforeEach
      fun setUp() = runTest {
        repository.save(
          CourtCaseMapping(
            dpsCourtCaseId = dpsId,
            nomisCourtCaseId = fromNomisId,
            mappingType = CourtCaseMappingType.NOMIS_CREATED,
          ),
        )
        courtAppearanceRepository.save(
          CourtAppearanceMapping(
            nomisCourtAppearanceId = fromNomisId,
            dpsCourtAppearanceId = dpsId,
            mappingType = CourtAppearanceMappingType.NOMIS_CREATED,
          ),
        )
        courtChargeRepository.save(
          CourtChargeMapping(
            nomisCourtChargeId = fromNomisId,
            dpsCourtChargeId = dpsId,
            mappingType = CourtChargeMappingType.NOMIS_CREATED,
          ),
        )
        sentenceRepository.save(
          SentenceMapping(
            dpsSentenceId = dpsId,
            nomisSentenceSequence = fromNomisSentenceSequence,
            nomisBookingId = fromNomisBookingId,
            mappingType = SentenceMappingType.NOMIS_CREATED,
          ),
        )
        sentenceTermRepository.save(
          SentenceTermMapping(
            dpsTermId = dpsId,
            nomisSentenceSequence = fromNomisSentenceSequence,
            nomisTermSequence = fromNomisSequence,
            nomisBookingId = fromNomisBookingId,
            mappingType = SentenceTermMappingType.NOMIS_CREATED,
          ),
        )
      }

      @Test
      fun `will update existing mappings`() = runTest {
        assertThat(repository.findById(dpsId)!!.nomisCourtCaseId).isEqualTo(fromNomisId)
        assertThat(courtAppearanceRepository.findById(dpsId)!!.nomisCourtAppearanceId).isEqualTo(fromNomisId)
        assertThat(courtChargeRepository.findById(dpsId)!!.nomisCourtChargeId).isEqualTo(fromNomisId)
        assertThat(sentenceRepository.findById(dpsId)!!.nomisBookingId).isEqualTo(fromNomisBookingId)
        assertThat(sentenceRepository.findById(dpsId)!!.nomisSentenceSequence).isEqualTo(fromNomisSentenceSequence)
        assertThat(sentenceTermRepository.findById(dpsId)!!.nomisBookingId).isEqualTo(fromNomisBookingId)
        assertThat(sentenceTermRepository.findById(dpsId)!!.nomisSentenceSequence).isEqualTo(fromNomisSentenceSequence)
        assertThat(sentenceTermRepository.findById(dpsId)!!.nomisTermSequence).isEqualTo(fromNomisSequence)

        webTestClient.put()
          .uri("/mapping/court-sentencing/court-cases/update-create")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CourtCaseBatchUpdateAndCreateMappingDto(
                mappingsToCreate = CourtCaseBatchMappingDto(),
                mappingsToUpdate = CourtCaseBatchUpdateMappingDto(
                  courtCases = listOf(SimpleCourtSentencingIdPair(fromNomisId, toNomisId)),
                  courtAppearances = listOf(SimpleCourtSentencingIdPair(fromNomisId, toNomisId)),
                  courtCharges = listOf(SimpleCourtSentencingIdPair(fromNomisId, toNomisId)),
                  sentences = listOf(CourtSentenceIdPair(SentenceId(fromNomisBookingId, fromNomisSentenceSequence), SentenceId(toNomisBookingId, toNomisSentenceSequence))),
                  sentenceTerms = listOf(CourtSentenceTermIdPair(SentenceTermId(SentenceId(fromNomisBookingId, fromNomisSentenceSequence), fromNomisSequence), SentenceTermId(SentenceId(toNomisBookingId, toNomisSentenceSequence), toNomisSequence))),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(200)

        assertThat(repository.findById(dpsId)!!.nomisCourtCaseId).isEqualTo(toNomisId)
        assertThat(courtAppearanceRepository.findById(dpsId)!!.nomisCourtAppearanceId).isEqualTo(toNomisId)
        assertThat(courtChargeRepository.findById(dpsId)!!.nomisCourtChargeId).isEqualTo(toNomisId)
        assertThat(sentenceRepository.findById(dpsId)!!.nomisBookingId).isEqualTo(toNomisBookingId)
        assertThat(sentenceRepository.findById(dpsId)!!.nomisSentenceSequence).isEqualTo(toNomisSentenceSequence)
        assertThat(sentenceTermRepository.findById(dpsId)!!.nomisBookingId).isEqualTo(toNomisBookingId)
        assertThat(sentenceTermRepository.findById(dpsId)!!.nomisSentenceSequence).isEqualTo(toNomisSentenceSequence)
        assertThat(sentenceTermRepository.findById(dpsId)!!.nomisTermSequence).isEqualTo(toNomisSequence)
      }

      @Test
      fun `will create any mappings requests`() = runTest {
        webTestClient.put()
          .uri("/mapping/court-sentencing/court-cases/update-create")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CourtCaseBatchUpdateAndCreateMappingDto(
                mappingsToCreate = CourtCaseBatchMappingDto(courtCases = listOf(CourtCaseMappingDto(nomisCourtCaseId = 1123L, dpsCourtCaseId = newDpsId))),
                mappingsToUpdate = CourtCaseBatchUpdateMappingDto(),
              ),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(200)

        assertThat(repository.findById(newDpsId)!!.nomisCourtCaseId).isEqualTo(1123L)
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
      clearDown()
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${mapping.dpsCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${mapping.dpsCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${mapping.dpsCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
      clearDown()
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${mapping.dpsCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        // delete using nomis id
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-cases/nomis-court-case-id/${mapping.nomisCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-cases/dps-court-case-id/${mapping.dpsCourtCaseId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @DisplayName("GET /mapping/court-sentencing/prisoner/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationIdTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      clearDown()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/court-sentencing/prisoner/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/court-sentencing/prisoner/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/court-sentencing/prisoner/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Test
    fun `can retrieve all mappings by migration Id`() = runTest {
      (1L..4L).forEach {
        prisonerCourtCaseRepository.save(
          CourtCasePrisonerMigration(
            offenderNo = "offender$it",
            label = "2023-01-01T12:45:12",
            mappingType = CourtCaseMappingType.MIGRATED,
          ),
        )
      }

// different migration id
      prisonerCourtCaseRepository.save(
        CourtCasePrisonerMigration(
          offenderNo = "offender5",
          label = "2021-01-01T12:45:12",
          mappingType = CourtCaseMappingType.MIGRATED,
        ),
      )

      webTestClient.get().uri("/mapping/court-sentencing/prisoner/migration-id/2023-01-01T12:45:12")
        .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..offenderNo").value(
          Matchers.contains(
            "offender1",
            "offender2",
            "offender3",
            "offender4",
          ),
        )
        .jsonPath("$.content[0].whenCreated").isNotEmpty
    }

    @Test
    fun `200 response even when no mappings are found`() {
      webTestClient.get().uri("/mapping/court-sentencing/prisoner/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
        .jsonPath("content").isEmpty
    }

    @Test
    fun `can request a different page size`() = runTest {
      (1L..6L).forEach {
        prisonerCourtCaseRepository.save(
          CourtCasePrisonerMigration(
            offenderNo = "offender$it",
            label = "2023-01-01T12:45:12",
            mappingType = CourtCaseMappingType.MIGRATED,
          ),
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/court-sentencing/prisoner/migration-id/2023-01-01T12:45:12")
          .queryParam("size", "2")
          .queryParam("sort", "offenderNo,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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

  @Nested
  @DisplayName("DELETE /mapping/court-sentencing/all")
  inner class DeleteAllMappings {
    @BeforeEach
    fun setUp() = runTest {
      repository.save(
        CourtCaseMapping(
          dpsCourtCaseId = DPS_COURT_CASE_ID,
          nomisCourtCaseId = NOMIS_COURT_CASE_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtCaseMappingType.MIGRATED,
        ),
      )
      prisonerCourtCaseRepository.save(
        CourtCasePrisonerMigration(
          offenderNo = OFFENDER_NO,
          label = "2023-01-01T12:45:12",
        ),
      )
      courtAppearanceRepository.save(
        CourtAppearanceMapping(
          nomisCourtAppearanceId = EXISTING_NOMIS_COURT_APPEARANCE_ID,
          dpsCourtAppearanceId = "dps123",
          mappingType = CourtAppearanceMappingType.NOMIS_CREATED,
        ),
      )

      courtChargeRepository.save(
        CourtChargeMapping(
          dpsCourtChargeId = "122",
          nomisCourtChargeId = 123,
          mappingType = CourtChargeMappingType.MIGRATED,
        ),
      )
      sentenceRepository.save(
        SentenceMapping(
          dpsSentenceId = "dps321",
          nomisSentenceSequence = EXISTING_NOMIS_SENTENCE_SEQ,
          nomisBookingId = NOMIS_BOOKING_ID,
          mappingType = SentenceMappingType.NOMIS_CREATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      clearDown()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/all")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/all")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/all")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return 204 and delete all mappings`() = runTest {
        assertThat(repository.count()).isEqualTo(1)
        assertThat(prisonerCourtCaseRepository.count()).isEqualTo(1)
        assertThat(courtAppearanceRepository.count()).isEqualTo(1)
        assertThat(courtChargeRepository.count()).isEqualTo(1)
        assertThat(sentenceRepository.count()).isEqualTo(1)

        webTestClient.delete()
          .uri("/mapping/court-sentencing/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(repository.count()).isEqualTo(0)
        assertThat(prisonerCourtCaseRepository.count()).isEqualTo(0)
        assertThat(courtAppearanceRepository.count()).isEqualTo(0)
        assertThat(courtChargeRepository.count()).isEqualTo(0)
        assertThat(sentenceRepository.count()).isEqualTo(0)
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/court-sentencing/court-cases/nomis-case-ids/get-list")
  inner class GetMappingsByListOfNomisIds {
    lateinit var courtCaseMapping: CourtCaseMapping
    lateinit var courtCaseMapping2: CourtCaseMapping
    lateinit var courtCaseMapping3: CourtCaseMapping

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
      courtCaseMapping2 = repository.save(
        CourtCaseMapping(
          dpsCourtCaseId = DPS_COURT_CASE_2_ID,
          nomisCourtCaseId = NOMIS_COURT_CASE_2_ID,
          label = "2023-01-01T12:00:12",
          mappingType = CourtCaseMappingType.MIGRATED,
        ),
      )
      courtCaseMapping3 = repository.save(
        CourtCaseMapping(
          dpsCourtCaseId = DPS_COURT_CASE_3_ID,
          nomisCourtCaseId = NOMIS_COURT_CASE_3_ID,
          label = "2023-01-01T11:00:12",
          mappingType = CourtCaseMappingType.MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      clearDown()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases/nomis-case-ids/get-list")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(listOf(NOMIS_COURT_CASE_ID, NOMIS_COURT_CASE_2_ID)))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases/nomis-case-ids/get-list")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(listOf(NOMIS_COURT_CASE_ID, NOMIS_COURT_CASE_2_ID)))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases/nomis-case-ids/get-list")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(listOf(NOMIS_COURT_CASE_ID, NOMIS_COURT_CASE_2_ID)))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return partial list of not all found`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases/nomis-case-ids/get-list")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(listOf(NOMIS_COURT_CASE_ID, 47564756)))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(1)
          .jsonPath("[0].nomisCourtCaseId").isEqualTo(courtCaseMapping.nomisCourtCaseId)
          .jsonPath("[0].dpsCourtCaseId").isEqualTo(courtCaseMapping.dpsCourtCaseId)
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases/nomis-case-ids/get-list")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(listOf(NOMIS_COURT_CASE_ID, NOMIS_COURT_CASE_2_ID)))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(2)
          .jsonPath("[0].nomisCourtCaseId").isEqualTo(courtCaseMapping.nomisCourtCaseId)
          .jsonPath("[0].dpsCourtCaseId").isEqualTo(courtCaseMapping.dpsCourtCaseId)
          .jsonPath("[1].nomisCourtCaseId").isEqualTo(courtCaseMapping2.nomisCourtCaseId)
          .jsonPath("[1].dpsCourtCaseId").isEqualTo(courtCaseMapping2.dpsCourtCaseId)
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/court-sentencing/court-cases/delete-by-dps-ids")
  inner class DeleteBatchByDpsIds {
    private val dpsCaseId = "dps-batch-case-1"
    private val dpsAppearanceId = "dps-batch-appearance-1"
    private val dpsChargeId = "dps-batch-charge-1"
    private val dpsSentenceId = "dps-batch-sentence-1"
    private val dpsTermId = "dps-batch-term-1"

    @AfterEach
    fun tearDown() = runTest { clearDown() }

    @BeforeEach
    fun seedAll() = runTest {
      repository.save(
        CourtCaseMapping(
          dpsCourtCaseId = dpsCaseId,
          nomisCourtCaseId = 4444L,
          label = "2023-01-01T12:45:12",
          mappingType = CourtCaseMappingType.MIGRATED,
        ),
      )
      courtAppearanceRepository.save(
        CourtAppearanceMapping(
          dpsCourtAppearanceId = dpsAppearanceId,
          nomisCourtAppearanceId = 5555L,
          label = "2023-01-01T12:45:12",
          mappingType = CourtAppearanceMappingType.MIGRATED,
        ),
      )
      courtChargeRepository.save(
        CourtChargeMapping(
          dpsCourtChargeId = dpsChargeId,
          nomisCourtChargeId = 6666L,
          label = "2023-01-01T12:45:12",
          mappingType = CourtChargeMappingType.MIGRATED,
        ),
      )
      sentenceRepository.save(
        SentenceMapping(
          dpsSentenceId = dpsSentenceId,
          nomisBookingId = 7777L,
          nomisSentenceSequence = 1,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceMappingType.MIGRATED,
        ),
      )
      sentenceTermRepository.save(
        SentenceTermMapping(
          dpsTermId = dpsTermId,
          nomisBookingId = 7777L,
          nomisSentenceSequence = 1,
          nomisTermSequence = 1,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceTermMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases/delete-by-dps-ids")
          .body(BodyInserters.fromValue(DpsCourtCaseBatchMappingDto()))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases/delete-by-dps-ids")
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(DpsCourtCaseBatchMappingDto()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases/delete-by-dps-ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(DpsCourtCaseBatchMappingDto()))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete case, appearance, charge, sentence and sentence term mappings`() = runTest {
        // verify present
        assertThat(repository.findById(dpsCaseId)).isNotNull()
        assertThat(courtAppearanceRepository.findById(dpsAppearanceId)).isNotNull()
        assertThat(courtChargeRepository.findById(dpsChargeId)).isNotNull()
        assertThat(sentenceRepository.findById(dpsSentenceId)).isNotNull()
        assertThat(sentenceTermRepository.findById(dpsTermId)).isNotNull()

        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases/delete-by-dps-ids")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .body(
            BodyInserters.fromValue(
              DpsCourtCaseBatchMappingDto(
                courtCases = listOf(dpsCaseId),
                courtAppearances = listOf(dpsAppearanceId),
                courtCharges = listOf(dpsChargeId),
                sentences = listOf(dpsSentenceId),
                sentenceTerms = listOf(dpsTermId),
              ),
            ),
          )
          .exchange()
          .expectStatus().isNoContent

        // verify deleted
        assertThat(repository.findById(dpsCaseId)).isNull()
        assertThat(courtAppearanceRepository.findById(dpsAppearanceId)).isNull()
        assertThat(courtChargeRepository.findById(dpsChargeId)).isNull()
        assertThat(sentenceRepository.findById(dpsSentenceId)).isNull()
        assertThat(sentenceTermRepository.findById(dpsTermId)).isNull()
      }

      @Test
      fun `will return 204 even when none exist`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/court-cases/delete-by-dps-ids")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .body(
            BodyInserters.fromValue(
              DpsCourtCaseBatchMappingDto(
                courtCases = listOf("nope1"),
                courtAppearances = listOf("nope2"),
                courtCharges = listOf("nope3"),
                sentences = listOf("nope4"),
                sentenceTerms = listOf("nope5"),
              ),
            ),
          )
          .exchange()
          .expectStatus().isNoContent
      }
    }
  }
}
