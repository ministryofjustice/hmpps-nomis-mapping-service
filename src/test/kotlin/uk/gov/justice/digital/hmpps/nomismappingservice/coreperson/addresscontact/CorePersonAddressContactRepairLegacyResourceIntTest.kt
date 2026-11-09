package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingIdDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime

class CorePersonAddressContactRepairLegacyResourceIntTest(
  @Autowired private val corePersonAddressMappingRepository: CorePersonAddressMappingRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("POST /mapping/core-person/replace")
  inner class ReplaceCorePersonMappings {
    val nomisPrisonNumber = "A1234BC"

    val mapping = CorePersonAddressContactMappingsDto(
      personMapping = CorePersonMappingIdDto(
        cprId = "9a1313b1-5c03-4225-b6fd-dbc5ce992760",
        nomisPrisonNumber = nomisPrisonNumber,
      ),
      label = "2020-01-01T10:00",
      addresses = listOf(
        CorePersonAddressMappingDto(
          cprId = "fba8a521-fc9f-4801-bb02-3f3326c760ef",
          nomisId = 10000L,
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2020-01-01T10:00",
          mappingType = CorePersonMappingType.NOMIS_CREATED,
          whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
        ),
      ),
      contacts = emptyList(),
      addressUsages = emptyList(),
      mappingType = CorePersonMappingType.NOMIS_CREATED,
      whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
    )

    @AfterEach
    fun tearDown() = runTest { corePersonAddressMappingRepository.deleteAll() }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when a required field is missing`() {
        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("""{ "addresses": [], "contacts": [], "addressUsages": [] }""")
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `replaces mappings using the legacy path`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk

        with(corePersonAddressMappingRepository.findOneByCprId(mapping.addresses[0].cprId)!!) {
          assertThat(nomisId).isEqualTo(10000L)
          assertThat(nomisPrisonNumber).isEqualTo(this@ReplaceCorePersonMappings.nomisPrisonNumber)
        }
      }
    }
  }
}
