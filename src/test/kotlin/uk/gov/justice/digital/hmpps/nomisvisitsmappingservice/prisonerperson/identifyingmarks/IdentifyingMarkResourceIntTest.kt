package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonerperson.identifyingmarks

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkMappingType.NOMIS_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.IdentifyingMarkMappingDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

class IdentifyingMarkResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var identifyingMarkMappingRepository: IdentifyingMarkMappingRepository

  @Nested
  @DisplayName("GET /mapping/prisonperson/nomis-booking-id/{bookingId}/identifying-mark-sequence/{sequence}")
  inner class GetIdentifyingMarkMappingsByNomisId {
    lateinit var mapping: IdentifyingMarkMapping

    @BeforeEach
    fun setUp() = runTest {
      IdentifyingMarkMapping(
        nomisBookingId = 1,
        nomisMarksSequence = 1,
        dpsId = UUID.randomUUID(),
        offenderNo = "A1234AA",
        label = "some_label",
        whenCreated = LocalDateTime.now(),
        mappingType = NOMIS_CREATED,
      ).also {
        mapping = identifyingMarkMappingRepository.save(it)
      }
    }

    @AfterEach
    fun tearDown() = runTest {
      identifyingMarkMappingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-booking-id/${mapping.nomisBookingId}/identifying-mark-sequence/${mapping.nomisMarksSequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-booking-id/${mapping.nomisBookingId}/identifying-mark-sequence/${mapping.nomisMarksSequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-booking-id/${mapping.nomisBookingId}/identifying-mark-sequence/${mapping.nomisMarksSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should return identifying mark mapping by NOMIS id`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-booking-id/${mapping.nomisBookingId}/identifying-mark-sequence/${mapping.nomisMarksSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisBookingId").isEqualTo(mapping.nomisBookingId)
          .jsonPath("nomisMarksSequence").isEqualTo(mapping.nomisMarksSequence)
          .jsonPath("dpsId").isEqualTo(mapping.dpsId.toString())
          .jsonPath("offenderNo").isEqualTo(mapping.offenderNo)
          .jsonPath("label").isEqualTo(mapping.label)
          .jsonPath("whenCreated").value<String> { assertThat(it).startsWith("${LocalDate.now()}") }
          .jsonPath("mappingType").isEqualTo(mapping.mappingType)
      }

      @Test
      fun `should return not found if no mapping`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-booking-id/999/identifying-mark-sequence/999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/prisonperson/dps-identifying-mark-id/{dpsId}")
  inner class GetIdentifyingMarkMappingsByDpsId {
    private lateinit var mapping1: IdentifyingMarkMapping
    private lateinit var mapping2: IdentifyingMarkMapping

    @BeforeEach
    fun setUp() = runTest {
      IdentifyingMarkMapping(
        nomisBookingId = 1,
        nomisMarksSequence = 1,
        dpsId = UUID.randomUUID(),
        offenderNo = "A1234AA",
        label = "some_label",
        whenCreated = LocalDateTime.now(),
        mappingType = NOMIS_CREATED,
      ).also {
        mapping1 = identifyingMarkMappingRepository.save(it)
      }

      IdentifyingMarkMapping(
        nomisBookingId = 2,
        nomisMarksSequence = 1,
        dpsId = mapping1.dpsId,
        offenderNo = "A1234AA",
        label = "some_label",
        whenCreated = LocalDateTime.now(),
        mappingType = NOMIS_CREATED,
      ).also {
        mapping2 = identifyingMarkMappingRepository.save(it)
      }
    }

    @AfterEach
    fun tearDown() = runTest {
      identifyingMarkMappingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-identifying-mark-id/${mapping1.dpsId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-identifying-mark-id/${mapping1.dpsId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-identifying-mark-id/${mapping1.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should return identifying mark mappings by NOMIS id`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-identifying-mark-id/${mapping1.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody<List<IdentifyingMarkMappingDto>>()
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(this).extracting(
                "nomisBookingId",
                "nomisMarksSequence",
                "dpsId",
                "offenderNo",
                "label",
                "mappingType",
              )
                .containsExactlyInAnyOrder(
                  tuple(
                    mapping1.nomisBookingId,
                    mapping1.nomisMarksSequence,
                    mapping1.dpsId,
                    mapping1.offenderNo,
                    mapping1.label,
                    mapping1.mappingType,
                  ),
                  tuple(
                    mapping2.nomisBookingId,
                    mapping2.nomisMarksSequence,
                    mapping2.dpsId,
                    mapping2.offenderNo,
                    mapping2.label,
                    mapping2.mappingType,
                  ),
                )
            }
          }
      }

      @Test
      fun `should return empty list if no mappings`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-identifying-mark-id/${UUID.randomUUID()}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$").isEmpty
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/prisonperson/identifying-mark")
  inner class CreateMapping {
    private lateinit var existingMapping: IdentifyingMarkMapping
    private val mapping = IdentifyingMarkMappingDto(
      nomisBookingId = 3,
      nomisMarksSequence = 1,
      dpsId = UUID.randomUUID(),
      offenderNo = "B1234BB",
      label = "${LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)}",
      mappingType = MIGRATED,
      whenCreated = LocalDateTime.now(),
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = identifyingMarkMappingRepository.save(
        IdentifyingMarkMapping(
          nomisBookingId = 2,
          nomisMarksSequence = 1,
          dpsId = UUID.randomUUID(),
          offenderNo = "A1234AA",
          label = null,
          mappingType = NOMIS_CREATED,
          whenCreated = LocalDateTime.now(),
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      identifyingMarkMappingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/prisonperson/identifying-mark")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/prisonperson/identifying-mark")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/prisonperson/identifying-mark")
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
        webTestClient.postMapping(mapping)
          .expectStatus().isCreated

        with(identifyingMarkMappingRepository.findByNomisBookingIdAndNomisMarksSequence(mapping.nomisBookingId, mapping.nomisMarksSequence)!!) {
          assertThat(nomisBookingId).isEqualTo(mapping.nomisBookingId)
          assertThat(nomisMarksSequence).isEqualTo(mapping.nomisMarksSequence)
          assertThat(dpsId).isEqualTo(mapping.dpsId)
          assertThat(offenderNo).isEqualTo(mapping.offenderNo)
          assertThat(label).isEqualTo(mapping.label)
          assertThat(mappingType).isEqualTo(mapping.mappingType)
          assertThat(whenCreated.toLocalDate()).isEqualTo(LocalDate.now())
        }
      }

      @Test
      fun `can post and then get new and existing mapping`() {
        webTestClient.postMapping(mapping)
          .expectStatus().isCreated

        webTestClient.getNomisMapping(mapping.nomisBookingId, mapping.nomisMarksSequence)
          .expectStatus().isOk

        webTestClient.getNomisMapping(existingMapping.nomisBookingId, existingMapping.nomisMarksSequence)
          .expectStatus().isOk

        webTestClient.getDpsMapping(mapping.dpsId)
          .expectStatus().isOk

        webTestClient.getDpsMapping(existingMapping.dpsId)
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      private val requestedBookingId = Random.nextLong()
      private val requestedMarksSequence = 1L
      private val requestedDpsId = UUID.randomUUID()
      private val jsonRequest =
        //language=JSON
        """
        {
          "nomisBookingId": $requestedBookingId,
          "nomisMarksSequence": $requestedMarksSequence,
          "dpsId": "$requestedDpsId",
          "offenderNo": "A1234AA",
          "label": null,
          "mappingType": "NOMIS_CREATED",
          "whenCreated": "${LocalDateTime.now()}"
        }
        """.trimIndent()

      private fun String.with(field: String, value: Any) = this.replaceFirst(""""$field":.*?,""".toRegex(), """"$field": $value,""")

      @Test
      fun `returns 400 when nomis booking ID is missing`() {
        webTestClient.postMapping(jsonRequest.with("nomisBookingId", """null"""))
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when nomisMarksSequence is missing`() {
        webTestClient.postMapping(jsonRequest.with("nomisMarksSequence", """null"""))
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when dpsId is missing`() {
        webTestClient.postMapping(jsonRequest.with("dpsId", """null"""))
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when offenderNo is missing`() {
        webTestClient.postMapping(jsonRequest.with("offenderNo", """null"""))
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when mapping type is missing`() {
        webTestClient.postMapping(jsonRequest.with("mappingType", """null"""))
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when mapping type is invalid`() {
        webTestClient.postMapping(jsonRequest.with("mappingType", """"INVALID""""))
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 409 if nomis offender image ids already exist`() {
        webTestClient.postMapping(
          jsonRequest
            .with("nomisBookingId", existingMapping.nomisBookingId)
            .with("nomisMarksSequence", existingMapping.nomisMarksSequence),
        )
          .expectStatus().isEqualTo(409)
          .expectBody<DuplicateMappingErrorResponse<IdentifyingMarkMappingDto>>()
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(errorCode).isEqualTo(1409)
              with(moreInfo.existing) {
                assertThat(nomisBookingId).isEqualTo(existingMapping.nomisBookingId)
                assertThat(nomisMarksSequence).isEqualTo(existingMapping.nomisMarksSequence)
                assertThat(dpsId).isEqualTo(existingMapping.dpsId)
              }
              with(moreInfo.duplicate) {
                assertThat(nomisBookingId).isEqualTo(existingMapping.nomisBookingId)
                assertThat(nomisMarksSequence).isEqualTo(existingMapping.nomisMarksSequence)
                assertThat(dpsId).isEqualTo(requestedDpsId)
              }
            }
          }
      }

      // It is possible to have many NOMIS marks mapping to a single DPS mark as this is how DPS are modelling historical mappings
      @Test
      fun `returns CREATED if dps id already exist`() {
        webTestClient.postMapping(jsonRequest.with("dpsId", """"${existingMapping.dpsId}""""))
          .expectStatus().isCreated
      }
    }

    private fun WebTestClient.postMapping(requestBody: Any): WebTestClient.ResponseSpec =
      post()
        .uri("/mapping/prisonperson/identifying-mark")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(requestBody))
        .exchange()

    private fun WebTestClient.getNomisMapping(bookingId: Long, marksSeq: Long) =
      get()
        .uri("/mapping/prisonperson/nomis-booking-id/$bookingId/identifying-mark-sequence/$marksSeq")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
        .exchange()

    private fun WebTestClient.getDpsMapping(id: UUID) =
      get()
        .uri("/mapping/prisonperson/dps-identifying-mark-id/$id")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
        .exchange()
  }
}
