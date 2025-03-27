package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.api

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing.SentenceMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing.SentenceMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing.SentenceMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders.LocationRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.LocationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.LocationMappingType
import java.time.LocalDateTime

private const val DPS_LOCATION_ID = "f4499772-2e43-4951-861d-04ad86df43fc"
private const val DPS_LOCATION_ID_TWO = "a80d46fc-c445-460d-9029-3603e9543a77"
private const val DPS_LOCATION_ID_THREE = "7a75218a-01be-4cd1-8d23-f3cab9b50b7c"
private const val NOMIS_LOCATION_ID = 2318905L
private const val NOMIS_LOCATION_ID_TWO = 8354782L
private const val NOMIS_LOCATION_ID_THREE = 5247157L

class PublicApiResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var locationRepository: LocationRepository

  @Autowired
  private lateinit var sentenceRepository: SentenceMappingRepository

  @Nested
  inner class Locations {

    @BeforeEach
    fun setUp(): Unit = runBlocking {
      storeLocationMapping(DPS_LOCATION_ID, NOMIS_LOCATION_ID)
      storeLocationMapping(DPS_LOCATION_ID_TWO, NOMIS_LOCATION_ID_TWO)
      storeLocationMapping(DPS_LOCATION_ID_THREE, NOMIS_LOCATION_ID_THREE)
    }

    @AfterEach
    internal fun deleteData() = runBlocking {
      locationRepository.deleteAll()
    }

    @DisplayName("GET /api/locations/nomis/{nomisLocationId}")
    @Nested
    inner class GetNomisLocationMapping {
      @Nested
      inner class Security {
        @Test
        fun `access unauthorised when no authority`() {
          webTestClient.get()
            .uri("/api/locations/nomis/$NOMIS_LOCATION_ID")
            .exchange()
            .expectStatus().isUnauthorized
        }

        @Test
        fun `access forbidden with no roles`() {
          webTestClient.get()
            .uri("/api/locations/nomis/$NOMIS_LOCATION_ID")
            .headers(setAuthorisation(roles = listOf()))
            .exchange()
            .expectStatus().isForbidden
        }

        @Test
        fun `access forbidden with wrong role`() {
          webTestClient.get()
            .uri("/api/locations/nomis/$NOMIS_LOCATION_ID")
            .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
            .exchange()
            .expectStatus().isForbidden
        }
      }

      @Nested
      inner class HappyPath {
        @Test
        fun `returns DPS and NOMIS ids`() {
          webTestClient.get()
            .uri("/api/locations/nomis/$NOMIS_LOCATION_ID")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DPS_MAPPING__LOCATIONS__R")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("dpsLocationId").isEqualTo(DPS_LOCATION_ID)
            .jsonPath("nomisLocationId").isEqualTo(NOMIS_LOCATION_ID)
        }
      }

      @Nested
      inner class Validation {
        @Test
        fun `returns 404 when not found`() {
          webTestClient.get()
            .uri("/api/locations/nomis/9877654")
            .headers(setAuthorisation(roles = listOf("NOMIS_DPS_MAPPING__LOCATIONS__R")))
            .exchange()
            .expectStatus().isNotFound
        }
      }
    }

    @DisplayName("POST /api/locations/nomis")
    @Nested
    inner class GetMultipleNomisLocationMappings {

      private val nomisLocationIdList = listOf(NOMIS_LOCATION_ID, NOMIS_LOCATION_ID_TWO, NOMIS_LOCATION_ID_THREE)

      @Nested
      inner class Security {
        @Test
        fun `access unauthorised when no authority`() {
          webTestClient.post()
            .uri("/api/locations/nomis")
            .bodyValue(nomisLocationIdList)
            .exchange()
            .expectStatus().isUnauthorized
        }

        @Test
        fun `access forbidden with no roles`() {
          webTestClient.post()
            .uri("/api/locations/nomis")
            .headers(setAuthorisation(roles = listOf()))
            .bodyValue(nomisLocationIdList)
            .exchange()
            .expectStatus().isForbidden
        }

        @Test
        fun `access forbidden with wrong role`() {
          webTestClient.post()
            .uri("/api/locations/nomis")
            .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
            .bodyValue(nomisLocationIdList)
            .exchange()
            .expectStatus().isForbidden
        }
      }

      @Nested
      inner class HappyPath {
        @Test
        fun `returns DPS and NOMIS ids`() {
          webTestClient.post()
            .uri("/api/locations/nomis")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DPS_MAPPING__LOCATIONS__R")))
            .bodyValue(nomisLocationIdList)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].dpsLocationId").isEqualTo(DPS_LOCATION_ID)
            .jsonPath("$[0].nomisLocationId").isEqualTo(NOMIS_LOCATION_ID)
            .jsonPath("$[1].dpsLocationId").isEqualTo(DPS_LOCATION_ID_TWO)
            .jsonPath("$[1].nomisLocationId").isEqualTo(NOMIS_LOCATION_ID_TWO)
            .jsonPath("$[2].dpsLocationId").isEqualTo(DPS_LOCATION_ID_THREE)
            .jsonPath("$[2].nomisLocationId").isEqualTo(NOMIS_LOCATION_ID_THREE)
        }
      }

      @Nested
      inner class Validation {
        @Test
        fun `returns empty list when no mappings found`() {
          webTestClient.post()
            .uri("/api/locations/nomis")
            .headers(setAuthorisation(roles = listOf("NOMIS_DPS_MAPPING__LOCATIONS__R")))
            .bodyValue(listOf(9877654))
            .exchange()
            .expectStatus().isOk
            .expectBodyList<NomisDpsLocationMapping>()
            .hasSize(0)
        }

        @Test
        fun `returns 400 when invalid request body`() {
          webTestClient.post()
            .uri("/api/locations/nomis")
            .headers(setAuthorisation(roles = listOf("NOMIS_DPS_MAPPING__LOCATIONS__R")))
            .bodyValue(listOf("abcd"))
            .exchange()
            .expectStatus().isBadRequest
        }
      }
    }

    @DisplayName("GET /api/locations/dps/{dpsLocationId}")
    @Nested
    inner class GetDpsLocationMapping {
      @Nested
      inner class Security {
        @Test
        fun `access unauthorised when no authority`() {
          webTestClient.get()
            .uri("/api/locations/dps/$DPS_LOCATION_ID")
            .exchange()
            .expectStatus().isUnauthorized
        }

        @Test
        fun `access forbidden with no roles`() {
          webTestClient.get()
            .uri("/api/locations/dps/$DPS_LOCATION_ID")
            .headers(setAuthorisation(roles = listOf()))
            .exchange()
            .expectStatus().isForbidden
        }

        @Test
        fun `access forbidden with wrong role`() {
          webTestClient.get()
            .uri("/api/locations/dps/$DPS_LOCATION_ID")
            .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
            .exchange()
            .expectStatus().isForbidden
        }
      }

      @Nested
      inner class HappyPath {
        @Test
        fun `returns DPS and NOMIS ids`() {
          webTestClient.get()
            .uri("/api/locations/dps/$DPS_LOCATION_ID")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DPS_MAPPING__LOCATIONS__R")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("dpsLocationId").isEqualTo(DPS_LOCATION_ID)
            .jsonPath("nomisLocationId").isEqualTo(NOMIS_LOCATION_ID)
        }
      }

      @Nested
      inner class Validation {
        @Test
        fun `returns 404 when not found`() {
          webTestClient.get()
            .uri("/api/locations/dps/00001111-2222-3333-4444-555566667777")
            .headers(setAuthorisation(roles = listOf("NOMIS_DPS_MAPPING__LOCATIONS__R")))
            .exchange()
            .expectStatus().isNotFound
        }
      }
    }

    @DisplayName("GET /api/locations/dps")
    @Nested
    inner class GetMultipleDpsLocationMappings {

      private val dpsLocationIdList = listOf(DPS_LOCATION_ID, DPS_LOCATION_ID_TWO, DPS_LOCATION_ID_THREE)

      @Nested
      inner class Security {
        @Test
        fun `access unauthorised when no authority`() {
          webTestClient.post()
            .uri("/api/locations/dps")
            .bodyValue(dpsLocationIdList)
            .exchange()
            .expectStatus().isUnauthorized
        }

        @Test
        fun `access forbidden with no roles`() {
          webTestClient.post()
            .uri("/api/locations/dps")
            .headers(setAuthorisation(roles = listOf()))
            .bodyValue(dpsLocationIdList)
            .exchange()
            .expectStatus().isForbidden
        }

        @Test
        fun `access forbidden with wrong role`() {
          webTestClient.post()
            .uri("/api/locations/dps")
            .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
            .bodyValue(dpsLocationIdList)
            .exchange()
            .expectStatus().isForbidden
        }
      }

      @Nested
      inner class HappyPath {
        @Test
        fun `returns DPS and NOMIS ids`() {
          webTestClient.post()
            .uri("/api/locations/dps")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DPS_MAPPING__LOCATIONS__R")))
            .bodyValue(dpsLocationIdList)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].dpsLocationId").isEqualTo(DPS_LOCATION_ID)
            .jsonPath("$[0].nomisLocationId").isEqualTo(NOMIS_LOCATION_ID)
            .jsonPath("$[1].dpsLocationId").isEqualTo(DPS_LOCATION_ID_TWO)
            .jsonPath("$[1].nomisLocationId").isEqualTo(NOMIS_LOCATION_ID_TWO)
            .jsonPath("$[2].dpsLocationId").isEqualTo(DPS_LOCATION_ID_THREE)
            .jsonPath("$[2].nomisLocationId").isEqualTo(NOMIS_LOCATION_ID_THREE)
        }
      }

      @Nested
      inner class Validation {
        @Test
        fun `returns empty list when no mappings found`() {
          webTestClient.post()
            .uri("/api/locations/dps")
            .headers(setAuthorisation(roles = listOf("NOMIS_DPS_MAPPING__LOCATIONS__R")))
            .bodyValue(listOf("00001111-2222-3333-4444-555566667777"))
            .exchange()
            .expectStatus().isOk
            .expectBodyList<NomisDpsLocationMapping>()
            .hasSize(0)
        }

        @Test
        fun `returns 400 when invalid request body`() {
          webTestClient.post()
            .uri("/api/locations/dps")
            .headers(setAuthorisation(roles = listOf("NOMIS_DPS_MAPPING__LOCATIONS__R")))
            .bodyValue(mapOf("id" to DPS_LOCATION_ID))
            .exchange()
            .expectStatus().isBadRequest
        }
      }
    }

    private suspend fun storeLocationMapping(dpsLocationId: String, nomisLocationId: Long) = locationRepository.save(
      LocationMapping(
        dpsLocationId = dpsLocationId,
        nomisLocationId = nomisLocationId,
        label = "2024-05-10T09:28:29",
        mappingType = LocationMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.now(),
      ),
    )
  }

  @Nested
  inner class Sentences {
    val nomisBookingId = 123456L
    val nomisSentenceSequence = 1
    val dpsSentenceId = "12345678-1234-1234-1234-123456789012"

    @BeforeEach
    fun setUp(): Unit = runBlocking {
      sentenceRepository.save(
        SentenceMapping(
          dpsSentenceId = dpsSentenceId,
          nomisBookingId = nomisBookingId,
          nomisSentenceSequence = nomisSentenceSequence,
          mappingType = SentenceMappingType.NOMIS_CREATED,
        ),
      )
    }

    @AfterEach
    internal fun deleteData() = runBlocking {
      sentenceRepository.deleteAll()
    }

    @DisplayName("GET /api/sentence/nomis/booking-id/{nomisBookingId}/sentence-sequence/{nomisSentenceSequence}")
    @Nested
    inner class GetByNomisSentenceMapping {
      @Nested
      inner class Security {
        @Test
        fun `access unauthorised when no authority`() {
          webTestClient.get()
            .uri("/api/sentence/nomis/booking-id/$nomisBookingId/sentence-sequence/$nomisSentenceSequence")
            .exchange()
            .expectStatus().isUnauthorized
        }

        @Test
        fun `access forbidden with no roles`() {
          webTestClient.get()
            .uri("/api/sentence/nomis/booking-id/$nomisBookingId/sentence-sequence/$nomisSentenceSequence")
            .headers(setAuthorisation(roles = listOf()))
            .exchange()
            .expectStatus().isForbidden
        }

        @Test
        fun `access forbidden with wrong role`() {
          webTestClient.get()
            .uri("/api/sentence/nomis/booking-id/$nomisBookingId/sentence-sequence/$nomisSentenceSequence")
            .headers(setAuthorisation(roles = listOf("NOMIS_DPS_MAPPING__LOCATIONS__R")))
            .exchange()
            .expectStatus().isForbidden
        }
      }

      @Nested
      inner class HappyPath {
        @Test
        fun `returns DPS and NOMIS ids`() {
          webTestClient.get()
            .uri("/api/sentence/nomis/booking-id/$nomisBookingId/sentence-sequence/$nomisSentenceSequence")
            .headers(setAuthorisation(roles = listOf("NOMIS_DPS_MAPPING__SENTENCE__R")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("dpsSentenceId").isEqualTo(dpsSentenceId)
            .jsonPath("nomisBookingId").isEqualTo(nomisBookingId)
            .jsonPath("nomisSentenceSequence").isEqualTo(nomisSentenceSequence)
        }
      }

      @Nested
      inner class Validation {
        @Test
        fun `returns 404 when not found`() {
          webTestClient.get()
            .uri("/api/sentence/nomis/booking-id/9999/sentence-sequence/999")
            .headers(setAuthorisation(roles = listOf("NOMIS_DPS_MAPPING__SENTENCE__R")))
            .exchange()
            .expectStatus().isNotFound
        }
      }
    }
  }
}
