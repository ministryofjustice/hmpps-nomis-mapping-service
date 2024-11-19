package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.casenotes

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val DPS_CASENOTE_ID = "e52d7268-6e10-41a8-a0b9-2319b32520d6"
private const val DPS_CASENOTE_ID2 = "edcd118c-41ba-42ea-b5c4-404b453ad58b"
private const val NOMIS_CASENOTE_ID = 543211L
private const val OFFENDER_NO = "A1234AA"

class CaseNoteMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: CaseNoteMappingRepository

  private fun generateUUIDs(n: Long) = "de91dfa7-821f-4552-a427-000000${n.toString().padStart(6, '0')}"
  private fun generateUUID(n: Long) = UUID.fromString(generateUUIDs(n))

  private fun postCreateMappingRequest(
    nomisCaseNoteId: Long = NOMIS_CASENOTE_ID,
    dpsCaseNoteId: String = DPS_CASENOTE_ID,
    offenderNo: String = OFFENDER_NO,
    nomisBookingId: Long = 1,
    label: String = "2022-01-01",
    mappingType: CaseNoteMappingType = CaseNoteMappingType.DPS_CREATED,
  ) {
    webTestClient.post().uri("/mapping/casenotes")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          CaseNoteMappingDto(
            nomisCaseNoteId = nomisCaseNoteId,
            dpsCaseNoteId = dpsCaseNoteId,
            offenderNo = offenderNo,
            nomisBookingId = nomisBookingId,
            label = label,
            mappingType = mappingType,
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  @AfterEach
  internal fun deleteData() = runBlocking {
    repository.deleteAll()
  }

  @Nested
  @DisplayName("POST /mapping/casenotes")
  inner class CreateMapping {
    private lateinit var existingMapping: CaseNoteMapping
    private val mapping = CaseNoteMappingDto(
      dpsCaseNoteId = DPS_CASENOTE_ID,
      nomisCaseNoteId = NOMIS_CASENOTE_ID,
      offenderNo = OFFENDER_NO,
      nomisBookingId = 1,
      label = "2024-02-01T12:45:12",
      mappingType = CaseNoteMappingType.MIGRATED,
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        CaseNoteMapping(
          dpsCaseNoteId = UUID.fromString(DPS_CASENOTE_ID2),
          nomisCaseNoteId = 543210L,
          offenderNo = OFFENDER_NO,
          nomisBookingId = 1,
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

        val createdMapping = repository.findById(mapping.nomisCaseNoteId)!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisCaseNoteId).isEqualTo(mapping.nomisCaseNoteId)
        assertThat(createdMapping.dpsCaseNoteId.toString()).isEqualTo(mapping.dpsCaseNoteId)
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
              """
                {
                  "nomisCaseNoteId": 54321,
                  "dpsCaseNoteId": "$DPS_CASENOTE_ID",
                  "offenderNo": "A1234AA",
                  "nomisBookingId": 1
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        val createdMapping = repository.findById(54321)!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisCaseNoteId).isEqualTo(54321L)
        assertThat(createdMapping.dpsCaseNoteId).isEqualTo(UUID.fromString(DPS_CASENOTE_ID))
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
              """
                {
                  "nomisCaseNoteId": 54555,
                  "dpsCaseNoteId": "$DPS_CASENOTE_ID",
                  "offenderNo": "A1234AA",
                  "nomisBookingId": 1
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
          .uri("/mapping/casenotes/dps-casenote-id/$DPS_CASENOTE_ID")
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
              """
                {
                  "nomisCaseNoteId": 54321,
                  "dpsCaseNoteId": "$DPS_CASENOTE_ID",
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
              """
                {
                  "dpsCaseNoteId": "$DPS_CASENOTE_ID",
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
                offenderNo = OFFENDER_NO,
                nomisBookingId = 1,
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
            .containsEntry("dpsCaseNoteId", existingMapping.dpsCaseNoteId.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCaseNoteId", existingMapping.nomisCaseNoteId.toInt())
            .containsEntry("dpsCaseNoteId", dpsCaseNoteId)
        }
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/casenotes/batch")
  inner class CreateMappings {
    private var existingMapping: CaseNoteMapping = CaseNoteMapping(
      dpsCaseNoteId = generateUUID(1),
      nomisCaseNoteId = 50001L,
      offenderNo = OFFENDER_NO,
      nomisBookingId = 1,
      label = "2023-01-01T12:45:12",
      mappingType = CaseNoteMappingType.MIGRATED,
    )
    private val mappings = listOf(
      CaseNoteMappingDto(
        dpsCaseNoteId = "e52d7268-6e10-41a8-a0b9-000000000002",
        nomisCaseNoteId = 50002L,
        offenderNo = OFFENDER_NO,
        nomisBookingId = 2,
        mappingType = CaseNoteMappingType.DPS_CREATED,
      ),
      CaseNoteMappingDto(
        dpsCaseNoteId = "fd4e55a8-0805-439b-9e27-000000000003",
        nomisCaseNoteId = 50003L,
        offenderNo = OFFENDER_NO,
        nomisBookingId = 3,
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
          repository.findById(mappings[0].nomisCaseNoteId)!!

        assertThat(createdMapping1.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping1.nomisCaseNoteId).isEqualTo(mappings[0].nomisCaseNoteId)
        assertThat(createdMapping1.dpsCaseNoteId.toString()).isEqualTo(mappings[0].dpsCaseNoteId)
        assertThat(createdMapping1.nomisBookingId).isEqualTo(mappings[0].nomisBookingId)
        assertThat(createdMapping1.offenderNo).isEqualTo(mappings[0].offenderNo)
        assertThat(createdMapping1.mappingType).isEqualTo(mappings[0].mappingType)
        assertThat(createdMapping1.label).isEqualTo(mappings[0].label)

        val createdMapping2 = repository.findById(mappings[1].nomisCaseNoteId)!!

        assertThat(createdMapping2.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping2.nomisCaseNoteId).isEqualTo(mappings[1].nomisCaseNoteId)
        assertThat(createdMapping2.dpsCaseNoteId.toString()).isEqualTo(mappings[1].dpsCaseNoteId)
        assertThat(createdMapping2.nomisBookingId).isEqualTo(mappings[1].nomisBookingId)
        assertThat(createdMapping2.offenderNo).isEqualTo(mappings[1].offenderNo)
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
              """
                [
                    {
                      "nomisCaseNoteId": 54321,
                      "dpsCaseNoteId": "$DPS_CASENOTE_ID",
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
              mappings + existingMapping.copy(dpsCaseNoteId = UUID.fromString(dpsCaseNoteId)),
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
            .containsEntry("dpsCaseNoteId", existingMapping.dpsCaseNoteId.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCaseNoteId", existingMapping.nomisCaseNoteId.toInt())
            .containsEntry("dpsCaseNoteId", dpsCaseNoteId)
        }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/casenotes/{offenderNo}/all")
  inner class GetMappingByPrisoner {
    val dpsId1 = "85665bb9-ab28-458a-8386-b8cc91b311f7"
    val dpsId2 = DPS_CASENOTE_ID2
    val dpsId3 = "e52d7268-6e10-41a8-a0b9-2319b32520d6"
    val dpsId4 = "fd4e55a8-0805-439b-9e27-647583b96e4e"

    private var mapping1: CaseNoteMapping = CaseNoteMapping(
      dpsCaseNoteId = UUID.fromString(dpsId2),
      nomisBookingId = 54321L,
      nomisCaseNoteId = 2L,
      offenderNo = "A1234KT",
      mappingType = CaseNoteMappingType.DPS_CREATED,
    )
    private var mapping2: CaseNoteMapping = CaseNoteMapping(
      dpsCaseNoteId = UUID.fromString(dpsId1),
      nomisBookingId = 11111L,
      nomisCaseNoteId = 1L,
      offenderNo = "A1234KT",
      mappingType = CaseNoteMappingType.DPS_CREATED,
    )
    private val prisonerMappings = PrisonerCaseNoteMappingsDto(
      label = "2023-01-01T12:45:12",
      mappingType = CaseNoteMappingType.MIGRATED,
      mappings = listOf(
        CaseNoteMappingIdDto(
          dpsCaseNoteId = dpsId3,
          nomisBookingId = 54321L,
          nomisCaseNoteId = 3L,
        ),
        CaseNoteMappingIdDto(
          dpsCaseNoteId = dpsId4,
          nomisBookingId = 54321L,
          nomisCaseNoteId = 4L,
        ),
      ),
    )

    @BeforeEach
    fun setUp() = runTest {
      webTestClient.post()
        .uri("/mapping/casenotes/A1234KT/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(prisonerMappings))
        .exchange()
        .expectStatus().isCreated
      mapping1 = repository.save(mapping1)
      mapping2 = repository.save(mapping2)
      repository.save(
        CaseNoteMapping(
          dpsCaseNoteId = UUID.fromString("fd4e55a8-41ba-42ea-b5c4-404b453ad99b"),
          nomisBookingId = 9999L,
          nomisCaseNoteId = 5L,
          offenderNo = "A1111KT",
          mappingType = CaseNoteMappingType.MIGRATED,
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
          .uri("/mapping/casenotes/A1234KT/all")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/casenotes/A1234KT/all")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/casenotes/A1234KT/all")
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
          .uri("/mapping/casenotes/A9999KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("mappings.size()").isEqualTo(0)
      }

      @Test
      fun `will return all mappings for prisoner`() {
        webTestClient.get()
          .uri("/mapping/casenotes/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("mappings.size()").isEqualTo(4)
          .jsonPath("mappings[0].dpsCaseNoteId").isEqualTo(dpsId1)
          .jsonPath("mappings[0].nomisCaseNoteId").isEqualTo(1)
          .jsonPath("mappings[0].nomisBookingId").isEqualTo(11111)
          .jsonPath("mappings[0].offenderNo").isEqualTo("A1234KT")
          .jsonPath("mappings[1].dpsCaseNoteId").isEqualTo(dpsId2)
          .jsonPath("mappings[1].nomisCaseNoteId").isEqualTo(2)
          .jsonPath("mappings[1].nomisBookingId").isEqualTo(54321L)
          .jsonPath("mappings[2].dpsCaseNoteId").isEqualTo(dpsId3)
          .jsonPath("mappings[2].nomisCaseNoteId").isEqualTo(3)
          .jsonPath("mappings[2].nomisBookingId").isEqualTo(54321L)
          .jsonPath("mappings[3].dpsCaseNoteId").isEqualTo(dpsId4)
          .jsonPath("mappings[3].nomisCaseNoteId").isEqualTo(4)
          .jsonPath("mappings[3].nomisBookingId").isEqualTo(54321L)
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
          dpsCaseNoteId = UUID.fromString(DPS_CASENOTE_ID2),
          nomisCaseNoteId = 54321L,
          offenderNo = OFFENDER_NO,
          nomisBookingId = 1,
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
          .jsonPath("dpsCaseNoteId").isEqualTo(mapping.dpsCaseNoteId.toString())
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
          dpsCaseNoteId = generateUUID(1),
          nomisCaseNoteId = 54321L,
          offenderNo = OFFENDER_NO,
          nomisBookingId = 1,
          label = "2023-01-01T12:45:12",
          mappingType = CaseNoteMappingType.MIGRATED,
        ),
      )
      mapping2 = repository.save(
        CaseNoteMapping(
          dpsCaseNoteId = generateUUID(2),
          nomisCaseNoteId = 54322L,
          offenderNo = OFFENDER_NO,
          nomisBookingId = 1,
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
          .jsonPath("$[0].dpsCaseNoteId").isEqualTo(mapping1.dpsCaseNoteId.toString())
          .jsonPath("$[0].mappingType").isEqualTo(mapping1.mappingType.name)
          .jsonPath("$[0].label").isEqualTo(mapping1.label!!)
          .jsonPath("$[0].whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
          .jsonPath("$[1].nomisCaseNoteId").isEqualTo(mapping2.nomisCaseNoteId)
          .jsonPath("$[1].dpsCaseNoteId").isEqualTo(mapping2.dpsCaseNoteId.toString())
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
          dpsCaseNoteId = UUID.fromString(DPS_CASENOTE_ID2),
          nomisCaseNoteId = 54321L,
          offenderNo = OFFENDER_NO,
          nomisBookingId = 1,
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
          .uri("/mapping/casenotes/dps-casenote-id/00001111-0000-0000-0000-000011112222")
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
          .jsonPath("dpsCaseNoteId").isEqualTo(mapping.dpsCaseNoteId.toString())
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/casenotes/dps-casenote-id/{dpsCaseNoteId}/all")
  inner class GetMappingsByDpsId {
    lateinit var mapping1: CaseNoteMapping
    lateinit var mapping2: CaseNoteMapping
    val commonDpsCaseNoteId = generateUUID(1)

    @BeforeEach
    fun setUp() {
      runTest {
        mapping1 = repository.save(
          CaseNoteMapping(
            dpsCaseNoteId = commonDpsCaseNoteId,
            nomisCaseNoteId = 54321L,
            offenderNo = OFFENDER_NO,
            nomisBookingId = 1,
            label = "2023-01-01T12:45:12",
            mappingType = CaseNoteMappingType.MIGRATED,
          ),
        )
        mapping2 = repository.save(
          CaseNoteMapping(
            dpsCaseNoteId = commonDpsCaseNoteId,
            nomisCaseNoteId = 54322L,
            offenderNo = OFFENDER_NO,
            nomisBookingId = 1,
            label = "2023-06-01T12:45:12",
            mappingType = CaseNoteMappingType.DPS_CREATED,
          ),
        )
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/${mapping1.dpsCaseNoteId}/all")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/${mapping1.dpsCaseNoteId}/all")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/${mapping1.dpsCaseNoteId}/all")
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
          .uri("/mapping/casenotes/dps-casenote-id/00001111-0000-0000-0000-000011112222/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/${mapping1.dpsCaseNoteId}/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$[0].dpsCaseNoteId").isEqualTo(commonDpsCaseNoteId.toString())
          .jsonPath("$[0].nomisCaseNoteId").isEqualTo(mapping1.nomisCaseNoteId)
          .jsonPath("$[0].offenderNo").isEqualTo(OFFENDER_NO)
          .jsonPath("$[0].nomisBookingId").isEqualTo(mapping1.nomisBookingId)
          .jsonPath("$[0].mappingType").isEqualTo(mapping1.mappingType.name)
          .jsonPath("$[0].label").isEqualTo(mapping1.label!!)
          .jsonPath("$[0].whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
          .jsonPath("$[1].dpsCaseNoteId").isEqualTo(commonDpsCaseNoteId.toString())
          .jsonPath("$[1].nomisCaseNoteId").isEqualTo(mapping2.nomisCaseNoteId)
          .jsonPath("$[1].offenderNo").isEqualTo(OFFENDER_NO)
          .jsonPath("$[1].nomisBookingId").isEqualTo(mapping2.nomisBookingId)
          .jsonPath("$[1].mappingType").isEqualTo(mapping2.mappingType.name)
          .jsonPath("$[1].label").isEqualTo(mapping2.label!!)
          .jsonPath("$[1].whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @DisplayName("GET /mapping/casenotes/migration-id/{migrationId}/count-by-prisoner")
  @Nested
  inner class GetMappingCountByMigrationIdTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/casenotes/migration-id/2022-01-01T00:00:00/count-by-prisoner")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/casenotes/migration-id/2022-01-01T00:00:00/count-by-prisoner")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get casenote mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/casenotes/migration-id/2022-01-01T00:00:00/count-by-prisoner")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get casenote mapping count success`() {
      (1L..5L).forEach {
        postCreateMappingRequest(
          it,
          generateUUIDs(it),
          OFFENDER_NO,
          1,
          label = "2022-01-01",
          mappingType = CaseNoteMappingType.MIGRATED,
        )
      }

      webTestClient.get().uri("/mapping/casenotes/migration-id/2022-01-01/count-by-prisoner")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").isEqualTo(2) // i.e. 5 rows divided by average-case-notes-per-prisoner
    }

    @Test
    @Disabled
    fun `can request a different page size`() {
      (1L..6L).forEach {
        postCreateMappingRequest(
          it,
          generateUUIDs(it),
          offenderNo = "A000${it}AA",
          label = "2022-01-01",
          mappingType = CaseNoteMappingType.MIGRATED,
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/casenotes/migration-id/2022-01-01/count-by-prisoner")
          .queryParam("size", "2")
          .queryParam("sort", "nomisCaseNoteId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(6)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(3)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    @Disabled
    fun `can request a different page`() {
      (1L..3L).forEach {
        postCreateMappingRequest(it, generateUUIDs(it), label = "2022-01-01", mappingType = CaseNoteMappingType.MIGRATED)
      }
      webTestClient.get().uri {
        it.path("/mapping/casenotes/migration-id/2022-01-01/count-by-prisoner")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .queryParam("sort", "nomisCaseNoteId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(1)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
    }
  }

  @DisplayName("GET /mapping/casenotes/migrated/latest")
  @Nested
  inner class GeMappingMigratedLatestTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/casenotes/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/casenotes/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/casenotes/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {
      webTestClient.post().uri("/mapping/casenotes")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CaseNoteMappingDto(
              nomisCaseNoteId = 10,
              dpsCaseNoteId = generateUUIDs(10),
              offenderNo = OFFENDER_NO,
              nomisBookingId = 1,
              label = "2022-01-01T00:00:00",
              mappingType = CaseNoteMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/casenotes")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CaseNoteMappingDto(
              nomisCaseNoteId = 20,
              dpsCaseNoteId = generateUUIDs(20),
              offenderNo = OFFENDER_NO,
              nomisBookingId = 1,
              label = "2022-01-02T00:00:00",
              mappingType = CaseNoteMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/casenotes")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CaseNoteMappingDto(
              nomisCaseNoteId = 1,
              dpsCaseNoteId = generateUUIDs(1),
              offenderNo = OFFENDER_NO,
              nomisBookingId = 2,
              label = "2022-01-02T10:00:00",
              mappingType = CaseNoteMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/casenotes")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CaseNoteMappingDto(
              nomisCaseNoteId = 99,
              dpsCaseNoteId = generateUUIDs(199),
              offenderNo = OFFENDER_NO,
              nomisBookingId = 1,
              label = "whatever",
              mappingType = CaseNoteMappingType.DPS_CREATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/casenotes/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
        .exchange()
        .expectStatus().isOk
        .expectBody(CaseNoteMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisCaseNoteId).isEqualTo(1)
      assertThat(mapping.dpsCaseNoteId).isEqualTo(generateUUIDs(1))
      assertThat(mapping.offenderNo).isEqualTo(OFFENDER_NO)
      assertThat(mapping.nomisBookingId).isEqualTo(2)
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo(CaseNoteMappingType.MIGRATED)
      assertThat(mapping.whenCreated)
        .isCloseTo(LocalDateTime.now(), Assertions.byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/casenotes")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CaseNoteMappingDto(
              nomisCaseNoteId = 77,
              dpsCaseNoteId = generateUUIDs(77),
              offenderNo = OFFENDER_NO,
              nomisBookingId = 1,
              label = "whatever",
              mappingType = CaseNoteMappingType.DPS_CREATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/casenotes/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
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
          dpsCaseNoteId = UUID.fromString(DPS_CASENOTE_ID2),
          nomisCaseNoteId = 54321L,
          offenderNo = OFFENDER_NO,
          nomisBookingId = 1,
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
          dpsCaseNoteId = UUID.fromString(DPS_CASENOTE_ID2),
          nomisCaseNoteId = 54321L,
          offenderNo = OFFENDER_NO,
          nomisBookingId = 1,
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
          .uri("/mapping/casenotes/dps-casenote-id/00001111-0000-0000-0000-000011112222")
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
  @DisplayName("PUT /merge/from/{oldOffenderNo}/to/{newOffenderNo}")
  inner class PrisonerMergeMappings {
    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/casenotes/merge/from/A1234AA/to/A1234BB")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/casenotes/merge/from/A1234AA/to/A1234BB")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/casenotes/merge/from/A1234AA/to/A1234BB")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      private val dps1 = "00000000-1111-2222-3333-000088880001"
      private val dps2 = "00000000-1111-2222-3333-000088880002"
      private val dps3 = "00000000-1111-2222-3333-000088880003"

      @BeforeEach
      fun setUp() {
        runTest {
          repository.save(
            CaseNoteMapping(
              dpsCaseNoteId = UUID.fromString(dps1),
              nomisCaseNoteId = 54321L,
              offenderNo = "A1234AA",
              nomisBookingId = 1,
              mappingType = CaseNoteMappingType.MIGRATED,
            ),
          )
          repository.save(
            CaseNoteMapping(
              dpsCaseNoteId = UUID.fromString(dps2),
              nomisCaseNoteId = 54322L,
              offenderNo = "A1234BB",
              nomisBookingId = 2,
              mappingType = CaseNoteMappingType.NOMIS_CREATED,
            ),
          )
          repository.save(
            CaseNoteMapping(
              dpsCaseNoteId = UUID.fromString(dps3),
              nomisCaseNoteId = 54323L,
              offenderNo = "A1234BB",
              nomisBookingId = 2,
              mappingType = CaseNoteMappingType.NOMIS_CREATED,
            ),
          )
        }
      }

      @Test
      fun `Merge success`() = runTest {
        webTestClient.put().uri("/mapping/casenotes/merge/from/A1234AA/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk

        // first record has changed
        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo("B5678BB")

        // second has not
        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo("A1234BB")
      }

      @Test
      fun `Nothing happens if not found`() = runTest {
        webTestClient.put().uri("/mapping/casenotes/merge/from/A9999AA/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCaseNoteId").isEqualTo(54321)
          .jsonPath("offenderNo").isEqualTo("A1234AA")
          .jsonPath("nomisBookingId").isEqualTo(1)

        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCaseNoteId").isEqualTo(54322)
          .jsonPath("offenderNo").isEqualTo("A1234BB")
          .jsonPath("nomisBookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCaseNoteId").isEqualTo(54323)
          .jsonPath("offenderNo").isEqualTo("A1234BB")
          .jsonPath("nomisBookingId").isEqualTo(2)
      }

      @Test
      fun `Merge success - multiple candidates`() = runTest {
        webTestClient.put().uri("/mapping/casenotes/merge/from/A1234BB/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCaseNoteId").isEqualTo(54321)
          .jsonPath("offenderNo").isEqualTo("A1234AA")
          .jsonPath("nomisBookingId").isEqualTo(1)

        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCaseNoteId").isEqualTo(54322)
          .jsonPath("offenderNo").isEqualTo("B5678BB")
          .jsonPath("nomisBookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCaseNoteId").isEqualTo(54323)
          .jsonPath("offenderNo").isEqualTo("B5678BB")
          .jsonPath("nomisBookingId").isEqualTo(2)
      }
    }
  }

  @Nested
  @DisplayName("PUT /merge/booking-id/{bookingId}/to/{newOffenderNo}")
  inner class PrisonerMergeMappingsBookingId {
    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/casenotes/merge/booking-id/333/to/A1234BB")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/casenotes/merge/booking-id/333/to/A1234BB")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/casenotes/merge/booking-id/333/to/A1234BB")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      private val dps1 = "00000000-1111-2222-3333-000088880001"
      private val dps2 = "00000000-1111-2222-3333-000088880002"
      private val dps3 = "00000000-1111-2222-3333-000088880003"

      @BeforeEach
      fun setUp() {
        runTest {
          repository.save(
            CaseNoteMapping(
              dpsCaseNoteId = UUID.fromString(dps1),
              nomisCaseNoteId = 54321L,
              offenderNo = "A1234AA",
              nomisBookingId = 1,
              mappingType = CaseNoteMappingType.MIGRATED,
            ),
          )
          repository.save(
            CaseNoteMapping(
              dpsCaseNoteId = UUID.fromString(dps2),
              nomisCaseNoteId = 54322L,
              offenderNo = "A1234BB",
              nomisBookingId = 2,
              mappingType = CaseNoteMappingType.NOMIS_CREATED,
            ),
          )
          repository.save(
            CaseNoteMapping(
              dpsCaseNoteId = UUID.fromString(dps3),
              nomisCaseNoteId = 54323L,
              offenderNo = "A1234BB",
              nomisBookingId = 2,
              mappingType = CaseNoteMappingType.NOMIS_CREATED,
            ),
          )
        }
      }

      @Test
      fun `Merge success`() = runTest {
        webTestClient.put().uri("/mapping/casenotes/merge/booking-id/1/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  { 
    "nomisCaseNoteId": 54321,
    "dpsCaseNoteId": "$dps1",
    "offenderNo": "B5678BB",
    "nomisBookingId": 1
  }
]""",
          )

        // Check first record has changed
        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo("B5678BB")

        // second has not
        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo("A1234BB")
      }

      @Test
      fun `Nothing happens if not found`() = runTest {
        webTestClient.put().uri("/mapping/casenotes/merge/booking-id/999/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("[]")

        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCaseNoteId").isEqualTo(54321)
          .jsonPath("offenderNo").isEqualTo("A1234AA")
          .jsonPath("nomisBookingId").isEqualTo(1)

        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCaseNoteId").isEqualTo(54322)
          .jsonPath("offenderNo").isEqualTo("A1234BB")
          .jsonPath("nomisBookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCaseNoteId").isEqualTo(54323)
          .jsonPath("offenderNo").isEqualTo("A1234BB")
          .jsonPath("nomisBookingId").isEqualTo(2)
      }

      @Test
      fun `Merge success - multiple candidates`() = runTest {
        webTestClient.put().uri("/mapping/casenotes/merge/booking-id/2/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  { 
    "nomisCaseNoteId": 54322,
    "dpsCaseNoteId": "$dps2",
    "offenderNo": "B5678BB",
    "nomisBookingId": 2
  },
  { 
    "nomisCaseNoteId": 54323,
    "dpsCaseNoteId": "$dps3",
    "offenderNo": "B5678BB",
    "nomisBookingId": 2
  }
]""",
          )

        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCaseNoteId").isEqualTo(54321)
          .jsonPath("offenderNo").isEqualTo("A1234AA")
          .jsonPath("nomisBookingId").isEqualTo(1)

        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCaseNoteId").isEqualTo(54322)
          .jsonPath("offenderNo").isEqualTo("B5678BB")
          .jsonPath("nomisBookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/casenotes/dps-casenote-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCaseNoteId").isEqualTo(54323)
          .jsonPath("offenderNo").isEqualTo("B5678BB")
          .jsonPath("nomisBookingId").isEqualTo(2)
      }
    }
  }
}
