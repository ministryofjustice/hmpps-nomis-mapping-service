package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
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
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class CorePersonMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var corePersonMappingRepository: CorePersonMappingRepository

  @Autowired
  private lateinit var offenderAliasMappingRepository: OffenderAliasMappingRepository

  @Autowired
  private lateinit var corePersonAddressMappingRepository: CorePersonAddressMappingRepository

  @Autowired
  private lateinit var offenderIdentifierMappingRepository: OffenderIdentifierMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    offenderAliasMappingRepository.deleteAll()
    offenderIdentifierMappingRepository.deleteAll()
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
        aliases = emptyList(),
        identifiers = emptyList(),
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
        aliases = emptyList(),
        identifiers = emptyList(),
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
        offenderAliasMappingRepository.save(
          OffenderAliasMapping(
            nomisPrisonNumber = "A1234BC",
            cprId = "18e89dec-6ace-4706-9283-8e11e9ebe886",
            label = "2023-01-01T12:45:12",
            mappingType = CorePersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.now(),
            nomisOffenderId = 10000L,
          ),
        )
        offenderIdentifierMappingRepository.save(
          OffenderIdentifierMapping(
            nomisPrisonNumber = "A1234BC",
            cprId = "ffc0d3aa-6a6c-4f89-9c6c-0e9206629f5c",
            label = "2023-01-01T12:45:12",
            mappingType = CorePersonMappingType.MIGRATED,
            nomisOffenderId = 10000L,
            nomisIdentifierSequence = 1,
            whenCreated = LocalDateTime.now(),
          ),
        )
      }

      @Test
      fun `will not allow the same core person to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/core-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/core-person/migrate")
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
        aliases = emptyList(),
        identifiers = emptyList(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the core person mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
      fun `will persist the core person alias mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/migrate")
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
          assertThat(whenCreated).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.SECONDS),
          )
          assertThat(nomisOffenderId).isEqualTo(10000L)
        }

        with(offenderAliasMappingRepository.findOneByCprId("37611e56-3b4e-4cfa-994d-6c089794fd1b")!!) {
          assertThat(label).isEqualTo("2024-01-01T12:45:12")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.MIGRATED)
          assertThat(nomisPrisonNumber).isEqualTo("A1234BC")
          assertThat(whenCreated).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.SECONDS),
          )
          assertThat(nomisOffenderId).isEqualTo(10001L)
        }
      }

      @Test
      fun `will persist the core person identifier mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/migrate")
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
          assertThat(whenCreated).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.SECONDS),
          )
          assertThat(nomisIdentifierSequence).isEqualTo(1)
          assertThat(nomisOffenderId).isEqualTo(10000L)
        }
        with(offenderIdentifierMappingRepository.findOneByCprId("b0e578e3-5075-4404-8f17-4d2f71b43619")!!) {
          assertThat(label).isEqualTo("2024-03-01T12:45:12")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.MIGRATED)
          assertThat(nomisPrisonNumber).isEqualTo("A1234BC")
          assertThat(whenCreated).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.SECONDS),
          )
          assertThat(nomisIdentifierSequence).isEqualTo(2)
          assertThat(nomisOffenderId).isEqualTo(10000L)
        }
      }
    }
    // TODO add other child mapping tests when implemented
  }

  @Nested
  @DisplayName("POST /mapping/core-person/replace")
  inner class ReplaceCorePersonMappings {
    val nomisPrisonNumber = "A1234BC"

    @Nested
    inner class Security {
      val mapping = CorePersonMappingsDto(
        personMapping = CorePersonMappingIdDto(
          cprId = UUID.randomUUID().toString(),
          nomisPrisonNumber = nomisPrisonNumber,
        ),
        label = "2020-01-01T10:00",
        aliases = listOf(),
        identifiers = listOf(),
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = CorePersonMappingsDto(
        personMapping = CorePersonMappingIdDto(
          cprId = "9a1313b1-5c03-4225-b6fd-dbc5ce992760",
          nomisPrisonNumber = nomisPrisonNumber,
        ),
        label = "2020-01-01T10:00",
        aliases = listOf(
          OffenderAliasMappingDto(
            cprId = "fba8a521-fc9f-4801-bb02-3f3326c760ef",
            nomisOffenderId = 10000L,
            nomisPrisonNumber = nomisPrisonNumber,
            label = "2020-01-01T10:00",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
            whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
          ),
        ),
        identifiers = listOf(
          OffenderIdentifierMappingDto(
            cprId = "27f51212-4d32-4b8b-86c2-a2e2807e3e4e",
            nomisOffenderId = 10000L,
            nomisIdentifierSequence = 1,
            nomisPrisonNumber = nomisPrisonNumber,
            label = "2020-01-01T10:00",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
            whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
          ),
        ),
        mappingType = CorePersonMappingType.NOMIS_CREATED,
      )

      val existingAliasMapping = OffenderAliasMapping(
        cprId = "034c070d-aff5-4464-9d1e-24c20bc6f8e1",
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
        nomisOffenderId = 10000L,
      )

      val existingIdentifierMapping = OffenderIdentifierMapping(
        cprId = "8d2c37b7-3f07-4954-b9bb-5ba61357be3f",
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
        nomisOffenderId = 10000L,
        nomisIdentifierSequence = 1,
      )

      val individualMapping = CorePersonMapping(
        cprId = "fe2d494c-7652-4deb-9092-03b6b3bdd486",
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2026-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      @BeforeEach
      fun setUp() = runTest {
        offenderAliasMappingRepository.save(existingAliasMapping)
        offenderIdentifierMappingRepository.save(existingIdentifierMapping)
        corePersonMappingRepository.save(individualMapping)
      }

      @Test
      fun `returns 200 when mappings replaced`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk
      }

      @Test
      fun `will not re-persist the core person mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk

        // The original core person mapping should still be there, and not replaced
        val corePersonMapping = corePersonMappingRepository.findOneByCprId(individualMapping.cprId)!!
        assertThat(corePersonMapping.cprId).isEqualTo(individualMapping.cprId)
        assertThat(corePersonMapping.nomisPrisonNumber).isEqualTo(individualMapping.nomisPrisonNumber)
        assertThat(corePersonMapping.label).isEqualTo(individualMapping.label)
        assertThat(corePersonMapping.mappingType).isEqualTo(individualMapping.mappingType)
        assertThat(corePersonMapping.whenCreated).isEqualTo(individualMapping.whenCreated)

        // Do not expect a new core person mapping to be created for the new cprId in the mapping
        assertThat(corePersonMappingRepository.findOneByCprId(mapping.personMapping.cprId)).isNull()
      }

      @Test
      fun `will persist the alias and identifier mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk

        // New alias mapping
        with(offenderAliasMappingRepository.findOneByCprId(mapping.aliases[0].cprId)!!) {
          val expected = mapping.aliases[0] // the replacement mapping
          assertThat(nomisOffenderId).isEqualTo(expected.nomisOffenderId)
          assertThat(label).isEqualTo(expected.label)
          assertThat(mappingType).isEqualTo(expected.mappingType)
          assertThat(whenCreated).isEqualTo(expected.whenCreated)
        }

        // New identifier mapping
        with(offenderIdentifierMappingRepository.findOneByCprId(mapping.identifiers[0].cprId)!!) {
          val expected = mapping.identifiers[0] // the replacement mapping
          assertThat(cprId).isEqualTo(expected.cprId)
          assertThat(nomisOffenderId).isEqualTo(expected.nomisOffenderId)
          assertThat(nomisIdentifierSequence).isEqualTo(expected.nomisIdentifierSequence)
          assertThat(label).isEqualTo(expected.label)
          assertThat(mappingType).isEqualTo(expected.mappingType)
          assertThat(whenCreated).isEqualTo(expected.whenCreated)
        }
      }

      @Test
      fun `will delete any alias or identifier mappings persisted before the replace`() = runTest {
        // Check that the existing alias and identifier mappings are present before the replace
        suspend fun assertAliasesForCprId() = assertThat(offenderAliasMappingRepository.findOneByCprId(existingAliasMapping.cprId))
        suspend fun assertIdentifiersForCprId() = assertThat(offenderIdentifierMappingRepository.findOneByCprId(existingIdentifierMapping.cprId))
        assertAliasesForCprId().isNotNull()
        assertIdentifiersForCprId().isNotNull()
        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk

        // Check that the existing alias and identifier mappings have been deleted after the replace
        assertAliasesForCprId().isNull()
        assertIdentifiersForCprId().isNull()
      }

      @Test
      fun `will remove the alias and identifier mappings when passed empty lists`() = runTest {
        // Check that the existing alias and identifier mappings are present before the replace
        suspend fun assertIdentifiersForPrisonNumber() = assertThat(offenderIdentifierMappingRepository.findAll().toList().filter { it.nomisPrisonNumber == nomisPrisonNumber })
        suspend fun assertAliasesForPrisonNumber() = assertThat(offenderAliasMappingRepository.findAll().toList().filter { it.nomisPrisonNumber == nomisPrisonNumber })
        assertIdentifiersForPrisonNumber().isNotEmpty()
        assertAliasesForPrisonNumber().isNotEmpty()

        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsDto(
                label = "2021-01-01T01:00",
                mappingType = CorePersonMappingType.NOMIS_CREATED,
                whenCreated = LocalDateTime.of(2021, 1, 1, 1, 0),
                personMapping = CorePersonMappingIdDto(
                  cprId = "9a1313b1-5c03-4225-b6fd-dbc5ce992760",
                  nomisPrisonNumber = nomisPrisonNumber,
                ),
                aliases = emptyList(),
                identifiers = emptyList(),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        // Check that the existing alias and identifier mappings have been deleted after the replace
        assertIdentifiersForPrisonNumber().isEmpty()
        assertAliasesForPrisonNumber().isEmpty()
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
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
          .uri("/mapping/core-person/person/nomis-prison-number/{nomisPrisonNumber}", nomisPrisonNumber)
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
          .uri("/mapping/core-person/person/cpr-id/{cprId}", cprCoreId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisPrisonNumber = "A1234BA",
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      personAddressMapping = corePersonAddressMappingRepository.save(
        CorePersonAddressMapping(
          nomisPrisonNumber = "A1234BA",
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
          .uri("/mapping/core-person/address/cpr-address-id/{cprAddressId}", cprCorePersonAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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

  @Nested
  @DisplayName("DELETE /mapping/core-person")
  inner class DeleteAllMappings {
    @BeforeEach
    fun setUp() {
      val mappings = CorePersonMappingsDto(
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
        ),
      )
      webTestClient.post()
        .uri("/mapping/core-person/migrate")
        .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(mappings))
        .exchange()
        .expectStatus().isCreated
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/core-person")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/core-person")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/core-person")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns 204 when all mappings are deleted`() = runTest {
        // TODO add other child mappings when implemented
        assertThat(offenderIdentifierMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(offenderAliasMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(corePersonMappingRepository.findAll().count()).isEqualTo(1)

        webTestClient.delete()
          .uri("/mapping/core-person")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        // TODO add other child mappings when implemented
        assertThat(offenderAliasMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(offenderIdentifierMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(corePersonMappingRepository.findAll().count()).isEqualTo(0)
      }
    }
  }
}
