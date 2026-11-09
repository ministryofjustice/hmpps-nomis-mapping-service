package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact

import kotlinx.coroutines.flow.count
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
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingIdDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMigrationAddressContact
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMigrationAddressContactMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier.OffenderAliasMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier.OffenderAliasMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier.OffenderIdentifierMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier.OffenderIdentifierMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class CorePersonMigrationAddressContactResourceIntTest(
  @Autowired private val offenderAliasMappingRepository: OffenderAliasMappingRepository,
  @Autowired private val offenderIdentifierMappingRepository: OffenderIdentifierMappingRepository,
  @Autowired private val corePersonAddressMappingRepository: CorePersonAddressMappingRepository,
  @Autowired private val corePersonAddressUsageMappingRepository: CorePersonAddressUsageMappingRepository,
  @Autowired private val corePersonContactMappingRepository: CorePersonContactMappingRepository,
  @Autowired private val corePersonMigrationAddressContactMappingRepository: CorePersonMigrationAddressContactMappingRepository,
) : IntegrationTestBase() {

  @AfterEach
  fun tearDown() = runTest {
    offenderAliasMappingRepository.deleteAll()
    offenderIdentifierMappingRepository.deleteAll()
    corePersonAddressMappingRepository.deleteAll()
    corePersonAddressUsageMappingRepository.deleteAll()
    corePersonContactMappingRepository.deleteAll()
    corePersonMigrationAddressContactMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST mapping/core-person")
  inner class CreateMappings {

    @Nested
    inner class Security {
      val mappings = CorePersonAddressContactMappingsDto(
        personMapping = CorePersonMappingIdDto(
          cprId = UUID.randomUUID().toString(),
          nomisPrisonNumber = "A1234BC",
        ),
        label = null,
        mappingType = CorePersonMappingType.CPR_CREATED,
        whenCreated = LocalDateTime.now(),
        addresses = emptyList(),
        contacts = emptyList(),
        addressUsages = emptyList(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact-migration")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact-migration")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact-migration")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingCorePersonMapping: CorePersonMigrationAddressContact

      val mappings = CorePersonAddressContactMappingsDto(
        personMapping = CorePersonMappingIdDto(
          cprId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
          nomisPrisonNumber = "A1234BC",
        ),
        label = null,
        mappingType = CorePersonMappingType.MIGRATED,
        whenCreated = LocalDateTime.now(),
        addresses = emptyList(),
        contacts = emptyList(),
        addressUsages = emptyList(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingCorePersonMapping = corePersonMigrationAddressContactMappingRepository.save(
          CorePersonMigrationAddressContact(
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
          .uri("/mapping/core-person/address-contact-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/core-person/address-contact-migration")
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
      val mappings = CorePersonAddressContactMappingsDto(
        personMapping = CorePersonMappingIdDto(
          cprId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
          nomisPrisonNumber = "A1234BC",
        ),
        label = null,
        mappingType = CorePersonMappingType.CPR_CREATED,
        whenCreated = LocalDateTime.now(),
        addresses = emptyList(),
        contacts = emptyList(),
        addressUsages = emptyList(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the core person mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings.copy(label = "2023-01-01T12:45:12")))
          .exchange()
          .expectStatus().isCreated

        val corePersonMapping =
          corePersonMigrationAddressContactMappingRepository.findOneByNomisPrisonNumber(mappings.personMapping.nomisPrisonNumber)!!

        assertThat(corePersonMapping.cprId).isEqualTo(mappings.personMapping.cprId)
        assertThat(corePersonMapping.nomisPrisonNumber).isEqualTo(mappings.personMapping.nomisPrisonNumber)
        assertThat(corePersonMapping.label).isEqualTo("2023-01-01T12:45:12")
        assertThat(corePersonMapping.mappingType).isEqualTo(mappings.mappingType)
        assertThat(corePersonMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }

      @Test
      fun `will persist the core person addresses mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                addresses = listOf(
                  CorePersonAddressMappingDto(
                    cprId = "0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6",
                    nomisId = 10000L,
                    nomisPrisonNumber = "A1234BC",
                    label = "2023-01-01T12:45:12",
                    mappingType = CorePersonMappingType.MIGRATED,
                    whenCreated = LocalDateTime.now(),
                  ),
                  CorePersonAddressMappingDto(
                    cprId = "37611e56-3b4e-4cfa-994d-6c089794fd1b",
                    nomisId = 10001L,
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

        with(corePersonAddressMappingRepository.findOneByCprId("0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6")!!) {
          assertThat(label).isEqualTo("2023-01-01T12:45:12")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.MIGRATED)
          assertThat(nomisPrisonNumber).isEqualTo("A1234BC")
          assertThat(whenCreated).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.SECONDS),
          )
          assertThat(nomisId).isEqualTo(10000L)
        }

        with(corePersonAddressMappingRepository.findOneByCprId("37611e56-3b4e-4cfa-994d-6c089794fd1b")!!) {
          assertThat(label).isEqualTo("2024-01-01T12:45:12")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.MIGRATED)
          assertThat(nomisPrisonNumber).isEqualTo("A1234BC")
          assertThat(whenCreated).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.SECONDS),
          )
          assertThat(nomisId).isEqualTo(10001L)
        }
      }

      @Test
      fun `will persist the core person address usage mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                addressUsages = listOf(
                  CorePersonAddressUsageMappingDto(
                    cprId = "8dc8b0c7-2fd8-487e-897b-f1ce83e27c65",
                    nomisId = 10000L,
                    addressUsageCode = "CARE",
                    nomisPrisonNumber = "A1234BC",
                    label = "2024-02-01T12:45:12",
                    mappingType = CorePersonMappingType.MIGRATED,
                    whenCreated = LocalDateTime.now(),
                  ),
                  CorePersonAddressUsageMappingDto(
                    cprId = "b0e578e3-5075-4404-8f17-4d2f71b43619",
                    nomisId = 10000L,
                    addressUsageCode = "RELEASE",
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

        with(corePersonAddressUsageMappingRepository.findOneByCprId("8dc8b0c7-2fd8-487e-897b-f1ce83e27c65")!!) {
          assertThat(label).isEqualTo("2024-02-01T12:45:12")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.MIGRATED)
          assertThat(nomisPrisonNumber).isEqualTo("A1234BC")
          assertThat(whenCreated).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.SECONDS),
          )
          assertThat(addressUsageCode).isEqualTo("CARE")
          assertThat(nomisId).isEqualTo(10000L)
        }
        with(corePersonAddressUsageMappingRepository.findOneByCprId("b0e578e3-5075-4404-8f17-4d2f71b43619")!!) {
          assertThat(label).isEqualTo("2024-03-01T12:45:12")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.MIGRATED)
          assertThat(nomisPrisonNumber).isEqualTo("A1234BC")
          assertThat(whenCreated).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.SECONDS),
          )
          assertThat(addressUsageCode).isEqualTo("RELEASE")
          assertThat(nomisId).isEqualTo(10000L)
        }
      }

      @Test
      fun `will persist the core person email addresses mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                contacts = listOf(
                  CorePersonContactMappingDto(
                    cprId = "0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6",
                    nomisId = 10000L,
                    nomisContactType = NomisContactType.EMAIL,
                    nomisPrisonNumber = "A1234BC",
                    label = "2023-01-01T12:45:12",
                    mappingType = CorePersonMappingType.MIGRATED,
                    whenCreated = LocalDateTime.now(),
                  ),
                  CorePersonContactMappingDto(
                    cprId = "37611e56-3b4e-4cfa-994d-6c089794fd1b",
                    nomisId = 10001L,
                    nomisContactType = NomisContactType.EMAIL,
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

        with(corePersonContactMappingRepository.findOneByCprId("0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6")!!) {
          assertThat(label).isEqualTo("2023-01-01T12:45:12")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.MIGRATED)
          assertThat(nomisPrisonNumber).isEqualTo("A1234BC")
          assertThat(whenCreated).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.SECONDS),
          )
          assertThat(nomisId).isEqualTo(10000L)
        }

        with(corePersonContactMappingRepository.findOneByCprId("37611e56-3b4e-4cfa-994d-6c089794fd1b")!!) {
          assertThat(label).isEqualTo("2024-01-01T12:45:12")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.MIGRATED)
          assertThat(nomisPrisonNumber).isEqualTo("A1234BC")
          assertThat(whenCreated).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.SECONDS),
          )
          assertThat(nomisId).isEqualTo(10001L)
        }
      }

      @Test
      fun `will persist the core person phone number mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                contacts = listOf(
                  CorePersonContactMappingDto(
                    cprId = "0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6",
                    nomisId = 10000L,
                    nomisContactType = NomisContactType.PHONE,
                    nomisPrisonNumber = "A1234BC",
                    label = "2023-01-01T12:45:12",
                    mappingType = CorePersonMappingType.MIGRATED,
                    whenCreated = LocalDateTime.now(),
                  ),
                  CorePersonContactMappingDto(
                    cprId = "37611e56-3b4e-4cfa-994d-6c089794fd1b",
                    nomisId = 10001L,
                    nomisContactType = NomisContactType.PHONE,
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

        with(corePersonContactMappingRepository.findOneByCprId("0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6")!!) {
          assertThat(label).isEqualTo("2023-01-01T12:45:12")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.MIGRATED)
          assertThat(nomisPrisonNumber).isEqualTo("A1234BC")
          assertThat(whenCreated).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.SECONDS),
          )
          assertThat(nomisId).isEqualTo(10000L)
          assertThat(nomisContactType).isEqualTo(NomisContactType.PHONE)
        }

        with(corePersonContactMappingRepository.findOneByCprId("37611e56-3b4e-4cfa-994d-6c089794fd1b")!!) {
          assertThat(label).isEqualTo("2024-01-01T12:45:12")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.MIGRATED)
          assertThat(nomisPrisonNumber).isEqualTo("A1234BC")
          assertThat(whenCreated).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.SECONDS),
          )
          assertThat(nomisId).isEqualTo(10001L)
          assertThat(nomisContactType).isEqualTo(NomisContactType.PHONE)
        }
      }
    }
    // TODO add other child mapping tests when implemented
  }

  @DisplayName("GET /mapping/core-person/address-contact-migration/migration-id/{migrationId}")
  @Nested
  inner class GetCorePersonMappingsByMigrationId {

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/core-person/address-contact-migration/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/core-person/address-contact-migration/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/core-person/address-contact-migration/migration-id/2022-01-01T00:00:00")
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
          corePersonMigrationAddressContactMappingRepository.save(
            CorePersonMigrationAddressContact(
              cprId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
              nomisPrisonNumber = "A123${it}BC",
              label = "2023-01-01T12:45:12",
              mappingType = CorePersonMappingType.MIGRATED,
            ),
          )
        }

        corePersonMigrationAddressContactMappingRepository.save(
          CorePersonMigrationAddressContact(
            cprId = "edcd118c-91ba-42ea-b5c4-404b453ad58b",
            nomisPrisonNumber = "A4321BC",
            label = "2022-01-01T12:43:12",
            mappingType = CorePersonMappingType.MIGRATED,
          ),
        )

        webTestClient.get().uri("/mapping/core-person/address-contact-migration/migration-id/2023-01-01T12:45:12")
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
        webTestClient.get().uri("/mapping/core-person/address-contact-migration/migration-id/2044-01-01")
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
          corePersonMigrationAddressContactMappingRepository.save(
            CorePersonMigrationAddressContact(
              cprId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
              nomisPrisonNumber = "A${it}123BC",
              label = "2023-01-01T12:45:12",
              mappingType = CorePersonMappingType.MIGRATED,
            ),
          )
        }
        webTestClient.get().uri {
          it.path("/mapping/core-person/address-contact-migration/migration-id/2023-01-01T12:45:12")
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
  @DisplayName("GET /mapping/core-person/address-contact-migration/person/nomis-prison-number/{nomisPrisonNumber}")
  inner class GetCorePersonByNomisPrisonNumber {
    private val nomisPrisonNumber = "A1234BC"
    private lateinit var personMapping: CorePersonMigrationAddressContact

    @BeforeEach
    fun setUp() = runTest {
      personMapping = corePersonMigrationAddressContactMappingRepository.save(
        CorePersonMigrationAddressContact(
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
          .uri("/mapping/core-person/address-contact-migration/person/nomis-prison-number/{nomisPrisonNumber}", nomisPrisonNumber)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/address-contact-migration/person/nomis-prison-number/{nomisPrisonNumber}", nomisPrisonNumber)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/address-contact-migration/person/nomis-prison-number/{nomisPrisonNumber}", nomisPrisonNumber)
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
          .uri("/mapping/core-person/address-contact-migration/person/nomis-prison-number/{nomisPrisonNumber}", 99999)
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
          .uri("/mapping/core-person/address-contact-migration/person/nomis-prison-number/{nomisPrisonNumber}", nomisPrisonNumber)
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
  @DisplayName("DELETE /mapping/core-person/address-contact-migration")
  inner class DeleteAllMappings {
    @BeforeEach
    fun setUp() {
      val mappings = CorePersonAddressContactMappingsDto(
        personMapping = CorePersonMappingIdDto(
          cprId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
          nomisPrisonNumber = "A1234BC",
        ),
        label = null,
        mappingType = CorePersonMappingType.CPR_CREATED,
        whenCreated = LocalDateTime.now(),
        addresses = listOf(
          CorePersonAddressMappingDto(
            cprId = "96f9ea13-9c2a-4e05-8128-f32778edd9e9",
            nomisId = 10000L,
            nomisPrisonNumber = "A1234BC",
            label = "2025-03-01T12:45:12",
            mappingType = CorePersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.now(),
          ),
        ),
        addressUsages = listOf(
          CorePersonAddressUsageMappingDto(
            cprId = "7d2d6155-1a2d-404c-83d0-838dadd64f85",
            nomisId = 10000L,
            addressUsageCode = "RELEASE",
            nomisPrisonNumber = "A1234BC",
            label = "2024-03-01T12:45:12",
            mappingType = CorePersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.now(),
          ),
        ),
        contacts = listOf(
          CorePersonContactMappingDto(
            cprId = "96f9ea13-9c2a-4e05-8128-f32778edd9e8",
            nomisId = 10000L,
            nomisContactType = NomisContactType.EMAIL,
            nomisPrisonNumber = "A1234BC",
            label = "2025-03-01T12:45:12",
            mappingType = CorePersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.now(),
          ),
          CorePersonContactMappingDto(
            cprId = "96f9ea13-9c2a-4e05-8128-f32778edd9e9",
            nomisId = 10000L,
            nomisContactType = NomisContactType.PHONE,
            nomisPrisonNumber = "A1234BC",
            label = "2025-03-01T12:45:12",
            mappingType = CorePersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.now(),
          ),
        ),
      )
      webTestClient.post()
        .uri("/mapping/core-person/address-contact-migration")
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
          .uri("/mapping/core-person/address-contact-migration")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/address-contact-migration")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/address-contact-migration")
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
        assertThat(corePersonAddressMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(corePersonAddressUsageMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(corePersonContactMappingRepository.findAll().count()).isEqualTo(2)
        assertThat(corePersonMigrationAddressContactMappingRepository.findAll().count()).isEqualTo(1)

        webTestClient.delete()
          .uri("/mapping/core-person/address-contact-migration")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        // TODO add other child mappings when implemented
        assertThat(corePersonAddressMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(corePersonAddressUsageMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(corePersonContactMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(corePersonMigrationAddressContactMappingRepository.findAll().count()).isEqualTo(0)
      }
    }
  }
}
