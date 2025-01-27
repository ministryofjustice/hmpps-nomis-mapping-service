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

class CorePersonEmailAddressMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var corePersonMappingRepository: CorePersonMappingRepository

  @Autowired
  private lateinit var corePersonEmailAddressMappingRepository: CorePersonEmailAddressMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    corePersonEmailAddressMappingRepository.deleteAll()
    corePersonMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}")
  inner class GetPersonEmailByNomisId {
    private val nomisEmailAddressId = 12345L
    private val cprEmailAddressId = "54321"
    private lateinit var corePersonEmailAddressMapping: CorePersonEmailAddressMapping

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
      corePersonEmailAddressMapping = corePersonEmailAddressMappingRepository.save(
        CorePersonEmailAddressMapping(
          nomisPrisonNumber = "A1234AA",
          cprId = cprEmailAddressId,
          nomisId = nomisEmailAddressId,
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
          .uri("/mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}", nomisEmailAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}", nomisEmailAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}", nomisEmailAddressId)
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
          .uri("/mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}", 99999)
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
          .uri("/mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}", nomisEmailAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprEmailAddressId)
          .jsonPath("nomisId").isEqualTo(nomisEmailAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/email/cpr-email-id/{cprEmailAddressId}")
  inner class GetPersonEmailByCprId {
    private val nomisEmailAddressId = 7654321L
    private val cprEmailAddressId = "1234567"
    private lateinit var corePersonEmailAddressMapping: CorePersonEmailAddressMapping

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
      corePersonEmailAddressMapping = corePersonEmailAddressMappingRepository.save(
        CorePersonEmailAddressMapping(
          nomisPrisonNumber = "A1234AA",
          cprId = cprEmailAddressId,
          nomisId = nomisEmailAddressId,
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
          .uri("/mapping/core-person/email/cpr-email-address-id/{cprEmailAddressId}", cprEmailAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/email/cpr-email-address-id/{cprEmailAddressId}", cprEmailAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/email/cpr-email-address-id/{cprEmailAddressId}", cprEmailAddressId)
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
          .uri("/mapping/core-person/email/cpr-email-address-id/{cprEmailAddressId}", "99999")
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
          .uri("/mapping/core-person/email/cpr-email-address-id/{cprEmailAddressId}", cprEmailAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprEmailAddressId)
          .jsonPath("nomisId").isEqualTo(nomisEmailAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }
}
