package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingType.DPS_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingType.NOMIS_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.CSIPAttendeeMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.CSIPAttendeeMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.CSIPAttendeeMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.CSIPAttendeeMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.CSIPFactorMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.CSIPFactorMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.CSIPFactorMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.CSIPFactorMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.CSIPInterviewMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.CSIPInterviewMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.CSIPInterviewMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.CSIPInterviewMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.CSIPReviewMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.CSIPReviewMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.CSIPReviewMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.CSIPReviewMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val DPS_CSIP_ID = "edcd118c-41ba-42ea-b5c4-404b453ad58b"
private const val NOMIS_CSIP_ID = 1234L

class CSIPMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: CSIPMappingRepository

  @Autowired
  private lateinit var csipPlanRepository: CSIPPlanMappingRepository

  @Autowired
  private lateinit var csipInterviewRepository: CSIPInterviewMappingRepository

  @Autowired
  private lateinit var csipReviewRepository: CSIPReviewMappingRepository

  @Autowired
  private lateinit var csipAttendeeRepository: CSIPAttendeeMappingRepository

  @Autowired
  private lateinit var csipFactorRepository: CSIPFactorMappingRepository

  private fun createCSIPMapping(
    nomisCSIPId: Long = NOMIS_CSIP_ID,
    dpsCSIPId: String = DPS_CSIP_ID,
    label: String = "2022-01-01",
    mappingType: CSIPMappingType = NOMIS_CREATED,
  ): CSIPReportMappingDto = CSIPReportMappingDto(
    nomisCSIPReportId = nomisCSIPId,
    dpsCSIPReportId = dpsCSIPId,
    label = label,
    mappingType = mappingType,
  )

  private fun postCreateCSIPMappingRequest(
    nomisCSIPId: Long = NOMIS_CSIP_ID,
    dpsCSIPId: String = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
    label: String = "2022-01-01",
    mappingType: CSIPMappingType = NOMIS_CREATED,
  ) {
    webTestClient.post().uri("/mapping/csip")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createCSIPMapping(
            nomisCSIPId = nomisCSIPId,
            dpsCSIPId = dpsCSIPId,
            label = label,
            mappingType = mappingType,
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  @DisplayName("POST /mapping/csip")
  @Nested
  inner class CreateCSIPMapping {
    private lateinit var existingMapping: CSIPMapping

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        CSIPMapping(
          dpsCSIPId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisCSIPId = 54321L,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.post().uri("/mapping/csip")
          .body(BodyInserters.fromValue(createCSIPMapping()))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/mapping/csip")
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(createCSIPMapping()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `create forbidden with wrong role`() {
        webTestClient.post().uri("/mapping/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(createCSIPMapping()))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Test
    fun `returns 409 if nomis ids already exist`() {
      val dpsCSIPId = UUID.randomUUID().toString()
      val duplicateResponse = webTestClient.post()
        .uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CSIPReportMappingDto(
              nomisCSIPReportId = existingMapping.nomisCSIPId,
              dpsCSIPReportId = dpsCSIPId,
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
          .containsEntry("nomisCSIPReportId", existingMapping.nomisCSIPId.toInt())
          .containsEntry("dpsCSIPReportId", existingMapping.dpsCSIPId)
        assertThat(this.moreInfo.duplicate)
          .containsEntry("nomisCSIPReportId", existingMapping.nomisCSIPId.toInt())
          .containsEntry("dpsCSIPReportId", dpsCSIPId)
      }
    }

    @Test
    fun `returns 409 if dps id already exist`() {
      val nomisCSIPId = 90909L
      val duplicateResponse = webTestClient.post()
        .uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CSIPReportMappingDto(
              nomisCSIPReportId = nomisCSIPId,
              dpsCSIPReportId = existingMapping.dpsCSIPId,
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
          .containsEntry("nomisCSIPReportId", existingMapping.nomisCSIPId.toInt())
          .containsEntry("dpsCSIPReportId", existingMapping.dpsCSIPId)
        assertThat(this.moreInfo.duplicate)
          .containsEntry("nomisCSIPReportId", nomisCSIPId.toInt())
          .containsEntry("dpsCSIPReportId", existingMapping.dpsCSIPId)
      }
    }

    @Test
    fun `create mapping success`() {
      val nomisCSIPId = 67676L
      val dpsCSIPId = UUID.randomUUID().toString()
      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
              "nomisCSIPReportId" : $nomisCSIPId,
              "dpsCSIPReportId"   : "$dpsCSIPId",
              "label"             : "2022-01-01",
              "mappingType"       : "DPS_CREATED"
            }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping1 =
        webTestClient.get().uri("/mapping/csip/nomis-csip-id/$nomisCSIPId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody(CSIPReportMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping1.nomisCSIPReportId).isEqualTo(nomisCSIPId)
      assertThat(mapping1.dpsCSIPReportId).isEqualTo(dpsCSIPId)
      assertThat(mapping1.label).isEqualTo("2022-01-01")
      assertThat(mapping1.mappingType).isEqualTo(DPS_CREATED)

      val mapping2 = webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody(CSIPReportMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.nomisCSIPReportId).isEqualTo(nomisCSIPId)
      assertThat(mapping2.dpsCSIPReportId).isEqualTo(dpsCSIPId)
      assertThat(mapping2.label).isEqualTo("2022-01-01")
      assertThat(mapping2.mappingType).isEqualTo(DPS_CREATED)
    }

    @Test
    fun `create mapping - if dps and nomis id already exist`() = runTest {
      val nomisCSIPId = 35353L
      val dpsCSIPId = UUID.randomUUID().toString()
      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
              "nomisCSIPReportId" : $nomisCSIPId,
              "dpsCSIPReportId"   : "$dpsCSIPId",
              "label"             : "2022-01-01",
              "mappingType"       : "DPS_CREATED"
           }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val responseBody =
        webTestClient.post().uri("/mapping/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
                "nomisCSIPReportId" : $nomisCSIPId,
                "dpsCSIPReportId"   : "$dpsCSIPId",
                "label"             : "2022-01-01",
                "mappingType"       : "DPS_CREATED"
              }""",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<CSIPReportMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: CSIP mapping already exists")
        assertThat(errorCode).isEqualTo(1409)
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will default to DPS_CREATED if missing mapping type`() {
        webTestClient.post().uri("/mapping/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """{
                "nomisCSIPReportId" : "55665",
                "label"             : "2022-01-01",
                "dpsCSIPReportId"   : "3ecd118c-41ba-42ea-b5c4-404b453ad123"
              }""",
            ),
          )
          .exchange()
          .expectStatus().isCreated
          .expectBody()
          .jsonPath("mappingType").isEqualTo("DPS_CREATED")
      }

      @Test
      fun `returns 400 if mapping type invalid`() {
        assertThat(
          webTestClient.post().uri("/mapping/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                //language=JSON
                """{
                "nomisCSIPReportId" : $NOMIS_CSIP_ID,
                "label"             : "2022-01-01",
                "dpsCSIPReportId"   : "$DPS_CSIP_ID",
                "mappingType"       : "INVALID_MAPPING_TYPE"
              }""",
              ),
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ErrorResponse::class.java)
            .returnResult().responseBody?.userMessage,
        ).contains("DPS_CREATED, MIGRATED, NOMIS_CREATED")
      }

      @Test
      fun `returns 400 when Nomis CSIP Report Id is missing`() {
        assertThat(
          webTestClient.post().uri("/mapping/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                //language=JSON
                """{
                "dpsCSIPReportId" : "$DPS_CSIP_ID",
                "label"           : "2022-01-01",
                "mappingType"     : "DPS_CREATED"
              }""",
              ),
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ErrorResponse::class.java)
            .returnResult().responseBody?.userMessage,
        )
          .contains("Validation failure: JSON decoding error")
          .contains("nomisCSIPReportId")
      }

      @Test
      fun `returns 400 when DPS CSIP Report Id is missing`() {
        assertThat(
          webTestClient.post().uri("/mapping/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                """{
                "nomisCSIPReportId" : $NOMIS_CSIP_ID,
                "label"             : "2022-01-01",
                "mappingType"       : "DPS_CREATED"
              }""",
              ),
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ErrorResponse::class.java)
            .returnResult().responseBody?.userMessage,
        )
          .contains("Validation failure: JSON decoding error")
          .contains("dpsCSIPReportId")
      }
    }
  }

  @DisplayName("GET /mapping/csip/nomis-csip-id/{nomisCSIPId}")
  @Nested
  inner class GetNomisMapping {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/csip/nomis-csip-id/$NOMIS_CSIP_ID")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/csip/nomis-csip-id/$NOMIS_CSIP_ID")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/csip/nomis-csip-id/$NOMIS_CSIP_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createCSIPMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping =
        webTestClient.get().uri("/mapping/csip/nomis-csip-id/$NOMIS_CSIP_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody(CSIPReportMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping.nomisCSIPReportId).isEqualTo(NOMIS_CSIP_ID)
      assertThat(mapping.dpsCSIPReportId).isEqualTo(DPS_CSIP_ID)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/csip/nomis-csip-id/99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No CSIP Report mapping found for nomisCSIPReportId=99999")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createCSIPMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/csip/nomis-csip-id/$NOMIS_CSIP_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /mapping/csip/dps-csip-id/{dpsCSIPId}")
  @Nested
  inner class GetCSIPMapping {

    private val nomisCSIPId = 4422L
    private val dpsCSIPId = UUID.randomUUID().toString()

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPId")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPId")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPId")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createCSIPMapping(dpsCSIPId = dpsCSIPId, nomisCSIPId = nomisCSIPId)))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody(CSIPReportMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisCSIPReportId).isEqualTo(nomisCSIPId)
      assertThat(mapping.dpsCSIPReportId).isEqualTo(dpsCSIPId)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/csip/dps-csip-id/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No CSIP Report mapping found for dpsCSIPReportId=9999")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createCSIPMapping(nomisCSIPId = nomisCSIPId, dpsCSIPId = dpsCSIPId)))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /mapping/csip/dps-csip-id/{csipId}/all")
  @Nested
  inner class GetAllMappingsForDPSId {
    private val nomisCSIPReportId = 4422L
    private val dpsCSIPReportId = UUID.randomUUID().toString()

    @AfterEach
    internal fun deleteData() = runBlocking {
      csipAttendeeRepository.deleteAll()
      csipFactorRepository.deleteAll()
      csipInterviewRepository.deleteAll()
      csipPlanRepository.deleteAll()
      csipReviewRepository.deleteAll()
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPReportId/all")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPReportId/all")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPReportId/all")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success - no children`() {
      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createCSIPMapping(dpsCSIPId = dpsCSIPReportId, nomisCSIPId = nomisCSIPReportId)))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPReportId/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody(CSIPFullMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisCSIPReportId).isEqualTo(nomisCSIPReportId)
      assertThat(mapping.dpsCSIPReportId).isEqualTo(dpsCSIPReportId)
      assertThat(mapping.attendeeMappings).isEqualTo(emptyList<CSIPAttendeeMappingDto>())
      assertThat(mapping.factorMappings).isEqualTo(emptyList<CSIPFactorMappingDto>())
      assertThat(mapping.interviewMappings).isEqualTo(emptyList<CSIPInterviewMappingDto>())
      assertThat(mapping.planMappings).isEqualTo(emptyList<CSIPPlanMappingDto>())
      assertThat(mapping.reviewMappings).isEqualTo(emptyList<CSIPReviewMappingDto>())
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createCSIPMapping(dpsCSIPId = dpsCSIPReportId, nomisCSIPId = nomisCSIPReportId)))
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/csip/attendees")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CSIPAttendeeMappingDto(
              nomisCSIPAttendeeId = 343,
              dpsCSIPAttendeeId = UUID.randomUUID().toString(),
              dpsCSIPReportId = dpsCSIPReportId,
              label = "test",
              mappingType = CSIPAttendeeMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/csip/factors")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CSIPFactorMappingDto(
              nomisCSIPFactorId = 454,
              dpsCSIPFactorId = UUID.randomUUID().toString(),
              dpsCSIPReportId = dpsCSIPReportId,
              label = "test",
              mappingType = CSIPFactorMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/csip/interviews")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CSIPInterviewMappingDto(
              nomisCSIPInterviewId = 565,
              dpsCSIPInterviewId = UUID.randomUUID().toString(),
              dpsCSIPReportId = dpsCSIPReportId,
              label = "test",
              mappingType = CSIPInterviewMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/csip/plans")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CSIPPlanMappingDto(
              nomisCSIPPlanId = 676,
              dpsCSIPPlanId = UUID.randomUUID().toString(),
              dpsCSIPReportId = dpsCSIPReportId,
              label = "test",
              mappingType = CSIPPlanMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/csip/reviews")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CSIPReviewMappingDto(
              nomisCSIPReviewId = 787,
              dpsCSIPReviewId = UUID.randomUUID().toString(),
              dpsCSIPReportId = dpsCSIPReportId,
              label = "test",
              mappingType = CSIPReviewMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPReportId/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody(CSIPFullMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisCSIPReportId).isEqualTo(nomisCSIPReportId)
      assertThat(mapping.dpsCSIPReportId).isEqualTo(dpsCSIPReportId)
      assertThat(mapping.attendeeMappings).size().isEqualTo(1)
      assertThat(mapping.factorMappings).size().isEqualTo(1)
      assertThat(mapping.interviewMappings).size().isEqualTo(1)
      assertThat(mapping.planMappings).size().isEqualTo(1)
      assertThat(mapping.reviewMappings).size().isEqualTo(1)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/csip/dps-csip-id/9999/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No CSIP Report mapping found for dpsCSIPReportId=9999")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createCSIPMapping(nomisCSIPId = nomisCSIPReportId, dpsCSIPId = dpsCSIPReportId)))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPReportId/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("DELETE /mapping/csip/dps-csip-id/{dpsCSIPId}/all")
  @Nested
  inner class DeleteMapping {
    lateinit var mapping: CSIPMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CSIPMapping(
          dpsCSIPId = UUID.randomUUID().toString(),
          nomisCSIPId = 22334L,
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
      fun `access forbidden when no authority`() {
        webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}/all")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}/all")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}/all")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Test
    fun `delete specific mapping success`() {
      // it is present after creation by csip id
      webTestClient.get().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
      // it is also present after creation by nomis id
      webTestClient.get().uri("/mapping/csip/nomis-csip-id/${mapping.nomisCSIPId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk

      // delete mapping
      webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNoContent

      // no longer present by dps csip id
      webTestClient.get().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNotFound
      // and also no longer present by nomis csip id
      webTestClient.get().uri("/mapping/csip/nomis-csip-id/${mapping.nomisCSIPId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    internal fun `delete is idempotent`() {
      // delete mapping
      webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNoContent
      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @DisplayName("DELETE /mapping/csip/dps-csip-id/{dpsCSIPId}/all with children set")
  @Nested
  inner class DeleteChildMappings {
    lateinit var mapping: CSIPMapping
    private var dpsCsipPlanId = "0"
    private var dpsCsipPlanId2 = "0"
    private var dpsCsipInterviewId = "0"
    private var dpsCsipInterviewId2 = "0"
    private var dpsCsipReviewId = "0"
    private var dpsCsipReviewId2 = "0"
    private var dpsCsipAttendeeId = "0"
    private var dpsCsipAttendeeId2 = "0"
    private var dpsCsipFactorId = "0"
    private var dpsCsipFactorId2 = "0"

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CSIPMapping(
          dpsCSIPId = UUID.randomUUID().toString(),
          nomisCSIPId = 22334L,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )

      dpsCsipPlanId = csipPlanRepository.save(
        CSIPPlanMapping(
          dpsCSIPPlanId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPPlanId = 12345L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPPlanMappingType.MIGRATED,
        ),
      ).dpsCSIPPlanId

      dpsCsipPlanId2 = csipPlanRepository.save(
        CSIPPlanMapping(
          dpsCSIPPlanId = "c5e56441-04c9-40e1-bd37-553ec1abcdaa",
          nomisCSIPPlanId = 12346L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPPlanMappingType.DPS_CREATED,
        ),
      ).dpsCSIPPlanId

      dpsCsipInterviewId = csipInterviewRepository.save(
        CSIPInterviewMapping(
          dpsCSIPInterviewId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPInterviewId = 12345L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPInterviewMappingType.MIGRATED,
        ),
      ).dpsCSIPInterviewId

      dpsCsipInterviewId2 = csipInterviewRepository.save(
        CSIPInterviewMapping(
          dpsCSIPInterviewId = "c5e56441-04c9-40e1-bd37-553ec1abcdaa",
          nomisCSIPInterviewId = 12346L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPInterviewMappingType.DPS_CREATED,
        ),
      ).dpsCSIPInterviewId

      dpsCsipReviewId = csipReviewRepository.save(
        CSIPReviewMapping(
          dpsCSIPReviewId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPReviewId = 12345L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPReviewMappingType.MIGRATED,
        ),
      ).dpsCSIPReviewId

      dpsCsipReviewId2 = csipReviewRepository.save(
        CSIPReviewMapping(
          dpsCSIPReviewId = "c5e56441-04c9-40e1-bd37-553ec1abcdaa",
          nomisCSIPReviewId = 12346L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPReviewMappingType.DPS_CREATED,
        ),
      ).dpsCSIPReviewId

      dpsCsipAttendeeId = csipAttendeeRepository.save(
        CSIPAttendeeMapping(
          dpsCSIPAttendeeId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPAttendeeId = 12345L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPAttendeeMappingType.MIGRATED,
        ),
      ).dpsCSIPAttendeeId

      dpsCsipAttendeeId2 = csipAttendeeRepository.save(
        CSIPAttendeeMapping(
          dpsCSIPAttendeeId = "c5e56441-04c9-40e1-bd37-553ec1abcdaa",
          nomisCSIPAttendeeId = 12346L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPAttendeeMappingType.DPS_CREATED,
        ),
      ).dpsCSIPAttendeeId

      dpsCsipFactorId = csipFactorRepository.save(
        CSIPFactorMapping(
          dpsCSIPFactorId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPFactorId = 12345L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPFactorMappingType.MIGRATED,
        ),
      ).dpsCSIPFactorId

      dpsCsipFactorId2 = csipFactorRepository.save(
        CSIPFactorMapping(
          dpsCSIPFactorId = "c5e56441-04c9-40e1-bd37-553ec1abcdaa",
          nomisCSIPFactorId = 12346L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPFactorMappingType.DPS_CREATED,
        ),
      ).dpsCSIPFactorId
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
    }

    @Test
    internal fun `delete removes child mappings`() = runTest {
      checkChildStatusValues(HttpStatus.OK)

      // delete report mapping
      webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNoContent

      checkChildStatusValues(HttpStatus.NOT_FOUND)
    }

    @Test
    internal fun `delete only migrated removes all child mappings`() {
      checkChildStatusValues(HttpStatus.OK)

      // delete only migrated mappings
      webTestClient.delete().uri("/mapping/csip/all?onlyMigrated")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNoContent

      checkChildStatusValues(HttpStatus.NOT_FOUND)
    }

    @Test
    internal fun `delete all mappings removes child mappings`() {
      checkChildStatusValues(HttpStatus.OK)

      // delete all mappings
      webTestClient.delete().uri("/mapping/csip/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNoContent

      checkChildStatusValues(HttpStatus.NOT_FOUND)
    }

    private fun checkChildStatusValues(status: HttpStatus) {
      webTestClient.get()
        .uri("/mapping/csip/plans/dps-csip-plan-id/$dpsCsipPlanId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange().expectStatus().isEqualTo(status.value())
      webTestClient.get()
        .uri("/mapping/csip/plans/dps-csip-plan-id/$dpsCsipPlanId2")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange().expectStatus().isEqualTo(status.value())
      webTestClient.get()
        .uri("/mapping/csip/interviews/dps-csip-interview-id/$dpsCsipInterviewId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange().expectStatus().isEqualTo(status.value())
      webTestClient.get()
        .uri("/mapping/csip/interviews/dps-csip-interview-id/$dpsCsipInterviewId2")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange().expectStatus().isEqualTo(status.value())
      webTestClient.get()
        .uri("/mapping/csip/reviews/dps-csip-review-id/$dpsCsipReviewId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange().expectStatus().isEqualTo(status.value())
      webTestClient.get()
        .uri("/mapping/csip/reviews/dps-csip-review-id/$dpsCsipReviewId2")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange().expectStatus().isEqualTo(status.value())
      webTestClient.get()
        .uri("/mapping/csip/attendees/dps-csip-attendee-id/$dpsCsipAttendeeId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange().expectStatus().isEqualTo(status.value())
      webTestClient.get()
        .uri("/mapping/csip/attendees/dps-csip-attendee-id/$dpsCsipAttendeeId2")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange().expectStatus().isEqualTo(status.value())
      webTestClient.get()
        .uri("/mapping/csip/factors/dps-csip-factor-id/$dpsCsipFactorId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange().expectStatus().isEqualTo(status.value())
      webTestClient.get()
        .uri("/mapping/csip/factors/dps-csip-factor-id/$dpsCsipFactorId2")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange().expectStatus().isEqualTo(status.value())
    }
  }

  @DisplayName("DELETE /mapping/csip/all")
  @Nested
  inner class DeleteAllMappings {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/csip/all")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/csip/all")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/csip/all")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete mapping success`() {
      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createCSIPMapping(nomisCSIPId = 1111L, dpsCSIPId = UUID.randomUUID().toString())))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get()
        .uri("/mapping/csip/nomis-csip-id/1111")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/mapping/csip/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get()
        .uri("/mapping/csip/nomis-csip-id/1111")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `delete csip mappings - migrated mappings only`() {
      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createCSIPMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createCSIPMapping(
              nomisCSIPId = 2345,
              dpsCSIPId = "5432",
              mappingType = MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/csip/dps-csip-id/5432")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/mapping/csip/all?onlyMigrated=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get()
        .uri("/mapping/csip/nomis-csip-id/$NOMIS_CSIP_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk

      webTestClient.get().uri("/mapping/csip/dps-csip-id/5432")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @DisplayName("GET /mapping/csip/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationIdTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/migration-id/2022-01-01T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/csip/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get csip mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/csip/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get csip mappings by migration id success`() {
      (1L..4L).forEach {
        postCreateCSIPMappingRequest(
          nomisCSIPId = it,
          dpsCSIPId = "$it",
          label = "2022-01-01",
          mappingType = MIGRATED,
        )
      }
      (5L..9L).forEach {
        postCreateCSIPMappingRequest(
          nomisCSIPId = it,
          dpsCSIPId = "$it",
          label = "2099-01-01",
          mappingType = MIGRATED,
        )
      }
      postCreateCSIPMappingRequest(
        nomisCSIPId = 12,
        dpsCSIPId = "12",
        mappingType = DPS_CREATED,
      )

      webTestClient.get().uri("/mapping/csip/migration-id/2022-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..dpsCSIPReportId").value(Matchers.contains("1", "2", "3", "4"))
        .jsonPath("$.content..nomisCSIPReportId").value(Matchers.contains(1, 2, 3, 4))
        .jsonPath("$.content[0].whenCreated").isNotEmpty
    }

    @Test
    fun `get csip mappings by migration id - no records exist`() {
      (1L..4L).forEach {
        postCreateCSIPMappingRequest(
          nomisCSIPId = it,
          dpsCSIPId = "$it",
          label = "2022-01-01",
          mappingType = MIGRATED,
        )
      }

      webTestClient.get().uri("/mapping/csip/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
        .jsonPath("content").isEmpty
    }

    @Test
    fun `can request a different page size`() {
      (1L..6L).forEach {
        postCreateCSIPMappingRequest(
          nomisCSIPId = it,
          dpsCSIPId = "$it",
          label = "2022-01-01",
          mappingType = MIGRATED,
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/csip/migration-id/2022-01-01")
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

    @Test
    fun `can request a different page`() {
      (1L..3L).forEach {
        postCreateCSIPMappingRequest(
          nomisCSIPId = it,
          dpsCSIPId = "$it",
          label = "2022-01-01",
          mappingType = MIGRATED,
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/csip/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .queryParam("sort", "nomisCSIPId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
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

  @DisplayName("GET /mapping/csip/migrated/latest")
  @Nested
  inner class GetMappingMigratedLatestTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/csip/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/csip/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/csip/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {
      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createCSIPMapping(
              nomisCSIPId = 10,
              dpsCSIPId = "10",
              label = "2022-01-01T00:00:00",
              mappingType = MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createCSIPMapping(
              nomisCSIPId = 20,
              dpsCSIPId = "4",
              label = "2022-01-02T00:00:00",
              mappingType = MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createCSIPMapping(
              nomisCSIPId = 1,
              dpsCSIPId = "1",
              label = "2022-01-02T10:00:00",
              mappingType = MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createCSIPMapping(
              nomisCSIPId = 99,
              dpsCSIPId = "3",
              label = "whatever",
              mappingType = NOMIS_CREATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/csip/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody(CSIPReportMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisCSIPReportId).isEqualTo(1)
      assertThat(mapping.dpsCSIPReportId).isEqualTo("1")
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo(MIGRATED)
      assertThat(mapping.whenCreated).isCloseTo(LocalDateTime.now(), Assertions.byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createCSIPMapping(
              nomisCSIPId = 77,
              dpsCSIPId = "7",
              label = "whatever",
              mappingType = NOMIS_CREATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/csip/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }
}
