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

class CorePersonReligionMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var corePersonMappingRepository: CorePersonMappingRepository

  @Autowired
  private lateinit var corePersonReligionMappingRepository: CorePersonReligionMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    corePersonReligionMappingRepository.deleteAll()
    corePersonMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /mapping/core-person/religion/nomis-religion-id/{nomisReligionId}")
  inner class GetPersonReligionByNomisId {
    private val nomisReligionId = 12345L
    private val cprReligionId = "54321"
    private lateinit var corePersonReligionMapping: CorePersonReligionMapping

    @BeforeEach
    fun setUp() = runTest {
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisPrisonNumber = "A1234AA",
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      corePersonReligionMapping = corePersonReligionMappingRepository.save(
        CorePersonReligionMapping(
          nomisPrisonNumber = "A1234AA",
          cprId = cprReligionId,
          nomisId = nomisReligionId,
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
          .uri("/mapping/core-person/religion/nomis-religion-id/{nomisReligionId}", nomisReligionId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/religion/nomis-religion-id/{nomisReligionId}", nomisReligionId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/religion/nomis-religion-id/{nomisReligionId}", nomisReligionId)
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
          .uri("/mapping/core-person/religion/nomis-religion-id/{nomisReligionId}", 99999)
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
          .uri("/mapping/core-person/religion/nomis-religion-id/{nomisReligionId}", nomisReligionId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprReligionId)
          .jsonPath("nomisId").isEqualTo(nomisReligionId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/religion/cpr-religion-id/{cprReligionId}")
  inner class GetPersonReligionByCprId {
    private val nomisReligionId = 7654321L
    private val cprReligionId = "1234567"
    private lateinit var corePersonReligionMapping: CorePersonReligionMapping

    @BeforeEach
    fun setUp() = runTest {
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisPrisonNumber = "A1234AA",
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      corePersonReligionMapping = corePersonReligionMappingRepository.save(
        CorePersonReligionMapping(
          nomisPrisonNumber = "A1234AA",
          cprId = cprReligionId,
          nomisId = nomisReligionId,
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
          .uri("/mapping/core-person/religion/cpr-religion-id/{cprReligionId}", cprReligionId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/religion/cpr-religion-id/{cprReligionId}", cprReligionId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/religion/cpr-religion-id/{cprReligionId}", cprReligionId)
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
          .uri("/mapping/core-person/religion/cpr-religion-id/{cprReligionId}", "99999")
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
          .uri("/mapping/core-person/religion/cpr-religion-id/{cprReligionId}", cprReligionId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprReligionId)
          .jsonPath("nomisId").isEqualTo(nomisReligionId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }
}
