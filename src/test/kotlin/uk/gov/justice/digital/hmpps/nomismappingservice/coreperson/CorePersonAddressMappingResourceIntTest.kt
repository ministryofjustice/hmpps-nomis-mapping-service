package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import kotlinx.coroutines.flow.toList
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
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import java.time.LocalDateTime

class CorePersonAddressMappingResourceIntTest(
  @Autowired private val corePersonAddressUsageMappingRepository: CorePersonAddressUsageMappingRepository,
  @Autowired private val corePersonAddressMappingRepository: CorePersonAddressMappingRepository,
  @Autowired private val corePersonMappingRepository: CorePersonMappingRepository,
) : IntegrationTestBase() {
  private fun deleteAll() = runTest {
    corePersonAddressUsageMappingRepository.deleteAll()
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
    private val nomisAddressUsageCprId = "123456"

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
      corePersonAddressUsageMappingRepository.save(
        CorePersonAddressUsageMapping(
          nomisPrisonNumber = "C1234CC",
          cprId = nomisAddressUsageCprId,
          nomisId = nomisAddressId,
          addressUsageCode = "HOME",
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
        assertThat(corePersonAddressMappingRepository.findOneByNomisId(nomisAddressId)).isNotNull()
        assertThat(corePersonAddressUsageMappingRepository.findOneByCprId(nomisAddressUsageCprId)).isNotNull()
        webTestClient.delete()
          .uri("/mapping/core-person/address/nomis-address-id/{nomisAddressId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(corePersonAddressMappingRepository.findOneByNomisId(nomisAddressId)).isNull()
        assertThat(corePersonAddressUsageMappingRepository.findOneByCprId(nomisAddressUsageCprId)).isNull()
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
      private val mappingWithUsages = CorePersonAddressMappingDto(
        cprId = mapping.cprId,
        nomisId = mapping.nomisId,
        nomisPrisonNumber = mapping.nomisPrisonNumber,
        label = null,
        mappingType = mapping.mappingType,
        whenCreated = null,
        addressUsageMappings = listOf(
          addressUsageMapping(cprId = "854321", nomisId = mapping.nomisId, addressUsageCode = "HOME"),
          addressUsageMapping(cprId = "854322", nomisId = mapping.nomisId, addressUsageCode = "BUS"),
        ),
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

        @Test
        fun `will not persist address usage mappings when the address mapping is a duplicate`() = runTest {
          webTestClient.post()
            .uri("/mapping/core-person/address")
            .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mappingWithUsages)
            .exchange()
            .expectStatus().isDuplicateMapping

          mappingWithUsages.addressUsageMappings!!.forEach {
            assertThat(corePersonAddressUsageMappingRepository.findOneByCprId(it.cprId)).isNull()
          }
        }
      }

      @Nested
      inner class DuplicateAddressUsage {
        @BeforeEach
        fun setUp() = runTest {
          corePersonAddressUsageMappingRepository.save(mappingWithUsages.addressUsageMappings!!.first().toEntity())
        }

        @Test
        fun `will not persist the address mapping when an address usage mapping is a duplicate`() = runTest {
          webTestClient.post()
            .uri("/mapping/core-person/address")
            .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mappingWithUsages)
            .exchange()
            .expectStatus().isDuplicateMapping

          assertThat(corePersonAddressMappingRepository.findOneByNomisId(mapping.nomisId)).isNull()
          assertThat(corePersonAddressUsageMappingRepository.findOneByCprId(mappingWithUsages.addressUsageMappings!!.last().cprId)).isNull()
        }
      }

      @AfterEach
      fun tearDownPersonAddressMapping() = runTest {
        corePersonAddressUsageMappingRepository.deleteAll()
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
          assertThat(corePersonAddressUsageMappingRepository.findAll().toList().filter { it.nomisId == mapping.nomisId }).isEmpty()
        }

        @Test
        fun `will persist the person address mapping and address usage mappings`() = runTest {
          webTestClient.post()
            .uri("/mapping/core-person/address")
            .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mappingWithUsages)
            .exchange()
            .expectStatus().isCreated

          val persisted = corePersonAddressMappingRepository.findOneByNomisId(mappingWithUsages.nomisId)!!
          assertThat(persisted.cprId).isEqualTo(mappingWithUsages.cprId)
          assertThat(persisted.nomisPrisonNumber).isEqualTo(mappingWithUsages.nomisPrisonNumber)
          assertThat(persisted.mappingType).isEqualTo(mappingWithUsages.mappingType)

          mappingWithUsages.addressUsageMappings!!.forEach { expected ->
            with(corePersonAddressUsageMappingRepository.findOneByCprId(expected.cprId)!!) {
              assertThat(nomisId).isEqualTo(expected.nomisId)
              assertThat(addressUsageCode).isEqualTo(expected.addressUsageCode)
              assertThat(nomisPrisonNumber).isEqualTo(expected.nomisPrisonNumber)
              assertThat(label).isEqualTo(expected.label)
              assertThat(mappingType).isEqualTo(expected.mappingType)
            }
          }
        }
      }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("GET /mapping/core-person/address-usage/nomis-address-id/{nomisAddressId}/usage-code/{addressUsageCode}")
  inner class GetAddressUsageByNomisId {
    private val nomisAddressId = 7654321L
    private val addressUsageCode = "HOME"
    private val cprAddressUsageId = "1234567"
    private val uri = "/mapping/core-person/address-usage/nomis-address-id/$nomisAddressId/usage-code/$addressUsageCode"
    private val mapping = addressUsageMapping(cprAddressUsageId, nomisAddressId, addressUsageCode)

    @BeforeAll
    fun setUp() = runTest {
      corePersonAddressUsageMappingRepository.save(mapping.toEntity())
    }

    @AfterAll
    fun tearDown() = runTest {
      corePersonAddressUsageMappingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri(uri).exchange().expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri(uri).headers(setAuthorisation(roles = listOf())).exchange().expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri(uri).headers(setAuthorisation(roles = listOf("BANANAS"))).exchange().expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/core-person/address-usage/nomis-address-id/99999/usage-code/HOME")
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
          .uri(uri)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprAddressUsageId)
          .jsonPath("nomisId").isEqualTo(nomisAddressId)
          .jsonPath("addressUsageCode").isEqualTo(addressUsageCode)
          .jsonPath("nomisPrisonNumber").isEqualTo("A1234BA")
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("GET /mapping/core-person/address-usage/cpr-address-usage-id/{cprAddressUsageId}")
  inner class GetAddressUsageByCprId {
    private val cprAddressUsageId = "1234567"
    private val mapping = addressUsageMapping(cprAddressUsageId)
    private val uri = "/mapping/core-person/address-usage/cpr-address-usage-id/$cprAddressUsageId"

    @BeforeAll
    fun setUp() = runTest {
      corePersonAddressUsageMappingRepository.save(mapping.toEntity())
    }

    @AfterAll
    fun tearDown() = runTest {
      corePersonAddressUsageMappingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri(uri).exchange().expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri(uri).headers(setAuthorisation(roles = listOf())).exchange().expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri(uri).headers(setAuthorisation(roles = listOf("BANANAS"))).exchange().expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/core-person/address-usage/cpr-address-usage-id/99999")
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
          .uri(uri)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprAddressUsageId)
          .jsonPath("nomisId").isEqualTo(7654321)
          .jsonPath("addressUsageCode").isEqualTo("HOME")
      }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("POST /mapping/core-person/address-usage")
  inner class CreateAddressUsage {
    private val mapping = addressUsageMapping("1234567")
    private val uri = "/mapping/core-person/address-usage"

    @AfterEach
    fun tearDownAddressUsage() = runTest {
      corePersonAddressUsageMappingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post().uri(uri).contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping)).exchange().expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri(uri).headers(setAuthorisation(roles = listOf())).contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping)).exchange().expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri(uri).headers(setAuthorisation(roles = listOf("BANANAS"))).contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping)).exchange().expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns duplicate mapping details when mapping already exists`() = runTest {
        corePersonAddressUsageMappingRepository.save(mapping.toEntity())
        webTestClient.post().uri(uri)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create the mapping`() = runTest {
        webTestClient.post().uri(uri)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        with(corePersonAddressUsageMappingRepository.findOneByCprId(mapping.cprId)!!) {
          assertThat(nomisId).isEqualTo(mapping.nomisId)
          assertThat(addressUsageCode).isEqualTo(mapping.addressUsageCode)
          assertThat(nomisPrisonNumber).isEqualTo(mapping.nomisPrisonNumber)
          assertThat(label).isEqualTo(mapping.label)
          assertThat(mappingType).isEqualTo(mapping.mappingType)
          assertThat(whenCreated).isEqualTo(mapping.whenCreated)
        }
      }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("DELETE /mapping/core-person/address-usage/nomis-address-id/{nomisAddressId}/usage-code/{addressUsageCode}")
  inner class DeleteAddressUsageByNomisId {
    private val nomisAddressId = 7654321L
    private val addressUsageCode = "HOME"
    private val mapping = addressUsageMapping("1234567", nomisAddressId, addressUsageCode)
    private val uri = "/mapping/core-person/address-usage/nomis-address-id/$nomisAddressId/usage-code/$addressUsageCode"

    @AfterEach
    fun tearDownAddressUsage() = runTest {
      corePersonAddressUsageMappingRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete().uri(uri).exchange().expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri(uri).headers(setAuthorisation(roles = listOf())).exchange().expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri(uri).headers(setAuthorisation(roles = listOf("BANANAS"))).exchange().expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping`() = runTest {
        corePersonAddressUsageMappingRepository.save(mapping.toEntity())
        webTestClient.delete().uri(uri)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
        assertThat(corePersonAddressUsageMappingRepository.findOneByCprId(mapping.cprId)).isNull()
      }
    }
  }

  private fun addressUsageMapping(
    cprId: String,
    nomisId: Long = 7654321L,
    addressUsageCode: String = "HOME",
  ) = CorePersonAddressUsageMappingDto(
    cprId = cprId,
    nomisId = nomisId,
    addressUsageCode = addressUsageCode,
    nomisPrisonNumber = "A1234BA",
    label = "2023-01-01T12:45:12",
    mappingType = CorePersonMappingType.MIGRATED,
    whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
  )

  private fun CorePersonAddressUsageMappingDto.toEntity() = CorePersonAddressUsageMapping(
    nomisPrisonNumber = nomisPrisonNumber,
    cprId = cprId,
    nomisId = nomisId,
    addressUsageCode = addressUsageCode,
    label = label,
    mappingType = mappingType,
    whenCreated = whenCreated,
  )
}
