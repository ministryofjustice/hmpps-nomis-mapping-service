package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
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
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class CorePersonMappingResourceIntTest(
  @Autowired private val corePersonMappingRepository: CorePersonMappingRepository,
  @Autowired private val offenderAliasMappingRepository: OffenderAliasMappingRepository,
  @Autowired private val corePersonAddressMappingRepository: CorePersonAddressMappingRepository,
  @Autowired private val corePersonAddressUsageMappingRepository: CorePersonAddressUsageMappingRepository,
  @Autowired private val corePersonContactMappingRepository: CorePersonContactMappingRepository,
  @Autowired private val offenderIdentifierMappingRepository: OffenderIdentifierMappingRepository,
) : IntegrationTestBase() {

  @AfterEach
  fun tearDown() = runTest {
    offenderAliasMappingRepository.deleteAll()
    offenderIdentifierMappingRepository.deleteAll()
    corePersonAddressMappingRepository.deleteAll()
    corePersonAddressUsageMappingRepository.deleteAll()
    corePersonContactMappingRepository.deleteAll()
    corePersonMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST mapping/core-person")
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
        addresses = emptyList(),
        contacts = emptyList(),
        addressUsages = emptyList(),
      )

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
      private lateinit var existingCorePersonMapping: CorePersonMapping

      val mappings = CorePersonMappingsDto(
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
          .uri("/mapping/core-person")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/core-person")
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
        addresses = emptyList(),
        contacts = emptyList(),
        addressUsages = emptyList(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the core person mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person")
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
      fun `will persist the core person addresses mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person")
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
          .uri("/mapping/core-person")
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
          .uri("/mapping/core-person")
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
          .uri("/mapping/core-person")
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
        addresses = emptyList(),
        contacts = emptyList(),
        addressUsages = emptyList(),
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
        addresses = listOf(
          CorePersonAddressMappingDto(
            cprId = "fba8a521-fc9f-4801-bb02-3f3326c760ef",
            nomisId = 10000L,
            nomisPrisonNumber = nomisPrisonNumber,
            label = "2020-01-01T10:00",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
            whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
          ),
        ),
        addressUsages = listOf(
          CorePersonAddressUsageMappingDto(
            cprId = "27f51212-4d32-4b8b-86c2-a2e2807e3e4e",
            nomisId = 10000L,
            addressUsageCode = "RELEASE",
            nomisPrisonNumber = nomisPrisonNumber,
            label = "2020-01-01T10:00",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
            whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
          ),
        ),
        contacts = listOf(
          CorePersonContactMappingDto(
            cprId = "27f51212-4d32-4b8b-86c2-a2e2807e3e4e",
            nomisId = 10000L,
            nomisContactType = NomisContactType.EMAIL,
            nomisPrisonNumber = nomisPrisonNumber,
            label = "2020-01-01T10:00",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
            whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
          ),
          CorePersonContactMappingDto(
            cprId = "27f51212-4d32-4b8b-86c2-a2e2807e3e4f",
            nomisId = 10000L,
            nomisContactType = NomisContactType.PHONE,
            nomisPrisonNumber = nomisPrisonNumber,
            label = "2020-01-01T10:00",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
            whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
          ),
        ),
        mappingType = CorePersonMappingType.NOMIS_CREATED,
      )

      val existingAddressMapping = CorePersonAddressMapping(
        cprId = "034c070d-aff5-4464-9d1e-24c20bc6f8e1",
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
        nomisId = 10000L,
      )

      val existingAddressUsageMapping = CorePersonAddressUsageMapping(
        cprId = "8d2c37b7-3f07-4954-b9bb-5ba61357be3f",
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
        nomisId = 10000L,
        addressUsageCode = "RELEASE",
      )

      val existingEmailAddressMapping = CorePersonContactMapping(
        cprId = "8d2c37b7-3f07-4954-b9bb-5ba61357be3e",
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
        nomisId = 10000L,
        nomisContactType = NomisContactType.EMAIL,
      )

      val existingPhoneMapping = CorePersonContactMapping(
        cprId = "8d2c37b7-3f07-4954-b9bb-5ba61357be3f",
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
        nomisId = 10000L,
        nomisContactType = NomisContactType.PHONE,
      )

      @BeforeEach
      fun setUp() = runTest {
        corePersonAddressMappingRepository.save(existingAddressMapping)
        corePersonAddressUsageMappingRepository.save(existingAddressUsageMapping)
        corePersonContactMappingRepository.save(existingEmailAddressMapping)
        corePersonContactMappingRepository.save(existingPhoneMapping)
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
      fun `will persist the address, address usage, email and phone mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk

        // New address mapping
        with(corePersonAddressMappingRepository.findOneByCprId(mapping.addresses[0].cprId)!!) {
          val expected = mapping.addresses[0] // the replacement mapping
          assertThat(nomisId).isEqualTo(expected.nomisId)
          assertThat(label).isEqualTo(expected.label)
          assertThat(mappingType).isEqualTo(expected.mappingType)
          assertThat(whenCreated).isEqualTo(expected.whenCreated)
        }

        // New address usage mapping
        with(corePersonAddressUsageMappingRepository.findOneByCprId(mapping.addressUsages[0].cprId)!!) {
          val expected = mapping.addressUsages[0] // the replacement mapping
          assertThat(cprId).isEqualTo(expected.cprId)
          assertThat(nomisId).isEqualTo(expected.nomisId)
          assertThat(addressUsageCode).isEqualTo(expected.addressUsageCode)
          assertThat(label).isEqualTo(expected.label)
          assertThat(mappingType).isEqualTo(expected.mappingType)
          assertThat(whenCreated).isEqualTo(expected.whenCreated)
        }

        // New email mapping
        with(corePersonContactMappingRepository.findOneByCprId(mapping.contacts[0].cprId)!!) {
          val expected = mapping.contacts[0] // the replacement mapping
          assertThat(nomisId).isEqualTo(expected.nomisId)
          assertThat(nomisContactType).isEqualTo(expected.nomisContactType)
          assertThat(label).isEqualTo(expected.label)
          assertThat(mappingType).isEqualTo(expected.mappingType)
          assertThat(whenCreated).isEqualTo(expected.whenCreated)
        }

        // New phone mapping
        with(corePersonContactMappingRepository.findOneByCprId(mapping.contacts[1].cprId)!!) {
          val expected = mapping.contacts[1] // the replacement mapping
          assertThat(nomisId).isEqualTo(expected.nomisId)
          assertThat(nomisContactType).isEqualTo(expected.nomisContactType)
          assertThat(label).isEqualTo(expected.label)
          assertThat(mappingType).isEqualTo(expected.mappingType)
          assertThat(whenCreated).isEqualTo(expected.whenCreated)
        }
      }

      @Test
      fun `will delete any address, address usage, email or phone mappings persisted before the replace`() = runTest {
        // Check that the existing address, address usage, email or phone mappings are present before the replace
        assertAddressesForCprId(existingAddressMapping.cprId).isNotNull()
        assertAddressUsagesForCprId(existingAddressUsageMapping.cprId).isNotNull()
        assertEmailsForCprId(existingEmailAddressMapping.cprId).isNotNull()
        assertPhonesForCprId(existingPhoneMapping.cprId).isNotNull()
        webTestClient.post()
          .uri("/mapping/core-person/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk

        // Check that the address, address usage, email or phone mappings have been deleted after the replace
        assertAddressesForCprId(existingAddressMapping.cprId).isNull()
        assertAddressUsagesForCprId(existingAddressUsageMapping.cprId).isNull()
        assertEmailsForCprId(existingEmailAddressMapping.cprId).isNull()
        assertPhonesForCprId(existingPhoneMapping.cprId).isNull()
      }

      @Test
      fun `will remove the existing address, address usage, email and phone mappings when passed empty lists`() = runTest {
        // Check that the existing address, address usage, email and phone mappings are present before the replace
        assertAddressesForPrisonNumber(nomisPrisonNumber).isNotEmpty()
        assertAddressUsagesForPrisonNumber(nomisPrisonNumber).isNotEmpty()
        assertContactsForPrisonNumber(nomisPrisonNumber).isNotEmpty()

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
                addresses = emptyList(),
                contacts = emptyList(),
                addressUsages = emptyList(),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        // Check that the existing address, address usage, contact mappings have been deleted after the replace
        assertAddressesForPrisonNumber(nomisPrisonNumber).isEmpty()
        assertAddressUsagesForPrisonNumber(nomisPrisonNumber).isEmpty()
        assertContactsForPrisonNumber(nomisPrisonNumber).isEmpty()
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/core-person/merge")
  inner class MergeCorePersonMappings {
    val toNomisPrisonNumber = "A1234BC"
    val fromNomisPrisonNumber = "D1234EF"

    @Nested
    inner class Security {
      val mapping = CorePersonMergeMappingsDto(
        toNomisPrisonNumber = toNomisPrisonNumber,
        fromNomisPrisonNumber = fromNomisPrisonNumber,
        addresses = emptyList(),
        contacts = emptyList(),
        addressUsages = emptyList(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/merge")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/merge")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/merge")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when from and to prison numbers are the same`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/merge")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMergeMappingsDto(
                toNomisPrisonNumber = toNomisPrisonNumber,
                fromNomisPrisonNumber = toNomisPrisonNumber,
                addresses = emptyList(),
                contacts = emptyList(),
                addressUsages = emptyList(),
              ),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when a child mapping prison number does not match the target prisoner`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/merge")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMergeMappingsDto(
                toNomisPrisonNumber = toNomisPrisonNumber,
                fromNomisPrisonNumber = fromNomisPrisonNumber,
                addresses = listOf(
                  CorePersonAddressMappingDto(
                    cprId = "3d4cf77c-63d6-4c2e-9e7c-7ed0a8e56d72",
                    nomisId = 10000L,
                    nomisPrisonNumber = "Z9999ZZ",
                    label = "2020-01-01T10:00",
                    mappingType = CorePersonMappingType.NOMIS_CREATED,
                    whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
                  ),
                ),
                contacts = emptyList(),
                addressUsages = emptyList(),
              ),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = CorePersonMergeMappingsDto(
        toNomisPrisonNumber = toNomisPrisonNumber,
        fromNomisPrisonNumber = fromNomisPrisonNumber,
        addresses = listOf(
          CorePersonAddressMappingDto(
            cprId = "fba8a521-fc9f-4801-bb02-3f3326c760ef",
            nomisId = 10000L,
            nomisPrisonNumber = toNomisPrisonNumber,
            label = "2020-01-01T10:00",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
            whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
          ),
        ),
        addressUsages = listOf(
          CorePersonAddressUsageMappingDto(
            cprId = "27f51212-4d32-4b8b-86c2-a2e2807e3e4e",
            nomisId = 10000L,
            addressUsageCode = "RELEASE",
            nomisPrisonNumber = toNomisPrisonNumber,
            label = "2020-01-01T10:00",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
            whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
          ),
        ),
        contacts = listOf(
          CorePersonContactMappingDto(
            cprId = "27f51212-4d32-4b8b-86c2-a2e2807e3e4e",
            nomisId = 10000L,
            nomisContactType = NomisContactType.EMAIL,
            nomisPrisonNumber = toNomisPrisonNumber,
            label = "2020-01-01T10:00",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
            whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
          ),
          CorePersonContactMappingDto(
            cprId = "27f51212-4d32-4b8b-86c2-a2e2807e3e4f",
            nomisId = 10000L,
            nomisContactType = NomisContactType.PHONE,
            nomisPrisonNumber = toNomisPrisonNumber,
            label = "2020-01-01T10:00",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
            whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
          ),
        ),
      )

      val existingToPersonAddressMapping = CorePersonAddressMapping(
        cprId = "034c070d-aff5-4464-9d1e-24c20bc6f8e1",
        nomisPrisonNumber = toNomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
        nomisId = 10000L,
      )

      val existingToPersonAddressUsageMapping = CorePersonAddressUsageMapping(
        cprId = "8d2c37b7-3f07-4954-b9bb-5ba61357be3f",
        nomisPrisonNumber = toNomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
        nomisId = 10000L,
        addressUsageCode = "RELEASE",
      )

      val existingToPersonEmailAddressMapping = CorePersonContactMapping(
        cprId = "8d2c37b7-3f07-4954-b9bb-5ba61357be3e",
        nomisPrisonNumber = toNomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
        nomisId = 10000L,
        nomisContactType = NomisContactType.EMAIL,
      )

      val existingToPersonPhoneMapping = CorePersonContactMapping(
        cprId = "8d2c37b7-3f07-4954-b9bb-5ba61357be3f",
        nomisPrisonNumber = toNomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
        nomisId = 10000L,
        nomisContactType = NomisContactType.PHONE,
      )

      val existingFromPersonAddressMapping = CorePersonAddressMapping(
        cprId = "e2cc8f73-8ed7-4d33-9a87-b40a52a7c7d2",
        nomisPrisonNumber = fromNomisPrisonNumber,
        label = "2018-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2018-01-01T10:14"),
        nomisId = 20000L,
      )

      val existingFromPersonAddressUsageMapping = CorePersonAddressUsageMapping(
        cprId = "4f488182-e385-43ce-9c8c-996c8ad9f1d8",
        nomisPrisonNumber = fromNomisPrisonNumber,
        label = "2018-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2018-01-01T10:14"),
        nomisId = 20000L,
        addressUsageCode = "RELEASE",
      )

      val existingFromPersonEmailAddressMapping = CorePersonContactMapping(
        cprId = "51eb5fda-4e24-442b-a600-0e78546a2ad7",
        nomisPrisonNumber = fromNomisPrisonNumber,
        label = "2018-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2018-01-01T10:14"),
        nomisId = 20000L,
        nomisContactType = NomisContactType.EMAIL,
      )

      val existingFromPersonPhoneMapping = CorePersonContactMapping(
        cprId = "f61fca8a-f86b-4a90-9b8a-4d09ce5d7cb7",
        nomisPrisonNumber = fromNomisPrisonNumber,
        label = "2018-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2018-01-01T10:14"),
        nomisId = 20000L,
        nomisContactType = NomisContactType.PHONE,
      )

      @BeforeEach
      fun setUp() = runTest {
        corePersonAddressMappingRepository.save(existingToPersonAddressMapping)
        corePersonAddressUsageMappingRepository.save(existingToPersonAddressUsageMapping)
        corePersonContactMappingRepository.save(existingToPersonEmailAddressMapping)
        corePersonContactMappingRepository.save(existingToPersonPhoneMapping)

        corePersonAddressMappingRepository.save(existingFromPersonAddressMapping)
        corePersonAddressUsageMappingRepository.save(existingFromPersonAddressUsageMapping)
        corePersonContactMappingRepository.save(existingFromPersonEmailAddressMapping)
        corePersonContactMappingRepository.save(existingFromPersonPhoneMapping)
      }

      @Test
      fun `returns 200 when mappings replaced`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/merge")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk
      }

      @Test
      fun `will persist the address, address usage, email and phone mappings for the merged to person`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/merge")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk

        with(corePersonAddressMappingRepository.findOneByCprId(mapping.addresses[0].cprId)!!) {
          val expected = mapping.addresses[0]
          assertThat(nomisId).isEqualTo(expected.nomisId)
          assertThat(label).isEqualTo(expected.label)
          assertThat(mappingType).isEqualTo(expected.mappingType)
          assertThat(whenCreated).isEqualTo(expected.whenCreated)
        }

        with(corePersonAddressUsageMappingRepository.findOneByCprId(mapping.addressUsages[0].cprId)!!) {
          val expected = mapping.addressUsages[0]
          assertThat(cprId).isEqualTo(expected.cprId)
          assertThat(nomisId).isEqualTo(expected.nomisId)
          assertThat(addressUsageCode).isEqualTo(expected.addressUsageCode)
          assertThat(label).isEqualTo(expected.label)
          assertThat(mappingType).isEqualTo(expected.mappingType)
          assertThat(whenCreated).isEqualTo(expected.whenCreated)
        }

        with(corePersonContactMappingRepository.findOneByCprId(mapping.contacts[0].cprId)!!) {
          val expected = mapping.contacts[0]
          assertThat(nomisId).isEqualTo(expected.nomisId)
          assertThat(nomisContactType).isEqualTo(expected.nomisContactType)
          assertThat(label).isEqualTo(expected.label)
          assertThat(mappingType).isEqualTo(expected.mappingType)
          assertThat(whenCreated).isEqualTo(expected.whenCreated)
        }

        with(corePersonContactMappingRepository.findOneByCprId(mapping.contacts[1].cprId)!!) {
          val expected = mapping.contacts[1]
          assertThat(nomisId).isEqualTo(expected.nomisId)
          assertThat(nomisContactType).isEqualTo(expected.nomisContactType)
          assertThat(label).isEqualTo(expected.label)
          assertThat(mappingType).isEqualTo(expected.mappingType)
          assertThat(whenCreated).isEqualTo(expected.whenCreated)
        }
      }

      @Test
      fun `will delete all address, address usage, email and phone mappings persisted before the replace for both people`() = runTest {
        // Check that the existing address, address usage, email or phone mappings are present before the replace
        assertAddressesForCprId(existingToPersonAddressMapping.cprId).isNotNull()
        assertAddressUsagesForCprId(existingToPersonAddressUsageMapping.cprId).isNotNull()
        assertEmailsForCprId(existingToPersonEmailAddressMapping.cprId).isNotNull()
        assertPhonesForCprId(existingToPersonPhoneMapping.cprId).isNotNull()
        assertAddressesForCprId(existingFromPersonAddressMapping.cprId).isNotNull()
        assertAddressUsagesForCprId(existingFromPersonAddressUsageMapping.cprId).isNotNull()
        assertEmailsForCprId(existingFromPersonEmailAddressMapping.cprId).isNotNull()
        assertPhonesForCprId(existingFromPersonPhoneMapping.cprId).isNotNull()

        webTestClient.post()
          .uri("/mapping/core-person/merge")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk

        assertAddressesForCprId(existingToPersonAddressMapping.cprId).isNull()
        assertAddressUsagesForCprId(existingToPersonAddressUsageMapping.cprId).isNull()
        assertEmailsForCprId(existingToPersonEmailAddressMapping.cprId).isNull()
        assertPhonesForCprId(existingToPersonPhoneMapping.cprId).isNull()
        assertAddressesForCprId(existingFromPersonAddressMapping.cprId).isNull()
        assertAddressUsagesForCprId(existingFromPersonAddressUsageMapping.cprId).isNull()
        assertEmailsForCprId(existingFromPersonEmailAddressMapping.cprId).isNull()
        assertPhonesForCprId(existingFromPersonPhoneMapping.cprId).isNull()
      }

      @Test
      fun `will remove the existing address, address usage, email and phone mappings for both persons when passed empty lists`() = runTest {
        // Check that the existing address, address usage, email and phone mappings are present before the replace
        assertAddressesForPrisonNumber(toNomisPrisonNumber).isNotEmpty()
        assertAddressUsagesForPrisonNumber(toNomisPrisonNumber).isNotEmpty()
        assertContactsForPrisonNumber(toNomisPrisonNumber).isNotEmpty()
        assertAddressesForPrisonNumber(fromNomisPrisonNumber).isNotEmpty()
        assertAddressUsagesForPrisonNumber(fromNomisPrisonNumber).isNotEmpty()
        assertContactsForPrisonNumber(fromNomisPrisonNumber).isNotEmpty()

        webTestClient.post()
          .uri("/mapping/core-person/merge")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMergeMappingsDto(
                toNomisPrisonNumber = toNomisPrisonNumber,
                fromNomisPrisonNumber = fromNomisPrisonNumber,
                addresses = emptyList(),
                contacts = emptyList(),
                addressUsages = emptyList(),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        assertAddressesForPrisonNumber(toNomisPrisonNumber).isEmpty()
        assertAddressUsagesForPrisonNumber(toNomisPrisonNumber).isEmpty()
        assertContactsForPrisonNumber(toNomisPrisonNumber).isEmpty()
        assertAddressesForPrisonNumber(fromNomisPrisonNumber).isEmpty()
        assertAddressUsagesForPrisonNumber(fromNomisPrisonNumber).isEmpty()
        assertContactsForPrisonNumber(fromNomisPrisonNumber).isEmpty()
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/core-person/mappings/by-nomis-ids")
  inner class GetMappingsFromNomisIds {
    private val nomisPrisonNumber = "A1234BC"

    @BeforeEach
    fun setUp() = runTest {
      corePersonAddressMappingRepository.save(
        CorePersonAddressMapping(
          cprId = "a1c4d6e8-1111-4b2a-9c3d-000000000101",
          nomisId = 101L,
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      corePersonAddressMappingRepository.save(
        CorePersonAddressMapping(
          cprId = "a1c4d6e8-1111-4b2a-9c3d-000000000102",
          nomisId = 102L,
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      corePersonContactMappingRepository.save(
        CorePersonContactMapping(
          cprId = "b2d5e7f9-2222-4c3b-8d4e-000000000201",
          nomisId = 201L,
          nomisContactType = NomisContactType.EMAIL,
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      corePersonContactMappingRepository.save(
        CorePersonContactMapping(
          cprId = "b2d5e7f9-2222-4c3b-8d4e-000000000202",
          nomisId = 202L,
          nomisContactType = NomisContactType.PHONE,
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      corePersonAddressUsageMappingRepository.save(
        CorePersonAddressUsageMapping(
          cprId = "c3e6f8a0-3333-4d4c-9e5f-000000000301",
          nomisId = 101L,
          addressUsageCode = "HOME",
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      corePersonAddressUsageMappingRepository.save(
        CorePersonAddressUsageMapping(
          cprId = "c3e6f8a0-3333-4d4c-9e5f-000000000302",
          nomisId = 101L,
          addressUsageCode = "WORK",
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      corePersonAddressUsageMappingRepository.save(
        CorePersonAddressUsageMapping(
          cprId = "c3e6f8a0-3333-4d4c-9e5f-000000000303",
          nomisId = 102L,
          addressUsageCode = "HOME",
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      val request = CorePersonMappingsByNomisIdsRequest(
        addresses = listOf(101L),
        contacts = listOf(ContactByNomisIdRequest(nomisId = 201L, nomisContactType = NomisContactType.EMAIL)),
        addressUsages = listOf(AddressUsageByNomisIdRequest(nomisId = 101L, addressUsageCode = "HOME")),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when a required list is missing`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("""{ "addresses": [101], "contacts": [{"nomisId": 201, "nomisContactType": "EMAIL"}] }""")
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the address mappings for the requested nomis ids`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                addresses = listOf(101L, 102L, 999L),
                contacts = emptyList(),
                addressUsages = emptyList(),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("addresses.length()").isEqualTo(2)
          .jsonPath("addresses[*].nomisId").value<List<Int>> {
            assertThat(it).containsExactlyInAnyOrder(101, 102)
          }
          .jsonPath("addresses[*].cprId").value<List<String>> {
            assertThat(it).containsExactlyInAnyOrder(
              "a1c4d6e8-1111-4b2a-9c3d-000000000101",
              "a1c4d6e8-1111-4b2a-9c3d-000000000102",
            )
          }
          .jsonPath("addresses[0].nomisPrisonNumber").isEqualTo(nomisPrisonNumber)
          .jsonPath("addresses[0].label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("addresses[0].mappingType").isEqualTo("MIGRATED")
          .jsonPath("addresses[0].whenCreated").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("contacts").isEmpty
          .jsonPath("addressUsages").isEmpty
      }

      @Test
      fun `will return the contact mappings for the requested nomis ids`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                addresses = emptyList(),
                contacts = listOf(
                  ContactByNomisIdRequest(nomisId = 201L, nomisContactType = NomisContactType.EMAIL),
                  ContactByNomisIdRequest(nomisId = 202L, nomisContactType = NomisContactType.PHONE),
                ),
                addressUsages = emptyList(),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("contacts.length()").isEqualTo(2)
          .jsonPath("contacts[?(@.nomisId == 201)].nomisContactType").isEqualTo("EMAIL")
          .jsonPath("contacts[?(@.nomisId == 201)].cprId").isEqualTo("b2d5e7f9-2222-4c3b-8d4e-000000000201")
          .jsonPath("contacts[?(@.nomisId == 202)].nomisContactType").isEqualTo("PHONE")
          .jsonPath("contacts[?(@.nomisId == 202)].cprId").isEqualTo("b2d5e7f9-2222-4c3b-8d4e-000000000202")
          .jsonPath("addresses").isEmpty
          .jsonPath("addressUsages").isEmpty
      }

      @Test
      fun `will only return contact mappings where both the nomis id and contact type match`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                addresses = emptyList(),
                contacts = listOf(
                  ContactByNomisIdRequest(nomisId = 201L, nomisContactType = NomisContactType.PHONE),
                  ContactByNomisIdRequest(nomisId = 201L, nomisContactType = NomisContactType.EMAIL),
                  ContactByNomisIdRequest(nomisId = 202L, nomisContactType = NomisContactType.EMAIL),
                ),
                addressUsages = emptyList(),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("contacts.length()").isEqualTo(1)
          .jsonPath("contacts[0].nomisId").isEqualTo(201)
          .jsonPath("contacts[0].nomisContactType").isEqualTo("EMAIL")
          .jsonPath("contacts[0].cprId").isEqualTo("b2d5e7f9-2222-4c3b-8d4e-000000000201")
          .jsonPath("addresses").isEmpty
          .jsonPath("addressUsages").isEmpty
      }

      @Test
      fun `will only return address usage mappings where both the nomis id and usage code match`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                addresses = emptyList(),
                contacts = emptyList(),
                addressUsages = listOf(
                  AddressUsageByNomisIdRequest(nomisId = 101L, addressUsageCode = "HOME"),
                  // 102 only has a HOME usage so this should not match
                  AddressUsageByNomisIdRequest(nomisId = 102L, addressUsageCode = "WORK"),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("addressUsages.length()").isEqualTo(1)
          .jsonPath("addressUsages[0].cprId").isEqualTo("c3e6f8a0-3333-4d4c-9e5f-000000000301")
          .jsonPath("addressUsages[0].nomisId").isEqualTo(101)
          .jsonPath("addressUsages[0].addressUsageCode").isEqualTo("HOME")
          .jsonPath("addressUsages[0].nomisPrisonNumber").isEqualTo(nomisPrisonNumber)
          .jsonPath("addresses").isEmpty
          .jsonPath("contacts").isEmpty
      }

      @Test
      fun `will return all requested mapping types together`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                addresses = listOf(101L),
                contacts = listOf(ContactByNomisIdRequest(nomisId = 201L, nomisContactType = NomisContactType.EMAIL)),
                addressUsages = listOf(
                  AddressUsageByNomisIdRequest(nomisId = 101L, addressUsageCode = "HOME"),
                  AddressUsageByNomisIdRequest(nomisId = 101L, addressUsageCode = "WORK"),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("addresses.length()").isEqualTo(1)
          .jsonPath("addresses[0].nomisId").isEqualTo(101)
          .jsonPath("contacts.length()").isEqualTo(1)
          .jsonPath("contacts[0].nomisId").isEqualTo(201)
          .jsonPath("addressUsages.length()").isEqualTo(2)
          .jsonPath("addressUsages[*].addressUsageCode").value<List<String>> {
            assertThat(it).containsExactlyInAnyOrder("HOME", "WORK")
          }
      }

      @Test
      fun `will return empty lists when no mappings match`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                addresses = listOf(999L),
                contacts = listOf(ContactByNomisIdRequest(nomisId = 999L, nomisContactType = NomisContactType.EMAIL)),
                addressUsages = listOf(AddressUsageByNomisIdRequest(nomisId = 999L, addressUsageCode = "HOME")),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("addresses").isEmpty
          .jsonPath("contacts").isEmpty
          .jsonPath("addressUsages").isEmpty
      }

      @Test
      fun `will return empty lists when no ids are requested`() {
        webTestClient.post()
          .uri("/mapping/core-person/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                addresses = emptyList(),
                contacts = emptyList(),
                addressUsages = emptyList(),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("addresses").isEmpty
          .jsonPath("contacts").isEmpty
          .jsonPath("addressUsages").isEmpty
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/core-person/delete")
  inner class DeleteCorePersonMigration {
    private val nomisPrisonNumber = "A1234BC"

    @BeforeEach
    fun setUp() = runTest {
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "084bde26-a0e4-43dc-8fa9-c7e2cceab7f1",
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "6a0d1f7a-98df-4fdd-b168-cbbc4f7e0b79",
          nomisPrisonNumber = "D1234EF",
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/core-person/delete")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/delete")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/delete")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete all core person mappings`() = runTest {
        assertThat(corePersonMappingRepository.findAll().toList()).isNotEmpty()
        webTestClient.delete()
          .uri("/mapping/core-person/delete")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        assertThat(corePersonMappingRepository.findAll().toList()).isEmpty()
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
          .jsonPath("$.content..nomisPrisonNumber").value<List<String>> {
            assertThat(it).containsExactly("A1231BC", "A1232BC", "A1233BC", "A1234BC")
          }
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
        .uri("/mapping/core-person")
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
        assertThat(corePersonAddressMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(corePersonAddressUsageMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(corePersonContactMappingRepository.findAll().count()).isEqualTo(2)
        assertThat(corePersonMappingRepository.findAll().count()).isEqualTo(1)

        webTestClient.delete()
          .uri("/mapping/core-person")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        // TODO add other child mappings when implemented
        assertThat(corePersonAddressMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(corePersonAddressUsageMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(corePersonContactMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(corePersonMappingRepository.findAll().count()).isEqualTo(0)
      }
    }
  }

  private suspend fun assertAddressesForCprId(updateId: String) = assertThat(corePersonAddressMappingRepository.findOneByCprId(updateId))
  private suspend fun assertAddressUsagesForCprId(updateId: String) = assertThat(corePersonAddressUsageMappingRepository.findOneByCprId(updateId))
  private suspend fun assertEmailsForCprId(updateId: String) = assertThat(corePersonContactMappingRepository.findOneByCprId(updateId))
  private suspend fun assertPhonesForCprId(updateId: String) = assertThat(corePersonContactMappingRepository.findOneByCprId(updateId))
  private suspend fun assertAddressesForPrisonNumber(prisonNumber: String) = assertThat(corePersonAddressMappingRepository.findAll().toList().filter { it.nomisPrisonNumber == prisonNumber })
  private suspend fun assertAddressUsagesForPrisonNumber(prisonNumber: String) = assertThat(corePersonAddressUsageMappingRepository.findAll().toList().filter { it.nomisPrisonNumber == prisonNumber })
  private suspend fun assertContactsForPrisonNumber(prisonNumber: String) = assertThat(corePersonContactMappingRepository.findAll().toList().filter { it.nomisPrisonNumber == prisonNumber })
}
