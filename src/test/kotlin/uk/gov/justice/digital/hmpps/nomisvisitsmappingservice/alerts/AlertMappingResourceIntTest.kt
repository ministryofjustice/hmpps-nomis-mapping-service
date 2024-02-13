package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts.AlertMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AlertMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: AlertsMappingRepository

  @Nested
  @DisplayName("GET /mapping/alerts/nomis-booking-id/{bookingId}/nomis-alert-sequence/{alertSequence}")
  inner class GetMappingByNomisId {
    lateinit var mapping: AlertMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        AlertMapping(
          dpsAlertId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisBookingId = 54321L,
          nomisAlertSequence = 2L,
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get()
          .uri("/mapping/alerts/nomis-booking-id/${mapping.nomisBookingId}/nomis-alert-sequence/${mapping.nomisAlertSequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/alerts/nomis-booking-id/${mapping.nomisBookingId}/nomis-alert-sequence/${mapping.nomisAlertSequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `create visit forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/alerts/nomis-booking-id/${mapping.nomisBookingId}/nomis-alert-sequence/${mapping.nomisAlertSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return 404 when mapping does not exist`() {
        webTestClient.get()
          .uri("/mapping/alerts/nomis-booking-id/9999/nomis-alert-sequence/${mapping.nomisAlertSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/alerts/nomis-booking-id/${mapping.nomisBookingId}/nomis-alert-sequence/${mapping.nomisAlertSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisBookingId").isEqualTo(mapping.nomisBookingId)
          .jsonPath("nomisAlertSequence").isEqualTo(mapping.nomisAlertSequence)
          .jsonPath("dpsAlertId").isEqualTo(mapping.dpsAlertId)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }
}
