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

class CorePersonPhoneMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var corePersonPhoneMappingRepository: CorePersonPhoneMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    corePersonPhoneMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}")
  inner class GetPersonPhoneByNomisId {
    private val nomisPhoneId = 12345L
    private val cprPhoneId = "54321"
    private lateinit var personPhoneMapping: CorePersonPhoneMapping

    @BeforeEach
    fun setUp() = runTest {
      personPhoneMapping = corePersonPhoneMappingRepository.save(
        CorePersonPhoneMapping(
          cprId = cprPhoneId,
          nomisId = nomisPhoneId,
          label = "2023-01-01T12:45:12",
          cprPhoneType = CprPhoneType.CORE_PERSON,
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
          .uri("/mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
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
          .uri("/mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}", 99999)
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
          .uri("/mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprPhoneId)
          .jsonPath("nomisId").isEqualTo(nomisPhoneId)
          .jsonPath("cprPhoneType").isEqualTo("CORE_PERSON")
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/phone/cpr-phone-id/{cprPhoneId}")
  inner class GetPersonPhoneByCprId {
    private val nomisInternetAddressId = 7654321L
    private val cprPhoneId = "1234567"
    private lateinit var personPhoneMapping: CorePersonPhoneMapping

    @BeforeEach
    fun setUp() = runTest {
      personPhoneMapping = corePersonPhoneMappingRepository.save(
        CorePersonPhoneMapping(
          cprId = cprPhoneId,
          nomisId = nomisInternetAddressId,
          cprPhoneType = CprPhoneType.CORE_PERSON,
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
          .uri("/mapping/core-person/phone/cpr-phone-id/{cprPhoneId}", cprPhoneId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/phone/cpr-phone-id/{cprPhoneId}", cprPhoneId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/phone/cpr-phone-id/{cprPhoneId}", cprPhoneId)
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
          .uri("/mapping/core-person/phone/cpr-phone-id/{cprPhoneId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 when mapping not found even when address phone with same ID exists`() {
        webTestClient.get()
          .uri("/mapping/core-person/phone/cpr-core-address-phone-id/{cprCoreAddressPhoneId}", cprPhoneId)
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
          .uri("/mapping/core-person/phone/cpr-phone-id/{cprPhoneId}", cprPhoneId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprPhoneId)
          .jsonPath("nomisId").isEqualTo(nomisInternetAddressId)
          .jsonPath("cprPhoneType").isEqualTo("CORE_PERSON")
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }
}
