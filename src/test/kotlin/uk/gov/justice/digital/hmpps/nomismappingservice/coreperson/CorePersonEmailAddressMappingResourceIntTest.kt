package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import java.time.LocalDateTime

class CorePersonEmailAddressMappingResourceIntTest(
  @Autowired private val corePersonMappingRepository: CorePersonMappingRepository,
  @Autowired private val corePersonEmailAddressMappingRepository: CorePersonEmailAddressMappingRepository,
) : IntegrationTestBase() {

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("GET /mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}")
  inner class GetPersonEmailByNomisId {
    private val nomisEmailAddressId = 12345L
    private val cprEmailAddressId = "54321"
    private lateinit var corePersonEmailAddressMapping: CorePersonEmailAddressMapping

    @BeforeAll
    fun setUp() = runTest {
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "edcd118c-41ba-42ea-b5c4-404b453ad58c",
          nomisPrisonNumber = "B1234BB",
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      corePersonEmailAddressMapping = corePersonEmailAddressMappingRepository.save(
        CorePersonEmailAddressMapping(
          nomisPrisonNumber = "B1234BB",
          cprId = cprEmailAddressId,
          nomisId = nomisEmailAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @AfterAll
    fun tearDown() = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}", nomisEmailAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}", nomisEmailAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}", nomisEmailAddressId)
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
          .uri("/mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}", 99999)
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
          .uri("/mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}", nomisEmailAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprEmailAddressId)
          .jsonPath("nomisId").isEqualTo(nomisEmailAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("GET /mapping/core-person/email/cpr-email-id/{cprEmailAddressId}")
  inner class GetPersonEmailByCprId {
    private val nomisEmailAddressId = 7654321L
    private val cprEmailAddressId = "1234567"
    private lateinit var corePersonEmailAddressMapping: CorePersonEmailAddressMapping

    @BeforeAll
    fun setUp() = runTest {
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisPrisonNumber = "A1234AA",
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      corePersonEmailAddressMapping = corePersonEmailAddressMappingRepository.save(
        CorePersonEmailAddressMapping(
          nomisPrisonNumber = "A1234AA",
          cprId = cprEmailAddressId,
          nomisId = nomisEmailAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @AfterAll
    fun tearDown() = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/core-person/email/cpr-email-address-id/{cprEmailAddressId}", cprEmailAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/email/cpr-email-address-id/{cprEmailAddressId}", cprEmailAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/email/cpr-email-address-id/{cprEmailAddressId}", cprEmailAddressId)
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
          .uri("/mapping/core-person/email/cpr-email-address-id/{cprEmailAddressId}", "99999")
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
          .uri("/mapping/core-person/email/cpr-email-address-id/{cprEmailAddressId}", cprEmailAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprEmailAddressId)
          .jsonPath("nomisId").isEqualTo(nomisEmailAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("DELETE /mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}")
  inner class DeletePersonEmailByNomisId {
    private val nomisEmailAddressId = 22345L

    @BeforeAll
    fun setUp() = runTest {
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "c6f5c52c-1d13-4f86-bf2c-4a4f4f891e69",
          nomisPrisonNumber = "A1234AA",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      corePersonEmailAddressMappingRepository.save(
        CorePersonEmailAddressMapping(
          nomisPrisonNumber = "A1234AA",
          cprId = "654321",
          nomisId = nomisEmailAddressId,
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
    }

    @AfterAll
    fun tearDown() = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}", nomisEmailAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}", nomisEmailAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}", nomisEmailAddressId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping data`() = runTest {
        webTestClient.delete()
          .uri("/mapping/core-person/email/nomis-email-address-id/{nomisEmailAddressId}", nomisEmailAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(corePersonEmailAddressMappingRepository.findOneByNomisId(nomisEmailAddressId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/core-person/email")
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class CreatePersonEmailMapping {
    private val mapping = CorePersonEmailAddressMappingDto(
      cprId = "754321",
      nomisId = 32345L,
      nomisPrisonNumber = "A1234AA",
      label = null,
      mappingType = CorePersonMappingType.CPR_CREATED,
      whenCreated = null,
    )

    @BeforeAll
    fun setUp() = runTest {
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "d6f5c52c-1d13-4f86-bf2c-4a4f4f891e69",
          nomisPrisonNumber = "A1234AA",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
    }

    @AfterAll
    fun tearDown() = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/email")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(mapping)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/email")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(mapping)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/email")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(mapping)
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Validation {
      private val existingMapping = CorePersonEmailAddressMappingDto(
        cprId = "854321",
        nomisId = 42345L,
        nomisPrisonNumber = mapping.nomisPrisonNumber,
        label = mapping.label,
        mappingType = mapping.mappingType,
        whenCreated = mapping.whenCreated,
      )

      @BeforeAll
      fun setUp() = runTest {
        corePersonEmailAddressMappingRepository.save(
          CorePersonEmailAddressMapping(
            cprId = existingMapping.cprId,
            nomisId = existingMapping.nomisId,
            nomisPrisonNumber = existingMapping.nomisPrisonNumber,
            mappingType = existingMapping.mappingType,
          ),
        )
      }

      @Test
      fun `will not allow a duplicate email address mapping`() {
        webTestClient.post()
          .uri("/mapping/core-person/email")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(existingMapping)
          .exchange()
          .expectStatus().isDuplicateMapping
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will persist the person email mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/email")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(mapping)
          .exchange()
          .expectStatus().isCreated

        val persisted = corePersonEmailAddressMappingRepository.findOneByNomisId(mapping.nomisId)!!
        assertThat(persisted.cprId).isEqualTo(mapping.cprId)
        assertThat(persisted.nomisPrisonNumber).isEqualTo(mapping.nomisPrisonNumber)
        assertThat(persisted.mappingType).isEqualTo(mapping.mappingType)
      }
    }
  }

  private fun deleteAll() = runTest {
    corePersonEmailAddressMappingRepository.deleteAll()
    corePersonMappingRepository.deleteAll()
  }
}
