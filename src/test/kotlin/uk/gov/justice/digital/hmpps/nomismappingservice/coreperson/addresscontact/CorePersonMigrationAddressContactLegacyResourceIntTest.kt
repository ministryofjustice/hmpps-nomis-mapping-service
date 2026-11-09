package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact

import kotlinx.coroutines.flow.count
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingIdDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMigrationAddressContact
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMigrationAddressContactMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime

class CorePersonMigrationAddressContactLegacyResourceIntTest(
  @Autowired private val corePersonMigrationAddressContactMappingRepository: CorePersonMigrationAddressContactMappingRepository,
  @Autowired private val corePersonAddressMappingRepository: CorePersonAddressMappingRepository,
  @Autowired private val corePersonAddressUsageMappingRepository: CorePersonAddressUsageMappingRepository,
  @Autowired private val corePersonContactMappingRepository: CorePersonContactMappingRepository,
) : IntegrationTestBase() {

  private suspend fun deleteAllMappings() {
    corePersonAddressMappingRepository.deleteAll()
    corePersonAddressUsageMappingRepository.deleteAll()
    corePersonContactMappingRepository.deleteAll()
    corePersonMigrationAddressContactMappingRepository.deleteAll()
  }

  private suspend fun saveMigratedPerson() = corePersonMigrationAddressContactMappingRepository.save(
    CorePersonMigrationAddressContact(
      cprId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
      nomisPrisonNumber = "A1234BC",
      label = "2023-01-01T12:45:12",
      mappingType = CorePersonMappingType.MIGRATED,
      whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
    ),
  )

  @Nested
  @DisplayName("POST /mapping/core-person")
  inner class CreateMappings {
    val mappings = CorePersonAddressContactMappingsDto(
      personMapping = CorePersonMappingIdDto(
        cprId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
        nomisPrisonNumber = "A1234BC",
      ),
      label = "2023-01-01T12:45:12",
      mappingType = CorePersonMappingType.MIGRATED,
      whenCreated = LocalDateTime.now(),
      addresses = emptyList(),
      contacts = emptyList(),
      addressUsages = emptyList(),
    )

    @AfterEach
    fun tearDown() = runTest { deleteAllMappings() }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 409 when mapping already exists`() = runTest {
        saveMigratedPerson()

        webTestClient.post()
          .uri("/mapping/core-person")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isEqualTo(409)
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `creates mappings using the legacy path`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated

        val saved = corePersonMigrationAddressContactMappingRepository.findOneByNomisPrisonNumber("A1234BC")!!
        assertThat(saved.cprId).isEqualTo("c5a02cec-4aa3-4aa7-9871-41e9c9af50f7")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/migration-id/{migrationId}")
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class GetCorePersonMappingsByMigrationId {
    @BeforeAll
    fun setUp() = runTest { saveMigratedPerson() }

    @AfterAll
    fun tearDown() = runTest { deleteAllMappings() }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/core-person/migration-id/2023-01-01T12:45:12")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/core-person/migration-id/2023-01-01T12:45:12")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/core-person/migration-id/2023-01-01T12:45:12")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can retrieve mappings by migration Id using the legacy path`() {
        webTestClient.get().uri("/mapping/core-person/migration-id/2023-01-01T12:45:12")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(1)
          .jsonPath("$.content[0].nomisPrisonNumber").isEqualTo("A1234BC")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/person/nomis-prison-number/{nomisPrisonNumber}")
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class GetCorePersonByNomisPrisonNumber {
    @BeforeAll
    fun setUp() = runTest { saveMigratedPerson() }

    @AfterAll
    fun tearDown() = runTest { deleteAllMappings() }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/core-person/person/nomis-prison-number/A1234BC")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/core-person/person/nomis-prison-number/A1234BC")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/core-person/person/nomis-prison-number/A1234BC")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get().uri("/mapping/core-person/person/nomis-prison-number/Z9999ZZ")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data using the legacy path`() {
        webTestClient.get().uri("/mapping/core-person/person/nomis-prison-number/A1234BC")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo("edcd118c-41ba-42ea-b5c4-404b453ad58b")
          .jsonPath("nomisPrisonNumber").isEqualTo("A1234BC")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/core-person")
  inner class DeleteAllMappings {
    @AfterEach
    fun tearDown() = runTest { deleteAllMappings() }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete().uri("/mapping/core-person")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/mapping/core-person")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/mapping/core-person")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `deletes all mappings using the legacy path`() = runTest {
        saveMigratedPerson()

        webTestClient.delete().uri("/mapping/core-person")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(corePersonMigrationAddressContactMappingRepository.findAll().count()).isEqualTo(0)
      }
    }
  }
}
