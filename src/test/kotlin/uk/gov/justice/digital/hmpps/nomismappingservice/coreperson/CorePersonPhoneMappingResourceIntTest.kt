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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CorePersonPhoneMappingResourceIntTest(
  @Autowired private val corePersonMappingRepository: CorePersonMappingRepository,
  @Autowired private val corePersonPhoneMappingRepository: CorePersonPhoneMappingRepository,
) : IntegrationTestBase() {

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("GET /mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}")
  inner class GetPersonPhoneByNomisId {
    private val nomisPhoneId = 12345L
    private val cprPhoneId = "54321"
    private lateinit var personPhoneMapping: CorePersonPhoneMapping

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
      personPhoneMapping = corePersonPhoneMappingRepository.save(
        CorePersonPhoneMapping(
          nomisPrisonNumber = "B1234BB",
          cprId = cprPhoneId,
          nomisId = nomisPhoneId,
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
          .uri("/mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
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
          .uri("/mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}", 99999)
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
          .uri("/mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprPhoneId)
          .jsonPath("nomisId").isEqualTo(nomisPhoneId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("GET /mapping/core-person/phone/cpr-phone-id/{cprPhoneId}")
  inner class GetPersonPhoneByCprId {
    private val nomisInternetAddressId = 7654321L
    private val cprPhoneId = "1234567"
    private lateinit var personPhoneMapping: CorePersonPhoneMapping

    @BeforeAll
    fun setUp() = runTest {
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "b6f5c52c-1d13-4f86-bf2c-4a4f4f891e69",
          nomisPrisonNumber = "B1234BB",
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      personPhoneMapping = corePersonPhoneMappingRepository.save(
        CorePersonPhoneMapping(
          nomisPrisonNumber = "B1234BB",
          cprId = cprPhoneId,
          nomisId = nomisInternetAddressId,
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
          .uri("/mapping/core-person/phone/cpr-phone-id/{cprPhoneId}", cprPhoneId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/phone/cpr-phone-id/{cprPhoneId}", cprPhoneId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/phone/cpr-phone-id/{cprPhoneId}", cprPhoneId)
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
          .uri("/mapping/core-person/phone/cpr-phone-id/{cprPhoneId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 when mapping not found even when address phone with same ID exists`() {
        webTestClient.get()
          .uri("/mapping/core-person/phone/cpr-core-address-phone-id/{cprCoreAddressPhoneId}", cprPhoneId)
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
          .uri("/mapping/core-person/phone/cpr-phone-id/{cprPhoneId}", cprPhoneId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprPhoneId)
          .jsonPath("nomisId").isEqualTo(nomisInternetAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("DELETE /mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}")
  inner class DeletePersonPhoneByNomisId {
    private val nomisPhoneId = 22345L

    @BeforeAll
    fun setUp() = runTest {
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "c6f5c52c-1d13-4f86-bf2c-4a4f4f891e69",
          nomisPrisonNumber = "A1234AA",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      corePersonPhoneMappingRepository.save(
        CorePersonPhoneMapping(
          nomisPrisonNumber = "A1234AA",
          cprId = "654321",
          nomisId = nomisPhoneId,
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
          .uri("/mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
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
          .uri("/mapping/core-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(corePersonPhoneMappingRepository.findOneByNomisId(nomisPhoneId)).isNull()
      }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("POST /mapping/core-person/phone")
  inner class CreatePersonPhoneMapping {
    private val mapping = CorePersonPhoneMappingDto(
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
          .uri("/mapping/core-person/phone")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(mapping)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/phone")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(mapping)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/phone")
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
      private val existingMapping = CorePersonPhoneMappingDto(
        cprId = "854321",
        nomisId = 42345L,
        nomisPrisonNumber = mapping.nomisPrisonNumber,
        label = mapping.label,
        mappingType = mapping.mappingType,
        whenCreated = mapping.whenCreated,
      )

      @BeforeAll
      fun setUp() = runTest {
        corePersonPhoneMappingRepository.save(
          CorePersonPhoneMapping(
            nomisPrisonNumber = existingMapping.nomisPrisonNumber,
            cprId = existingMapping.cprId,
            nomisId = existingMapping.nomisId,
            mappingType = existingMapping.mappingType,
          ),
        )
      }

      @Test
      fun `will not allow a duplicate phone mapping`() {
        webTestClient.post()
          .uri("/mapping/core-person/phone")
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
      fun `will persist the person phone mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/phone")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(mapping)
          .exchange()
          .expectStatus().isCreated

        val persisted = corePersonPhoneMappingRepository.findOneByNomisId(mapping.nomisId)!!
        assertThat(persisted.cprId).isEqualTo(mapping.cprId)
        assertThat(persisted.nomisPrisonNumber).isEqualTo(mapping.nomisPrisonNumber)
        assertThat(persisted.mappingType).isEqualTo(mapping.mappingType)
      }
    }
  }

  private fun deleteAll() = runTest {
    corePersonPhoneMappingRepository.deleteAll()
    corePersonMappingRepository.deleteAll()
  }
}
