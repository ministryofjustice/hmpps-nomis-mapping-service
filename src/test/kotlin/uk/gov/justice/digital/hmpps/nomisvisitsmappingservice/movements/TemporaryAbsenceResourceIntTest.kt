package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.movements

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime

class TemporaryAbsenceResourceIntTest : IntegrationTestBase() {

  @Nested
  inner class Migrate {

    @Nested
    inner class Security {
      val mappings = TemporaryAbsencesPrisonerMappingDto(
        "A1234BC",
        listOf(),
        "some_migration",
        "${LocalDateTime.now()}",
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/migrate")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/migrate")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/temporary-absence/migrate")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }
}
