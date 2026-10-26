package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.movement

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.PersonLocationMappingType
import java.util.UUID

class PersonLocationMovementResourceIntTest(
  @Autowired private val movementRepository: PersonLocationMovementRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /mapping/person-location/movement/nomis-booking-id/{nomisBookingId}/nomis-movement-seq/{nomisMovementSeq}")
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class GetMovementMappingByNomisId {
    private val mapping = PersonLocationMovementMapping(
      dpsExternalMovementId = UUID.randomUUID(),
      nomisBookingId = 23456L,
      nomisMovementSeq = 3,
      mappingType = PersonLocationMappingType.NOMIS_CREATED,
    )

    @AfterAll
    fun tearDown() = runTest {
      movementRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/person-location/movement/nomis-booking-id/12345/nomis-movement-seq/3")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/person-location/movement/nomis-booking-id/12345/nomis-movement-seq/3")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/person-location/movement/nomis-booking-id/12345/nomis-movement-seq/3")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() {
        webTestClient.getMovementMapping(99999L, 1)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should get movement mapping by NOMIS booking ID and movement sequence`() = runTest {
        movementRepository.save(mapping)

        webTestClient.getMovementMapping(mapping.nomisBookingId, mapping.nomisMovementSeq)
          .expectStatus().isOk
          .expectBody<PersonLocationMovementIdMappingDto>()
          .returnResult().responseBody!!
          .apply {
            assertThat(nomisBookingId).isEqualTo(mapping.nomisBookingId)
            assertThat(nomisMovementSeq).isEqualTo(mapping.nomisMovementSeq)
            assertThat(dpsExternalMovementId).isEqualTo(mapping.dpsExternalMovementId)
            assertThat(mappingType).isEqualTo(PersonLocationMappingType.NOMIS_CREATED)
          }
      }
    }

    private fun WebTestClient.getMovementMapping(nomisBookingId: Long, nomisMovementSeq: Int) = get()
      .uri("/mapping/person-location/movement/nomis-booking-id/$nomisBookingId/nomis-movement-seq/$nomisMovementSeq")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }
}
