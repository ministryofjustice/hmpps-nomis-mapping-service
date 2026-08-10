package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.movement

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule.TransferMappingType
import java.util.UUID

class TransferMovementResourceIntTest : IntegrationTestBase() {

  @Nested
  @DisplayName("POST /mapping/transfer-scheduler/movement")
  inner class CreateTransferMovementMapping {

    @Nested
    inner class Security {
      val mapping = TransferMovementMappingDto(
        "A1234BC",
        12345L,
        3,
        UUID.randomUUID(),
        mappingType = TransferMappingType.NOMIS_CREATED,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/transfer-scheduler/movement")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/transfer-scheduler/movement")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/transfer-scheduler/movement")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }
}
