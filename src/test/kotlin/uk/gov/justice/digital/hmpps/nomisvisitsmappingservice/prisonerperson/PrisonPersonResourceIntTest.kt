package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonerperson

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.PrisonPersonMigrationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.PrisonPersonMigrationMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.PrisonPersonMigrationType.PHYSICAL_ATTRIBUTES
import java.time.LocalDate

class PrisonPersonResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: PrisonPersonMigrationMappingRepository

  @Nested
  inner class CreateMigrationMapping {
    @AfterEach
    fun teatDown() = runTest {
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/prisonperson/migration")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(PrisonPersonMigrationMapping("any", PHYSICAL_ATTRIBUTES, "any")))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/prisonperson/migration")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(PrisonPersonMigrationMapping("any", PHYSICAL_ATTRIBUTES, "any")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/prisonperson/migration")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(PrisonPersonMigrationMapping("any", PHYSICAL_ATTRIBUTES, "any")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should create migration mapping`() = runTest {
        webTestClient.createMigrationMapping("A1234AA", PHYSICAL_ATTRIBUTES.name, "label")
          .expectStatus().isCreated

        val mapping = repository.findByNomisPrisonerNumberAndMigrationType("A1234AA", PHYSICAL_ATTRIBUTES)
        assertThat(mapping?.nomisPrisonerNumber).isEqualTo("A1234AA")
        assertThat(mapping?.migrationType).isEqualTo(PHYSICAL_ATTRIBUTES)
        assertThat(mapping?.label).isEqualTo("label")
        assertThat(mapping?.whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return 409 if migration mapping already exists`() = runTest {
        webTestClient.createMigrationMapping("A1234AA", PHYSICAL_ATTRIBUTES.name, "label")
          .expectStatus().isCreated

        webTestClient.createMigrationMapping("A1234AA", PHYSICAL_ATTRIBUTES.name, "label")
          .expectStatus().isEqualTo(409)
      }
    }

    private fun WebTestClient.createMigrationMapping(prisonerNumber: String, type: String, label: String) =
      post()
        .uri("/mapping/prisonperson/migration")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            (
              """ { 
              "nomisPrisonerNumber": "$prisonerNumber", 
              "migrationType": "$type",
              "label": "$label" 
            
            } 
              """.trimIndent()
              ),
          ),
        )
        .exchange()
  }
}
