package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.byLessThan
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.SentencingAdjustmentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingMappingType.NOMIS_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingMappingType.SENTENCING_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.SentenceAdjustmentMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.SentencingMappingService
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SentencingAdjustmentMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var realRepository: SentenceAdjustmentMappingRepository
  private lateinit var repository: SentenceAdjustmentMappingRepository

  @Autowired
  private lateinit var sentencingMappingService: SentencingMappingService

  private val nomisAdjustId = 1234L
  private val nomisAdjustCategory = "SENTENCE"
  private val adjustId = "4444"

  @BeforeEach
  fun setup() {
    repository = mock(defaultAnswer = AdditionalAnswers.delegatesTo(realRepository))
    sentencingMappingService.sentenceAdjustmentRepository = repository
  }

  private fun createSentenceAdjustmentMapping(
    nomisAdjustmentId: Long = nomisAdjustId,
    nomisAdjustmentCategory: String = nomisAdjustCategory,
    adjustmentId: String = adjustId,
    label: String = "2022-01-01",
    mappingType: String = NOMIS_CREATED.name,
  ): SentencingAdjustmentMappingDto = SentencingAdjustmentMappingDto(
    nomisAdjustmentId = nomisAdjustmentId,
    nomisAdjustmentCategory = nomisAdjustmentCategory,
    adjustmentId = adjustmentId,
    label = label,
    mappingType = mappingType,
  )

  private fun postCreateSentenceAdjustmentMappingRequest(
    nomisAdjustmentId: Long = 1234L,
    nomisAdjustmentCategory: String = "SENTENCE",
    adjustmentId: String = "4444",
    label: String = "2022-01-01",
    mappingType: String = NOMIS_CREATED.name,
  ) {
    webTestClient.post().uri("/mapping/sentencing/adjustments")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createSentenceAdjustmentMapping(
            nomisAdjustmentId = nomisAdjustmentId,
            nomisAdjustmentCategory = nomisAdjustmentCategory,
            adjustmentId = adjustmentId,
            label = label,
            mappingType = mappingType,
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  @DisplayName("POST /mapping/sentencing/adjustments")
  @Nested
  inner class CreateSentencingAdjustmentMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create when mapping for sentence adjustment id already exists for another mapping`() {
      postCreateSentenceAdjustmentMappingRequest()

      val responseBody = webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping().copy(nomisAdjustmentId = 21)))
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<SentencingAdjustmentMappingDto>>() {})
        .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Sentence adjustment mapping already exists. \nExisting mapping: SentencingAdjustmentMappingDto(nomisAdjustmentId=1234, nomisAdjustmentCategory=SENTENCE, adjustmentId=4444, label=2022-01-01, mappingType=NOMIS_CREATED")
        assertThat(userMessage).contains("Duplicate mapping: SentencingAdjustmentMappingDto(nomisAdjustmentId=21, nomisAdjustmentCategory=SENTENCE, adjustmentId=4444, label=2022-01-01, mappingType=NOMIS_CREATED, whenCreated=null)")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existingAdjustment = responseBody.moreInfo?.existing!!
      with(existingAdjustment) {
        assertThat(adjustmentId).isEqualTo("4444")
        assertThat(nomisAdjustmentId).isEqualTo(1234)
        assertThat(nomisAdjustmentCategory).isEqualTo("SENTENCE")
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }

      val duplicateAdjustment = responseBody.moreInfo?.duplicate!!
      with(duplicateAdjustment) {
        assertThat(adjustmentId).isEqualTo("4444")
        assertThat(nomisAdjustmentId).isEqualTo(21)
        assertThat(nomisAdjustmentCategory).isEqualTo("SENTENCE")
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }
    }

    @Test
    internal fun `create mapping does not error when the same mapping already exists for the same adjustment`() {
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisAdjustmentId"         : $nomisAdjustId,
            "nomisAdjustmentCategory"       : "$nomisAdjustCategory",
            "adjustmentId"      : "$adjustId",
            "label"                     : "2022-01-01",
            "mappingType"               : "SENTENCING_CREATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisAdjustmentId"         : $nomisAdjustId,
            "nomisAdjustmentCategory"       : "$nomisAdjustCategory",
            "adjustmentId"      : "$adjustId",
            "label"                     : "2022-01-01",
            "mappingType"               : "SENTENCING_CREATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    fun `create when mapping for nomis ids already exists`() {
      postCreateSentenceAdjustmentMappingRequest()

      val responseBody = webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping().copy(adjustmentId = "99")))
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<SentencingAdjustmentMappingDto>>() {})
        .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Sentence adjustment mapping already exists. \nExisting mapping: SentencingAdjustmentMappingDto(nomisAdjustmentId=1234, nomisAdjustmentCategory=SENTENCE, adjustmentId=4444, label=2022-01-01, mappingType=NOMIS_CREATED")
        assertThat(userMessage).contains("Duplicate mapping: SentencingAdjustmentMappingDto(nomisAdjustmentId=1234, nomisAdjustmentCategory=SENTENCE, adjustmentId=99, label=2022-01-01, mappingType=NOMIS_CREATED, whenCreated=null)")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existingAdjustment = responseBody.moreInfo?.existing!!
      with(existingAdjustment) {
        assertThat(adjustmentId).isEqualTo("4444")
        assertThat(nomisAdjustmentId).isEqualTo(1234)
        assertThat(nomisAdjustmentCategory).isEqualTo("SENTENCE")
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }

      val duplicateAdjustment = responseBody.moreInfo?.duplicate!!
      with(duplicateAdjustment) {
        assertThat(adjustmentId).isEqualTo("99")
        assertThat(nomisAdjustmentId).isEqualTo(1234)
        assertThat(nomisAdjustmentCategory).isEqualTo("SENTENCE")
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }
    }

    @Test
    fun `create mapping success`() {
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisAdjustmentId"         : $nomisAdjustId,
            "nomisAdjustmentCategory"       : "$nomisAdjustCategory",
            "adjustmentId"      : "$adjustId",
            "label"       : "2022-01-01",
            "mappingType" : "SENTENCING_CREATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping1 =
        webTestClient.get().uri("/mapping/sentencing/adjustments/nomis-adjustment-category/$nomisAdjustCategory/nomis-adjustment-id/$nomisAdjustId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody(SentencingAdjustmentMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping1.nomisAdjustmentId).isEqualTo(nomisAdjustId)
      assertThat(mapping1.nomisAdjustmentCategory).isEqualTo(nomisAdjustCategory)
      assertThat(mapping1.adjustmentId).isEqualTo(adjustId)
      assertThat(mapping1.label).isEqualTo("2022-01-01")
      assertThat(mapping1.mappingType).isEqualTo("SENTENCING_CREATED")

      val mapping2 = webTestClient.get().uri("/mapping/sentencing/adjustments/adjustment-id/$adjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody(SentencingAdjustmentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.nomisAdjustmentId).isEqualTo(nomisAdjustId)
      assertThat(mapping2.nomisAdjustmentCategory).isEqualTo(nomisAdjustCategory)
      assertThat(mapping2.adjustmentId).isEqualTo(adjustId)
      assertThat(mapping2.label).isEqualTo("2022-01-01")
      assertThat(mapping2.mappingType).isEqualTo("SENTENCING_CREATED")
    }

    @Test
    fun `create mapping - Duplicate db error`() = runTest {
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisAdjustmentId"         : 101,
            "nomisAdjustmentCategory"   : "$nomisAdjustCategory",
            "adjustmentId"      : "$adjustId",
            "label"       : "2022-01-01",
            "mappingType" : "SENTENCING_CREATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      // Emulate calling service simultaneously twice by disabling the duplicate check
      // Note: the spy is automatically reset by ResetMocksTestExecutionListener
      whenever(repository.findById(adjustId)).thenReturn(null)

      val responseBody =
        webTestClient.post().uri("/mapping/sentencing/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "nomisAdjustmentId"         : 102,
            "nomisAdjustmentCategory"   : "$nomisAdjustCategory",
            "adjustmentId"      : "$adjustId",
            "label"       : "2022-01-01",
            "mappingType" : "SENTENCING_CREATED"
          }""",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<SentencingAdjustmentMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Sentencing mapping already exists, detected by org.springframework.dao.DuplicateKeyException")
        assertThat(errorCode).isEqualTo(1409)
      }
    }

    @Test
    fun `create rejects bad filter data - missing mapping type`() {
      assertThat(
        webTestClient.post().uri("/mapping/sentencing/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "nomisAdjustmentId"         : $nomisAdjustId,
            "nomisAdjustmentCategory"       : "$nomisAdjustCategory",
            "label"       : "2022-01-01",
            "adjustmentId" : "$adjustId"
          }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage,
      ).contains("missing (therefore NULL) value for creator parameter mappingType which is a non-nullable type")
    }

    @Test
    fun `create rejects bad filter data - mapping max 20`() {
      assertThat(
        webTestClient.post().uri("/mapping/sentencing/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "nomisAdjustmentId"         : $nomisAdjustId,
            "nomisAdjustmentCategory"       : "$nomisAdjustCategory",
            "label"       : "2022-01-01",
            "adjustmentId" : "$adjustId",
            "mappingType" : "MASSIVELY_LONG_PROPERTY_SENTENCING_CREATED"
          }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage,
      ).contains("mappingType has a maximum length of 20")
    }

    @Test
    fun `create rejects bad filter data - sentence adjustment property must be present (Long)`() {
      assertThat(
        webTestClient.post().uri("/mapping/sentencing/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "nomisAdjustmentId"         : $nomisAdjustId,
            "nomisAdjustmentCategory"       : "$nomisAdjustCategory",
            "label"       : "2022-01-01",
            "mappingType" : "SENTENCING_CREATED"
          }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage,
      )
        .contains("Validation failure: JSON decoding error")
        .contains("adjustmentId")
    }
  }

  @DisplayName("GET /mapping/sentencing/adjustments/nomis-adjustment-id/{nomisAdjustId}/nomis-adjustment-category/{nomisAdjustCategory}")
  @Nested
  inner class GetNomisMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/sentencing/adjustments/nomis-adjustment-category/$nomisAdjustCategory/nomis-adjustment-id/$nomisAdjustId")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/sentencing/adjustments/nomis-adjustment-category/$nomisAdjustCategory/nomis-adjustment-id/$nomisAdjustId")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/sentencing/adjustments/nomis-adjustment-category/$nomisAdjustCategory/nomis-adjustment-id/$nomisAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping =
        webTestClient.get().uri("/mapping/sentencing/adjustments/nomis-adjustment-category/$nomisAdjustCategory/nomis-adjustment-id/$nomisAdjustId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody(SentencingAdjustmentMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping.nomisAdjustmentId).isEqualTo(nomisAdjustId)
      assertThat(mapping.nomisAdjustmentCategory).isEqualTo(nomisAdjustCategory)
      assertThat(mapping.adjustmentId).isEqualTo(adjustId)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED.name)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/sentencing/adjustments/adjustment-id/99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: Sentencing adjustmentId id=99999")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/sentencing/adjustments/nomis-adjustment-category/$nomisAdjustCategory/nomis-adjustment-id/$nomisAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /mapping/sentencing/adjustments/migrated/latest")
  @Nested
  inner class GeMappingMigratedLatestTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/sentencing/adjustments/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/sentencing/adjustments/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/sentencing/adjustments/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createSentenceAdjustmentMapping(
              nomisAdjustmentId = 10,
              nomisAdjustmentCategory = "SENTENCE",
              adjustmentId = "10",
              label = "2022-01-01T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createSentenceAdjustmentMapping(
              nomisAdjustmentId = 20,
              nomisAdjustmentCategory = "SENTENCE",
              adjustmentId = "20",
              label = "2022-01-02T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createSentenceAdjustmentMapping(
              nomisAdjustmentId = 1,
              nomisAdjustmentCategory = "SENTENCE",
              adjustmentId = "1",
              label = "2022-01-02T10:00:00",
              mappingType = MIGRATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createSentenceAdjustmentMapping(
              nomisAdjustmentId = 99,
              nomisAdjustmentCategory = "SENTENCE",
              adjustmentId = "199",
              label = "whatever",
              mappingType = SENTENCING_CREATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/sentencing/adjustments/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody(SentencingAdjustmentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisAdjustmentId).isEqualTo(1)
      assertThat(mapping.nomisAdjustmentCategory).isEqualTo("SENTENCE")
      assertThat(mapping.adjustmentId).isEqualTo("1")
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo("MIGRATED")
      assertThat(mapping.whenCreated)
        .isCloseTo(LocalDateTime.now(), byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createSentenceAdjustmentMapping(
              nomisAdjustmentId = 77,
              nomisAdjustmentCategory = "SENTENCE",
              adjustmentId = "77",
              label = "whatever",
              mappingType = SENTENCING_CREATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/sentencing/adjustments/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }

  @DisplayName("GET /mapping/sentencing/adjustments/adjustment-id/{sentenceAdjustId}")
  @Nested
  inner class GetSentencingAdjustmentMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/sentencing/adjustments/adjustment-id/$adjustId")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/sentencing/adjustments/adjustment-id/$adjustId")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/sentencing/adjustments/adjustment-id/$adjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/sentencing/adjustments/adjustment-id/$adjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody(SentencingAdjustmentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisAdjustmentId).isEqualTo(nomisAdjustId)
      assertThat(mapping.nomisAdjustmentCategory).isEqualTo(nomisAdjustCategory)
      assertThat(mapping.adjustmentId).isEqualTo(adjustId)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED.name)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/sentencing/adjustments/adjustment-id/765")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: Sentencing adjustmentId id=765")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/sentencing/adjustments/adjustment-id/$adjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("DELETE /mapping/sentencing/adjustments")
  @Nested
  inner class DeleteMappingsTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/sentencing/adjustments")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete mapping success`() {
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/sentencing/adjustments/nomis-adjustment-category/$nomisAdjustCategory/nomis-adjustment-id/$nomisAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/mapping/sentencing/adjustments/nomis-adjustment-category/$nomisAdjustCategory/nomis-adjustment-id/$nomisAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `delete incentive mappings - migrated mappings only`() {
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createSentenceAdjustmentMapping(
              nomisAdjustmentId = 333,
              nomisAdjustmentCategory = "SENTENCE",
              adjustmentId = "222",
              mappingType = MIGRATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.delete().uri("/mapping/sentencing/adjustments?onlyMigrated=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/mapping/sentencing/adjustments/nomis-adjustment-category/$nomisAdjustCategory/nomis-adjustment-id/$nomisAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk

      webTestClient.get().uri("/mapping/sentencing/adjustments/nomis-adjustment-category/$nomisAdjustCategory/nomis-adjustment-id/222")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @DisplayName("DELETE /mapping/sentencing/adjustments/adjustment-id/{sentenceAdjustId}")
  @Nested
  inner class DeleteMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/sentencing/adjustments/adjustment-id/999")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/sentencing/adjustments/adjustment-id/999")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/sentencing/adjustments/adjustment-id/999")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete specific mapping success`() {
      // create mapping
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      // it is present after creation by incentive id
      webTestClient.get().uri("/mapping/sentencing/adjustments/adjustment-id/$adjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
      // it is also present after creation by nomis id
      webTestClient.get().uri("/mapping/sentencing/adjustments/nomis-adjustment-category/$nomisAdjustCategory/nomis-adjustment-id/$nomisAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk

      // delete mapping
      webTestClient.delete().uri("/mapping/sentencing/adjustments/adjustment-id/$adjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      // no longer present by incentive id
      webTestClient.get().uri("/mapping/sentencing/adjustments/adjustment-id/$adjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
      // and also no longer present by nomis id
      webTestClient.get().uri("/mapping/sentencing/adjustments/nomis-adjustment-category/$nomisAdjustCategory/nomis-adjustment-id/$nomisAdjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    internal fun `delete is idempotent`() {
      // create mapping
      webTestClient.post().uri("/mapping/sentencing/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createSentenceAdjustmentMapping()))
        .exchange()
        .expectStatus().isCreated

      // delete mapping
      webTestClient.delete().uri("/mapping/sentencing/adjustments/adjustment-id/$adjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent
      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/sentencing/adjustments/adjustment-id/$adjustId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @DisplayName("GET /mapping/sentencing/adjustments/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationIdTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/sentencing/adjustments/migration-id/2022-01-01T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/sentencing/adjustments/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get sentence adjustment mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/sentencing/adjustments/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get sentence adjustment mappings by migration id success`() {
      (1L..4L).forEach {
        postCreateSentenceAdjustmentMappingRequest(
          nomisAdjustmentId = it,
          nomisAdjustmentCategory = "SENTENCE",
          adjustmentId = "$it",
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      (5L..9L).forEach {
        postCreateSentenceAdjustmentMappingRequest(
          nomisAdjustmentId = it,
          nomisAdjustmentCategory = "SENTENCE",
          adjustmentId = "$it",
          label = "2099-01-01",
          mappingType = "MIGRATED",
        )
      }
      postCreateSentenceAdjustmentMappingRequest(
        nomisAdjustmentId = 12,
        nomisAdjustmentCategory = "SENTENCE",
        adjustmentId = "12",
        mappingType = SENTENCING_CREATED.name,
      )

      webTestClient.get().uri("/mapping/sentencing/adjustments/migration-id/2022-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..nomisAdjustmentId").value(
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
    fun `get sentence adjustment mappings by migration id - no records exist`() {
      (1L..4L).forEach {
        postCreateSentenceAdjustmentMappingRequest(
          nomisAdjustmentId = it,
          nomisAdjustmentCategory = "SENTENCE",
          adjustmentId = "$it",
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }

      webTestClient.get().uri("/mapping/sentencing/adjustments/migration-id/2044-01-01")
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
          nomisAdjustmentId = it,
          nomisAdjustmentCategory = "SENTENCE",
          adjustmentId = "$it",
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/sentencing/adjustments/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("sort", "nomisAdjustmentId,asc")
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
          nomisAdjustmentId = it,
          nomisAdjustmentCategory = "SENTENCE",
          adjustmentId = "$it",
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/sentencing/adjustments/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .queryParam("sort", "nomisAdjustmentId,asc")
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
