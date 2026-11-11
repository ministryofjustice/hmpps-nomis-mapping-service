package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingIdDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.util.UUID

class CorePersonAddressContactRepairResourceIntTest(
  @Autowired private val corePersonAddressMappingRepository: CorePersonAddressMappingRepository,
  @Autowired private val corePersonAddressUsageMappingRepository: CorePersonAddressUsageMappingRepository,
  @Autowired private val corePersonContactMappingRepository: CorePersonContactMappingRepository,
) : IntegrationTestBase() {

  @AfterEach
  fun tearDown() = runTest {
    corePersonAddressMappingRepository.deleteAll()
    corePersonAddressUsageMappingRepository.deleteAll()
    corePersonContactMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST /mapping/core-person/address-contact/replace")
  inner class ReplaceCorePersonMappings {
    val nomisPrisonNumber = "A1234BC"

    @Nested
    inner class Security {
      val mapping = CorePersonAddressContactMappingsDto(
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
          .uri("/mapping/core-person/address-contact/replace")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact/replace")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact/replace")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = CorePersonAddressContactMappingsDto(
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
          .uri("/mapping/core-person/address-contact/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk
      }

      @Test
      fun `will persist the address, address usage, email and phone mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact/replace")
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
          .uri("/mapping/core-person/address-contact/replace")
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
          .uri("/mapping/core-person/address-contact/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonAddressContactMappingsDto(
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
  @DisplayName("POST /mapping/core-person/address-contact/mappings/by-nomis-ids")
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
      val request = CorePersonAddressContactMappingsByNomisIdsRequest(
        addresses = listOf(101L),
        contacts = listOf(ContactByNomisIdRequest(nomisId = 201L, nomisContactType = NomisContactType.EMAIL)),
        addressUsages = listOf(AddressUsageByNomisIdRequest(nomisId = 101L, addressUsageCode = "HOME")),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact/mappings/by-nomis-ids")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/address-contact/mappings/by-nomis-ids")
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
          .uri("/mapping/core-person/address-contact/mappings/by-nomis-ids")
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
          .uri("/mapping/core-person/address-contact/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonAddressContactMappingsByNomisIdsRequest(
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
          .uri("/mapping/core-person/address-contact/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonAddressContactMappingsByNomisIdsRequest(
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
          .uri("/mapping/core-person/address-contact/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonAddressContactMappingsByNomisIdsRequest(
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
          .uri("/mapping/core-person/address-contact/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonAddressContactMappingsByNomisIdsRequest(
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
          .uri("/mapping/core-person/address-contact/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonAddressContactMappingsByNomisIdsRequest(
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
          .uri("/mapping/core-person/address-contact/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonAddressContactMappingsByNomisIdsRequest(
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
          .uri("/mapping/core-person/address-contact/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonAddressContactMappingsByNomisIdsRequest(
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

  private suspend fun assertAddressesForCprId(updateId: String) = assertThat(corePersonAddressMappingRepository.findOneByCprId(updateId))
  private suspend fun assertAddressUsagesForCprId(updateId: String) = assertThat(corePersonAddressUsageMappingRepository.findOneByCprId(updateId))
  private suspend fun assertEmailsForCprId(updateId: String) = assertThat(corePersonContactMappingRepository.findOneByCprId(updateId))
  private suspend fun assertPhonesForCprId(updateId: String) = assertThat(corePersonContactMappingRepository.findOneByCprId(updateId))
  private suspend fun assertAddressesForPrisonNumber(prisonNumber: String) = assertThat(corePersonAddressMappingRepository.findAll().toList().filter { it.nomisPrisonNumber == prisonNumber })
  private suspend fun assertAddressUsagesForPrisonNumber(prisonNumber: String) = assertThat(corePersonAddressUsageMappingRepository.findAll().toList().filter { it.nomisPrisonNumber == prisonNumber })
  private suspend fun assertContactsForPrisonNumber(prisonNumber: String) = assertThat(corePersonContactMappingRepository.findAll().toList().filter { it.nomisPrisonNumber == prisonNumber })
}
