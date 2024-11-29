package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonerperson.identifyingmarks

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkImageMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkImageMappingRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class IdentifyingMarkImageResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var identifyingMarkImageMappingRepository: IdentifyingMarkImageMappingRepository

  @Nested
  @DisplayName("GET /mapping/prisonperson/nomis-offender-image-id/{nomisId}")
  inner class GetIdentifyingMarkImageMappingsByNomisId {
    lateinit var mapping: IdentifyingMarkImageMapping

    @BeforeEach
    fun setUp() = runTest {
      IdentifyingMarkImageMapping(
        nomisOffenderImageId = 1,
        dpsId = UUID.randomUUID(),
        nomisBookingId = 1,
        nomisMarksSequence = 1,
        offenderNo = "A1234AA",
        label = "some_label",
        whenCreated = LocalDateTime.now(),
        mappingType = "some_mapping_type",
      ).also {
        mapping = identifyingMarkImageMappingRepository.save(it)
      }
    }

    @AfterEach
    fun tearDown() = runTest {
      identifyingMarkImageMappingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-offender-image-id/${mapping.nomisOffenderImageId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-offender-image-id/${mapping.nomisOffenderImageId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-offender-image-id/${mapping.nomisOffenderImageId}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should return identifying mark image mapping by NOMIS id`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-offender-image-id/${mapping.nomisOffenderImageId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisOffenderImageId").isEqualTo(mapping.nomisOffenderImageId)
          .jsonPath("nomisBookingId").isEqualTo(mapping.nomisBookingId)
          .jsonPath("nomisMarksSequence").isEqualTo(mapping.nomisMarksSequence)
          .jsonPath("dpsId").isEqualTo(mapping.dpsId.toString())
          .jsonPath("offenderNo").isEqualTo(mapping.offenderNo)
          .jsonPath("label").isEqualTo(mapping.label)
          .jsonPath("whenCreated").value<String> { assertThat(it).startsWith("${LocalDate.now()}") }
          .jsonPath("mappingType").isEqualTo(mapping.mappingType)
      }

      @Test
      fun `should return not found if no mapping`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-offender-image-id/9999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/prisonperson/dps-image-id/{dpsId}")
  inner class GetIdentifyingMarkMappingsByDpsId {
    private lateinit var mapping: IdentifyingMarkImageMapping

    @BeforeEach
    fun setUp() = runTest {
      IdentifyingMarkImageMapping(
        nomisOffenderImageId = 1,
        nomisBookingId = 1,
        nomisMarksSequence = 1,
        dpsId = UUID.randomUUID(),
        offenderNo = "A1234AA",
        label = "some_label",
        whenCreated = LocalDateTime.now(),
        mappingType = "some_mapping_type",
      ).also {
        mapping = identifyingMarkImageMappingRepository.save(it)
      }
    }

    @AfterEach
    fun tearDown() = runTest {
      identifyingMarkImageMappingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-image-id/${mapping.dpsId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-image-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-image-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should return identifying mark mappings by NOMIS id`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-image-id/${mapping.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisOffenderImageId").isEqualTo(mapping.nomisOffenderImageId)
          .jsonPath("nomisBookingId").isEqualTo(mapping.nomisBookingId)
          .jsonPath("nomisMarksSequence").isEqualTo(mapping.nomisMarksSequence)
          .jsonPath("dpsId").isEqualTo(mapping.dpsId.toString())
          .jsonPath("offenderNo").isEqualTo(mapping.offenderNo)
          .jsonPath("label").isEqualTo(mapping.label)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType)
          .jsonPath("whenCreated").value<String> { assertThat(it).startsWith("${LocalDate.now()}") }
      }

      @Test
      fun `should return not found if no mappings`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-image-id/${UUID.randomUUID()}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }
}
