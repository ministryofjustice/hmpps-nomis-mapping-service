package uk.gov.justice.digital.hmpps.nomismappingservice.courtsentencing

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
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

private const val DPS_TERM_ID = "dps123"
private const val NOMIS_BOOKING_ID = 12345L
private const val NOMIS_SENTENCE_SEQUENCE = 2
private const val NOMIS_TERM_SEQUENCE = 4
private const val EXISTING_NOMIS_SENTENCE_SEQUENCE = 1
private const val EXISTING_NOMIS_TERM_SEQUENCE = 3
private const val EXISTING_DPS_TERM_ID = "dps456"
class SentenceTermResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: SentenceTermMappingRepository

  @Nested
  @DisplayName("GET /mapping/court-sentencing/sentence-terms/dps-term-id/{dpsTermId}")
  inner class GetMappingByDpsId {
    lateinit var sentenceTermMapping: SentenceTermMapping

    @BeforeEach
    fun setUp() = runTest {
      sentenceTermMapping = repository.save(
        SentenceTermMapping(
          dpsTermId = DPS_TERM_ID,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQUENCE,
          nomisTermSequence = NOMIS_TERM_SEQUENCE,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceTermMappingType.MIGRATED,
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
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/${sentenceTermMapping.dpsTermId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/${sentenceTermMapping.dpsTermId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/${sentenceTermMapping.dpsTermId}")
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
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence term mapping not found with dpsTermId =DOESNOTEXIST")
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/${sentenceTermMapping.dpsTermId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisBookingId").isEqualTo(NOMIS_BOOKING_ID)
          .jsonPath("nomisSentenceSequence").isEqualTo(NOMIS_SENTENCE_SEQUENCE)
          .jsonPath("dpsTermId").isEqualTo(sentenceTermMapping.dpsTermId)
          .jsonPath("mappingType").isEqualTo(sentenceTermMapping.mappingType.name)
          .jsonPath("label").isEqualTo(sentenceTermMapping.label!!)
          .jsonPath("whenCreated").value<String> {
            Assertions.assertThat(LocalDateTime.parse(it))
              .isCloseTo(LocalDateTime.now(), Assertions.within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/court-sentencing/sentence-terms/nomis-booking-id/{bookingId}/nomis-sentence-sequence/{sentenceSequence}/nomis-term-sequence/{termSequence}")
  inner class GetMappingByNomisId {
    lateinit var mapping: SentenceTermMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        SentenceTermMapping(
          dpsTermId = DPS_TERM_ID,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQUENCE,
          nomisTermSequence = NOMIS_TERM_SEQUENCE,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceTermMappingType.MIGRATED,
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
          .uri("/mapping/court-sentencing/sentence-terms/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/${mapping.nomisSentenceSequence}/nomis-term-sequence/${mapping.nomisTermSequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentence-terms/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/${mapping.nomisSentenceSequence}/nomis-term-sequence/${mapping.nomisTermSequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentence-terms/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/${mapping.nomisSentenceSequence}/nomis-term-sequence/${mapping.nomisTermSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return 404 when mapping does not exist for sentence sequence`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentence-terms/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/78/nomis-term-sequence/${mapping.nomisTermSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence term mapping not found with nomisBookingId =12345, nomisSentenceSeq =78, nomisTermSeq =4")
      }

      @Test
      fun `will return 404 when mapping does not exist for booking id`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentence-terms/nomis-booking-id/8989/nomis-sentence-sequence/${mapping.nomisSentenceSequence}/nomis-term-sequence/${mapping.nomisTermSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence term mapping not found with nomisBookingId =8989, nomisSentenceSeq =2, nomisTermSeq =4")
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentence-terms/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/${mapping.nomisSentenceSequence}/nomis-term-sequence/${mapping.nomisTermSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisBookingId").isEqualTo(mapping.nomisBookingId)
          .jsonPath("nomisSentenceSequence").isEqualTo(mapping.nomisSentenceSequence)
          .jsonPath("dpsTermId").isEqualTo(mapping.dpsTermId)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            Assertions.assertThat(LocalDateTime.parse(it))
              .isCloseTo(LocalDateTime.now(), Assertions.within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/court-sentencing/sentence-terms")
  inner class CreateMapping {
    private lateinit var existingMapping: SentenceTermMapping
    private val mapping = SentenceTermMappingDto(
      dpsTermId = DPS_TERM_ID,
      nomisBookingId = NOMIS_BOOKING_ID,
      nomisSentenceSequence = NOMIS_SENTENCE_SEQUENCE,
      nomisTermSequence = NOMIS_TERM_SEQUENCE,
      label = "2023-01-01T12:45:12",
      mappingType = SentenceTermMappingType.DPS_CREATED,
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        SentenceTermMapping(
          dpsTermId = EXISTING_DPS_TERM_ID,
          nomisSentenceSequence = EXISTING_NOMIS_SENTENCE_SEQUENCE,
          nomisTermSequence = EXISTING_NOMIS_TERM_SEQUENCE,
          nomisBookingId = NOMIS_BOOKING_ID,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceTermMappingType.MIGRATED,
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
        webTestClient.post()
          .uri("/mapping/court-sentencing/sentence-terms")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/sentence-terms")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/sentence-terms")
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
          .uri("/mapping/court-sentencing/sentence-terms")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMapping =
          repository.findById(
            id = mapping.dpsTermId,
          )!!

        Assertions.assertThat(createdMapping.whenCreated)
          .isCloseTo(LocalDateTime.now(), Assertions.within(10, ChronoUnit.SECONDS))
        Assertions.assertThat(createdMapping.nomisBookingId).isEqualTo(mapping.nomisBookingId)
        Assertions.assertThat(createdMapping.nomisSentenceSequence).isEqualTo(mapping.nomisSentenceSequence)
        Assertions.assertThat(createdMapping.nomisTermSequence).isEqualTo(mapping.nomisTermSequence)
        Assertions.assertThat(createdMapping.dpsTermId).isEqualTo(mapping.dpsTermId)
        Assertions.assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        Assertions.assertThat(createdMapping.label).isEqualTo(mapping.label)
      }

      @Test
      fun `can post and then get new and existing mapping`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/sentence-terms")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                 {
                  "nomisBookingId": $NOMIS_BOOKING_ID,
                  "nomisSentenceSequence": $NOMIS_SENTENCE_SEQUENCE,
                  "nomisTermSequence": $NOMIS_TERM_SEQUENCE,
                  "dpsTermId": "$DPS_TERM_ID"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        webTestClient.get()
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/$DPS_TERM_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/$EXISTING_DPS_TERM_ID")
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
          .uri("/mapping/court-sentencing/sentence-terms")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisBookingId": $NOMIS_BOOKING_ID,
                  "nomisSentenceSequence": $NOMIS_SENTENCE_SEQUENCE,
                  "nomisTermSequence": $NOMIS_TERM_SEQUENCE,
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
            Assertions.assertThat(it).contains("not one of the values accepted for Enum class: [DPS_CREATED, MIGRATED, NOMIS_CREATED]")
          }
      }

      @Test
      fun `returns 400 when DPS id is missing`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/sentence-terms")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisSentenceSequence": 1,
                  "nomisTermSequence": 1,
                  "nomisBookingId": 54321
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            Assertions.assertThat(it).contains("missing (therefore NULL) value for creator parameter dpsTermId")
          }
      }

      @Test
      fun `returns 400 when NOMIS id is missing`() {
        webTestClient.post()
          .uri("/mapping/court-sentencing/sentence-terms")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "dpsTermId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            Assertions.assertThat(it).contains("Missing required creator property 'nomisBookingId'")
          }
      }

      @Test
      fun `returns 409 if nomis id already exist`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/court-sentencing/sentence-terms")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              SentenceTermMappingDto(
                nomisSentenceSequence = existingMapping.nomisSentenceSequence,
                nomisBookingId = existingMapping.nomisBookingId,
                nomisTermSequence = existingMapping.nomisTermSequence,
                dpsTermId = "DPS888",
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
          Assertions.assertThat(this.moreInfo.existing)
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisSentenceSequence", existingMapping.nomisSentenceSequence)
            .containsEntry("nomisTermSequence", existingMapping.nomisTermSequence)
            .containsEntry("dpsTermId", existingMapping.dpsTermId)
          Assertions.assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisSentenceSequence", existingMapping.nomisSentenceSequence)
            .containsEntry("nomisTermSequence", existingMapping.nomisTermSequence)
            .containsEntry("dpsTermId", "DPS888")
        }
      }

      @Test
      fun `returns 409 if dps id already exists`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/court-sentencing/sentence-terms")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              SentenceTermMappingDto(
                nomisBookingId = NOMIS_BOOKING_ID,
                nomisSentenceSequence = NOMIS_SENTENCE_SEQUENCE,
                nomisTermSequence = NOMIS_TERM_SEQUENCE,
                dpsTermId = existingMapping.dpsTermId,
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
          Assertions.assertThat(this.moreInfo.existing)
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisSentenceSequence", existingMapping.nomisSentenceSequence)
            .containsEntry("nomisTermSequence", existingMapping.nomisTermSequence)
            .containsEntry("dpsTermId", existingMapping.dpsTermId)
          Assertions.assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisBookingId", NOMIS_BOOKING_ID.toInt())
            .containsEntry("nomisSentenceSequence", NOMIS_SENTENCE_SEQUENCE)
            .containsEntry("nomisTermSequence", NOMIS_TERM_SEQUENCE)
            .containsEntry("dpsTermId", existingMapping.dpsTermId)
        }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/court-sentencing/sentence-terms/dps-term-id/{termId}")
  inner class DeleteMappingByDpsId {
    lateinit var mapping: SentenceTermMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        SentenceTermMapping(
          dpsTermId = DPS_TERM_ID,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQUENCE,
          nomisTermSequence = NOMIS_TERM_SEQUENCE,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceTermMappingType.MIGRATED,
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
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/${mapping.dpsTermId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/${mapping.dpsTermId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/${mapping.dpsTermId}")
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
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/NOPE")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/${mapping.dpsTermId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/${mapping.dpsTermId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/${mapping.dpsTermId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/court-sentencing/sentence-terms/nomis-booking-id/{bookingId}/nomis-sentence-sequence/{sentenceSequence}/nomis-term-sequence/{termSequence}")
  inner class DeleteMappingByNomisId {
    lateinit var mapping: SentenceTermMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        SentenceTermMapping(
          dpsTermId = DPS_TERM_ID,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisSentenceSequence = NOMIS_SENTENCE_SEQUENCE,
          nomisTermSequence = NOMIS_TERM_SEQUENCE,
          label = "2023-01-01T12:45:12",
          mappingType = SentenceTermMappingType.MIGRATED,
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
          .uri("/mapping/court-sentencing/sentence-terms/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/${mapping.nomisSentenceSequence}/nomis-term-sequence/${mapping.nomisTermSequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/sentence-terms/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/${mapping.nomisSentenceSequence}/nomis-term-sequence/${mapping.nomisTermSequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/sentence-terms/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/${mapping.nomisSentenceSequence}/nomis-term-sequence/${mapping.nomisTermSequence}")
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
          .uri("/mapping/court-sentencing/sentence-terms/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/33/nomis-term-sequence/${mapping.nomisTermSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/${mapping.dpsTermId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        // delete using nomis id
        webTestClient.delete()
          .uri("/mapping/court-sentencing/sentence-terms/nomis-booking-id/${mapping.nomisBookingId}/nomis-sentence-sequence/${mapping.nomisSentenceSequence}/nomis-term-sequence/${mapping.nomisTermSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/court-sentencing/sentence-terms/dps-term-id/${mapping.dpsTermId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }
}
