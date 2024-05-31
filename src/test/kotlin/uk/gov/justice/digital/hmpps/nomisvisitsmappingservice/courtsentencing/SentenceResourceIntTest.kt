package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val DPS_SENTENCE_ID = "dps123"
private const val NOMIS_BOOKING_ID = 12345L
private const val NOMIS_SENTENCE_SEQUENCE = 2
private const val EXISTING_NOMIS_SENTENCE_SEQUENCE = 1
private const val EXISTING_DPS_SENTENCE_ID = "dps456"
class SentenceResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: SentenceMappingRepository

  @Nested
  @DisplayName("GET /mapping/court-sentencing/sentences/dps-sentence-id/{dpsSentenceId}")
  inner class GetMappingByDpsId {
    lateinit var sentenceMapping: SentenceMapping

    @BeforeEach
    fun setUp() = runTest {
      sentenceMapping = repository.save(
        SentenceMapping(
          dpsSentenceId = DPS_SENTENCE_ID,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQUENCE,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceMappingType.MIGRATED,
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
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/${sentenceMapping.dpsSentenceId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/${sentenceMapping.dpsSentenceId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/${sentenceMapping.dpsSentenceId}")
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
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("DPS Sentence Id =DOESNOTEXIST")
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/${sentenceMapping.dpsSentenceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisBookingId").isEqualTo(NOMIS_BOOKING_ID)
          .jsonPath("nomisSentenceSequence").isEqualTo(NOMIS_SENTENCE_SEQUENCE)
          .jsonPath("dpsSentenceId").isEqualTo(sentenceMapping.dpsSentenceId)
          .jsonPath("mappingType").isEqualTo(sentenceMapping.mappingType.name)
          .jsonPath("label").isEqualTo(sentenceMapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it))
              .isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/court-sentencing/sentences")
  inner class CreateMapping {
    private lateinit var existingMapping: SentenceMapping
    private val mapping = SentenceAllMappingDto(
      dpsSentenceId = DPS_SENTENCE_ID,
      nomisBookingId = NOMIS_BOOKING_ID,
      nomisSentenceSequence = NOMIS_SENTENCE_SEQUENCE,
      label = "2023-01-01T12:45:12",
      mappingType = SentenceMappingType.DPS_CREATED,
      // TODO charges
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        SentenceMapping(
          dpsSentenceId = EXISTING_DPS_SENTENCE_ID,
          nomisSentenceSequence = EXISTING_NOMIS_SENTENCE_SEQUENCE,
          nomisBookingId = NOMIS_BOOKING_ID,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceMappingType.MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
      // TODO courtChargeRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/sentences")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/sentences")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/sentences")
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
          .uri("/mapping/court-sentencing/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMapping =
          repository.findById(
            id = mapping.dpsSentenceId,
          )!!

        assertThat(createdMapping.whenCreated)
          .isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisBookingId).isEqualTo(mapping.nomisBookingId)
        assertThat(createdMapping.nomisSentenceSequence).isEqualTo(mapping.nomisSentenceSequence)
        assertThat(createdMapping.dpsSentenceId).isEqualTo(mapping.dpsSentenceId)
        assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(createdMapping.label).isEqualTo(mapping.label)
      }

      @Test
      fun `can post and then get new and existing mapping`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                 {
                  "nomisBookingId": $NOMIS_BOOKING_ID,
                  "nomisSentenceSequence": $NOMIS_SENTENCE_SEQUENCE,
                  "dpsSentenceId": "$DPS_SENTENCE_ID"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        webTestClient.get()
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/$DPS_SENTENCE_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/$EXISTING_DPS_SENTENCE_ID")
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
          .uri("/mapping/court-sentencing/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisBookingId": $NOMIS_BOOKING_ID,
                  "nomisSentenceSequence": $NOMIS_SENTENCE_SEQUENCE,
                  "dpsSentenceId": "e52d7268-6e10-41a8-a0b9-2319b32520d6",
                  "mappingType": "INVALID_TYPE"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("not one of the values accepted for Enum class: [DPS_CREATED, MIGRATED, NOMIS_CREATED]")
          }
      }

      @Test
      fun `returns 400 when DPS id is missing`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisSentenceSequence": 1,
                  "nomisBookingId": 54321
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("missing (therefore NULL) value for creator parameter dpsSentenceId")
          }
      }

      @Test
      fun `returns 400 when NOMIS id is missing`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "dpsSentenceId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Missing required creator property 'nomisBookingId'")
          }
      }

      @Test
      fun `returns 409 if nomis id already exist`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/court-sentencing/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              SentenceAllMappingDto(
                nomisSentenceSequence = existingMapping.nomisSentenceSequence,
                nomisBookingId = existingMapping.nomisBookingId,
                dpsSentenceId = "DPS888",
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
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisSentenceSequence", existingMapping.nomisSentenceSequence)
            .containsEntry("dpsSentenceId", existingMapping.dpsSentenceId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisSentenceSequence", existingMapping.nomisSentenceSequence)
            .containsEntry("dpsSentenceId", "DPS888")
        }
      }

      @Test
      fun `returns 409 if dps id already exists`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/court-sentencing/sentences")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              SentenceAllMappingDto(
                nomisBookingId = NOMIS_BOOKING_ID,
                nomisSentenceSequence = NOMIS_SENTENCE_SEQUENCE,
                dpsSentenceId = existingMapping.dpsSentenceId,
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
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisSentenceSequence", existingMapping.nomisSentenceSequence)
            .containsEntry("dpsSentenceId", existingMapping.dpsSentenceId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisBookingId", NOMIS_BOOKING_ID.toInt())
            .containsEntry("nomisSentenceSequence", NOMIS_SENTENCE_SEQUENCE)
            .containsEntry("dpsSentenceId", existingMapping.dpsSentenceId)
        }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/court-sentencing/sentences/dps-sentences-id/{sentenceId}")
  inner class DeleteMappingByDpsId {
    lateinit var mapping: SentenceMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        SentenceMapping(
          dpsSentenceId = DPS_SENTENCE_ID,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQUENCE,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceMappingType.MIGRATED,
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
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/${mapping.dpsSentenceId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/${mapping.dpsSentenceId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/${mapping.dpsSentenceId}")
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
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/NOPE")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/${mapping.dpsSentenceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/${mapping.dpsSentenceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/${mapping.dpsSentenceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/court-sentencing/sentences/nomis-booking-id/{bookingId}/nomis-sentence-sequence/{sentenceSequence}")
  inner class DeleteMappingByNomisId {
    lateinit var mapping: SentenceMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        SentenceMapping(
          dpsSentenceId = DPS_SENTENCE_ID,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQUENCE,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceMappingType.MIGRATED,
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
          .uri("/mapping/court-sentencing/sentences/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/${mapping.nomisSentenceSequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/sentences/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/${mapping.nomisSentenceSequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/sentences/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/${mapping.nomisSentenceSequence}")
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
          .uri("/mapping/court-sentencing/sentences/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/33")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/${mapping.dpsSentenceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        // delete using nomis id
        webTestClient.delete()
          .uri("/mapping/court-sentencing/sentences/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/${mapping.nomisSentenceSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/court-sentencing/sentences/dps-sentence-id/${mapping.dpsSentenceId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }
}
