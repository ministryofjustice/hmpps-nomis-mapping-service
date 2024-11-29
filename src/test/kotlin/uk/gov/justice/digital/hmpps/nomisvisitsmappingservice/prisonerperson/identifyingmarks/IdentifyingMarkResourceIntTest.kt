package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonerperson.identifyingmarks

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkImageMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkImageMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.IdentifyingMarkMappingResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class IdentifyingMarkResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var identifyingMarkMappingRepository: IdentifyingMarkMappingRepository

  @Autowired
  private lateinit var identifyingMarkImageMappingRepository: IdentifyingMarkImageMappingRepository

  @Nested
  @DisplayName("GET /mapping/prisonperson/nomis-booking-id/{bookingId}/identifying-mark-sequence/{sequence}")
  inner class GetIdentifyingMarkMappingsByNomisId {
    lateinit var mapping: IdentifyingMarkMapping

    @BeforeEach
    fun setUp() = runTest {
      IdentifyingMarkMapping(
        nomisBookingId = 1,
        nomisMarksSequence = 1,
        dpsId = UUID.randomUUID(),
        offenderNo = "A1234AA",
        label = "some_label",
        whenCreated = LocalDateTime.now(),
        mappingType = "some_mapping_type",
      ).also {
        mapping = identifyingMarkMappingRepository.save(it)
      }
    }

    @AfterEach
    fun tearDown() = runTest {
      identifyingMarkMappingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-booking-id/${mapping.nomisBookingId}/identifying-mark-sequence/${mapping.nomisMarksSequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-booking-id/${mapping.nomisBookingId}/identifying-mark-sequence/${mapping.nomisMarksSequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-booking-id/${mapping.nomisBookingId}/identifying-mark-sequence/${mapping.nomisMarksSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should return identifying mark mapping by NOMIS id`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/nomis-booking-id/${mapping.nomisBookingId}/identifying-mark-sequence/${mapping.nomisMarksSequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
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
          .uri("/mapping/prisonperson/nomis-booking-id/999/identifying-mark-sequence/999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/prisonperson/dps-identifying-mark-id/{dpsId}")
  inner class GetIdentifyingMarkMappingsByDpsId {
    private lateinit var mapping1: IdentifyingMarkMapping
    private lateinit var mapping2: IdentifyingMarkMapping

    @BeforeEach
    fun setUp() = runTest {
      IdentifyingMarkMapping(
        nomisBookingId = 1,
        nomisMarksSequence = 1,
        dpsId = UUID.randomUUID(),
        offenderNo = "A1234AA",
        label = "some_label",
        whenCreated = LocalDateTime.now(),
        mappingType = "some_mapping_type",
      ).also {
        mapping1 = identifyingMarkMappingRepository.save(it)
      }

      IdentifyingMarkMapping(
        nomisBookingId = 2,
        nomisMarksSequence = 1,
        dpsId = mapping1.dpsId,
        offenderNo = "A1234AA",
        label = "some_label",
        whenCreated = LocalDateTime.now(),
        mappingType = "some_mapping_type",
      ).also {
        mapping2 = identifyingMarkMappingRepository.save(it)
      }
    }

    @AfterEach
    fun tearDown() = runTest {
      identifyingMarkMappingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-identifying-mark-id/${mapping1.dpsId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-identifying-mark-id/${mapping1.dpsId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-identifying-mark-id/${mapping1.dpsId}")
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
          .uri("/mapping/prisonperson/dps-identifying-mark-id/${mapping1.dpsId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody<List<IdentifyingMarkMappingResponse>>()
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(this).extracting(
                "nomisBookingId",
                "nomisMarksSequence",
                "dpsId",
                "offenderNo",
                "label",
                "mappingType",
              )
                .containsExactlyInAnyOrder(
                  tuple(
                    mapping1.nomisBookingId,
                    mapping1.nomisMarksSequence,
                    mapping1.dpsId,
                    mapping1.offenderNo,
                    mapping1.label,
                    mapping1.mappingType,
                  ),
                  tuple(
                    mapping2.nomisBookingId,
                    mapping2.nomisMarksSequence,
                    mapping2.dpsId,
                    mapping2.offenderNo,
                    mapping2.label,
                    mapping2.mappingType,
                  ),
                )
            }
          }
      }

      @Test
      fun `should return empty list if no mappings`() {
        webTestClient.get()
          .uri("/mapping/prisonperson/dps-identifying-mark-id/${UUID.randomUUID()}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
          .exchange()
          .expectBody()
          .jsonPath("$").isEmpty
      }
    }
  }

  // TODO temporary tests to check JPA mappings - remove these when we test some actual functionality
  @Nested
  inner class TestJpa {
    @Nested
    inner class IdentifyingMarkImageMappingJpa {
      private suspend fun createMapping() =
        IdentifyingMarkImageMapping(
          nomisOffenderImageId = Random.nextLong(),
          dpsId = UUID.randomUUID(),
          nomisBookingId = Random.nextLong(),
          nomisMarksSequence = Random.nextLong(),
          offenderNo = "A134AA",
          label = "some_label",
          whenCreated = LocalDateTime.now(),
          mappingType = "some_mapping_type",
        ).let {
          identifyingMarkImageMappingRepository.save(it)
        }

      @Test
      fun `should create and load identifying marks mappings by NOMIS id`() = runTest {
        val nomisOffenderImageId = createMapping().nomisOffenderImageId

        with(identifyingMarkImageMappingRepository.findById(nomisOffenderImageId)!!) {
          assertThat(this.nomisOffenderImageId).isEqualTo(nomisOffenderImageId)
          assertThat(nomisBookingId).isNotNull()
          assertThat(nomisMarksSequence).isNotNull()
          assertThat(dpsId).isNotNull
          assertThat(offenderNo).isEqualTo("A134AA")
          assertThat(label).isEqualTo("some_label")
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(mappingType).isEqualTo("some_mapping_type")
        }
      }

      @Test
      fun `should create and load identifying marks mappings by DPS id`() = runTest {
        val dpsId = createMapping().dpsId

        with(identifyingMarkImageMappingRepository.findByDpsId(dpsId)!!) {
          assertThat(nomisOffenderImageId).isNotNull()
          assertThat(nomisBookingId).isNotNull()
          assertThat(nomisMarksSequence).isNotNull()
          assertThat(this.dpsId).isEqualTo(dpsId)
          assertThat(offenderNo).isEqualTo("A134AA")
          assertThat(label).isEqualTo("some_label")
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(mappingType).isEqualTo("some_mapping_type")
        }
      }
    }
  }
}
