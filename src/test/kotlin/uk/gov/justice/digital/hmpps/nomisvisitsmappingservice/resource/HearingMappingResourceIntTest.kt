package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationHearingMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.isDuplicateMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.AdjudicationHearingMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.AdjudicationMappingService

private const val DPS_HEARING_ID = "AB123"
private const val NOMIS_HEARING_ID = 4444L

class HearingMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var realHearingRepository: AdjudicationHearingMappingRepository
  private lateinit var hearingRepository: AdjudicationHearingMappingRepository

  @Autowired
  private lateinit var adjudicationMappingService: AdjudicationMappingService

  @BeforeEach
  fun setup() {
    hearingRepository = mock(defaultAnswer = AdditionalAnswers.delegatesTo(realHearingRepository))
    adjudicationMappingService.adjudicationHearingMappingRepository = hearingRepository
  }

  private fun createMapping(
    dpsHearingId: String = DPS_HEARING_ID,
    nomisHearingId: Long = NOMIS_HEARING_ID,
    label: String = "2022-01-01",
    mappingType: String = AdjudicationMappingType.ADJUDICATION_CREATED.name,
  ): AdjudicationHearingMappingDto = AdjudicationHearingMappingDto(
    dpsHearingId = dpsHearingId,
    nomisHearingId = nomisHearingId,
    label = label,
    mappingType = mappingType,
  )

  private fun postCreateMappingRequest(
    dpsHearingId: String = DPS_HEARING_ID,
    nomisHearingId: Long = NOMIS_HEARING_ID,
    label: String = "2022-01-01",
    mappingType: String = AdjudicationMappingType.ADJUDICATION_CREATED.name,
  ) {
    webTestClient.post().uri("/mapping/hearings")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createMapping(
            dpsHearingId = dpsHearingId,
            nomisHearingId = nomisHearingId,
            label = label,
            mappingType = mappingType,
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  @BeforeEach
  fun deleteData() = runBlocking {
    hearingRepository.deleteAll()
  }

  @DisplayName("POST /mapping/hearings")
  @Nested
  inner class CreateMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/hearings")
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/hearings")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/hearings")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create mapping success - ADJUDICATION_CREATED`() {
      webTestClient.post().uri("/mapping/hearings")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            // language=json
            """{
             "dpsHearingId" : "$DPS_HEARING_ID",
            "nomisHearingId": $NOMIS_HEARING_ID
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val createdMappingByNomisId = webTestClient.get()
        .uri("/mapping/hearings/nomis/$NOMIS_HEARING_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AdjudicationHearingMappingDto::class.java)
        .returnResult().responseBody!!

      Assertions.assertThat(createdMappingByNomisId.dpsHearingId).isEqualTo(DPS_HEARING_ID)
      Assertions.assertThat(createdMappingByNomisId.nomisHearingId).isEqualTo(NOMIS_HEARING_ID)
      Assertions.assertThat(createdMappingByNomisId.mappingType).isEqualTo("ADJUDICATION_CREATED")

      val createdMappingByDpsId =
        webTestClient.get().uri("/mapping/hearings/dps/$DPS_HEARING_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(AdjudicationHearingMappingDto::class.java)
          .returnResult().responseBody!!

      Assertions.assertThat(createdMappingByNomisId.dpsHearingId).isEqualTo(DPS_HEARING_ID)
      Assertions.assertThat(createdMappingByNomisId.nomisHearingId).isEqualTo(NOMIS_HEARING_ID)
      Assertions.assertThat(createdMappingByDpsId.mappingType).isEqualTo("ADJUDICATION_CREATED")
    }

    @Test
    fun `create mapping failure - adjudication exists`() {
      webTestClient.post().uri("/mapping/hearings")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            // language=json
            """{
            "dpsHearingId" : "$DPS_HEARING_ID",
            "nomisHearingId": $NOMIS_HEARING_ID,
            "label"                 : "2023-04-20",
            "mappingType"           : "MIGRATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/hearings")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            // language=json
            """{
            "dpsHearingId" : "$DPS_HEARING_ID",
            "nomisHearingId": $NOMIS_HEARING_ID,
            "label"                 : "2023-04-25",
            "mappingType"           : "MIGRATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isDuplicateMapping

      val createdMappingByDpsId =
        webTestClient.get().uri("/mapping/hearings/nomis/$NOMIS_HEARING_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(AdjudicationHearingMappingDto::class.java)
          .returnResult().responseBody!!

      Assertions.assertThat(createdMappingByDpsId.dpsHearingId).isEqualTo(DPS_HEARING_ID)
      Assertions.assertThat(createdMappingByDpsId.label).isEqualTo("2023-04-20")
      Assertions.assertThat(createdMappingByDpsId.mappingType).isEqualTo("MIGRATED")
    }

    @Test
    fun `create mapping failure - Duplicate db error`() = runTest {
      webTestClient.post().uri("/mapping/hearings")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            // language=json
            """{
            "dpsHearingId" : "$DPS_HEARING_ID",
            "nomisHearingId": $NOMIS_HEARING_ID,
            "label"                 : "2023-04-25",
            "mappingType"           : "MIGRATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      // Emulate calling service simultaneously twice by disabling the duplicate check
      // Note: the spy is automatically reset by ResetMocksTestExecutionListener
      whenever(hearingRepository.findById(DPS_HEARING_ID)).thenReturn(null)

      val responseBody =
        webTestClient.post().uri("/mapping/hearings")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              // language=json
              """{
            "dpsHearingId" : "$DPS_HEARING_ID",
            "nomisHearingId": $NOMIS_HEARING_ID,
            "label"                 : "2023-04-25",
            "mappingType"           : "MIGRATED"
          }""",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<AdjudicationHearingMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        Assertions.assertThat(userMessage)
          .contains("Conflict: Hearing mapping already exists, detected by org.springframework.dao.DuplicateKeyException")
        Assertions.assertThat(errorCode).isEqualTo(1409)
      }
    }
  }

  @DisplayName("GET /mapping/hearings/dps/{dpsHearingId}}")
  @Nested
  inner class GetMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/hearings/dps/$DPS_HEARING_ID")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get()
        .uri("/mapping/hearings/dps/$DPS_HEARING_ID")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get()
        .uri("/mapping/hearings/dps/$DPS_HEARING_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/hearings")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get()
        .uri("/mapping/hearings/dps/$DPS_HEARING_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AdjudicationHearingMappingDto::class.java)
        .returnResult().responseBody!!

      Assertions.assertThat(mapping.nomisHearingId).isEqualTo(NOMIS_HEARING_ID)
    }

    @Test
    fun `mapping not found`() {
      webTestClient.get().uri("/mapping/hearings/dps/888")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("$.userMessage").value<String> {
          Assertions.assertThat(it).isEqualTo("Not Found: DPS hearing Id=888")
        }
      webTestClient.get().uri("/mapping/hearings/nomis/999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("$.userMessage").value<String> {
          Assertions.assertThat(it).isEqualTo("Not Found: NOMIS hearing Id=999")
        }
    }
  }

  @DisplayName("DELETE /mapping/hearings/dps/{id}")
  @Nested
  inner class DeleteMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/hearings/dps/$DPS_HEARING_ID")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/hearings/dps/$DPS_HEARING_ID")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/hearings/dps/$DPS_HEARING_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete specific mapping success`() {
      // create mapping
      webTestClient.post().uri("/mapping/hearings")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      // it is present after creation by adjudication id
      webTestClient.get().uri("/mapping/hearings/dps/$DPS_HEARING_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk

      // delete mapping
      webTestClient.delete().uri("/mapping/hearings/dps/$DPS_HEARING_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent

      // no longer present by dps id
      webTestClient.get().uri("/mapping/hearings/dps/$DPS_HEARING_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `delete is idempotent`() {
      // create mapping
      webTestClient.post().uri("/mapping/hearings")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      // delete mapping
      webTestClient.delete().uri("/mapping/hearings/dps/$DPS_HEARING_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent

      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/hearings/dps/$DPS_HEARING_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent
    }
  }
}
