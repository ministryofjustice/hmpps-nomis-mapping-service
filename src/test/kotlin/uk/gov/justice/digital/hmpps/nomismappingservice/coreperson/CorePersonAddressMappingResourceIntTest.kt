package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import java.time.LocalDateTime

class CorePersonAddressMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var corePersonMappingRepository: CorePersonMappingRepository

  @Autowired
  private lateinit var corePersonAddressMappingRepository: CorePersonAddressMappingRepository

  private fun deleteAll() = runTest {
    corePersonAddressMappingRepository.deleteAll()
    corePersonMappingRepository.deleteAll()
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("GET /mapping/core-person/address/nomis-address-id/{nomisAddressId}")
  inner class GetPersonAddressByNomisId {
    private val nomisAddressId = 12345L
    private val cprAddressId = "54321"
    private lateinit var personAddressMapping: CorePersonAddressMapping

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
      personAddressMapping = corePersonAddressMappingRepository.save(
        CorePersonAddressMapping(
          nomisPrisonNumber = "A1234AA",
          cprId = cprAddressId,
          nomisId = nomisAddressId,
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
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", nomisAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", nomisAddressId)
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
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", 99999)
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
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprAddressId)
          .jsonPath("nomisId").isEqualTo(nomisAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("GET /mapping/core-person/address/cpr-address-id/{cprAddressId}")
  inner class GetCorePersonAddressByCprId {
    private val nomisAddressId = 7654321L
    private val cprAddressId = "1234567"
    private lateinit var personAddressMapping: CorePersonAddressMapping

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
      personAddressMapping = corePersonAddressMappingRepository.save(
        CorePersonAddressMapping(
          nomisPrisonNumber = "A1234AA",
          cprId = cprAddressId,
          nomisId = nomisAddressId,
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
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", cprAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", cprAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", cprAddressId)
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
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", "99999")
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
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", cprAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprAddressId)
          .jsonPath("nomisId").isEqualTo(nomisAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("DELETE /mapping/core-person/address/nomis-address-id/{nomisAddressId}")
  inner class DeleteCorePersonAddressByNomisId {
    private val nomisAddressId = 22345L

    @BeforeAll
    fun setUp() = runTest {
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "c6f5c52c-1d13-4f86-bf2c-4a4f4f891e69",
          nomisPrisonNumber = "C1234CC",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      corePersonAddressMappingRepository.save(
        CorePersonAddressMapping(
          nomisPrisonNumber = "C1234CC",
          cprId = "654321",
          nomisId = nomisAddressId,
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
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", nomisAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", nomisAddressId)
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
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(corePersonAddressMappingRepository.findOneByNomisId(nomisAddressId)).isNull()
      }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("POST /mapping/core-person/address")
    inner class CreateCorePersonAddressMapping {
      private val mapping = CorePersonAddressMappingDto(
        cprId = "754321",
        nomisId = 32345L,
        nomisPrisonNumber = "D1234DD",
        label = null,
        mappingType = CorePersonMappingType.CPR_CREATED,
        whenCreated = null,
      )

      @BeforeAll
      fun setUp() = runTest {
        corePersonMappingRepository.save(
          CorePersonMapping(
            cprId = "d6f5c52c-1d13-4f86-bf2c-4a4f4f891e69",
            nomisPrisonNumber = "D1234DD",
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
            .uri("/mapping/core-person/address")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapping)
            .exchange()
            .expectStatus().isUnauthorized
        }

        @Test
        fun `access forbidden when no role`() {
          webTestClient.post()
            .uri("/mapping/core-person/address")
            .headers(setAuthorisation(roles = listOf()))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapping)
            .exchange()
            .expectStatus().isForbidden
        }

        @Test
        fun `access forbidden with wrong role`() {
          webTestClient.post()
            .uri("/mapping/core-person/address")
            .headers(setAuthorisation(roles = listOf("BANANAS")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapping)
            .exchange()
            .expectStatus().isForbidden
        }
      }

      @Nested
      inner class Validation {
        @BeforeEach
        fun setUp() = runTest {
          corePersonAddressMappingRepository.save(
            CorePersonAddressMapping(
              nomisPrisonNumber = mapping.nomisPrisonNumber,
              cprId = mapping.cprId,
              nomisId = mapping.nomisId,
              mappingType = mapping.mappingType,
            ),
          )
        }

        @Test
        fun `will not allow a duplicate address mapping`() {
          webTestClient.post()
            .uri("/mapping/core-person/address")
            .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapping)
            .exchange()
            .expectStatus().isDuplicateMapping
        }
      }

      @AfterEach
      fun tearDownPersonAddressMapping() = runTest {
        corePersonAddressMappingRepository.deleteAll()
      }

      @Nested
      inner class HappyPath {
        @Test
        fun `will persist the person address mapping`() = runTest {
          webTestClient.post()
            .uri("/mapping/core-person/address")
            .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapping)
            .exchange()
            .expectStatus().isCreated

          val persisted = corePersonAddressMappingRepository.findOneByNomisId(mapping.nomisId)!!
          assertThat(persisted.cprId).isEqualTo(mapping.cprId)
          assertThat(persisted.nomisPrisonNumber).isEqualTo(mapping.nomisPrisonNumber)
          assertThat(persisted.mappingType).isEqualTo(mapping.mappingType)
        }
      }
    }
  }
}
