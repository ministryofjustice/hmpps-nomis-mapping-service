package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingType.DPS_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingType.NOMIS_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.CSIPAttendeeMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.CSIPAttendeeMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.CSIPFactorMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.CSIPFactorMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.CSIPInterviewMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.CSIPInterviewMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.CSIPReviewMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.CSIPReviewMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

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
    nomisCSIPId: Long = 1234,
    dpsCSIPId: String = "dcd118c-41ba-42ea-b5c4-404b453ad58b",
    label: String = "2022-01-01",
    mappingType: CSIPMappingType = NOMIS_CREATED,
  ): CSIPReportMappingDto = CSIPReportMappingDto(
    nomisCSIPReportId = nomisCSIPId,
    dpsCSIPReportId = dpsCSIPId,
    label = label,
    mappingType = mappingType,
  )

  private fun createFullCSIPMapping(
    nomisCSIPId: Long = 1234,
    dpsCSIPId: String = "dcd118c-41ba-42ea-b5c4-404b453ad58b",
    label: String = "2022-01-01",
    mappingType: CSIPMappingType = NOMIS_CREATED,
  ): CSIPFullMappingDto = CSIPFullMappingDto(
    nomisCSIPReportId = nomisCSIPId,
    dpsCSIPReportId = dpsCSIPId,
    label = label,
    mappingType = mappingType,
  )

  private fun postCreateCSIPMappingRequest(
    nomisCSIPId: Long = 1234,
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
                "nomisCSIPReportId" : 1234,
                "label"             : "2022-01-01",
                "dpsCSIPReportId"   : "dcd118c-41ba-42ea-b5c4-404b453ad58b",
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
                "dpsCSIPReportId" : "dcd118c-41ba-42ea-b5c4-404b453ad58b",
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
                "nomisCSIPReportId" : 1234,
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

  @DisplayName("POST /mapping/csip/all")
  @Nested
  inner class CreateFullCSIPMapping {
    private val dpsCSIPId = UUID.randomUUID().toString()
    private lateinit var mapping: CSIPMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CSIPMapping(
          dpsCSIPId = dpsCSIPId,
          nomisCSIPId = 22334L,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    internal fun deleteData() = runBlocking {
      csipAttendeeRepository.deleteAll()
      csipFactorRepository.deleteAll()
      csipInterviewRepository.deleteAll()
      csipPlanRepository.deleteAll()
      csipReviewRepository.deleteAll()
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.post().uri("/mapping/csip/all")
          .body(BodyInserters.fromValue(createFullCSIPMapping()))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/mapping/csip/all")
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(createFullCSIPMapping()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `create forbidden with wrong role`() {
        webTestClient.post().uri("/mapping/csip/all")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(createFullCSIPMapping()))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Test
    fun `returns 409 if nomis ids already exist`() {
      val dpsCSIPId = UUID.randomUUID().toString()
      val duplicateResponse = webTestClient.post()
        .uri("/mapping/csip/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CSIPFullMappingDto(
              nomisCSIPReportId = mapping.nomisCSIPId,
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
          .containsEntry("nomisCSIPReportId", mapping.nomisCSIPId.toInt())
          .containsEntry("dpsCSIPReportId", mapping.dpsCSIPId)
        assertThat(this.moreInfo.duplicate)
          .containsEntry("nomisCSIPReportId", mapping.nomisCSIPId.toInt())
          .containsEntry("dpsCSIPReportId", dpsCSIPId)
      }
    }

    @Test
    fun `returns 409 if dps id already exist`() {
      val nomisCSIPId = 90909L
      val duplicateResponse = webTestClient.post()
        .uri("/mapping/csip/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CSIPFullMappingDto(
              nomisCSIPReportId = nomisCSIPId,
              dpsCSIPReportId = mapping.dpsCSIPId,
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
          .containsEntry("nomisCSIPReportId", mapping.nomisCSIPId.toInt())
          .containsEntry("dpsCSIPReportId", mapping.dpsCSIPId)
        assertThat(this.moreInfo.duplicate)
          .containsEntry("nomisCSIPReportId", nomisCSIPId.toInt())
          .containsEntry("dpsCSIPReportId", mapping.dpsCSIPId)
      }
    }

    @Test
    fun `create mapping Happy Path`() {
      val nomisCSIPId = 67676L
      val dpsCSIPId = UUID.randomUUID().toString()

      val attendeeMappingDto = CSIPChildMappingDto(
        dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
        nomisId = 12345L,
        dpsCSIPReportId = dpsCSIPId,
        label = "2023-01-01T12:45:12",
        mappingType = CSIPChildMappingType.MIGRATED,
      )

      val factorMappingDto =
        CSIPChildMappingDto(
          dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisId = 12345L,
          dpsCSIPReportId = dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.MIGRATED,
        )

      val planMappingDto = CSIPChildMappingDto(
        dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
        nomisId = 12345L,
        dpsCSIPReportId = dpsCSIPId,
        label = "2023-01-01T12:45:12",
        mappingType = CSIPChildMappingType.MIGRATED,
      )

      val planMappingDto2 = CSIPChildMappingDto(
        dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcdaa",
        nomisId = 12346L,
        dpsCSIPReportId = dpsCSIPId,
        label = "2023-01-01T12:45:12",
        mappingType = CSIPChildMappingType.DPS_CREATED,
      )

      val interviewMappingDto = CSIPChildMappingDto(
        dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
        nomisId = 12345L,
        dpsCSIPReportId = dpsCSIPId,
        label = "2023-01-01T12:45:12",
        mappingType = CSIPChildMappingType.MIGRATED,
      )

      val reviewMappingDto = CSIPChildMappingDto(
        dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
        nomisId = 12345L,
        dpsCSIPReportId = dpsCSIPId,
        label = "2023-01-01T12:45:12",
        mappingType = CSIPChildMappingType.MIGRATED,
      )

      webTestClient.post().uri("/mapping/csip/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CSIPFullMappingDto(
              nomisCSIPReportId = nomisCSIPId,
              dpsCSIPReportId = dpsCSIPId,
              label = "2022-01-01",
              mappingType = DPS_CREATED,
              attendeeMappings = listOf(attendeeMappingDto),
              factorMappings = listOf(factorMappingDto),
              interviewMappings = listOf(interviewMappingDto),
              planMappings = listOf(planMappingDto, planMappingDto2),
              reviewMappings = listOf(reviewMappingDto),
            ),
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

      val mapping = webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPId/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody(CSIPFullMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisCSIPReportId).isEqualTo(nomisCSIPId)
      assertThat(mapping.dpsCSIPReportId).isEqualTo(dpsCSIPId)
      assertThat(mapping.attendeeMappings).size().isEqualTo(1)
      assertThat(mapping.factorMappings).size().isEqualTo(1)
      assertThat(mapping.interviewMappings).size().isEqualTo(1)
      assertThat(mapping.planMappings).size().isEqualTo(2)
      assertThat(mapping.reviewMappings).size().isEqualTo(1)
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
        webTestClient.post().uri("/mapping/csip/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(

            BodyInserters.fromValue(
              CSIPFullMappingDto(
                nomisCSIPReportId = nomisCSIPId,
                dpsCSIPReportId = dpsCSIPId,
                label = "2022-01-01",
                mappingType = DPS_CREATED,
              ),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<CSIPFullMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: CSIP mapping already exists")
        assertThat(errorCode).isEqualTo(1409)
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 if mapping type invalid`() {
        assertThat(
          webTestClient.post().uri("/mapping/csip/all")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                //language=JSON
                """{
                  "nomisCSIPReportId" : 1234,
                  "label"             : "2022-01-01",
                  "dpsCSIPReportId"   : "dcd118c-41ba-42ea-b5c4-404b453ad58b",
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
          webTestClient.post().uri("/mapping/csip/all")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                //language=JSON
                """{
                  "label"             : "2022-01-01",
                  "dpsCSIPReportId"   : "dcd118c-41ba-42ea-b5c4-404b453ad58b",
                  "mappingType"       : "DPS_CREATED",
                  "attendeeMappings"  : [],
                  "factorMappings"    : [],
                  "interviewMappings" : [],
                  "planMappings"      : [],
                  "reviewMappings"    : []
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
      fun `is success when child mapping lists are missing`() {
        assertThat(
          webTestClient.post().uri("/mapping/csip/all")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                //language=JSON
                """{
                  "label"             : "2022-01-01",
                  "nomisCSIPReportId" : 1234,
                  "dpsCSIPReportId"   : "dcd118c-41ba-42ea-b5c4-404b453ad58b",
                  "mappingType"       : "DPS_CREATED"
                }""",
              ),
            )
            .exchange()
            .expectStatus().isCreated,
        )
      }

      @Test
      fun `returns 400 when DPS CSIP Report Id is missing`() {
        assertThat(
          webTestClient.post().uri("/mapping/csip/all")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                """{
                  "label"             : "2022-01-01",
                  "nomisCSIPReportId" : 1234,
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
      webTestClient.get().uri("/mapping/csip/nomis-csip-id/1234")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/csip/nomis-csip-id/1234")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/csip/nomis-csip-id/1234")
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
        webTestClient.get().uri("/mapping/csip/nomis-csip-id/1234")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody(CSIPReportMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping.nomisCSIPReportId).isEqualTo(1234)
      assertThat(mapping.dpsCSIPReportId).isEqualTo("dcd118c-41ba-42ea-b5c4-404b453ad58b")
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

      webTestClient.get().uri("/mapping/csip/nomis-csip-id/1234")
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
      assertThat(mapping.attendeeMappings).isEqualTo(emptyList<CSIPChildMappingDto>())
      assertThat(mapping.factorMappings).isEqualTo(emptyList<CSIPChildMappingDto>())
      assertThat(mapping.interviewMappings).isEqualTo(emptyList<CSIPChildMappingDto>())
      assertThat(mapping.planMappings).isEqualTo(emptyList<CSIPChildMappingDto>())
      assertThat(mapping.reviewMappings).isEqualTo(emptyList<CSIPChildMappingDto>())
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
            CSIPChildMappingDto(
              nomisId = 343,
              dpsId = UUID.randomUUID().toString(),
              dpsCSIPReportId = dpsCSIPReportId,
              label = "test",
              mappingType = CSIPChildMappingType.MIGRATED,
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
            CSIPChildMappingDto(
              nomisId = 454,
              dpsId = UUID.randomUUID().toString(),
              dpsCSIPReportId = dpsCSIPReportId,
              label = "test",
              mappingType = CSIPChildMappingType.MIGRATED,
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
            CSIPChildMappingDto(
              nomisId = 565,
              dpsId = UUID.randomUUID().toString(),
              dpsCSIPReportId = dpsCSIPReportId,
              label = "test",
              mappingType = CSIPChildMappingType.MIGRATED,
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
            CSIPChildMappingDto(
              nomisId = 676,
              dpsId = UUID.randomUUID().toString(),
              dpsCSIPReportId = dpsCSIPReportId,
              label = "test",
              mappingType = CSIPChildMappingType.MIGRATED,
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
            CSIPChildMappingDto(
              nomisId = 787,
              dpsId = UUID.randomUUID().toString(),
              dpsCSIPReportId = dpsCSIPReportId,
              label = "test",
              mappingType = CSIPChildMappingType.MIGRATED,
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

  @DisplayName("DELETE /mapping/csip/dps-csip-id/{dpsCSIPId}/children")
  @Nested
  inner class DeleteChildMappings {
    private val dpsCSIPId = UUID.randomUUID().toString()
    private val nomisCSIPId = 22334L
    private lateinit var mapping: CSIPMapping

    private var dpsCsipPlanId = "0"
    private var dpsCsipInterviewId = "0"
    private var dpsCsipReviewId = "0"
    private var dpsCsipAttendeeId = "0"
    private var dpsCsipFactorId = "0"

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CSIPMapping(
          dpsCSIPId = dpsCSIPId,
          nomisCSIPId = nomisCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
      dpsCsipFactorId = csipFactorRepository.save(
        CSIPFactorMapping(
          dpsCSIPFactorId = "c5e56441-04c9-40e1-bd37-553ec1abcaaa",
          nomisCSIPFactorId = 11111L,
          dpsCSIPReportId = dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.MIGRATED,
        ),
      ).dpsCSIPFactorId

      dpsCsipPlanId = csipPlanRepository.save(
        CSIPPlanMapping(
          dpsCSIPPlanId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPPlanId = 12345L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.MIGRATED,
        ),
      ).dpsCSIPPlanId

      dpsCsipInterviewId = csipInterviewRepository.save(
        CSIPInterviewMapping(
          dpsCSIPInterviewId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPInterviewId = 12345L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.MIGRATED,
        ),
      ).dpsCSIPInterviewId

      dpsCsipReviewId = csipReviewRepository.save(
        CSIPReviewMapping(
          dpsCSIPReviewId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPReviewId = 12345L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.MIGRATED,
        ),
      ).dpsCSIPReviewId

      dpsCsipAttendeeId = csipAttendeeRepository.save(
        CSIPAttendeeMapping(
          dpsCSIPAttendeeId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPAttendeeId = 12345L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.MIGRATED,
        ),
      ).dpsCSIPAttendeeId
    }

    @AfterEach
    fun tearDown() = runTest {
      csipAttendeeRepository.deleteAll()
      csipFactorRepository.deleteAll()
      csipInterviewRepository.deleteAll()
      csipPlanRepository.deleteAll()
      csipReviewRepository.deleteAll()
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}/children")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}/children")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}/children")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Test
    fun `delete specific mapping success`() {
      // it is present after creation by csip id
      webTestClient.get().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("nomisCSIPReportId").isEqualTo(nomisCSIPId)
        .jsonPath("dpsCSIPReportId").isEqualTo(dpsCSIPId)
        .jsonPath("attendeeMappings.size()").isEqualTo(1)
        .jsonPath("factorMappings.size()").isEqualTo(1)
        .jsonPath("interviewMappings.size()").isEqualTo(1)
        .jsonPath("planMappings.size()").isEqualTo(1)
        .jsonPath("reviewMappings.size()").isEqualTo(1)

      // it is also present after creation by nomis id
      webTestClient.get().uri("/mapping/csip/nomis-csip-id/${mapping.nomisCSIPId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk

      // delete children mappings
      webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}/children")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNoContent

      // still present by dps csip id but children deleted
      webTestClient.get().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("nomisCSIPReportId").isEqualTo(nomisCSIPId)
        .jsonPath("dpsCSIPReportId").isEqualTo(dpsCSIPId)
        .jsonPath("attendeeMappings").doesNotExist()
        .jsonPath("factorMappings").doesNotExist()
        .jsonPath("interviewMappings").doesNotExist()
        .jsonPath("planMappings").doesNotExist()
        .jsonPath("reviewMappings").doesNotExist()

      // and also still present by nomis csip id
      webTestClient.get().uri("/mapping/csip/nomis-csip-id/${mapping.nomisCSIPId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    internal fun `delete is idempotent`() {
      // delete mapping
      webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}/children")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNoContent
      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}/children")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @DisplayName("POST /mapping/csip/children/all")
  @Nested
  inner class CreateFullChildCSIPMapping {
    private val dpsCSIPId = UUID.randomUUID().toString()
    private val nomisCSIPId = 22334L
    private lateinit var mapping: CSIPMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CSIPMapping(
          dpsCSIPId = dpsCSIPId,
          nomisCSIPId = nomisCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
      csipFactorRepository.save(
        CSIPFactorMapping(
          dpsCSIPFactorId = "c5e56441-04c9-40e1-bd37-553ec1abcaaa",
          nomisCSIPFactorId = 11111L,
          dpsCSIPReportId = dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.MIGRATED,
        ),
      )
    }

    @AfterEach
    internal fun deleteData() = runBlocking {
      csipAttendeeRepository.deleteAll()
      csipFactorRepository.deleteAll()
      csipInterviewRepository.deleteAll()
      csipPlanRepository.deleteAll()
      csipReviewRepository.deleteAll()
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.post().uri("/mapping/csip/children/all")
          .body(BodyInserters.fromValue(createFullCSIPMapping()))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/mapping/csip/children/all")
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(createFullCSIPMapping()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `create forbidden with wrong role`() {
        webTestClient.post().uri("/mapping/csip/children/all")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(createFullCSIPMapping()))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Test
    fun `returns 409 if child csip factor already exists`() {
      val duplicateResponse = webTestClient.post()
        .uri("/mapping/csip/children/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CSIPFullMappingDto(
              nomisCSIPReportId = nomisCSIPId,
              dpsCSIPReportId = dpsCSIPId,
              factorMappings = listOf(
                CSIPChildMappingDto(
                  dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcaaa",
                  nomisId = 11111L,
                  dpsCSIPReportId = dpsCSIPId,
                  label = "2023-01-01T12:45:12",
                  mappingType = CSIPChildMappingType.MIGRATED,
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
        // since this is an untyped map an int will be assumed for such small numbers
        assertThat(this.moreInfo.existing)
          .containsEntry("nomisCSIPReportId", mapping.nomisCSIPId.toInt())
          .containsEntry("dpsCSIPReportId", mapping.dpsCSIPId)
        assertThat(this.moreInfo.duplicate)
          .containsEntry("nomisCSIPReportId", nomisCSIPId.toInt())
          .containsEntry("dpsCSIPReportId", mapping.dpsCSIPId)
      }
    }

    @Test
    fun `returns 404 if csip report does not exist`() {
      val missingDpsCSIPReportId = UUID.randomUUID().toString()
      webTestClient.post().uri("/mapping/csip/children/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CSIPFullMappingDto(
              nomisCSIPReportId = nomisCSIPId,
              dpsCSIPReportId = missingDpsCSIPReportId,
              factorMappings = listOf(
                CSIPChildMappingDto(
                  dpsId = "c5e56441-04c9-40e1-bd37-553ec1abc111",
                  nomisId = 12233L,
                  dpsCSIPReportId = missingDpsCSIPReportId,
                  label = "2023-01-01T12:45:12",
                  mappingType = CSIPChildMappingType.MIGRATED,
                ),
              ),
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(404)
    }

    @Test
    fun `returns exception if csip report does not exist on child`() {
      val attendeeMappingDto = CSIPChildMappingDto(
        dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
        nomisId = 3333L,
        dpsCSIPReportId = dpsCSIPId,
        label = "2023-01-01T12:45:12",
        mappingType = CSIPChildMappingType.MIGRATED,
      )

      val planMappingDto = CSIPChildMappingDto(
        dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
        nomisId = 3333L,
        dpsCSIPReportId = UUID.randomUUID().toString(),
        label = "2023-01-01T12:45:12",
        mappingType = CSIPChildMappingType.MIGRATED,
      )

      webTestClient.post().uri("/mapping/csip/children/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CSIPFullMappingDto(
              nomisCSIPReportId = nomisCSIPId,
              dpsCSIPReportId = dpsCSIPId,
              label = "2022-01-01",
              mappingType = DPS_CREATED,
              attendeeMappings = listOf(attendeeMappingDto),
              factorMappings = listOf(),
              interviewMappings = listOf(),
              planMappings = listOf(planMappingDto),
              reviewMappings = listOf(),
            ),
          ),
        )
        .exchange()
        .expectStatus().is5xxServerError

      // Ensure transaction rolled back
      val mapping =
        webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPId/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody(CSIPFullMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping.nomisCSIPReportId).isEqualTo(nomisCSIPId)
      assertThat(mapping.dpsCSIPReportId).isEqualTo(dpsCSIPId)
      assertThat(mapping.attendeeMappings).isEmpty()
      assertThat(mapping.planMappings).isEmpty()
      // 1 factor mapping added setUp
      assertThat(mapping.factorMappings).size().isEqualTo(1)
    }

    @Test
    fun `update mapping Happy Path`() {
      val attendeeMappingDto = CSIPChildMappingDto(
        dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
        nomisId = 12345L,
        dpsCSIPReportId = dpsCSIPId,
        label = "2023-01-01T12:45:12",
        mappingType = CSIPChildMappingType.MIGRATED,
      )

      val factorMappingDto =
        CSIPChildMappingDto(
          dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisId = 12345L,
          dpsCSIPReportId = dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.MIGRATED,
        )

      val planMappingDto = CSIPChildMappingDto(
        dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
        nomisId = 12345L,
        dpsCSIPReportId = dpsCSIPId,
        label = "2023-01-01T12:45:12",
        mappingType = CSIPChildMappingType.MIGRATED,
      )

      val planMappingDto2 = CSIPChildMappingDto(
        dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcdaa",
        nomisId = 12346L,
        dpsCSIPReportId = dpsCSIPId,
        label = "2023-01-01T12:45:12",
        mappingType = CSIPChildMappingType.DPS_CREATED,
      )

      val interviewMappingDto = CSIPChildMappingDto(
        dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
        nomisId = 12345L,
        dpsCSIPReportId = dpsCSIPId,
        label = "2023-01-01T12:45:12",
        mappingType = CSIPChildMappingType.MIGRATED,
      )

      val reviewMappingDto = CSIPChildMappingDto(
        dpsId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
        nomisId = 12345L,
        dpsCSIPReportId = dpsCSIPId,
        label = "2023-01-01T12:45:12",
        mappingType = CSIPChildMappingType.MIGRATED,
      )

      webTestClient.post().uri("/mapping/csip/children/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CSIPFullMappingDto(
              nomisCSIPReportId = nomisCSIPId,
              dpsCSIPReportId = dpsCSIPId,
              label = "2022-01-01",
              mappingType = DPS_CREATED,
              attendeeMappings = listOf(attendeeMappingDto),
              factorMappings = listOf(factorMappingDto),
              interviewMappings = listOf(interviewMappingDto),
              planMappings = listOf(planMappingDto, planMappingDto2),
              reviewMappings = listOf(reviewMappingDto),
            ),
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
      assertThat(mapping1.label).isEqualTo("2023-01-01T12:45:12")
      assertThat(mapping1.mappingType).isEqualTo(MIGRATED)

      val mapping2 = webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody(CSIPReportMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.nomisCSIPReportId).isEqualTo(nomisCSIPId)
      assertThat(mapping2.dpsCSIPReportId).isEqualTo(dpsCSIPId)
      assertThat(mapping2.label).isEqualTo("2023-01-01T12:45:12")
      assertThat(mapping2.mappingType).isEqualTo(MIGRATED)

      val mapping = webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPId/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody(CSIPFullMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisCSIPReportId).isEqualTo(nomisCSIPId)
      assertThat(mapping.dpsCSIPReportId).isEqualTo(dpsCSIPId)
      assertThat(mapping.attendeeMappings).size().isEqualTo(1)
      assertThat(mapping.factorMappings).size().isEqualTo(2)
      assertThat(mapping.interviewMappings).size().isEqualTo(1)
      assertThat(mapping.planMappings).size().isEqualTo(2)
      assertThat(mapping.reviewMappings).size().isEqualTo(1)
    }

    @Nested
    inner class Validation {

      @Test
      fun `returns 400 if mapping type invalid`() {
        assertThat(
          webTestClient.post().uri("/mapping/csip/children/all")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                //language=JSON
                """{
                  "nomisCSIPReportId" : $nomisCSIPId,
                  "label"             : "2022-01-01",
                  "dpsCSIPReportId"   : "$dpsCSIPId",
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
          webTestClient.post().uri("/mapping/csip/children/all")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                //language=JSON
                """{
                  "label"             : "2022-01-01",
                  "dpsCSIPReportId"   : "$dpsCSIPId",
                  "mappingType"       : "DPS_CREATED",
                  "attendeeMappings"  : [],
                  "factorMappings"    : [],
                  "interviewMappings" : [],
                  "planMappings"      : [],
                  "reviewMappings"    : []
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
      fun `is success when child mapping lists are missing`() {
        assertThat(
          webTestClient.post().uri("/mapping/csip/children/all")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                //language=JSON
                """{
                  "label"             : "2022-01-01",
                  "nomisCSIPReportId" : $nomisCSIPId,
                  "dpsCSIPReportId"   : "$dpsCSIPId",
                  "mappingType"       : "DPS_CREATED"
                }""",
              ),
            )
            .exchange()
            .expectStatus().isCreated,
        )
      }

      @Test
      fun `returns 400 when DPS CSIP Report Id is missing`() {
        assertThat(
          webTestClient.post().uri("/mapping/csip/children/all")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                //language=JSON
                """{
                  "label"             : "2022-01-01",
                  "nomisCSIPReportId" : $nomisCSIPId,
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

  @DisplayName("DELETE /mapping/csip/dps-csip-id/{dpsCSIPId}/all with children set")
  @Nested
  inner class DeleteAllWithChildMappings {
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
          mappingType = CSIPChildMappingType.MIGRATED,
        ),
      ).dpsCSIPPlanId

      dpsCsipPlanId2 = csipPlanRepository.save(
        CSIPPlanMapping(
          dpsCSIPPlanId = "c5e56441-04c9-40e1-bd37-553ec1abcdaa",
          nomisCSIPPlanId = 12346L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.DPS_CREATED,
        ),
      ).dpsCSIPPlanId

      dpsCsipInterviewId = csipInterviewRepository.save(
        CSIPInterviewMapping(
          dpsCSIPInterviewId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPInterviewId = 12345L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.MIGRATED,
        ),
      ).dpsCSIPInterviewId

      dpsCsipInterviewId2 = csipInterviewRepository.save(
        CSIPInterviewMapping(
          dpsCSIPInterviewId = "c5e56441-04c9-40e1-bd37-553ec1abcdaa",
          nomisCSIPInterviewId = 12346L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.DPS_CREATED,
        ),
      ).dpsCSIPInterviewId

      dpsCsipReviewId = csipReviewRepository.save(
        CSIPReviewMapping(
          dpsCSIPReviewId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPReviewId = 12345L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.MIGRATED,
        ),
      ).dpsCSIPReviewId

      dpsCsipReviewId2 = csipReviewRepository.save(
        CSIPReviewMapping(
          dpsCSIPReviewId = "c5e56441-04c9-40e1-bd37-553ec1abcdaa",
          nomisCSIPReviewId = 12346L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.DPS_CREATED,
        ),
      ).dpsCSIPReviewId

      dpsCsipAttendeeId = csipAttendeeRepository.save(
        CSIPAttendeeMapping(
          dpsCSIPAttendeeId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPAttendeeId = 12345L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.MIGRATED,
        ),
      ).dpsCSIPAttendeeId

      dpsCsipAttendeeId2 = csipAttendeeRepository.save(
        CSIPAttendeeMapping(
          dpsCSIPAttendeeId = "c5e56441-04c9-40e1-bd37-553ec1abcdaa",
          nomisCSIPAttendeeId = 12346L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.DPS_CREATED,
        ),
      ).dpsCSIPAttendeeId

      dpsCsipFactorId = csipFactorRepository.save(
        CSIPFactorMapping(
          dpsCSIPFactorId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPFactorId = 12345L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.MIGRATED,
        ),
      ).dpsCSIPFactorId

      dpsCsipFactorId2 = csipFactorRepository.save(
        CSIPFactorMapping(
          dpsCSIPFactorId = "c5e56441-04c9-40e1-bd37-553ec1abcdaa",
          nomisCSIPFactorId = 12346L,
          dpsCSIPReportId = mapping.dpsCSIPId,
          label = "2023-01-01T12:45:12",
          mappingType = CSIPChildMappingType.DPS_CREATED,
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

  @Nested
  @DisplayName("GET /mapping/nomis-csip-id")
  inner class GetMappingsByNomisIds {
    lateinit var mapping1: CSIPMapping
    lateinit var mapping2: CSIPMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping1 = repository.save(
        CSIPMapping(
          dpsCSIPId = UUID.randomUUID().toString(),
          nomisCSIPId = 54321,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
      mapping2 = repository.save(
        CSIPMapping(
          dpsCSIPId = UUID.randomUUID().toString(),
          nomisCSIPId = 54322,
          label = "2023-06-01T12:45:12",
          mappingType = DPS_CREATED,
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
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/csip/nomis-csip-id?nomisCSIPId=54321")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/csip/nomis-csip-id?nomisCSIPId=54321")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/csip/nomis-csip-id?nomisCSIPId=54321")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return Not Found when no mappings exist`() {
        webTestClient.get()
          .uri("/mapping/csip/nomis-csip-id?nomisCSIPId=99999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will ensure at least one csip Id is passed in`() {
        webTestClient.get()
          .uri("/mapping/csip/nomis-csip-id")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `will return Not Found if any of the mappings don't exist`() {
        webTestClient.get()
          .uri("/mapping/csip/nomis-csip-id?nomisCSIPId=54321&nomisCSIPId=54322&nomisCSIPId=99999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/csip/nomis-csip-id?nomisCSIPId=54321&nomisCSIPId=54322")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("[0].nomisCSIPReportId").isEqualTo(mapping1.nomisCSIPId)
          .jsonPath("[0].dpsCSIPReportId").isEqualTo(mapping1.dpsCSIPId)
          .jsonPath("[0].mappingType").isEqualTo(mapping1.mappingType.name)
          .jsonPath("[0].label").isEqualTo(mapping1.label!!)
          .jsonPath("[0].whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
          .jsonPath("[1].nomisCSIPReportId").isEqualTo(mapping2.nomisCSIPId)
          .jsonPath("[1].dpsCSIPReportId").isEqualTo(mapping2.dpsCSIPId)
          .jsonPath("[1].mappingType").isEqualTo(mapping2.mappingType.name)
          .jsonPath("[1].label").isEqualTo(mapping2.label!!)
          .jsonPath("[1].whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
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
        .uri("/mapping/csip/nomis-csip-id/1234")
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
