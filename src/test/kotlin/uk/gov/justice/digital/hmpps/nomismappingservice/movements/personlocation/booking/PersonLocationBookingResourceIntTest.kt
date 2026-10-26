package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.booking

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

class PersonLocationBookingResourceIntTest(
  @Autowired private val bookingRepository: PersonLocationBookingRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /mapping/person-location/booking/nomis-booking-id/{nomisBookingId}")
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class GetBookingMappingByNomisId {
    private val mapping = PersonLocationBookingMapping(
      dpsCustodialSeriesId = UUID.randomUUID(),
      nomisBookingId = 23456L,
      mappingType = PersonLocationMappingType.NOMIS_CREATED,
    )

    @AfterAll
    fun tearDown() = runTest {
      bookingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/person-location/booking/nomis-booking-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/person-location/booking/nomis-booking-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/person-location/booking/nomis-booking-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() {
        webTestClient.getBookingMapping(99999L)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should get booking mapping by NOMIS booking ID`() = runTest {
        bookingRepository.save(mapping)

        webTestClient.getBookingMapping(mapping.nomisBookingId)
          .expectStatus().isOk
          .expectBody<PersonLocationBookingIdMappingDto>()
          .returnResult().responseBody!!
          .apply {
            assertThat(nomisBookingId).isEqualTo(mapping.nomisBookingId)
            assertThat(dpsCustodialSeriesId).isEqualTo(mapping.dpsCustodialSeriesId)
            assertThat(mappingType).isEqualTo(PersonLocationMappingType.NOMIS_CREATED)
          }
      }
    }

    private fun WebTestClient.getBookingMapping(nomisBookingId: Long) = get()
      .uri("/mapping/person-location/booking/nomis-booking-id/$nomisBookingId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }
}
