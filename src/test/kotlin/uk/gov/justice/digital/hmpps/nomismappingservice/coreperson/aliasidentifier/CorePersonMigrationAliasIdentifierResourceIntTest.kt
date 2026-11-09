package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier

import kotlinx.coroutines.flow.count
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingIdDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class CorePersonMigrationAliasIdentifierResourceIntTest(
  @Autowired private val offenderAliasMappingRepository: OffenderAliasMappingRepository,
  @Autowired private val offenderIdentifierMappingRepository: OffenderIdentifierMappingRepository,
  @Autowired private val corePersonMigrationAliasIdentifierMappingRepository: CorePersonMigrationAliasIdentifierMappingRepository,
) : IntegrationTestBase() {

  private suspend fun deleteAllMappings() {
    offenderAliasMappingRepository.deleteAll()
    offenderIdentifierMappingRepository.deleteAll()
    corePersonMigrationAliasIdentifierMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST /mapping/core-person/alias-identifier-migration")
  inner class CreateMappings {

    @AfterEach
    fun tearDown() = runTest { deleteAllMappings() }

    @Nested
    inner class Security {
      val mappings = CorePersonAliasIdentifierMappingsDto(
        personMapping = CorePersonMappingIdDto(
          cprId = UUID.randomUUID().toString(),
          nomisPrisonNumber = "A1234BC",
        ),
        label = null,
        mappingType = CorePersonMappingType.CPR_CREATED,
        whenCreated = LocalDateTime.now(),
        aliases = emptyList(),
        identifiers = emptyList(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier-migration")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier-migration")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier-migration")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingCorePersonMapping: CorePersonMigrationAliasIdentifier

      val mappings = CorePersonAliasIdentifierMappingsDto(
        personMapping = CorePersonMappingIdDto(
          cprId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
          nomisPrisonNumber = "A1234BC",
        ),
        label = null,
        mappingType = CorePersonMappingType.MIGRATED,
        whenCreated = LocalDateTime.now(),
        aliases = emptyList(),
        identifiers = emptyList(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingCorePersonMapping = corePersonMigrationAliasIdentifierMappingRepository.save(
          CorePersonMigrationAliasIdentifier(
            cprId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
            nomisPrisonNumber = "A1234BC",
            label = "2023-01-01T12:45:12",
            mappingType = CorePersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `returns 400 when a required field is missing`() {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("""{ "aliases": [], "identifiers": [] }""")
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `will not allow the same core person to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/core-person/alias-identifier-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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

      @Test
      fun `will not persist any child mappings when the core person mapping is a duplicate`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                aliases = listOf(
                  OffenderAliasMappingDto(
                    cprId = "18e89dec-6ace-4706-9283-8e11e9ebe886",
                    nomisOffenderId = 10000L,
                    nomisPrisonNumber = "A1234BC",
                    label = "2023-01-01T12:45:12",
                    mappingType = CorePersonMappingType.MIGRATED,
                    whenCreated = LocalDateTime.now(),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isDuplicateMapping

        assertThat(offenderAliasMappingRepository.findOneByCprId("18e89dec-6ace-4706-9283-8e11e9ebe886")).isNull()
      }
    }

    @Nested
    inner class HappyPath {
      val mappings = CorePersonAliasIdentifierMappingsDto(
        personMapping = CorePersonMappingIdDto(
          cprId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
          nomisPrisonNumber = "A1234BC",
        ),
        label = null,
        mappingType = CorePersonMappingType.CPR_CREATED,
        whenCreated = LocalDateTime.now(),
        aliases = emptyList(),
        identifiers = emptyList(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the core person mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings.copy(label = "2023-01-01T12:45:12")))
          .exchange()
          .expectStatus().isCreated

        val corePersonMapping =
          corePersonMigrationAliasIdentifierMappingRepository.findOneByNomisPrisonNumber(mappings.personMapping.nomisPrisonNumber)!!

        assertThat(corePersonMapping.cprId).isEqualTo(mappings.personMapping.cprId)
        assertThat(corePersonMapping.nomisPrisonNumber).isEqualTo(mappings.personMapping.nomisPrisonNumber)
        assertThat(corePersonMapping.label).isEqualTo("2023-01-01T12:45:12")
        assertThat(corePersonMapping.mappingType).isEqualTo(mappings.mappingType)
        assertThat(corePersonMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }

      @Test
      fun `will persist the offender alias mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                aliases = listOf(
                  OffenderAliasMappingDto(
                    cprId = "0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6",
                    nomisOffenderId = 10000L,
                    nomisPrisonNumber = "A1234BC",
                    label = "2023-01-01T12:45:12",
                    mappingType = CorePersonMappingType.MIGRATED,
                    whenCreated = LocalDateTime.now(),
                  ),
                  OffenderAliasMappingDto(
                    cprId = "37611e56-3b4e-4cfa-994d-6c089794fd1b",
                    nomisOffenderId = 10001L,
                    nomisPrisonNumber = "A1234BC",
                    label = "2024-01-01T12:45:12",
                    mappingType = CorePersonMappingType.MIGRATED,
                    whenCreated = LocalDateTime.now(),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(offenderAliasMappingRepository.findOneByCprId("0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6")!!) {
          assertThat(label).isEqualTo("2023-01-01T12:45:12")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.MIGRATED)
          assertThat(nomisPrisonNumber).isEqualTo("A1234BC")
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          assertThat(nomisOffenderId).isEqualTo(10000L)
        }

        with(offenderAliasMappingRepository.findOneByCprId("37611e56-3b4e-4cfa-994d-6c089794fd1b")!!) {
          assertThat(label).isEqualTo("2024-01-01T12:45:12")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.MIGRATED)
          assertThat(nomisPrisonNumber).isEqualTo("A1234BC")
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          assertThat(nomisOffenderId).isEqualTo(10001L)
        }
      }

      @Test
      fun `will persist the offender identifier mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                identifiers = listOf(
                  OffenderIdentifierMappingDto(
                    cprId = "8dc8b0c7-2fd8-487e-897b-f1ce83e27c65",
                    nomisOffenderId = 10000L,
                    nomisIdentifierSequence = 1,
                    nomisPrisonNumber = "A1234BC",
                    label = "2024-02-01T12:45:12",
                    mappingType = CorePersonMappingType.MIGRATED,
                    whenCreated = LocalDateTime.now(),
                  ),
                  OffenderIdentifierMappingDto(
                    cprId = "b0e578e3-5075-4404-8f17-4d2f71b43619",
                    nomisOffenderId = 10000L,
                    nomisIdentifierSequence = 2,
                    nomisPrisonNumber = "A1234BC",
                    label = "2024-03-01T12:45:12",
                    mappingType = CorePersonMappingType.MIGRATED,
                    whenCreated = LocalDateTime.now(),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(offenderIdentifierMappingRepository.findOneByCprId("8dc8b0c7-2fd8-487e-897b-f1ce83e27c65")!!) {
          assertThat(label).isEqualTo("2024-02-01T12:45:12")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.MIGRATED)
          assertThat(nomisPrisonNumber).isEqualTo("A1234BC")
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          assertThat(nomisOffenderId).isEqualTo(10000L)
          assertThat(nomisIdentifierSequence).isEqualTo(1)
        }
        with(offenderIdentifierMappingRepository.findOneByCprId("b0e578e3-5075-4404-8f17-4d2f71b43619")!!) {
          assertThat(label).isEqualTo("2024-03-01T12:45:12")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.MIGRATED)
          assertThat(nomisPrisonNumber).isEqualTo("A1234BC")
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          assertThat(nomisOffenderId).isEqualTo(10000L)
          assertThat(nomisIdentifierSequence).isEqualTo(2)
        }
      }

      @Test
      fun `will replace any existing alias and identifier mappings for the prisoner`() = runTest {
        offenderAliasMappingRepository.save(
          OffenderAliasMapping(
            cprId = "18e89dec-6ace-4706-9283-8e11e9ebe886",
            nomisOffenderId = 20000L,
            nomisPrisonNumber = "A1234BC",
            label = "2022-01-01T12:45:12",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
          ),
        )
        offenderIdentifierMappingRepository.save(
          OffenderIdentifierMapping(
            cprId = "ffc0d3aa-6a6c-4f89-9c6c-0e9206629f5c",
            nomisOffenderId = 20000L,
            nomisIdentifierSequence = 1,
            nomisPrisonNumber = "A1234BC",
            label = "2022-01-01T12:45:12",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
          ),
        )

        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated

        assertThat(offenderAliasMappingRepository.findOneByCprId("18e89dec-6ace-4706-9283-8e11e9ebe886")).isNull()
        assertThat(offenderIdentifierMappingRepository.findOneByCprId("ffc0d3aa-6a6c-4f89-9c6c-0e9206629f5c")).isNull()
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/alias-identifier-migration/migration-id/{migrationId}")
  inner class GetCorePersonMappingsByMigrationId {

    @AfterEach
    fun tearDown() = runTest { deleteAllMappings() }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/core-person/alias-identifier-migration/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/core-person/alias-identifier-migration/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/core-person/alias-identifier-migration/migration-id/2022-01-01T00:00:00")
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
          corePersonMigrationAliasIdentifierMappingRepository.save(
            CorePersonMigrationAliasIdentifier(
              cprId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
              nomisPrisonNumber = "A123${it}BC",
              label = "2023-01-01T12:45:12",
              mappingType = CorePersonMappingType.MIGRATED,
            ),
          )
        }

        corePersonMigrationAliasIdentifierMappingRepository.save(
          CorePersonMigrationAliasIdentifier(
            cprId = "edcd118c-91ba-42ea-b5c4-404b453ad58b",
            nomisPrisonNumber = "A4321BC",
            label = "2022-01-01T12:43:12",
            mappingType = CorePersonMappingType.MIGRATED,
          ),
        )

        webTestClient.get().uri("/mapping/core-person/alias-identifier-migration/migration-id/2023-01-01T12:45:12")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(4)
          .jsonPath("$.content..nomisPrisonNumber").value<List<String>> {
            assertThat(it).containsExactly("A1231BC", "A1232BC", "A1233BC", "A1234BC")
          }
          .jsonPath("$.content[0].whenCreated").isNotEmpty
      }

      @Test
      fun `200 response even when no mappings are found`() {
        webTestClient.get().uri("/mapping/core-person/alias-identifier-migration/migration-id/2044-01-01")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(0)
          .jsonPath("content").isEmpty
      }

      @Test
      fun `can request a different page size`() = runTest {
        (1L..6L).forEach {
          corePersonMigrationAliasIdentifierMappingRepository.save(
            CorePersonMigrationAliasIdentifier(
              cprId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
              nomisPrisonNumber = "A${it}123BC",
              label = "2023-01-01T12:45:12",
              mappingType = CorePersonMappingType.MIGRATED,
            ),
          )
        }
        webTestClient.get().uri {
          it.path("/mapping/core-person/alias-identifier-migration/migration-id/2023-01-01T12:45:12")
            .queryParam("size", "2")
            .queryParam("sort", "nomisPrisonNumber,asc")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
  @DisplayName("GET /mapping/core-person/alias-identifier-migration/person/nomis-prison-number/{nomisPrisonNumber}")
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class GetCorePersonByNomisPrisonNumber {
    private val nomisPrisonNumber = "A1234BC"

    @BeforeAll
    fun setUp() = runTest {
      corePersonMigrationAliasIdentifierMappingRepository.save(
        CorePersonMigrationAliasIdentifier(
          cprId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @AfterAll
    fun tearDown() = runTest { deleteAllMappings() }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/core-person/alias-identifier-migration/person/nomis-prison-number/{nomisPrisonNumber}", nomisPrisonNumber)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/alias-identifier-migration/person/nomis-prison-number/{nomisPrisonNumber}", nomisPrisonNumber)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/alias-identifier-migration/person/nomis-prison-number/{nomisPrisonNumber}", nomisPrisonNumber)
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
          .uri("/mapping/core-person/alias-identifier-migration/person/nomis-prison-number/{nomisPrisonNumber}", 99999)
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
          .uri("/mapping/core-person/alias-identifier-migration/person/nomis-prison-number/{nomisPrisonNumber}", nomisPrisonNumber)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
  @DisplayName("DELETE /mapping/core-person/alias-identifier-migration")
  inner class DeleteAllMappings {
    @BeforeEach
    fun setUp() {
      val mappings = CorePersonAliasIdentifierMappingsDto(
        personMapping = CorePersonMappingIdDto(
          cprId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
          nomisPrisonNumber = "A1234BC",
        ),
        label = null,
        mappingType = CorePersonMappingType.CPR_CREATED,
        whenCreated = LocalDateTime.now(),
        aliases = listOf(
          OffenderAliasMappingDto(
            cprId = "96f9ea13-9c2a-4e05-8128-f32778edd9e9",
            nomisOffenderId = 10000L,
            nomisPrisonNumber = "A1234BC",
            label = "2025-03-01T12:45:12",
            mappingType = CorePersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.now(),
          ),
        ),
        identifiers = listOf(
          OffenderIdentifierMappingDto(
            cprId = "7d2d6155-1a2d-404c-83d0-838dadd64f85",
            nomisOffenderId = 10000L,
            nomisIdentifierSequence = 1,
            nomisPrisonNumber = "A1234BC",
            label = "2024-03-01T12:45:12",
            mappingType = CorePersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.now(),
          ),
          OffenderIdentifierMappingDto(
            cprId = "96f9ea13-9c2a-4e05-8128-f32778edd9e8",
            nomisOffenderId = 10000L,
            nomisIdentifierSequence = 2,
            nomisPrisonNumber = "A1234BC",
            label = "2025-03-01T12:45:12",
            mappingType = CorePersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.now(),
          ),
        ),
      )
      webTestClient.post()
        .uri("/mapping/core-person/alias-identifier-migration")
        .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(mappings))
        .exchange()
        .expectStatus().isCreated
    }

    @AfterEach
    fun tearDown() = runTest { deleteAllMappings() }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/core-person/alias-identifier-migration")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/alias-identifier-migration")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/alias-identifier-migration")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns 204 when all mappings are deleted`() = runTest {
        assertThat(offenderAliasMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(offenderIdentifierMappingRepository.findAll().count()).isEqualTo(2)
        assertThat(corePersonMigrationAliasIdentifierMappingRepository.findAll().count()).isEqualTo(1)

        webTestClient.delete()
          .uri("/mapping/core-person/alias-identifier-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(offenderAliasMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(offenderIdentifierMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(corePersonMigrationAliasIdentifierMappingRepository.findAll().count()).isEqualTo(0)
      }
    }
  }
}
