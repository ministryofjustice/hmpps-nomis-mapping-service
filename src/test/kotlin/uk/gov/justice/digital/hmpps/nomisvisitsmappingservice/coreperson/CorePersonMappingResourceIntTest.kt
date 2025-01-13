package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.coreperson

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hamcrest.Matchers
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

  @Autowired
  private lateinit var corePersonAddressMappingRepository: CorePersonAddressMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    corePersonAddressMappingRepository.deleteAll()
    corePersonMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST mapping/core-person/migrate")
  inner class CreateMappings {

    @Nested
    inner class Security {
      val mappings = CorePersonMappingsDto(
        personMapping = CorePersonMappingIdDto(
          cprId = UUID.randomUUID().toString(),
          nomisPrisonNumber = "A1234BC",
        ),
        label = null,
        mappingType = CorePersonMappingType.CPR_CREATED,
        whenCreated = LocalDateTime.now(),
        addressMappings = emptyList(),
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
        personMapping = CorePersonMappingIdDto(
          cprId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
          nomisPrisonNumber = "A1234BC",
        ),
        label = null,
        mappingType = CorePersonMappingType.MIGRATED,
        whenCreated = LocalDateTime.now(),
        addressMappings = emptyList(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingCorePersonMapping = corePersonMappingRepository.save(
          CorePersonMapping(
            cprId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
            nomisPrisonNumber = "A1234BC",
            label = "2023-01-01T12:45:12",
            mappingType = CorePersonMappingType.MIGRATED,
          ),
        )
        corePersonAddressMappingRepository.save(
          CorePersonAddressMapping(
            cprId = "18e89dec-6ace-4706-9283-8e11e9ebe886",
            nomisId = 54321,
            // Do we need this?
            // nomisSequenceNumber = 1,
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
            .containsEntry("nomisPrisonNumber", existingCorePersonMapping.nomisPrisonNumber)
            .containsEntry("cprId", existingCorePersonMapping.cprId)
            .containsEntry("mappingType", existingCorePersonMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisPrisonNumber", mappings.personMapping.nomisPrisonNumber)
            .containsEntry("cprId", mappings.personMapping.cprId)
            .containsEntry("mappingType", existingCorePersonMapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mappings = CorePersonMappingsDto(
        personMapping = CorePersonMappingIdDto(
          cprId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
          nomisPrisonNumber = "A1234BC",
        ),
        label = null,
        mappingType = CorePersonMappingType.CPR_CREATED,
        whenCreated = LocalDateTime.now(),
        addressMappings = emptyList(),
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

        val corePersonMapping =
          corePersonMappingRepository.findOneByNomisPrisonNumber(mappings.personMapping.nomisPrisonNumber)!!

        assertThat(corePersonMapping.cprId).isEqualTo(mappings.personMapping.cprId)
        assertThat(corePersonMapping.nomisPrisonNumber).isEqualTo(mappings.personMapping.nomisPrisonNumber)
        assertThat(corePersonMapping.label).isEqualTo("2023-01-01T12:45:12")
        assertThat(corePersonMapping.mappingType).isEqualTo(mappings.mappingType)
        assertThat(corePersonMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }

      @Test
      fun `will persist the person address mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                addressMappings = listOf(
                  CorePersonSimpleMappingIdDto(cprId = "0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6", nomisId = 1),
                  CorePersonSimpleMappingIdDto(cprId = "e96babce-4a24-49d7-8447-b45f8768f6c1", nomisId = 2),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(corePersonAddressMappingRepository.findOneByNomisId(1)!!) {
          assertThat(cprId).isEqualTo("0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6")
          assertThat(nomisId).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(java.time.LocalDateTime.now(), within(10, java.time.temporal.ChronoUnit.SECONDS))
        }
        with(corePersonAddressMappingRepository.findOneByCprId("e96babce-4a24-49d7-8447-b45f8768f6c1")!!) {
          assertThat(cprId).isEqualTo("e96babce-4a24-49d7-8447-b45f8768f6c1")
          assertThat(nomisId).isEqualTo(2L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(java.time.LocalDateTime.now(), within(10, java.time.temporal.ChronoUnit.SECONDS))
        }
      }
    }
  }

  @DisplayName("GET /mapping/core-person/migration-id/{migrationId}")
  @Nested
  inner class GetCorePersonMappingsByMigrationId {

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/core-person/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/core-person/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/core-person/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `can retrieve all mappings by migration Id`() = runTest {
        (1L..4L).forEach {
          corePersonMappingRepository.save(
            CorePersonMapping(
              cprId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
              nomisPrisonNumber = "A123${it}BC",
              label = "2023-01-01T12:45:12",
              mappingType = CorePersonMappingType.MIGRATED,
            ),
          )
        }

        corePersonMappingRepository.save(
          CorePersonMapping(
            cprId = "edcd118c-91ba-42ea-b5c4-404b453ad58b",
            nomisPrisonNumber = "A4321BC",
            label = "2022-01-01T12:43:12",
            mappingType = CorePersonMappingType.MIGRATED,
          ),
        )

        webTestClient.get().uri("/mapping/core-person/migration-id/2023-01-01T12:45:12")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(4)
          .jsonPath("$.content..nomisPrisonNumber").value(
            Matchers.contains("A1231BC", "A1232BC", "A1233BC", "A1234BC"),
          )
          .jsonPath("$.content[0].whenCreated").isNotEmpty
      }

      @Test
      fun `200 response even when no mappings are found`() {
        webTestClient.get().uri("/mapping/core-person/migration-id/2044-01-01")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(0)
          .jsonPath("content").isEmpty
      }

      @Test
      fun `can request a different page size`() = runTest {
        (1L..6L).forEach {
          corePersonMappingRepository.save(
            CorePersonMapping(
              cprId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
              nomisPrisonNumber = "A${it}123BC",
              label = "2023-01-01T12:45:12",
              mappingType = CorePersonMappingType.MIGRATED,
            ),
          )
        }
        webTestClient.get().uri {
          it.path("/mapping/core-person/migration-id/2023-01-01T12:45:12")
            .queryParam("size", "2")
            .queryParam("sort", "nomisPrisonNumber,asc")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(6)
          .jsonPath("numberOfElements").isEqualTo(2)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(3)
          .jsonPath("size").isEqualTo(2)
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/person/nomis-prison-number/{nomisPrisonNumber}")
  inner class GetCorePersonByNomisPrisonNumber {
    private val nomisPrisonNumber = "A1234BC"
    private lateinit var personMapping: CorePersonMapping

    @BeforeEach
    fun setUp() = runTest {
      personMapping = corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/core-person/person/nomis-prison-number/{nomisPrisonNumber}", nomisPrisonNumber)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/person/nomis-prison-number/{nomisPrisonNumber}", nomisPrisonNumber)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/person/nomis-prison-number/{nomisPrisonNumber}", nomisPrisonNumber)
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
          .uri("/mapping/core-person/person/nomis-prison-number/{nomisPrisonNumber}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/core-person/person/nomis-prison-number/{nomisPrisonNumber}", nomisPrisonNumber)
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo("edcd118c-41ba-42ea-b5c4-404b453ad58b")
          .jsonPath("nomisPrisonNumber").isEqualTo(nomisPrisonNumber)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/person/cpr-id/{cprId}")
  inner class GetCorePersonByCprId {
    private val cprCoreId = "12345"
    private lateinit var personMapping: CorePersonMapping

    @BeforeEach
    fun setUp() = runTest {
      personMapping = corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = cprCoreId,
          nomisPrisonNumber = "A1234BC",
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/core-person/person/cpr-id/{cprId}", cprCoreId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/person/cpr-id/{cprId}", cprCoreId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/person/cpr-id/{cprId}", cprCoreId)
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
          .uri("/mapping/core-person/person/cpr-id/{cprId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/core-person/person/cpr-id/{cprId}", cprCoreId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprCoreId)
          .jsonPath("nomisPrisonNumber").isEqualTo("A1234BC")
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/address/cpr-address-id/{cprAddressId}")
  inner class GetPersonAddressByCprId {
    private val nomisPersonAddressId = 7654321L
    private val cprCorePersonAddressId = "1234567"
    private lateinit var personAddressMapping: CorePersonAddressMapping

    @BeforeEach
    fun setUp() = runTest {
      personAddressMapping = corePersonAddressMappingRepository.save(
        CorePersonAddressMapping(
          cprId = cprCorePersonAddressId,
          nomisId = nomisPersonAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", cprCorePersonAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", cprCorePersonAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", cprCorePersonAddressId)
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
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", cprCorePersonAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprCorePersonAddressId)
          .jsonPath("nomisId").isEqualTo(nomisPersonAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }
}
