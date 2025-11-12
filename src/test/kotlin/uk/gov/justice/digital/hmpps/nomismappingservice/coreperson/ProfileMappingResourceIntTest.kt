package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val CPR_ID = "edcd118c-41ba-42ea-b5c4-404b453ad58b"
private const val NOMIS_BOOKING_ID = 12345678L
private const val NOMIS_PROFILE_TYPE = "IMM"
private const val NOMIS_PRISON_NUMBER = "A1234AA"

class ProfileMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var profileMappingRepository: ProfileMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    profileMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST /mapping/core-person/profile")
  inner class CreateProfile {
    @Nested
    inner class Security {
      val dto = ProfileMappingDto(
        cprId = CPR_ID,
        nomisPrisonNumber = NOMIS_PRISON_NUMBER,
        nomisBookingId = NOMIS_BOOKING_ID,
        nomisProfileType = NOMIS_PROFILE_TYPE,
        label = "2023-01-01T12:45:12",
        mappingType = CorePersonMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2025-11-12T10:00"),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/profile")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(dto))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/profile")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(dto))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/profile")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(dto))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingCorePersonMapping: ProfileMapping

      val mappingWithDuplicateNomisIds = ProfileMappingIdDto(
        cprId = "18e89dec-6ace-4706-9283-8e11e9ebe886",
        nomisBookingId = NOMIS_BOOKING_ID,
        nomisProfileType = NOMIS_PROFILE_TYPE,
        nomisPrisonNumber = NOMIS_PRISON_NUMBER,
      )

      val mappingWithDuplicateCprId = ProfileMappingIdDto(
        nomisProfileType = "OTHER",
        cprId = CPR_ID,
        nomisBookingId = NOMIS_BOOKING_ID,
        nomisPrisonNumber = NOMIS_PRISON_NUMBER,
      )

      @BeforeEach
      fun setUp() = runTest {
        existingCorePersonMapping = profileMappingRepository.save(
          ProfileMapping(
            cprId = UUID.fromString(CPR_ID),
            nomisPrisonNumber = NOMIS_PRISON_NUMBER,
            nomisBookingId = NOMIS_BOOKING_ID,
            nomisProfileType = NOMIS_PROFILE_TYPE,
            label = "2023-01-01T12:45:12",
            mappingType = CorePersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow a profile with duplicate nomis mappings and return details`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/core-person/profile")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappingWithDuplicateNomisIds))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisPrisonNumber", existingCorePersonMapping.nomisPrisonNumber)
            .containsEntry("cprId", existingCorePersonMapping.cprId.toString())
            .containsEntry("nomisBookingId", existingCorePersonMapping.nomisBookingId.toInt())
            .containsEntry("nomisProfileType", existingCorePersonMapping.nomisProfileType)
            .containsEntry("mappingType", existingCorePersonMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisPrisonNumber", NOMIS_PRISON_NUMBER)
            .containsEntry("cprId", mappingWithDuplicateNomisIds.cprId)
            .containsEntry("nomisBookingId", mappingWithDuplicateNomisIds.nomisBookingId.toInt())
            .containsEntry("nomisProfileType", mappingWithDuplicateNomisIds.nomisProfileType)
            .containsEntry("mappingType", CorePersonMappingType.NOMIS_CREATED.toString())
        }
      }

      @Test
      fun `will not allow a profile with duplicate cpr id and return details`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/core-person/profile")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappingWithDuplicateCprId))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisPrisonNumber", existingCorePersonMapping.nomisPrisonNumber)
            .containsEntry("cprId", existingCorePersonMapping.cprId.toString())
            .containsEntry("nomisBookingId", existingCorePersonMapping.nomisBookingId.toInt())
            .containsEntry("nomisProfileType", existingCorePersonMapping.nomisProfileType)
            .containsEntry("mappingType", existingCorePersonMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisPrisonNumber", NOMIS_PRISON_NUMBER)
            .containsEntry("cprId", mappingWithDuplicateCprId.cprId)
            .containsEntry("nomisBookingId", mappingWithDuplicateCprId.nomisBookingId.toInt())
            .containsEntry("nomisProfileType", mappingWithDuplicateCprId.nomisProfileType)
            .containsEntry("mappingType", CorePersonMappingType.NOMIS_CREATED.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = ProfileMappingIdDto(
        cprId = CPR_ID,
        nomisBookingId = NOMIS_BOOKING_ID,
        nomisProfileType = NOMIS_PROFILE_TYPE,
        nomisPrisonNumber = NOMIS_PRISON_NUMBER,
      )

      @Test
      fun `will store the mapping data`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/profile")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        with(profileMappingRepository.findById(CPR_ID)!!) {
          assertThat(cprId.toString()).isEqualTo(CPR_ID)
          assertThat(nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
          assertThat(nomisProfileType).isEqualTo(NOMIS_PROFILE_TYPE)
          assertThat(nomisPrisonNumber).isEqualTo(NOMIS_PRISON_NUMBER)
          assertThat(label).isNull()
          assertThat(mappingType).isEqualTo(CorePersonMappingType.NOMIS_CREATED)
          assertThat(whenCreated).isCloseToUtcNow(within(5, ChronoUnit.SECONDS))
        }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/profile/booking/{bookingId}/type/{profileType}")
  inner class GetProfileByNomisId {
    @BeforeEach
    fun setUp() = runTest {
      profileMappingRepository.save(
        ProfileMapping(
          cprId = UUID.fromString(CPR_ID),
          nomisPrisonNumber = NOMIS_PRISON_NUMBER,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisProfileType = NOMIS_PROFILE_TYPE,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2025-11-12T10:00"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/core-person/profile/booking/9999/type/IMM")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/profile/booking/9999/type/IMM")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/profile/booking/9999/type/IMM")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/core-person/profile/booking/9999/type/IMM")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/core-person/profile/booking/$NOMIS_BOOKING_ID/type/$NOMIS_PROFILE_TYPE")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(CPR_ID)
          .jsonPath("nomisBookingId").isEqualTo(NOMIS_BOOKING_ID)
          .jsonPath("nomisProfileType").isEqualTo(NOMIS_PROFILE_TYPE)
          .jsonPath("nomisPrisonNumber").isEqualTo(NOMIS_PRISON_NUMBER)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2025-11-12T10:00:00")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/profile/cpr-id/{cprId}")
  inner class GetProfileByCprId {
    @BeforeEach
    fun setUp() = runTest {
      profileMappingRepository.save(
        ProfileMapping(
          cprId = UUID.fromString(CPR_ID),
          nomisPrisonNumber = NOMIS_PRISON_NUMBER,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisProfileType = NOMIS_PROFILE_TYPE,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2025-11-12T10:00"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/core-person/profile/cpr-id/{cprId}", CPR_ID)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/profile/cpr-id/{cprId}", CPR_ID)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/profile/cpr-id/{cprId}", CPR_ID)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/core-person/profile/cpr-id/{cprId}", "edcd118c-41ba-42ea-b5c4-999999999999")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/core-person/profile/cpr-id/{cprId}", CPR_ID)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(CPR_ID)
          .jsonPath("nomisBookingId").isEqualTo(NOMIS_BOOKING_ID)
          .jsonPath("nomisProfileType").isEqualTo(NOMIS_PROFILE_TYPE)
          .jsonPath("nomisPrisonNumber").isEqualTo(NOMIS_PRISON_NUMBER)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2025-11-12T10:00:00")
      }
    }
  }
}
