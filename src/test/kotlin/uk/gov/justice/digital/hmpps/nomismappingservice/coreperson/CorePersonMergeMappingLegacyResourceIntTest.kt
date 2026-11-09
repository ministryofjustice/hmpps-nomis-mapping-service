package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressContactMappingsByNomisIdsRequest
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime

class CorePersonMergeMappingLegacyResourceIntTest(
  @Autowired private val corePersonAddressMappingRepository: CorePersonAddressMappingRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("POST /mapping/core-person/mappings/by-nomis-ids")
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class GetMappingsFromNomisIds {
    val request = CorePersonAddressContactMappingsByNomisIdsRequest(
      addresses = listOf(101L),
      contacts = emptyList(),
      addressUsages = emptyList(),
    )

    @BeforeAll
    fun setUp() = runTest {
      corePersonAddressMappingRepository.save(
        CorePersonAddressMapping(
          cprId = "a1c4d6e8-1111-4b2a-9c3d-000000000101",
          nomisId = 101L,
          nomisPrisonNumber = "A1234BC",
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @AfterAll
    fun tearDown() = runTest { corePersonAddressMappingRepository.deleteAll() }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when a required list is missing`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("""{"addresses": [], "contacts": []}""")
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns mappings using the legacy path`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("addresses.length()").isEqualTo(1)
          .jsonPath("addresses[0].cprId").isEqualTo("a1c4d6e8-1111-4b2a-9c3d-000000000101")
          .jsonPath("contacts").isEmpty
          .jsonPath("addressUsages").isEmpty
      }
    }
  }
}
