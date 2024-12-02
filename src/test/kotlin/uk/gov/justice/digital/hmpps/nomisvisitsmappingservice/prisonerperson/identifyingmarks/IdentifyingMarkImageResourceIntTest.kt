package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonerperson.identifyingmarks

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkImageMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkImageMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkImageMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkImageMappingType.NOMIS_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.IdentifyingMarkImageMappingDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

class IdentifyingMarkImageResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var identifyingMarkImageMappingRepository: IdentifyingMarkImageMappingRepository

  @Nested
  @DisplayName("GET /mapping/prisonperson/nomis-offender-image-id/{nomisId}")
  inner class GetIdentifyingMarkImageMappingsByNomisId {
    lateinit var mapping: IdentifyingMarkImageMapping

    @BeforeEach
    fun setUp() = runTest {
      IdentifyingMarkImageMapping(
        nomisOffenderImageId = 1,
        dpsId = UUID.randomUUID(),
        nomisBookingId = 1,
        nomisMarksSequence = 1,
        offenderNo = "A1234AA",
        label = "some_label",
        whenCreated = LocalDateTime.now(),
        mappingType = NOMIS_CREATED,
      ).also {
        mapping = identifyingMarkImageMappingRepository.save(it)
      }
    }

    @AfterEach
    fun tearDown() = runTest {
      identifyingMarkImageMappingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-offender-image-id/${mapping.nomisOffenderImageId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-offender-image-id/${mapping.nomisOffenderImageId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-offender-image-id/${mapping.nomisOffenderImageId}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should return identifying mark image mapping by NOMIS id`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-offender-image-id/${mapping.nomisOffenderImageId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisOffenderImageId").isEqualTo(mapping.nomisOffenderImageId)
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
          .uri("/mapping/prisonperson/nomis-offender-image-id/9999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/prisonperson/dps-image-id/{dpsId}")
  inner class GetIdentifyingMarkMappingsByDpsId {
    private lateinit var mapping: IdentifyingMarkImageMapping

    @BeforeEach
    fun setUp() = runTest {
      IdentifyingMarkImageMapping(
        nomisOffenderImageId = 1,
        nomisBookingId = 1,
        nomisMarksSequence = 1,
        dpsId = UUID.randomUUID(),
        offenderNo = "A1234AA",
        label = "some_label",
        whenCreated = LocalDateTime.now(),
        mappingType = NOMIS_CREATED,
      ).also {
        mapping = identifyingMarkImageMappingRepository.save(it)
      }
    }

    @AfterEach
    fun tearDown() = runTest {
      identifyingMarkImageMappingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-image-id/${mapping.dpsId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-image-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-image-id/${mapping.dpsId}")
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
          .uri("/mapping/prisonperson/dps-image-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisOffenderImageId").isEqualTo(mapping.nomisOffenderImageId)
          .jsonPath("nomisBookingId").isEqualTo(mapping.nomisBookingId)
          .jsonPath("nomisMarksSequence").isEqualTo(mapping.nomisMarksSequence)
          .jsonPath("dpsId").isEqualTo(mapping.dpsId.toString())
          .jsonPath("offenderNo").isEqualTo(mapping.offenderNo)
          .jsonPath("label").isEqualTo(mapping.label)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType)
          .jsonPath("whenCreated").value<String> { assertThat(it).startsWith("${LocalDate.now()}") }
      }

      @Test
      fun `should return not found if no mappings`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-image-id/${UUID.randomUUID()}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/prisonperson/identifying-mark-image")
  inner class CreateMapping {
    private lateinit var existingMapping: IdentifyingMarkImageMapping
    private val mapping = IdentifyingMarkImageMappingDto(
      nomisOffenderImageId = 2,
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
      existingMapping = identifyingMarkImageMappingRepository.save(
        IdentifyingMarkImageMapping(
          nomisOffenderImageId = 1,
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
      identifyingMarkImageMappingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/prisonperson/identifying-mark-image")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/prisonperson/identifying-mark-image")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/prisonperson/identifying-mark-image")
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

        with(identifyingMarkImageMappingRepository.findById(mapping.nomisOffenderImageId)!!) {
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

        webTestClient.getNomisMapping(mapping.nomisOffenderImageId)
          .expectStatus().isOk

        webTestClient.getNomisMapping(existingMapping.nomisOffenderImageId)
          .expectStatus().isOk

        webTestClient.getDpsMapping(mapping.dpsId)
          .expectStatus().isOk

        webTestClient.getDpsMapping(existingMapping.dpsId)
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      private val requestedDpsId = UUID.randomUUID()
      private val requestedNomisOffenderImageId = Random.nextLong()
      private val jsonRequest =
        //language=JSON
        """
        {
          "nomisOffenderImageId": $requestedNomisOffenderImageId,
          "nomisBookingId": 2,
          "nomisMarksSequence": 1,
          "dpsId": "$requestedDpsId",
          "offenderNo": "A1234AA",
          "label": null,
          "mappingType": "NOMIS_CREATED",
          "whenCreated": "${LocalDateTime.now()}"
        }
        """.trimIndent()

      private fun String.with(field: String, value: Any) = this.replaceFirst(""""$field":.*?,""".toRegex(), """"$field": $value,""")

      @Test
      fun `returns 400 when nomis image ID is missing`() {
        webTestClient.postMapping(jsonRequest.with("nomisOffenderImageId", """null"""))
          .expectStatus().isBadRequest
      }

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
        webTestClient.postMapping(jsonRequest.with("nomisOffenderImageId", existingMapping.nomisOffenderImageId))
          .expectStatus().isEqualTo(409)
          .expectBody<DuplicateMappingErrorResponse<IdentifyingMarkImageMappingDto>>()
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(errorCode).isEqualTo(1409)
              with(moreInfo.existing) {
                assertThat(nomisOffenderImageId).isEqualTo(existingMapping.nomisOffenderImageId)
                assertThat(dpsId).isEqualTo(existingMapping.dpsId)
              }
              with(moreInfo.duplicate) {
                assertThat(nomisOffenderImageId).isEqualTo(existingMapping.nomisOffenderImageId)
                assertThat(dpsId).isEqualTo(requestedDpsId)
              }
            }
          }
      }

      @Test
      fun `returns 409 if dps id already exist`() {
        webTestClient.postMapping(jsonRequest.with("dpsId", """"${existingMapping.dpsId}""""))
          .expectStatus().isEqualTo(409)
          .expectBody<DuplicateMappingErrorResponse<IdentifyingMarkImageMappingDto>>()
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(errorCode).isEqualTo(1409)
              with(moreInfo.existing) {
                assertThat(nomisOffenderImageId).isEqualTo(existingMapping.nomisOffenderImageId)
                assertThat(dpsId).isEqualTo(existingMapping.dpsId)
              }
              with(moreInfo.duplicate) {
                assertThat(nomisOffenderImageId).isEqualTo(requestedNomisOffenderImageId)
                assertThat(dpsId).isEqualTo(existingMapping.dpsId)
              }
            }
          }
      }
    }

    private fun WebTestClient.postMapping(requestBody: Any): WebTestClient.ResponseSpec =
      post()
        .uri("/mapping/prisonperson/identifying-mark-image")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(requestBody))
        .exchange()

    private fun WebTestClient.getNomisMapping(id: Long) =
      get()
        .uri("/mapping/prisonperson/nomis-offender-image-id/$id")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
        .exchange()

    private fun WebTestClient.getDpsMapping(id: UUID) =
      get()
        .uri("/mapping/prisonperson/dps-image-id/$id")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
        .exchange()
  }
}
