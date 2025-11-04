package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType
import java.time.DayOfWeek
import java.time.LocalDateTime

class VisitSlotsResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var visitTimeSlotMappingRepository: VisitTimeSlotMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    visitTimeSlotMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /mapping/visit-slots/time-slots/nomis-prison-id/{nomisPrisonId}/nomis-day-of-week/{nomisDayOfWeek}/nomis-slot-sequence/{nomisSlotSequence}")
  inner class GetVisitTimeSlotMappingByNomisIds {
    val nomisPrisonId = "WWI"
    val nomisDayOfWeek = DayOfWeek.MONDAY
    val nomisSlotSequence = 2
    val dpsId = "123456789"

    @BeforeEach
    fun setUp() = runTest {
      visitTimeSlotMappingRepository.save(
        VisitTimeSlotMapping(
          dpsId = dpsId,
          nomisPrisonId = nomisPrisonId,
          nomisDayOfWeek = nomisDayOfWeek,
          nomisSlotSequence = nomisSlotSequence,
          label = "2020-01-01T10:00",
          mappingType = StandardMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access unauthorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/$nomisSlotSequence")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/$nomisSlotSequence")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/$nomisSlotSequence")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `400 error when invalid day of week supplied`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/AUGUST/nomis-slot-sequence/$nomisSlotSequence")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/99")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will retrieve mapping`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/$nomisSlotSequence")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsId)
          .jsonPath("nomisPrisonId").isEqualTo(nomisPrisonId)
          .jsonPath("nomisDayOfWeek").isEqualTo("MONDAY")
          .jsonPath("nomisSlotSequence").isEqualTo(nomisSlotSequence)
          .jsonPath("label").isEqualTo("2020-01-01T10:00")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
      }
    }
  }
}
