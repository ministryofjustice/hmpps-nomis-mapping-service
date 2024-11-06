package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.api

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders.LocationRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.LocationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.LocationMappingType
import java.time.LocalDateTime

private const val dpsLocationId = "f4499772-2e43-4951-861d-04ad86df43fc"
private const val nomisLocationId = 2318905L

class PublicApiResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var locationRepository: LocationRepository

  @BeforeEach
  fun setUp(): Unit = runBlocking {
    locationRepository.save(
      LocationMapping(
        dpsLocationId = dpsLocationId,
        nomisLocationId = nomisLocationId,
        label = "2024-05-10T09:28:29",
        mappingType = LocationMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.now(),
      ),
    )
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
          .uri("/api/locations/nomis/$nomisLocationId")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden with no roles`() {
        webTestClient.get()
          .uri("/api/locations/nomis/$nomisLocationId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/api/locations/nomis/$nomisLocationId")
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
          .uri("/api/locations/nomis/$nomisLocationId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DPS_MAPPING__LOCATIONS__R")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsLocationId").isEqualTo(dpsLocationId)
          .jsonPath("nomisLocationId").isEqualTo(nomisLocationId)
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

  @DisplayName("GET /api/locations/dps/{dpsLocationId}")
  @Nested
  inner class GetDpsLocationMapping {
    @Nested
    inner class Security {
      @Test
      fun `access unauthorised when no authority`() {
        webTestClient.get()
          .uri("/api/locations/dps/$dpsLocationId")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden with no roles`() {
        webTestClient.get()
          .uri("/api/locations/dps/$dpsLocationId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/api/locations/dps/$dpsLocationId")
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
          .uri("/api/locations/dps/$dpsLocationId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_DPS_MAPPING__LOCATIONS__R")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsLocationId").isEqualTo(dpsLocationId)
          .jsonPath("nomisLocationId").isEqualTo(nomisLocationId)
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
}
