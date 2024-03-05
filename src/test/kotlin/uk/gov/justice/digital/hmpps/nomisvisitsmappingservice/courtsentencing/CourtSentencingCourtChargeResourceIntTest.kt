package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class CourtSentencingCourtChargeResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var courtChargeRepository: CourtChargeMappingRepository

  @Nested
  @DisplayName("GET /mapping/court-sentencing/court-charges/dps-court-charge-id/{courtChargeId}")
  inner class GetCourtChargeMappingByDpsId {
    lateinit var courtChargeMapping: CourtChargeMapping

    @BeforeEach
    fun setUp() = runTest {
      courtChargeMapping = courtChargeRepository.save(
        CourtChargeMapping(
          dpsCourtChargeId = "DPS123",
          nomisCourtChargeId = 4321L,
          label = "2023-01-01T12:45:12",
          mappingType = CourtChargeMappingType.MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      courtChargeRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-charges/dps-court-charge-id/${courtChargeMapping.dpsCourtChargeId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-charges/dps-court-charge-id/${courtChargeMapping.dpsCourtChargeId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-charges/dps-court-charge-id/${courtChargeMapping.dpsCourtChargeId}")
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
          .uri("/mapping/court-sentencing/court-charges/dps-court-charge-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-charges/dps-court-charge-id/${courtChargeMapping.dpsCourtChargeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCourtChargeId").isEqualTo(courtChargeMapping.nomisCourtChargeId)
          .jsonPath("dpsCourtChargeId").isEqualTo(courtChargeMapping.dpsCourtChargeId)
          .jsonPath("mappingType").isEqualTo(courtChargeMapping.mappingType.name)
          .jsonPath("label").isEqualTo(courtChargeMapping.label!!)
          .jsonPath("whenCreated").value<String> {
            Assertions.assertThat(LocalDateTime.parse(it))
              .isCloseTo(LocalDateTime.now(), Assertions.within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }
}
