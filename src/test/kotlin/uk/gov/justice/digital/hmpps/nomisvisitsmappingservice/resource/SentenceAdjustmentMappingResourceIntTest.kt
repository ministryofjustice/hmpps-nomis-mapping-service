package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.byLessThan
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.SentenceAdjustmentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingMappingType.NOMIS_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingMappingType.SENTENCING_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.SentenceAdjustmentMappingRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SentenceAdjustmentMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var repository: SentenceAdjustmentMappingRepository

  private val nomisSentenceAdjustId = 1234L
  private val sentenceAdjustId = 4444L

  private fun createSentenceAdjustmentMapping(
    nomisSentenceAdjustmentId: Long = nomisSentenceAdjustId,
    sentenceAdjustmentId: Long = sentenceAdjustId,
    label: String = "2022-01-01",
    mappingType: String = NOMIS_CREATED.name
  ): SentenceAdjustmentMappingDto = SentenceAdjustmentMappingDto(
    nomisSentenceAdjustmentId = nomisSentenceAdjustmentId,
    sentenceAdjustmentId = sentenceAdjustmentId,
    label = label,
    mappingType = mappingType
  )

  private fun postCreateSentenceAdjustmentMappingRequest(
    nomisSentenceAdjustmentId: Long = 1234L,
    sentenceAdjustmentId: Long = 4444L,
    label: String = "2022-01-01",
    mappingType: String = NOMIS_CREATED.name
  ) {
    webTestClient.post().uri("/mapping/sentence-adjustments")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createSentenceAdjustmentMapping(
            nomisSentenceAdjustmentId = nomisSentenceAdjustmentId,
            sentenceAdjustmentId = sentenceAdjustmentId,
            label = label,
            mappingType = mappingType
          )
        )
      )
      .exchange()
      .expectStatus().isCreated
  }

  @DisplayName("POST /mapping/sentence-adjustments")
  @Nested
  inner class CreateSentenceAdjustmentMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/sentence-adjustments")
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create when mapping for sentence adjustment id already exists for another mapping`() {
      postCreateSentenceAdjustmentMappingRequest()

      assertThat(
        webTestClient.post().uri("/mapping/sentence-adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createSentenceAdjustmentMapping().copy(nomisSentenceAdjustmentId = 21)))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Validation failure: Sentence adjustment mapping nomisSentenceAdjustmentId = 1234 and sentenceAdjustmentId = 4444 already exists")
    }

    @Test
    internal fun `create mapping does not error when the same mapping already exists for the same adjustment`() {
      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisSentenceAdjustmentId"     : $nomisSentenceAdjustId,
            "sentenceAdjustmentId"      : $sentenceAdjustId,
            "label"       : "2022-01-01",
            "mappingType" : "SENTENCING_CREATED"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisSentenceAdjustmentId"     : $nomisSentenceAdjustId,
            "sentenceAdjustmentId"      : $sentenceAdjustId,
            "label"       : "2022-01-01",
            "mappingType" : "SENTENCING_CREATED"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    fun `create when mapping for nomis ids already exists`() {
      postCreateSentenceAdjustmentMappingRequest()

      assertThat(
        webTestClient.post().uri("/mapping/sentence-adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createSentenceAdjustmentMapping().copy(sentenceAdjustmentId = 99)))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Validation failure: Sentence adjustment mapping nomisSentenceAdjustmentId = 1234 and sentenceAdjustmentId = 4444 already exists")
    }

    @Test
    fun `create mapping success`() {
      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisSentenceAdjustmentId"     : $nomisSentenceAdjustId,
            "sentenceAdjustmentId"      : $sentenceAdjustId,
            "label"       : "2022-01-01",
            "mappingType" : "SENTENCING_CREATED"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated

      val mapping1 =
        webTestClient.get().uri("/mapping/sentence-adjustments/nomis-sentence-adjustment-id/$nomisSentenceAdjustId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody(SentenceAdjustmentMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping1.nomisSentenceAdjustmentId).isEqualTo(nomisSentenceAdjustId)
      assertThat(mapping1.sentenceAdjustmentId).isEqualTo(sentenceAdjustId)
      assertThat(mapping1.label).isEqualTo("2022-01-01")
      assertThat(mapping1.mappingType).isEqualTo("SENTENCING_CREATED")

      val mapping2 = webTestClient.get().uri("/mapping/sentence-adjustments/sentence-adjustment-id/$sentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody(SentenceAdjustmentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.nomisSentenceAdjustmentId).isEqualTo(nomisSentenceAdjustId)
      assertThat(mapping2.sentenceAdjustmentId).isEqualTo(sentenceAdjustId)
      assertThat(mapping2.label).isEqualTo("2022-01-01")
      assertThat(mapping2.mappingType).isEqualTo("SENTENCING_CREATED")
    }

    @Test
    fun `create rejects bad filter data - missing mapping type`() {
      assertThat(
        webTestClient.post().uri("/mapping/sentence-adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "nomisSentenceAdjustmentId"     : $nomisSentenceAdjustId,
            "label"       : "2022-01-01",
            "sentenceAdjustmentId" : $sentenceAdjustId
          }"""
            )
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).contains("missing (therefore NULL) value for creator parameter mappingType which is a non-nullable type")
    }

    @Test
    fun `create rejects bad filter data - mapping max 20`() {
      assertThat(
        webTestClient.post().uri("/mapping/sentence-adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "nomisSentenceAdjustmentId"     : $nomisSentenceAdjustId,
            "label"       : "2022-01-01",
            "sentenceAdjustmentId" : $sentenceAdjustId,
            "mappingType" : "MASSIVELY_LONG_PROPERTY_SENTENCING_CREATED"
          }"""
            )
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).contains("mappingType has a maximum length of 20")
    }

    @Test
    fun `create rejects bad filter data - sentence adjustment property must be present (Long)`() {
      assertThat(
        webTestClient.post().uri("/mapping/sentence-adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "nomisSentenceAdjustmentId"     : $nomisSentenceAdjustId,
            "label"       : "2022-01-01",
            "mappingType" : "SENTENCING_CREATED"
          }"""
            )
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Validation failure: JSON decoding error: Missing required creator property 'sentenceAdjustmentId' (index 1)")
    }
  }

  @DisplayName("GET /mapping/sentence-adjustments/nomis-sentence-adjustment-id/nomisSentenceAdjustId}")
  @Nested
  inner class GetNomisMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/sentence-adjustments/nomis-sentence-adjustment-id/$nomisSentenceAdjustId")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/sentence-adjustments/nomis-sentence-adjustment-id/$nomisSentenceAdjustId")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/sentence-adjustments/nomis-sentence-adjustment-id/$nomisSentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {

      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping =
        webTestClient.get().uri("/mapping/sentence-adjustments/nomis-sentence-adjustment-id/$nomisSentenceAdjustId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody(SentenceAdjustmentMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping.nomisSentenceAdjustmentId).isEqualTo(nomisSentenceAdjustId)
      assertThat(mapping.sentenceAdjustmentId).isEqualTo(sentenceAdjustId)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED.name)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/sentence-adjustments/sentence-adjustment-id/99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: Sentencing sentenceAdjustmentId id=99999")
    }

    @Test
    fun `get mapping success with update role`() {

      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/sentence-adjustments/nomis-sentence-adjustment-id/$nomisSentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /mapping/sentence-adjustments/migrated/latest")
  @Nested
  inner class GeMappingMigratedLatestTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/sentence-adjustments/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/sentence-adjustments/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/sentence-adjustments/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {

      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createSentenceAdjustmentMapping(
              nomisSentenceAdjustmentId = 10,
              sentenceAdjustmentId = 10,
              label = "2022-01-01T00:00:00",
              mappingType = "MIGRATED"
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createSentenceAdjustmentMapping(
              nomisSentenceAdjustmentId = 20,
              sentenceAdjustmentId = 20,
              label = "2022-01-02T00:00:00",
              mappingType = "MIGRATED"
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createSentenceAdjustmentMapping(
              nomisSentenceAdjustmentId = 1,
              sentenceAdjustmentId = 1,
              label = "2022-01-02T10:00:00",
              mappingType = MIGRATED.name
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createSentenceAdjustmentMapping(
              nomisSentenceAdjustmentId = 99,
              sentenceAdjustmentId = 199,
              label = "whatever",
              mappingType = SENTENCING_CREATED.name
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/sentence-adjustments/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody(SentenceAdjustmentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisSentenceAdjustmentId).isEqualTo(1)
      assertThat(mapping.sentenceAdjustmentId).isEqualTo(1)
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo("MIGRATED")
      assertThat(mapping.whenCreated)
        .isCloseTo(LocalDateTime.now(), byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createSentenceAdjustmentMapping(
              nomisSentenceAdjustmentId = 77,
              sentenceAdjustmentId = 77,
              label = "whatever",
              mappingType = SENTENCING_CREATED.name
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/sentence-adjustments/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }

  @DisplayName("GET /mapping/sentence-adjustments/incentive/{sentenceAdjustId}")
  @Nested
  inner class GetSentenceAdjustmentMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/sentence-adjustments/sentence-adjustment-id/$sentenceAdjustId")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/sentence-adjustments/sentence-adjustment-id/$sentenceAdjustId")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/sentence-adjustments/sentence-adjustment-id/$sentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {

      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/sentence-adjustments/sentence-adjustment-id/$sentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody(SentenceAdjustmentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisSentenceAdjustmentId).isEqualTo(nomisSentenceAdjustId)
      assertThat(mapping.sentenceAdjustmentId).isEqualTo(sentenceAdjustId)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED.name)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/sentence-adjustments/sentence-adjustment-id/765")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: Sentencing sentenceAdjustmentId id=765")
    }

    @Test
    fun `get mapping success with update role`() {

      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/sentence-adjustments/sentence-adjustment-id/$sentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("DELETE /mapping/sentence-adjustments")
  @Nested
  inner class DeleteMappingsTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/sentence-adjustments")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete mapping success`() {
      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/sentence-adjustments/nomis-sentence-adjustment-id/$nomisSentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/mapping/sentence-adjustments/nomis-sentence-adjustment-id/$nomisSentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `delete incentive mappings - migrated mappings only`() {
      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createSentenceAdjustmentMapping(
              nomisSentenceAdjustmentId = 333,
              sentenceAdjustmentId = 222,
              mappingType = MIGRATED.name
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.delete().uri("/mapping/sentence-adjustments?onlyMigrated=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/mapping/sentence-adjustments/nomis-sentence-adjustment-id/$nomisSentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk

      webTestClient.get().uri("/mapping/sentence-adjustments/sentence-adjustment-id/222")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @DisplayName("DELETE /mapping/sentence-adjustments/sentence-adjustment-id/{sentenceAdjustId}")
  @Nested
  inner class DeleteMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/sentence-adjustments/sentence-adjustment-id/999")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/sentence-adjustments/sentence-adjustment-id/999")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/sentence-adjustments/sentence-adjustment-id/999")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete specific mapping success`() {
      // create mapping
      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      // it is present after creation by incentive id
      webTestClient.get().uri("/mapping/sentence-adjustments/sentence-adjustment-id/$sentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
      // it is also present after creation by nomis id
      webTestClient.get().uri("/mapping/sentence-adjustments/nomis-sentence-adjustment-id/$nomisSentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk

      // delete mapping
      webTestClient.delete().uri("/mapping/sentence-adjustments/sentence-adjustment-id/$sentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      // no longer present by incentive id
      webTestClient.get().uri("/mapping/sentence-adjustments/sentence-adjustment-id/$sentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
      // and also no longer present by nomis id
      webTestClient.get().uri("/mapping/sentence-adjustments/nomis-sentence-adjustment-id/$nomisSentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    internal fun `delete is idempotent`() {
      // create mapping
      webTestClient.post().uri("/mapping/sentence-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      // delete mapping
      webTestClient.delete().uri("/mapping/sentence-adjustments/sentence-adjustment-id/$sentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent
      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/sentence-adjustments/sentence-adjustment-id/$sentenceAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @DisplayName("GET /mapping/sentence-adjustments/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationIdTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/sentence-adjustments/migration-id/2022-01-01T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/sentence-adjustments/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get sentence adjustment mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/sentence-adjustments/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get sentence adjustment mappings by migration id success`() {

      (1L..4L).forEach {
        postCreateSentenceAdjustmentMappingRequest(
          nomisSentenceAdjustmentId = it,
          sentenceAdjustmentId = it,
          label = "2022-01-01",
          mappingType = "MIGRATED"
        )
      }
      (5L..9L).forEach {
        postCreateSentenceAdjustmentMappingRequest(
          nomisSentenceAdjustmentId = it,
          sentenceAdjustmentId = it,
          label = "2099-01-01",
          mappingType = "MIGRATED"
        )
      }
      postCreateSentenceAdjustmentMappingRequest(
        nomisSentenceAdjustmentId = 12,
        sentenceAdjustmentId = 12, mappingType = SENTENCING_CREATED.name
      )

      webTestClient.get().uri("/mapping/sentence-adjustments/migration-id/2022-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..nomisSentenceAdjustmentId").value(
          Matchers.contains(
            1, 2, 3, 4
          )
        )
        .jsonPath("$.content[0].whenCreated").isNotEmpty
    }

    @Test
    fun `get sentence adjustment mappings by migration id - no records exist`() {

      (1L..4L).forEach {
        postCreateSentenceAdjustmentMappingRequest(
          nomisSentenceAdjustmentId = it,
          sentenceAdjustmentId = it,
          label = "2022-01-01",
          mappingType = "MIGRATED"
        )
      }

      webTestClient.get().uri("/mapping/sentence-adjustments/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
        .jsonPath("content").isEmpty
    }

    @Test
    fun `can request a different page size`() {

      (1L..6L).forEach {
        postCreateSentenceAdjustmentMappingRequest(
          nomisSentenceAdjustmentId = it,
          sentenceAdjustmentId = it,
          label = "2022-01-01",
          mappingType = "MIGRATED"
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/sentence-adjustments/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("sort", "nomisSentenceAdjustmentId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
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
        postCreateSentenceAdjustmentMappingRequest(
          nomisSentenceAdjustmentId = it,
          sentenceAdjustmentId = it,
          label = "2022-01-01",
          mappingType = "MIGRATED"
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/sentence-adjustments/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .queryParam("sort", "nomisSentenceAdjustmentId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
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
}
