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
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingType.DPS_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingType.NOMIS_CREATED
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

  private fun createCSIPMapping(
    nomisCSIPId: Long = NOMIS_CSIP_ID,
    dpsCSIPId: String = DPS_CSIP_ID,
    label: String = "2022-01-01",
    mappingType: CSIPMappingType = NOMIS_CREATED,
  ): CSIPMappingDto = CSIPMappingDto(
    nomisCSIPId = nomisCSIPId,
    dpsCSIPId = dpsCSIPId,
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
            CSIPMappingDto(
              nomisCSIPId = existingMapping.nomisCSIPId,
              dpsCSIPId = dpsCSIPId,
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
          .containsEntry("nomisCSIPId", existingMapping.nomisCSIPId.toInt())
          .containsEntry("dpsCSIPId", existingMapping.dpsCSIPId)
        assertThat(this.moreInfo.duplicate)
          .containsEntry("nomisCSIPId", existingMapping.nomisCSIPId.toInt())
          .containsEntry("dpsCSIPId", dpsCSIPId)
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
            CSIPMappingDto(
              nomisCSIPId = nomisCSIPId,
              dpsCSIPId = existingMapping.dpsCSIPId,
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
          .containsEntry("nomisCSIPId", existingMapping.nomisCSIPId.toInt())
          .containsEntry("dpsCSIPId", existingMapping.dpsCSIPId)
        assertThat(this.moreInfo.duplicate)
          .containsEntry("nomisCSIPId", nomisCSIPId.toInt())
          .containsEntry("dpsCSIPId", existingMapping.dpsCSIPId)
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
              "nomisCSIPId" : $nomisCSIPId,
              "dpsCSIPId"   : "$dpsCSIPId",
              "label"       : "2022-01-01",
              "mappingType" : "DPS_CREATED"
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
          .expectBody(CSIPMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping1.nomisCSIPId).isEqualTo(nomisCSIPId)
      assertThat(mapping1.dpsCSIPId).isEqualTo(dpsCSIPId)
      assertThat(mapping1.label).isEqualTo("2022-01-01")
      assertThat(mapping1.mappingType).isEqualTo(DPS_CREATED)

      val mapping2 = webTestClient.get().uri("/mapping/csip/dps-csip-id/$dpsCSIPId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody(CSIPMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.nomisCSIPId).isEqualTo(nomisCSIPId)
      assertThat(mapping2.dpsCSIPId).isEqualTo(dpsCSIPId)
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
              "nomisCSIPId" : $nomisCSIPId,
              "dpsCSIPId"   : "$dpsCSIPId",
              "label"       : "2022-01-01",
              "mappingType" : "DPS_CREATED"
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
                "nomisCSIPId" : $nomisCSIPId,
                "dpsCSIPId"   : "$dpsCSIPId",
                "label"       : "2022-01-01",
                "mappingType" : "DPS_CREATED"
              }""",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<CSIPMappingDto>>() {})
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
                "nomisCSIPId" : "55665",
                "label"       : "2022-01-01",
                "dpsCSIPId"   : "3ecd118c-41ba-42ea-b5c4-404b453ad123"
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
                "nomisCSIPId" : $NOMIS_CSIP_ID,
                "label"       : "2022-01-01",
                "dpsCSIPId"   : "$DPS_CSIP_ID",
                "mappingType" : "INVALID_MAPPING_TYPE"
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
      fun `returns 400 when Nomis CSIP Id is missing`() {
        assertThat(
          webTestClient.post().uri("/mapping/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                //language=JSON
                """{
                "dpsCSIPId" : "$DPS_CSIP_ID",
                "label"       : "2022-01-01",
                "mappingType" : "DPS_CREATED"
              }""",
              ),
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ErrorResponse::class.java)
            .returnResult().responseBody?.userMessage,
        )
          .contains("Validation failure: JSON decoding error")
          .contains("nomisCSIPId")
      }

      @Test
      fun `returns 400 when DPS CSIP Id is missing`() {
        assertThat(
          webTestClient.post().uri("/mapping/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                """{
                "nomisCSIPId" : $NOMIS_CSIP_ID,
                "label"       : "2022-01-01",
                "mappingType" : "DPS_CREATED"
              }""",
              ),
            )
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(ErrorResponse::class.java)
            .returnResult().responseBody?.userMessage,
        )
          .contains("Validation failure: JSON decoding error")
          .contains("dpsCSIPId")
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
          .expectBody(CSIPMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping.nomisCSIPId).isEqualTo(NOMIS_CSIP_ID)
      assertThat(mapping.dpsCSIPId).isEqualTo(DPS_CSIP_ID)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/csip/dps-csip-id/99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No CSIP mapping found for dpsCSIPId=99999")
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
        .expectBody(CSIPMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisCSIPId).isEqualTo(nomisCSIPId)
      assertThat(mapping.dpsCSIPId).isEqualTo(dpsCSIPId)
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

      assertThat(error.userMessage).isEqualTo("Not Found: No CSIP mapping found for dpsCSIPId=9999")
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

  @DisplayName("DELETE /mapping/csip/dps-csip-id/{dpsCSIPId}")
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
        webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}")
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
      webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}")
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
      webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNoContent
      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/csip/dps-csip-id/${mapping.dpsCSIPId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @DisplayName("DELETE /mapping/csip")
  @Nested
  inner class DeleteAllMappings {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/csip")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/csip")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/csip")
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

      webTestClient.delete().uri("/mapping/csip")
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

      webTestClient.delete().uri("/mapping/csip?onlyMigrated=true")
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
        .jsonPath("$.content..dpsCSIPId").value(Matchers.contains("1", "2", "3", "4"))
        .jsonPath("$.content..nomisCSIPId").value(Matchers.contains(1, 2, 3, 4))
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
  inner class GeMappingMigratedLatestTest {

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
        .expectBody(CSIPMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisCSIPId).isEqualTo(1)
      assertThat(mapping.dpsCSIPId).isEqualTo("1")
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
