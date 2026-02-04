package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime

class CorePersonBeliefMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var personBeliefMappingRepository: CorePersonBeliefMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    personBeliefMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /mapping/core-person/belief/nomis-belief-id/{nomisBeliefId}")
  inner class GetPersonBeliefByNomisId {
    private val nomisBeliefId = 12345L
    private val cprBeliefId = "54321"

    @BeforeEach
    fun setUp() = runTest {
      personBeliefMappingRepository.save(
        CorePersonBeliefMapping(
          cprId = cprBeliefId,
          nomisId = nomisBeliefId,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/core-person/belief/nomis-belief-id/{nomisBeliefId}", nomisBeliefId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/belief/nomis-belief-id/{nomisBeliefId}", nomisBeliefId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/belief/nomis-belief-id/{nomisBeliefId}", nomisBeliefId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/core-person/belief/nomis-belief-id/{nomisBeliefId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/core-person/belief/nomis-belief-id/{nomisBeliefId}", nomisBeliefId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprBeliefId)
          .jsonPath("nomisId").isEqualTo(nomisBeliefId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/belief/cpr-belief-id/{cprBeliefId}")
  inner class GetCorePersonBeliefByCprId {
    private val nomisBeliefId = 7654321L
    private val cprBeliefId = "1234567"

    @BeforeEach
    fun setUp() = runTest {
      personBeliefMappingRepository.save(
        CorePersonBeliefMapping(
          cprId = cprBeliefId,
          nomisId = nomisBeliefId,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/core-person/Belief/cpr-Belief-id/{cprBeliefId}", cprBeliefId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/belief/cpr-belief-id/{cprBeliefId}", cprBeliefId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/belief/cpr-belief-id/{cprBeliefId}", cprBeliefId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/core-person/Belief/cpr-Belief-id/{cprBeliefId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/core-person/belief/cpr-belief-id/{cprBeliefId}", cprBeliefId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprBeliefId)
          .jsonPath("nomisId").isEqualTo(nomisBeliefId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }
}
