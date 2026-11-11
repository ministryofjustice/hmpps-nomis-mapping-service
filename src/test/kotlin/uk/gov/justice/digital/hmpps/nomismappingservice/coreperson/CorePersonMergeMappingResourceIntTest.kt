package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

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
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.AddressUsageByNomisIdRequest
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.ContactByNomisIdRequest
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressContactMergeMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressUsageMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressUsageMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressUsageMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonContactMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonContactMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonContactMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.NomisContactType
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier.IdentifierByNomisIdRequest
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier.OffenderAliasMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier.OffenderAliasMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier.OffenderIdentifierMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier.OffenderIdentifierMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime

class CorePersonMergeMappingResourceIntTest(
  @Autowired private val corePersonAddressMappingRepository: CorePersonAddressMappingRepository,
  @Autowired private val corePersonAddressUsageMappingRepository: CorePersonAddressUsageMappingRepository,
  @Autowired private val corePersonContactMappingRepository: CorePersonContactMappingRepository,
  @Autowired private val offenderAliasMappingRepository: OffenderAliasMappingRepository,
  @Autowired private val offenderIdentifierMappingRepository: OffenderIdentifierMappingRepository,
) : IntegrationTestBase() {

  @AfterEach
  fun tearDown() = runTest {
    corePersonAddressMappingRepository.deleteAll()
    corePersonAddressUsageMappingRepository.deleteAll()
    corePersonContactMappingRepository.deleteAll()
    offenderAliasMappingRepository.deleteAll()
    offenderIdentifierMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST /mapping/core-person/merge")
  inner class MergeCorePersonMappings {
    val toNomisPrisonNumber = "A1234BC"
    val fromNomisPrisonNumber = "D1234EF"

    @Nested
    inner class Security {
      val mapping = CorePersonAddressContactMergeMappingsDto(
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
              CorePersonAddressContactMergeMappingsDto(
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
              CorePersonAddressContactMergeMappingsDto(
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
      val mapping = CorePersonAddressContactMergeMappingsDto(
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
              CorePersonAddressContactMergeMappingsDto(
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
  @DisplayName("POST /mapping/core-person/merge/mappings/by-nomis-ids")
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
      offenderAliasMappingRepository.save(
        OffenderAliasMapping(
          cprId = "d4f7a9b1-4444-4e5d-8f6a-000000000401",
          nomisOffenderId = 401L,
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      offenderAliasMappingRepository.save(
        OffenderAliasMapping(
          cprId = "d4f7a9b1-4444-4e5d-8f6a-000000000402",
          nomisOffenderId = 402L,
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      offenderIdentifierMappingRepository.save(
        OffenderIdentifierMapping(
          cprId = "e5a8b0c2-5555-4f6e-9a7b-000000000501",
          nomisOffenderId = 401L,
          nomisIdentifierSequence = 1,
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      offenderIdentifierMappingRepository.save(
        OffenderIdentifierMapping(
          cprId = "e5a8b0c2-5555-4f6e-9a7b-000000000502",
          nomisOffenderId = 401L,
          nomisIdentifierSequence = 2,
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      offenderIdentifierMappingRepository.save(
        OffenderIdentifierMapping(
          cprId = "e5a8b0c2-5555-4f6e-9a7b-000000000503",
          nomisOffenderId = 402L,
          nomisIdentifierSequence = 1,
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
        aliases = emptyList(),
        identifiers = emptyList(),
        addresses = listOf(101L),
        contacts = listOf(ContactByNomisIdRequest(nomisId = 201L, nomisContactType = NomisContactType.EMAIL)),
        addressUsages = listOf(AddressUsageByNomisIdRequest(nomisId = 101L, addressUsageCode = "HOME")),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/merge/mappings/by-nomis-ids")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/merge/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/merge/mappings/by-nomis-ids")
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
          .uri("/mapping/core-person/merge/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("""{ "aliases": [], "identifiers": [], "addresses": [101], "contacts": [{"nomisId": 201, "nomisContactType": "EMAIL"}] }""")
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the address mappings for the requested nomis ids`() {
        webTestClient.post()
          .uri("/mapping/core-person/merge/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                aliases = emptyList(),
                identifiers = emptyList(),
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
          .uri("/mapping/core-person/merge/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                aliases = emptyList(),
                identifiers = emptyList(),
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
          .uri("/mapping/core-person/merge/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                aliases = emptyList(),
                identifiers = emptyList(),
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
          .uri("/mapping/core-person/merge/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                aliases = emptyList(),
                identifiers = emptyList(),
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
      fun `will return the alias mappings for the requested nomis ids`() {
        webTestClient.post()
          .uri("/mapping/core-person/merge/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                aliases = listOf(401L, 402L, 999L),
                identifiers = emptyList(),
                addresses = emptyList(),
                contacts = emptyList(),
                addressUsages = emptyList(),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("aliases.length()").isEqualTo(2)
          .jsonPath("aliases[*].nomisOffenderId").value<List<Int>> {
            assertThat(it).containsExactlyInAnyOrder(401, 402)
          }
          .jsonPath("aliases[*].cprId").value<List<String>> {
            assertThat(it).containsExactlyInAnyOrder(
              "d4f7a9b1-4444-4e5d-8f6a-000000000401",
              "d4f7a9b1-4444-4e5d-8f6a-000000000402",
            )
          }
          .jsonPath("aliases[0].nomisPrisonNumber").isEqualTo(nomisPrisonNumber)
          .jsonPath("aliases[0].mappingType").isEqualTo("MIGRATED")
          .jsonPath("identifiers").isEmpty
          .jsonPath("addresses").isEmpty
          .jsonPath("contacts").isEmpty
          .jsonPath("addressUsages").isEmpty
      }

      @Test
      fun `will only return identifier mappings where both the nomis id and sequence match`() {
        webTestClient.post()
          .uri("/mapping/core-person/merge/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                aliases = emptyList(),
                identifiers = listOf(
                  IdentifierByNomisIdRequest(nomisId = 401L, nomisSequence = 1),
                  // 402 only has sequence 1 so this should not match
                  IdentifierByNomisIdRequest(nomisId = 402L, nomisSequence = 2),
                ),
                addresses = emptyList(),
                contacts = emptyList(),
                addressUsages = emptyList(),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("identifiers.length()").isEqualTo(1)
          .jsonPath("identifiers[0].cprId").isEqualTo("e5a8b0c2-5555-4f6e-9a7b-000000000501")
          .jsonPath("identifiers[0].nomisOffenderId").isEqualTo(401)
          .jsonPath("identifiers[0].nomisIdentifierSequence").isEqualTo(1)
          .jsonPath("identifiers[0].nomisPrisonNumber").isEqualTo(nomisPrisonNumber)
          .jsonPath("aliases").isEmpty
          .jsonPath("addresses").isEmpty
          .jsonPath("contacts").isEmpty
          .jsonPath("addressUsages").isEmpty
      }

      @Test
      fun `will return all requested mapping types together`() {
        webTestClient.post()
          .uri("/mapping/core-person/merge/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                aliases = listOf(401L),
                identifiers = listOf(
                  IdentifierByNomisIdRequest(nomisId = 401L, nomisSequence = 1),
                  IdentifierByNomisIdRequest(nomisId = 401L, nomisSequence = 2),
                ),
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
          .jsonPath("aliases.length()").isEqualTo(1)
          .jsonPath("aliases[0].nomisOffenderId").isEqualTo(401)
          .jsonPath("identifiers.length()").isEqualTo(2)
          .jsonPath("identifiers[*].nomisIdentifierSequence").value<List<Int>> {
            assertThat(it).containsExactlyInAnyOrder(1, 2)
          }
      }

      @Test
      fun `will return empty lists when no mappings match`() {
        webTestClient.post()
          .uri("/mapping/core-person/merge/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                aliases = listOf(999L),
                identifiers = listOf(IdentifierByNomisIdRequest(nomisId = 999L, nomisSequence = 1)),
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
          .jsonPath("aliases").isEmpty
          .jsonPath("identifiers").isEmpty
      }

      @Test
      fun `will return empty lists when no ids are requested`() {
        webTestClient.post()
          .uri("/mapping/core-person/merge/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CorePersonMappingsByNomisIdsRequest(
                aliases = emptyList(),
                identifiers = emptyList(),
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
          .jsonPath("aliases").isEmpty
          .jsonPath("identifiers").isEmpty
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
