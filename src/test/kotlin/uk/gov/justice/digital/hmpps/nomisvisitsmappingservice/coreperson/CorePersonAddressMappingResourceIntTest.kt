package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.coreperson

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime

class CorePersonAddressMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var corePersonAddressMappingRepository: CorePersonAddressMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    corePersonAddressMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /mapping/core-person/address/nomis-address-id/{nomisAddressId}")
  inner class GetPersonAddressByNomisId {
    private val nomisAddressId = 12345L
    private val cprAddressId = "54321"
    private lateinit var personAddressMapping: CorePersonAddressMapping

    @BeforeEach
    fun setUp() = runTest {
      personAddressMapping = corePersonAddressMappingRepository.save(
        CorePersonAddressMapping(
          cprId = cprAddressId,
          nomisId = nomisAddressId,
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
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", nomisAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", nomisAddressId)
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
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprAddressId)
          .jsonPath("nomisId").isEqualTo(nomisAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/address/cpr-address-id/{cprAddressId}")
  inner class GetCorePersonAddressByCprId {
    private val nomisAddressId = 7654321L
    private val cprAddressId = "1234567"
    private lateinit var personAddressMapping: CorePersonAddressMapping

    @BeforeEach
    fun setUp() = runTest {
      personAddressMapping = corePersonAddressMappingRepository.save(
        CorePersonAddressMapping(
          cprId = cprAddressId,
          nomisId = nomisAddressId,
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
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", cprAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", cprAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", cprAddressId)
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
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", cprAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprAddressId)
          .jsonPath("nomisId").isEqualTo(nomisAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }
}
