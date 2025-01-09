package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.coreperson

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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.isDuplicateMapping
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class CorePersonMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var corePersonMappingRepository: CorePersonMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    corePersonMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST mapping/core-person/migrate")
  inner class CreateMappings {

    @Nested
    inner class Security {
      val mappings = CorePersonMappingsDto(
        personMapping = CorePersonSimpleMappingIdDto(
          cprId = UUID.randomUUID().toString(),
          prisonNumber = "A1234BC",
        ),
        label = null,
        mappingType = CorePersonMappingType.CPR_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/migrate")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/migrate")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/migrate")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingCorePersonMapping: CorePersonMapping

      val mappings = CorePersonMappingsDto(
        personMapping = CorePersonSimpleMappingIdDto(
          cprId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
          prisonNumber = "A1234BC",
        ),
        label = null,
        mappingType = CorePersonMappingType.MIGRATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingCorePersonMapping = corePersonMappingRepository.save(
          CorePersonMapping(
            cprId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
            prisonNumber = "A1234BC",
            label = "2023-01-01T12:45:12",
            mappingType = CorePersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same core person to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/core-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/core-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          // since this is an untyped map an int will be assumed for such small numbers
          assertThat(this.moreInfo.existing)
            .containsEntry("prisonNumber", existingCorePersonMapping.prisonNumber)
            .containsEntry("cprId", existingCorePersonMapping.cprId)
            .containsEntry("mappingType", existingCorePersonMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("prisonNumber", mappings.personMapping.prisonNumber)
            .containsEntry("cprId", mappings.personMapping.cprId)
            .containsEntry("mappingType", existingCorePersonMapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mappings = CorePersonMappingsDto(
        personMapping = CorePersonSimpleMappingIdDto(
          cprId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
          prisonNumber = "A1234BC",
        ),
        label = null,
        mappingType = CorePersonMappingType.CPR_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the person mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings.copy(label = "2023-01-01T12:45:12")))
          .exchange()
          .expectStatus().isCreated

        val personMapping =
          corePersonMappingRepository.findOneByPrisonNumber(mappings.personMapping.prisonNumber)!!

        assertThat(personMapping.cprId).isEqualTo(mappings.personMapping.cprId)
        assertThat(personMapping.prisonNumber).isEqualTo(mappings.personMapping.prisonNumber)
        assertThat(personMapping.label).isEqualTo("2023-01-01T12:45:12")
        assertThat(personMapping.mappingType).isEqualTo(mappings.mappingType)
        assertThat(personMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }
}
