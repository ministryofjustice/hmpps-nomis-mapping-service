package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import kotlinx.coroutines.flow.count
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
import java.util.*

class ContactPersonMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var personMappingRepository: PersonMappingRepository

  @Autowired
  private lateinit var personAddressMappingRepository: PersonAddressMappingRepository

  @Autowired
  private lateinit var personPhoneMappingRepository: PersonPhoneMappingRepository

  @Autowired
  private lateinit var personEmailMappingRepository: PersonEmailMappingRepository

  @Autowired
  private lateinit var personEmploymentMappingRepository: PersonEmploymentMappingRepository

  @Autowired
  private lateinit var personIdentifierMappingRepository: PersonIdentifierMappingRepository

  @Autowired
  private lateinit var personRestrictionMappingRepository: PersonRestrictionMappingRepository

  @Autowired
  private lateinit var prisonerRestrictionMappingRepository: PrisonerRestrictionMappingRepository

  @Autowired
  private lateinit var personContactMappingRepository: PersonContactMappingRepository

  @Autowired
  private lateinit var personContactRestrictionMappingRepository: PersonContactRestrictionMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    personContactRestrictionMappingRepository.deleteAll()
    personContactMappingRepository.deleteAll()
    personRestrictionMappingRepository.deleteAll()
    prisonerRestrictionMappingRepository.deleteAll()
    personIdentifierMappingRepository.deleteAll()
    personEmploymentMappingRepository.deleteAll()
    personEmailMappingRepository.deleteAll()
    personPhoneMappingRepository.deleteAll()
    personAddressMappingRepository.deleteAll()
    personMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST mapping/contact-person/migrate")
  inner class CreateMappings {

    @Nested
    inner class Security {
      val mappings = ContactPersonMappingsDto(
        personMapping = ContactPersonSimpleMappingIdDto(
          dpsId = UUID.randomUUID().toString(),
          nomisId = 12345L,
        ),
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
        personContactMapping = emptyList(),
        personContactRestrictionMapping = emptyList(),
        personEmailMapping = emptyList(),
        personRestrictionMapping = emptyList(),
        personPhoneMapping = emptyList(),
        personAddressMapping = emptyList(),
        personEmploymentMapping = emptyList(),
        personIdentifierMapping = emptyList(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingPersonMapping: PersonMapping

      val mappings = ContactPersonMappingsDto(
        personMapping = ContactPersonSimpleMappingIdDto(
          dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
          nomisId = 12345L,
        ),
        label = null,
        mappingType = ContactPersonMappingType.MIGRATED,
        whenCreated = LocalDateTime.now(),
        personContactMapping = emptyList(),
        personContactRestrictionMapping = emptyList(),
        personEmailMapping = emptyList(),
        personRestrictionMapping = emptyList(),
        personPhoneMapping = emptyList(),
        personAddressMapping = emptyList(),
        personEmploymentMapping = emptyList(),
        personIdentifierMapping = emptyList(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingPersonMapping = personMappingRepository.save(
          PersonMapping(
            dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
            nomisId = 12345L,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )
        personIdentifierMappingRepository.save(
          PersonIdentifierMapping(
            dpsId = "18e89dec-6ace-4706-9283-8e11e9ebe886",
            nomisPersonId = 54321,
            nomisSequenceNumber = 1,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same person to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will not allow a child of a person to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                // unique person who has never been migrated
                personMapping = ContactPersonSimpleMappingIdDto(
                  dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
                  nomisId = 54321,
                ),
                // an identifier from a different person - this would be coding error - a can't happen
                personIdentifierMapping = listOf(
                  ContactPersonSequenceMappingIdDto(
                    dpsId = "18e89dec-6ace-4706-9283-8e11e9ebe886",
                    nomisPersonId = 54321,
                    nomisSequenceNumber = 1,
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
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
            .containsEntry("nomisId", existingPersonMapping.nomisId.toInt())
            .containsEntry("dpsId", existingPersonMapping.dpsId)
            .containsEntry("mappingType", existingPersonMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mappings.personMapping.nomisId.toInt())
            .containsEntry("dpsId", mappings.personMapping.dpsId)
            .containsEntry("mappingType", existingPersonMapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mappings = ContactPersonMappingsDto(
        personMapping = ContactPersonSimpleMappingIdDto(
          dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
          nomisId = 12345L,
        ),
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
        personContactMapping = emptyList(),
        personContactRestrictionMapping = emptyList(),
        personEmailMapping = emptyList(),
        personRestrictionMapping = emptyList(),
        personPhoneMapping = emptyList(),
        personAddressMapping = emptyList(),
        personEmploymentMapping = emptyList(),
        personIdentifierMapping = emptyList(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the person mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings.copy(label = "2023-01-01T12:45:12")))
          .exchange()
          .expectStatus().isCreated

        val personMapping =
          personMappingRepository.findOneByNomisId(mappings.personMapping.nomisId)!!

        assertThat(personMapping.dpsId).isEqualTo(mappings.personMapping.dpsId)
        assertThat(personMapping.nomisId).isEqualTo(mappings.personMapping.nomisId)
        assertThat(personMapping.label).isEqualTo("2023-01-01T12:45:12")
        assertThat(personMapping.mappingType).isEqualTo(mappings.mappingType)
        assertThat(personMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }

      @Test
      fun `will persist the person address mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                personAddressMapping = listOf(
                  ContactPersonSimpleMappingIdDto(dpsId = "0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6", nomisId = 1),
                  ContactPersonSimpleMappingIdDto(dpsId = "e96babce-4a24-49d7-8447-b45f8768f6c1", nomisId = 2),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(personAddressMappingRepository.findOneByNomisId(1)!!) {
          assertThat(dpsId).isEqualTo("0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6")
          assertThat(nomisId).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(personAddressMappingRepository.findOneByDpsId("e96babce-4a24-49d7-8447-b45f8768f6c1")!!) {
          assertThat(dpsId).isEqualTo("e96babce-4a24-49d7-8447-b45f8768f6c1")
          assertThat(nomisId).isEqualTo(2L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `will persist the person phone mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                personPhoneMapping = listOf(
                  ContactPersonPhoneMappingIdDto(dpsId = "0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6", dpsPhoneType = DpsPersonPhoneType.PERSON, nomisId = 1),
                  ContactPersonPhoneMappingIdDto(dpsId = "e96babce-4a24-49d7-8447-b45f8768f6c1", dpsPhoneType = DpsPersonPhoneType.PERSON, nomisId = 2),
                  ContactPersonPhoneMappingIdDto(dpsId = "e96babce-4a24-49d7-8447-b45f8768f6c1", dpsPhoneType = DpsPersonPhoneType.ADDRESS, nomisId = 3),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(personPhoneMappingRepository.findOneByNomisId(1)!!) {
          assertThat(dpsId).isEqualTo("0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6")
          assertThat(dpsPhoneType).isEqualTo(DpsPersonPhoneType.PERSON)
          assertThat(nomisId).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(personPhoneMappingRepository.findOneByDpsIdAndDpsPhoneType("e96babce-4a24-49d7-8447-b45f8768f6c1", DpsPersonPhoneType.PERSON)!!) {
          assertThat(dpsId).isEqualTo("e96babce-4a24-49d7-8447-b45f8768f6c1")
          assertThat(dpsPhoneType).isEqualTo(DpsPersonPhoneType.PERSON)
          assertThat(nomisId).isEqualTo(2L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(personPhoneMappingRepository.findOneByDpsIdAndDpsPhoneType("e96babce-4a24-49d7-8447-b45f8768f6c1", DpsPersonPhoneType.ADDRESS)!!) {
          assertThat(dpsId).isEqualTo("e96babce-4a24-49d7-8447-b45f8768f6c1")
          assertThat(dpsPhoneType).isEqualTo(DpsPersonPhoneType.ADDRESS)
          assertThat(nomisId).isEqualTo(3L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `will persist the person email mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                personEmailMapping = listOf(
                  ContactPersonSimpleMappingIdDto(dpsId = "0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6", nomisId = 1),
                  ContactPersonSimpleMappingIdDto(dpsId = "e96babce-4a24-49d7-8447-b45f8768f6c1", nomisId = 2),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(personEmailMappingRepository.findOneByNomisId(1)!!) {
          assertThat(dpsId).isEqualTo("0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6")
          assertThat(nomisId).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(personEmailMappingRepository.findOneByDpsId("e96babce-4a24-49d7-8447-b45f8768f6c1")!!) {
          assertThat(dpsId).isEqualTo("e96babce-4a24-49d7-8447-b45f8768f6c1")
          assertThat(nomisId).isEqualTo(2L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `will persist the person employment mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                personEmploymentMapping = listOf(
                  ContactPersonSequenceMappingIdDto(dpsId = "0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6", nomisPersonId = 11234, nomisSequenceNumber = 1),
                  ContactPersonSequenceMappingIdDto(dpsId = "e96babce-4a24-49d7-8447-b45f8768f6c1", nomisPersonId = 11234, nomisSequenceNumber = 2),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(
          personEmploymentMappingRepository.findOneByNomisPersonIdAndNomisSequenceNumber(
            nomisPersonId = 11234,
            nomisSequenceNumber = 1,
          )!!,
        ) {
          assertThat(dpsId).isEqualTo("0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6")
          assertThat(nomisPersonId).isEqualTo(11234L)
          assertThat(nomisSequenceNumber).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(personEmploymentMappingRepository.findOneByDpsId("e96babce-4a24-49d7-8447-b45f8768f6c1")!!) {
          assertThat(dpsId).isEqualTo("e96babce-4a24-49d7-8447-b45f8768f6c1")
          assertThat(nomisPersonId).isEqualTo(11234L)
          assertThat(nomisSequenceNumber).isEqualTo(2L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `will persist the person identifier mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                personIdentifierMapping = listOf(
                  ContactPersonSequenceMappingIdDto(dpsId = "0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6", nomisPersonId = 11234, nomisSequenceNumber = 1),
                  ContactPersonSequenceMappingIdDto(dpsId = "e96babce-4a24-49d7-8447-b45f8768f6c1", nomisPersonId = 11234, nomisSequenceNumber = 2),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(
          personIdentifierMappingRepository.findOneByNomisPersonIdAndNomisSequenceNumber(
            nomisPersonId = 11234,
            nomisSequenceNumber = 1,
          )!!,
        ) {
          assertThat(dpsId).isEqualTo("0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6")
          assertThat(nomisPersonId).isEqualTo(11234L)
          assertThat(nomisSequenceNumber).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(personIdentifierMappingRepository.findOneByDpsId("e96babce-4a24-49d7-8447-b45f8768f6c1")!!) {
          assertThat(dpsId).isEqualTo("e96babce-4a24-49d7-8447-b45f8768f6c1")
          assertThat(nomisPersonId).isEqualTo(11234L)
          assertThat(nomisSequenceNumber).isEqualTo(2L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `will persist the person restriction mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                personRestrictionMapping = listOf(
                  ContactPersonSimpleMappingIdDto(dpsId = "0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6", nomisId = 1),
                  ContactPersonSimpleMappingIdDto(dpsId = "e96babce-4a24-49d7-8447-b45f8768f6c1", nomisId = 2),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(personRestrictionMappingRepository.findOneByNomisId(1)!!) {
          assertThat(dpsId).isEqualTo("0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6")
          assertThat(nomisId).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(personRestrictionMappingRepository.findOneByDpsId("e96babce-4a24-49d7-8447-b45f8768f6c1")!!) {
          assertThat(dpsId).isEqualTo("e96babce-4a24-49d7-8447-b45f8768f6c1")
          assertThat(nomisId).isEqualTo(2L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `will persist the person contact mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                personContactMapping = listOf(
                  ContactPersonSimpleMappingIdDto(dpsId = "0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6", nomisId = 1),
                  ContactPersonSimpleMappingIdDto(dpsId = "e96babce-4a24-49d7-8447-b45f8768f6c1", nomisId = 2),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(personContactMappingRepository.findOneByNomisId(1)!!) {
          assertThat(dpsId).isEqualTo("0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6")
          assertThat(nomisId).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(personContactMappingRepository.findOneByDpsId("e96babce-4a24-49d7-8447-b45f8768f6c1")!!) {
          assertThat(dpsId).isEqualTo("e96babce-4a24-49d7-8447-b45f8768f6c1")
          assertThat(nomisId).isEqualTo(2L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `will persist the person contact restriction mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                personContactRestrictionMapping = listOf(
                  ContactPersonSimpleMappingIdDto(dpsId = "0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6", nomisId = 1),
                  ContactPersonSimpleMappingIdDto(dpsId = "e96babce-4a24-49d7-8447-b45f8768f6c1", nomisId = 2),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(personContactRestrictionMappingRepository.findOneByNomisId(1)!!) {
          assertThat(dpsId).isEqualTo("0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6")
          assertThat(nomisId).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(personContactRestrictionMappingRepository.findOneByDpsId("e96babce-4a24-49d7-8447-b45f8768f6c1")!!) {
          assertThat(dpsId).isEqualTo("e96babce-4a24-49d7-8447-b45f8768f6c1")
          assertThat(nomisId).isEqualTo(2L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/contact-person/replace/prisoner/{offenderNo}")
  inner class ReplaceMappingsPrisoner {

    @Nested
    inner class Security {
      val mappings = ContactPersonPrisonerMappingsDto(
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
        personContactMapping = emptyList(),
        personContactRestrictionMapping = emptyList(),
        personContactMappingsToRemoveByDpsId = emptyList(),
        personContactRestrictionMappingsToRemoveByDpsId = emptyList(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner/A1234KT")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner/A1234KT")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner/A1234KT")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      private val oldDpsContactRestrictionId = "98765"
      private val newDpsContactRestrictionId = "32442442"
      private val oldDpsContactId1 = "3482842"
      private val newDpsContactId1 = "28842482"
      private val oldDpsContactId2 = "375357"
      private val newDpsContactId2 = "28842485"

      val mappings = ContactPersonPrisonerMappingsDto(
        mappingType = ContactPersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.now(),
        personContactMapping = listOf(
          ContactPersonSimpleMappingIdDto(dpsId = newDpsContactId1, nomisId = 111),
          ContactPersonSimpleMappingIdDto(dpsId = newDpsContactId2, nomisId = 22234),
        ),
        personContactRestrictionMapping = listOf(
          ContactPersonSimpleMappingIdDto(dpsId = newDpsContactRestrictionId, nomisId = 123),
        ),
        personContactMappingsToRemoveByDpsId = listOf(oldDpsContactId1, oldDpsContactId2),
        personContactRestrictionMappingsToRemoveByDpsId = listOf(oldDpsContactRestrictionId),
      )

      @BeforeEach
      fun setUp() = runTest {
        personContactRestrictionMappingRepository.save(
          PersonContactRestrictionMapping(
            dpsId = oldDpsContactRestrictionId,
            nomisId = 123,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        personContactMappingRepository.save(
          PersonContactMapping(
            dpsId = oldDpsContactId1,
            nomisId = 111,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        personContactMappingRepository.save(
          PersonContactMapping(
            dpsId = oldDpsContactId2,
            nomisId = 222,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        // rouge item that previously was not deleted to due to some other failre
        personContactMappingRepository.save(
          PersonContactMapping(
            dpsId = newDpsContactId1,
            nomisId = 444,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        personContactMappingRepository.save(
          PersonContactMapping(
            dpsId = "2477292942",
            nomisId = 22234,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
      }

      @Test
      fun `returns 200 when are mappings replaced`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner/A1234KT")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isOk
      }

      @Test
      fun `will persist the new contact mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner/A1234KT")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isOk

        with(personContactMappingRepository.findOneByDpsId(newDpsContactId1)!!) {
          assertThat(dpsId).isEqualTo(newDpsContactId1)
          assertThat(nomisId).isEqualTo(111)
          assertThat(label).isNull()
          assertThat(mappingType).isEqualTo(ContactPersonMappingType.NOMIS_CREATED)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }

        with(personContactMappingRepository.findOneByDpsId(newDpsContactId2)!!) {
          assertThat(dpsId).isEqualTo(newDpsContactId2)
          assertThat(nomisId).isEqualTo(22234)
          assertThat(label).isNull()
          assertThat(mappingType).isEqualTo(ContactPersonMappingType.NOMIS_CREATED)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `will persist the new contact restriction mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner/A1234KT")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isOk

        with(personContactRestrictionMappingRepository.findOneByDpsId(newDpsContactRestrictionId)!!) {
          assertThat(dpsId).isEqualTo(newDpsContactRestrictionId)
          assertThat(nomisId).isEqualTo(123)
          assertThat(label).isNull()
          assertThat(mappingType).isEqualTo(ContactPersonMappingType.NOMIS_CREATED)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `will remove replaced mappings`() = runTest {
        assertThat(personContactMappingRepository.existsById(oldDpsContactId1)).isTrue
        assertThat(personContactMappingRepository.existsById(oldDpsContactId2)).isTrue
        assertThat(personContactRestrictionMappingRepository.existsById(oldDpsContactRestrictionId)).isTrue

        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner/A1234KT")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isOk

        assertThat(personContactMappingRepository.existsById(oldDpsContactId1)).isFalse
        assertThat(personContactMappingRepository.existsById(oldDpsContactId2)).isFalse
        assertThat(personContactRestrictionMappingRepository.existsById(oldDpsContactRestrictionId)).isFalse
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/contact-person/replace/person/{personId}")
  inner class ReplaceMappingsPerson {

    @Nested
    inner class Security {
      val mappings = ContactPersonMappingsDto(
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
        personContactMapping = emptyList(),
        personContactRestrictionMapping = emptyList(),
        label = null,
        personMapping = ContactPersonSimpleMappingIdDto(dpsId = "0", nomisId = 0),
        personAddressMapping = emptyList(),
        personPhoneMapping = emptyList(),
        personEmailMapping = emptyList(),
        personEmploymentMapping = emptyList(),
        personIdentifierMapping = emptyList(),
        personRestrictionMapping = emptyList(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/person/12345")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/person/12345")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/person/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      val mappings = ContactPersonMappingsDto(
        mappingType = ContactPersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.now(),
        personMapping = ContactPersonSimpleMappingIdDto(dpsId = "2", nomisId = 2),
        personContactMapping = listOf(
          ContactPersonSimpleMappingIdDto(dpsId = "2", nomisId = 2),
        ),
        personContactRestrictionMapping = listOf(
          ContactPersonSimpleMappingIdDto(dpsId = "2", nomisId = 2),
        ),
        personAddressMapping = listOf(
          ContactPersonSimpleMappingIdDto(dpsId = "2", nomisId = 2),
        ),
        personPhoneMapping = listOf(
          ContactPersonPhoneMappingIdDto(dpsId = "2", nomisId = 2, dpsPhoneType = DpsPersonPhoneType.PERSON),
        ),
        personEmailMapping = listOf(
          ContactPersonSimpleMappingIdDto(dpsId = "2", nomisId = 2),
        ),
        personEmploymentMapping = listOf(
          ContactPersonSequenceMappingIdDto(dpsId = "2", nomisPersonId = 2, nomisSequenceNumber = 2),
        ),
        personIdentifierMapping = listOf(
          ContactPersonSequenceMappingIdDto(dpsId = "2", nomisPersonId = 2, nomisSequenceNumber = 2),
        ),
        personRestrictionMapping = listOf(
          ContactPersonSimpleMappingIdDto(dpsId = "2", nomisId = 2),
        ),
      )

      @BeforeEach
      fun setUp() = runTest {
        personMappingRepository.save(
          PersonMapping(
            dpsId = "1",
            nomisId = 2,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        personPhoneMappingRepository.save(
          PersonPhoneMapping(
            dpsId = "1",
            nomisId = 2,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            dpsPhoneType = DpsPersonPhoneType.PERSON,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        personAddressMappingRepository.save(
          PersonAddressMapping(
            dpsId = "1",
            nomisId = 2,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        personEmailMappingRepository.save(
          PersonEmailMapping(
            dpsId = "1",
            nomisId = 2,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        personRestrictionMappingRepository.save(
          PersonRestrictionMapping(
            dpsId = "1",
            nomisId = 2,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        personIdentifierMappingRepository.save(
          PersonIdentifierMapping(
            dpsId = "1",
            nomisPersonId = 2,
            nomisSequenceNumber = 2,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        personEmploymentMappingRepository.save(
          PersonEmploymentMapping(
            dpsId = "1",
            nomisPersonId = 2,
            nomisSequenceNumber = 2,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        personContactRestrictionMappingRepository.save(
          PersonContactRestrictionMapping(
            dpsId = "1",
            nomisId = 2,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        personContactMappingRepository.save(
          PersonContactMapping(
            dpsId = "1",
            nomisId = 2,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
      }

      @Test
      fun `returns 200 when are mappings replaced`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/person/12345")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isOk
      }

      @Test
      fun `will persist the new contact mappings`() = runTest {
        assertThat(personMappingRepository.findOneByNomisId(2)!!.dpsId).isEqualTo("1")
        assertThat(personMappingRepository.findOneByDpsId("1")).isNotNull
        assertThat(personAddressMappingRepository.findOneByNomisId(2)!!.dpsId).isEqualTo("1")
        assertThat(personAddressMappingRepository.findOneByDpsId("1")).isNotNull
        assertThat(personPhoneMappingRepository.findOneByNomisId(2)!!.dpsId).isEqualTo("1")
        assertThat(personPhoneMappingRepository.findOneByDpsIdAndDpsPhoneType("1", DpsPersonPhoneType.PERSON)).isNotNull
        assertThat(personEmailMappingRepository.findOneByNomisId(2)!!.dpsId).isEqualTo("1")
        assertThat(personEmailMappingRepository.findOneByDpsId("1")).isNotNull
        assertThat(personEmploymentMappingRepository.findOneByNomisPersonIdAndNomisSequenceNumber(2, 2)!!.dpsId).isEqualTo("1")
        assertThat(personEmploymentMappingRepository.findOneByDpsId("1")).isNotNull
        assertThat(personIdentifierMappingRepository.findOneByNomisPersonIdAndNomisSequenceNumber(2, 2)!!.dpsId).isEqualTo("1")
        assertThat(personIdentifierMappingRepository.findOneByDpsId("1")).isNotNull
        assertThat(personRestrictionMappingRepository.findOneByNomisId(2)!!.dpsId).isEqualTo("1")
        assertThat(personRestrictionMappingRepository.findOneByDpsId("1")).isNotNull
        assertThat(personContactMappingRepository.findOneByNomisId(2)!!.dpsId).isEqualTo("1")
        assertThat(personContactMappingRepository.findOneByDpsId("1")).isNotNull
        assertThat(personContactRestrictionMappingRepository.findOneByNomisId(2)!!.dpsId).isEqualTo("1")
        assertThat(personContactRestrictionMappingRepository.findOneByDpsId("1")).isNotNull

        webTestClient.post()
          .uri("/mapping/contact-person/replace/person/12345")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isOk

        assertThat(personMappingRepository.findOneByNomisId(2)!!.dpsId).isEqualTo("2")
        assertThat(personMappingRepository.findOneByDpsId("1")).isNull()
        assertThat(personAddressMappingRepository.findOneByNomisId(2)!!.dpsId).isEqualTo("2")
        assertThat(personAddressMappingRepository.findOneByDpsId("1")).isNull()
        assertThat(personPhoneMappingRepository.findOneByNomisId(2)!!.dpsId).isEqualTo("2")
        assertThat(personPhoneMappingRepository.findOneByDpsIdAndDpsPhoneType("1", DpsPersonPhoneType.PERSON)).isNull()
        assertThat(personEmailMappingRepository.findOneByNomisId(2)!!.dpsId).isEqualTo("2")
        assertThat(personEmailMappingRepository.findOneByDpsId("1")).isNull()
        assertThat(personEmploymentMappingRepository.findOneByNomisPersonIdAndNomisSequenceNumber(2, 2)!!.dpsId).isEqualTo("2")
        assertThat(personEmploymentMappingRepository.findOneByDpsId("1")).isNull()
        assertThat(personIdentifierMappingRepository.findOneByNomisPersonIdAndNomisSequenceNumber(2, 2)!!.dpsId).isEqualTo("2")
        assertThat(personIdentifierMappingRepository.findOneByDpsId("1")).isNull()
        assertThat(personRestrictionMappingRepository.findOneByNomisId(2)!!.dpsId).isEqualTo("2")
        assertThat(personRestrictionMappingRepository.findOneByDpsId("1")).isNull()
        assertThat(personContactMappingRepository.findOneByNomisId(2)!!.dpsId).isEqualTo("2")
        assertThat(personContactMappingRepository.findOneByDpsId("1")).isNull()
        assertThat(personContactRestrictionMappingRepository.findOneByNomisId(2)!!.dpsId).isEqualTo("2")
        assertThat(personContactRestrictionMappingRepository.findOneByDpsId("1")).isNull()
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/person/nomis-person-id/{personId}")
  inner class GetPersonByNomisId {
    private val nomisPersonId = 12345L
    private lateinit var personMapping: PersonMapping

    @BeforeEach
    fun setUp() = runTest {
      personMapping = personMappingRepository.save(
        PersonMapping(
          dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisId = nomisPersonId,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person/nomis-person-id/{personId}", nomisPersonId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person/nomis-person-id/{personId}", nomisPersonId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person/nomis-person-id/{personId}", nomisPersonId)
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
          .uri("/mapping/contact-person/person/nomis-person-id/{personId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person/nomis-person-id/{personId}", nomisPersonId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo("edcd118c-41ba-42ea-b5c4-404b453ad58b")
          .jsonPath("nomisId").isEqualTo(nomisPersonId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/contact-person/person/nomis-person-id/{personId}")
  inner class DeletePersonByNomisId {
    private val nomisPersonId = 12345L
    private lateinit var personMapping: PersonMapping

    @BeforeEach
    fun setUp() = runTest {
      personMapping = personMappingRepository.save(
        PersonMapping(
          dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisId = nomisPersonId,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/person/nomis-person-id/{personId}", nomisPersonId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/person/nomis-person-id/{personId}", nomisPersonId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/person/nomis-person-id/{personId}", nomisPersonId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping does not exist`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/person/nomis-person-id/{personId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping data`() = runTest {
        assertThat(personMappingRepository.findOneByNomisId(nomisPersonId)).isNotNull()

        webTestClient.delete()
          .uri("/mapping/contact-person/person/nomis-person-id/{personId}", nomisPersonId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(personMappingRepository.findOneByNomisId(nomisPersonId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/contact-person/person/dps-contact-id/{contactId}")
  inner class DeletePersonByDpsId {
    private val dpsContactId = "12345"
    private lateinit var personMapping: PersonMapping

    @BeforeEach
    fun setUp() = runTest {
      personMapping = personMappingRepository.save(
        PersonMapping(
          dpsId = dpsContactId,
          nomisId = 8348383,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/person/dps-contact-id/{contactId}", dpsContactId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/person/dps-contact-id/{contactId}", dpsContactId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/person/dps-contact-id/{contactId}", dpsContactId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping does not exist`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/person/dps-contact-id/{contactId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping data`() = runTest {
        assertThat(personMappingRepository.findOneByDpsId(dpsContactId)).isNotNull()

        webTestClient.delete()
          .uri("/mapping/contact-person/person/dps-contact-id/{contactId}", dpsContactId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(personMappingRepository.findOneByDpsId(dpsContactId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/person/dps-contact-id/{contactId}")
  inner class GetPersonByDpsId {
    private val dpsContactId = "12345"
    private lateinit var personMapping: PersonMapping

    @BeforeEach
    fun setUp() = runTest {
      personMapping = personMappingRepository.save(
        PersonMapping(
          dpsId = dpsContactId,
          nomisId = 123456,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person/dps-contact-id/{contactId}", dpsContactId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person/dps-contact-id/{contactId}", dpsContactId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person/dps-contact-id/{contactId}", dpsContactId)
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
          .uri("/mapping/contact-person/person/dps-contact-id/{contactId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person/dps-contact-id/{contactId}", dpsContactId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsContactId)
          .jsonPath("nomisId").isEqualTo(123456)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @DisplayName("GET /mapping/contact-person/person/migration-id/{migrationId}")
  @Nested
  inner class GetPersonMappingsByMigrationId {

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/contact-person/person/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/contact-person/person/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/contact-person/person/migration-id/2022-01-01T00:00:00")
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
          personMappingRepository.save(
            PersonMapping(
              dpsId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
              nomisId = it,
              label = "2023-01-01T12:45:12",
              mappingType = ContactPersonMappingType.MIGRATED,
            ),
          )
        }

        personMappingRepository.save(
          PersonMapping(
            dpsId = "edcd118c-91ba-42ea-b5c4-404b453ad58b",
            nomisId = 54321L,
            label = "2022-01-01T12:43:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )

        webTestClient.get().uri("/mapping/contact-person/person/migration-id/2023-01-01T12:45:12")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(4)
          .jsonPath("$.content..nomisId").value(
            Matchers.contains(
              1,
              2,
              3,
              4,
            ),
          )
          .jsonPath("$.content[0].whenCreated").isNotEmpty
      }

      @Test
      fun `200 response even when no mappings are found`() {
        webTestClient.get().uri("/mapping/contact-person/person/migration-id/2044-01-01")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(0)
          .jsonPath("content").isEmpty
      }

      @Test
      fun `can request a different page size`() = runTest {
        (1L..6L).forEach {
          personMappingRepository.save(
            PersonMapping(
              dpsId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
              nomisId = it,
              label = "2023-01-01T12:45:12",
              mappingType = ContactPersonMappingType.MIGRATED,
            ),
          )
        }
        webTestClient.get().uri {
          it.path("/mapping/contact-person/person/migration-id/2023-01-01T12:45:12")
            .queryParam("size", "2")
            .queryParam("sort", "nomisId,asc")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
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

  @DisplayName("DELETE /mapping/contact-person/prisoner-restriction")
  @Nested
  inner class DeleteAllPrisonerRestrictionMappings {
    @BeforeEach
    fun setUp() = runTest {
      prisonerRestrictionMappingRepository.save(
        PrisonerRestrictionMapping(
          dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
          nomisId = 12345L,
          offenderNo = "A1234BC",
          label = null,
          mappingType = ContactPersonMappingType.DPS_CREATED,
          whenCreated = LocalDateTime.now(),
        ),
      )
      prisonerRestrictionMappingRepository.save(
        PrisonerRestrictionMapping(
          dpsId = "d6b03ded-5bb4-5bb8-9872-52f0d0bf61g8",
          nomisId = 67890L,
          offenderNo = "D5678EF",
          label = null,
          mappingType = ContactPersonMappingType.DPS_CREATED,
          whenCreated = LocalDateTime.now(),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/prisoner-restriction")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/prisoner-restriction")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/prisoner-restriction")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns 204 when all prisoner restriction mappings are deleted`() = runTest {
        assertThat(prisonerRestrictionMappingRepository.findAll().count()).isEqualTo(2)

        webTestClient.delete()
          .uri("/mapping/contact-person/prisoner-restriction")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(prisonerRestrictionMappingRepository.findAll().count()).isEqualTo(0)
      }
    }
  }

  @DisplayName("GET /mapping/contact-person/prisoner-restriction/migration-id/{migrationId}")
  @Nested
  inner class GetPrisonerRestrictionMappingsByMigrationId {

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/contact-person/prisoner-restriction/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/contact-person/prisoner-restriction/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/contact-person/prisoner-restriction/migration-id/2022-01-01T00:00:00")
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
          prisonerRestrictionMappingRepository.save(
            PrisonerRestrictionMapping(
              dpsId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
              nomisId = it,
              offenderNo = "A${it}234BC",
              label = "2023-01-01T12:45:12",
              mappingType = ContactPersonMappingType.MIGRATED,
            ),
          )
        }

        prisonerRestrictionMappingRepository.save(
          PrisonerRestrictionMapping(
            dpsId = "edcd118c-91ba-42ea-b5c4-404b453ad58b",
            nomisId = 54321L,
            offenderNo = "Z9876YX",
            label = "2022-01-01T12:43:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )

        webTestClient.get().uri("/mapping/contact-person/prisoner-restriction/migration-id/2023-01-01T12:45:12")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(4)
          .jsonPath("$.content..nomisId").value(
            Matchers.contains(
              1,
              2,
              3,
              4,
            ),
          )
          .jsonPath("$.content[0].whenCreated").isNotEmpty
      }

      @Test
      fun `200 response even when no mappings are found`() {
        webTestClient.get().uri("/mapping/contact-person/prisoner-restriction/migration-id/2044-01-01")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(0)
          .jsonPath("content").isEmpty
      }

      @Test
      fun `can request a different page size`() = runTest {
        (1L..6L).forEach {
          prisonerRestrictionMappingRepository.save(
            PrisonerRestrictionMapping(
              dpsId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
              nomisId = it,
              offenderNo = "A${it}234BC",
              label = "2023-01-01T12:45:12",
              mappingType = ContactPersonMappingType.MIGRATED,
            ),
          )
        }
        webTestClient.get().uri {
          it.path("/mapping/contact-person/prisoner-restriction/migration-id/2023-01-01T12:45:12")
            .queryParam("size", "2")
            .queryParam("sort", "nomisId,asc")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
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

  @DisplayName("POST /mapping/contact-person/replace/prisoner-restrictions/{offenderNo}")
  @Nested
  inner class ReplacePrisonerRestrictionMappings {

    @Nested
    inner class Security {
      val mappings = PrisonerRestrictionMappingsDto(
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
        mappings = emptyList(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner-restrictions/A1234BC")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner-restrictions/A1234BC")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner-restrictions/A1234BC")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      private val oldDpsId1 = "98765"
      private val oldDpsId2 = "12345"
      private val newDpsId1 = "32442442"
      private val newDpsId2 = "28842482"
      private val offenderNo = "A1234BC"

      val newMappings = PrisonerRestrictionMappingsDto(
        mappingType = ContactPersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.now(),
        mappings = listOf(
          ContactPersonSimpleMappingIdDto(dpsId = newDpsId1, nomisId = 111),
          ContactPersonSimpleMappingIdDto(dpsId = newDpsId2, nomisId = 222),
        ),
      )

      @BeforeEach
      fun setUp() = runTest {
        prisonerRestrictionMappingRepository.save(
          PrisonerRestrictionMapping(
            dpsId = oldDpsId1,
            nomisId = 123,
            offenderNo = offenderNo,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        prisonerRestrictionMappingRepository.save(
          PrisonerRestrictionMapping(
            dpsId = oldDpsId2,
            nomisId = 456,
            offenderNo = offenderNo,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        // A mapping for a different offender that should not be deleted
        prisonerRestrictionMappingRepository.save(
          PrisonerRestrictionMapping(
            dpsId = "different-offender",
            nomisId = 789,
            offenderNo = "B5678CD",
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
      }

      @Test
      fun `returns 200 when mappings are replaced`() {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner-restrictions/$offenderNo")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(newMappings))
          .exchange()
          .expectStatus().isOk
      }

      @Test
      fun `will delete existing mappings for the offender`() = runTest {
        // Verify initial state
        assertThat(prisonerRestrictionMappingRepository.findAllByOffenderNo(offenderNo).size).isEqualTo(2)

        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner-restrictions/$offenderNo")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(newMappings))
          .exchange()
          .expectStatus().isOk

        // Verify old mappings are deleted
        assertThat(prisonerRestrictionMappingRepository.findOneByDpsId(oldDpsId1)).isNull()
        assertThat(prisonerRestrictionMappingRepository.findOneByDpsId(oldDpsId2)).isNull()

        // Verify mappings for other offenders are not affected
        assertThat(prisonerRestrictionMappingRepository.findOneByDpsId("different-offender")).isNotNull()
      }

      @Test
      fun `will create new mappings for the offender`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner-restrictions/$offenderNo")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(newMappings))
          .exchange()
          .expectStatus().isOk

        // Verify new mappings are created
        val newMapping1 = prisonerRestrictionMappingRepository.findOneByDpsId(newDpsId1)
        assertThat(newMapping1).isNotNull
        assertThat(newMapping1!!.nomisId).isEqualTo(111)
        assertThat(newMapping1.offenderNo).isEqualTo(offenderNo)
        assertThat(newMapping1.mappingType).isEqualTo(newMappings.mappingType)
        assertThat(newMapping1.whenCreated).isCloseTo(newMappings.whenCreated, within(10, ChronoUnit.SECONDS))

        val newMapping2 = prisonerRestrictionMappingRepository.findOneByDpsId(newDpsId2)
        assertThat(newMapping2).isNotNull
        assertThat(newMapping2!!.nomisId).isEqualTo(222)
        assertThat(newMapping2.offenderNo).isEqualTo(offenderNo)
        assertThat(newMapping2.mappingType).isEqualTo(newMappings.mappingType)
        assertThat(newMapping2.whenCreated).isCloseTo(newMappings.whenCreated, within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @DisplayName("POST /mapping/contact-person/replace/prisoner-restrictions/{retainedOffenderNo}/replaces/{removedOffenderNo}")
  @Nested
  inner class ReplacePrisonerRestrictionAfterMergeMappings {

    @Nested
    inner class Security {
      val mappings = PrisonerRestrictionMappingsDto(
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
        mappings = emptyList(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner-restrictions/A1234KT/replaces/A4321KT")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner-restrictions/A1234KT/replaces/A4321KT")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner-restrictions/A1234KT/replaces/A4321KT")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      private val oldDpsId1 = "98765"
      private val oldDpsId2 = "12345"
      private val oldDpsId3 = "762626"
      private val newDpsId1 = "32442442"
      private val newDpsId2 = "28842482"
      private val retainedOffenderNo = "A1234KT"
      private val removedOffenderNo = "A4321KT"

      val newMappings = PrisonerRestrictionMappingsDto(
        mappingType = ContactPersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.now(),
        mappings = listOf(
          ContactPersonSimpleMappingIdDto(dpsId = newDpsId1, nomisId = 111),
          ContactPersonSimpleMappingIdDto(dpsId = newDpsId2, nomisId = 222),
        ),
      )

      @BeforeEach
      fun setUp() = runTest {
        prisonerRestrictionMappingRepository.save(
          PrisonerRestrictionMapping(
            dpsId = oldDpsId1,
            nomisId = 123,
            offenderNo = retainedOffenderNo,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        prisonerRestrictionMappingRepository.save(
          PrisonerRestrictionMapping(
            dpsId = oldDpsId2,
            nomisId = 456,
            offenderNo = retainedOffenderNo,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        prisonerRestrictionMappingRepository.save(
          PrisonerRestrictionMapping(
            dpsId = oldDpsId3,
            nomisId = 73737,
            offenderNo = removedOffenderNo,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
        // A mapping for a different offender that should not be deleted
        prisonerRestrictionMappingRepository.save(
          PrisonerRestrictionMapping(
            dpsId = "different-offender",
            nomisId = 789,
            offenderNo = "B5678CD",
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
          ),
        )
      }

      @Test
      fun `returns 200 when mappings are replaced`() {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner-restrictions/$retainedOffenderNo/replaces/$removedOffenderNo")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(newMappings))
          .exchange()
          .expectStatus().isOk
      }

      @Test
      fun `will delete existing mappings for the offender`() = runTest {
        // Verify initial state
        assertThat(prisonerRestrictionMappingRepository.findAllByOffenderNo(retainedOffenderNo).size).isEqualTo(2)
        assertThat(prisonerRestrictionMappingRepository.findAllByOffenderNo(removedOffenderNo).size).isEqualTo(1)

        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner-restrictions/$retainedOffenderNo/replaces/$removedOffenderNo")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(newMappings))
          .exchange()
          .expectStatus().isOk

        // Verify old mappings are deleted
        assertThat(prisonerRestrictionMappingRepository.findOneByDpsId(oldDpsId1)).isNull()
        assertThat(prisonerRestrictionMappingRepository.findOneByDpsId(oldDpsId2)).isNull()
        assertThat(prisonerRestrictionMappingRepository.findAllByOffenderNo(removedOffenderNo).size).isEqualTo(0)
        assertThat(prisonerRestrictionMappingRepository.findOneByDpsId(oldDpsId3)).isNull()

        // Verify mappings for other offenders are not affected
        assertThat(prisonerRestrictionMappingRepository.findOneByDpsId("different-offender")).isNotNull()
      }

      @Test
      fun `will create new mappings for the offender`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/replace/prisoner-restrictions/$retainedOffenderNo/replaces/$removedOffenderNo")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(newMappings))
          .exchange()
          .expectStatus().isOk

        // Verify new mappings are created
        val newMapping1 = prisonerRestrictionMappingRepository.findOneByDpsId(newDpsId1)
        assertThat(newMapping1).isNotNull
        assertThat(newMapping1!!.nomisId).isEqualTo(111)
        assertThat(newMapping1.offenderNo).isEqualTo(retainedOffenderNo)
        assertThat(newMapping1.mappingType).isEqualTo(newMappings.mappingType)
        assertThat(newMapping1.whenCreated).isCloseTo(newMappings.whenCreated, within(10, ChronoUnit.SECONDS))

        val newMapping2 = prisonerRestrictionMappingRepository.findOneByDpsId(newDpsId2)
        assertThat(newMapping2).isNotNull
        assertThat(newMapping2!!.nomisId).isEqualTo(222)
        assertThat(newMapping2.offenderNo).isEqualTo(retainedOffenderNo)
        assertThat(newMapping2.mappingType).isEqualTo(newMappings.mappingType)
        assertThat(newMapping2.whenCreated).isCloseTo(newMappings.whenCreated, within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @DisplayName("GET /mapping/contact-person/person/")
  @Nested
  inner class GetAllPersonMappings {

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/contact-person/person")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/contact-person/person")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/contact-person/person")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `can retrieve all mappings`() = runTest {
        (1L..4L).forEach {
          personMappingRepository.save(
            PersonMapping(
              dpsId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
              nomisId = it,
              label = "2023-01-01T12:45:12",
              mappingType = ContactPersonMappingType.MIGRATED,
            ),
          )
        }

        personMappingRepository.save(
          PersonMapping(
            dpsId = "edcd118c-91ba-42ea-b5c4-404b453ad58b",
            nomisId = 54321L,
            label = "2022-01-01T12:43:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )

        webTestClient.get().uri("/mapping/contact-person/person")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(5)
          .jsonPath("$.content..nomisId").value(
            Matchers.contains(
              1,
              2,
              3,
              4,
              54321,
            ),
          )
          .jsonPath("$.content[0].whenCreated").isNotEmpty
      }

      @Test
      fun `can request a different page size`() = runTest {
        (1L..6L).forEach {
          personMappingRepository.save(
            PersonMapping(
              dpsId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
              nomisId = it,
              label = "2023-01-01T12:45:12",
              mappingType = ContactPersonMappingType.MIGRATED,
            ),
          )
        }
        webTestClient.get().uri {
          it.path("/mapping/contact-person/person")
            .queryParam("size", "2")
            .queryParam("sort", "nomisId,asc")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
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
  @DisplayName("DELETE /mapping/contact-person")
  inner class DeleteAllMappings {
    @BeforeEach
    fun setUp() {
      val mappings = ContactPersonMappingsDto(
        personMapping = ContactPersonSimpleMappingIdDto(
          dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
          nomisId = 12345L,
        ),
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
        personContactMapping = listOf(
          ContactPersonSimpleMappingIdDto(
            dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
            nomisId = 12345L,
          ),
        ),
        personContactRestrictionMapping = listOf(
          ContactPersonSimpleMappingIdDto(
            dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
            nomisId = 12345L,
          ),
        ),
        personEmailMapping = listOf(
          ContactPersonSimpleMappingIdDto(
            dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
            nomisId = 12345L,
          ),
        ),
        personRestrictionMapping = listOf(
          ContactPersonSimpleMappingIdDto(
            dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
            nomisId = 12345L,
          ),
        ),
        personPhoneMapping = listOf(
          ContactPersonPhoneMappingIdDto(
            dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
            dpsPhoneType = DpsPersonPhoneType.PERSON,
            nomisId = 12345L,
          ),
        ),
        personAddressMapping = listOf(
          ContactPersonSimpleMappingIdDto(
            dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
            nomisId = 12345L,
          ),
        ),
        personEmploymentMapping = listOf(
          ContactPersonSequenceMappingIdDto(
            dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
            nomisPersonId = 12345L,
            nomisSequenceNumber = 1,
          ),
        ),
        personIdentifierMapping = listOf(
          ContactPersonSequenceMappingIdDto(
            dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
            nomisPersonId = 12345L,
            nomisSequenceNumber = 1,
          ),
        ),
      )
      webTestClient.post()
        .uri("/mapping/contact-person/migrate")
        .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
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
          .uri("/mapping/contact-person")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns 204 when all mappings are deleted`() = runTest {
        assertThat(personContactRestrictionMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(personContactMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(personRestrictionMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(personIdentifierMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(personEmploymentMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(personEmailMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(personPhoneMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(personAddressMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(personMappingRepository.findAll().count()).isEqualTo(1)

        webTestClient.delete()
          .uri("/mapping/contact-person")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(personContactRestrictionMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(personContactMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(personRestrictionMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(personIdentifierMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(personEmploymentMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(personEmailMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(personPhoneMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(personAddressMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(personMappingRepository.findAll().count()).isEqualTo(0)
      }
    }
  }

  @Nested
  @DisplayName("POST mapping/contact-person/person")
  inner class CreatePersonMapping {

    @Nested
    inner class Security {
      val mapping = PersonMappingDto(
        dpsId = UUID.randomUUID().toString(),
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/person")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/person")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/person")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingPersonMapping: PersonMapping

      val mapping = PersonMappingDto(
        dpsId = UUID.randomUUID().toString(),
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingPersonMapping = personMappingRepository.save(
          PersonMapping(
            dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
            nomisId = 12345L,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )
        personIdentifierMappingRepository.save(
          PersonIdentifierMapping(
            dpsId = "18e89dec-6ace-4706-9283-8e11e9ebe886",
            nomisPersonId = 54321,
            nomisSequenceNumber = 1,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same person to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/contact-person/person")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/contact-person/person")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
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
            .containsEntry("nomisId", existingPersonMapping.nomisId.toInt())
            .containsEntry("dpsId", existingPersonMapping.dpsId)
            .containsEntry("mappingType", existingPersonMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mapping.nomisId.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = PersonMappingDto(
        dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/person")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the person mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/person")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val personMapping =
          personMappingRepository.findOneByNomisId(mapping.nomisId)!!

        assertThat(personMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(personMapping.nomisId).isEqualTo(mapping.nomisId)
        assertThat(personMapping.label).isNull()
        assertThat(personMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(personMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/contact/nomis-contact-id/{contactId}")
  inner class GetPersonContactByNomisId {
    private val nomisPersonContactId = 12345L
    private lateinit var personContactMapping: PersonContactMapping

    @BeforeEach
    fun setUp() = runTest {
      personContactMapping = personContactMappingRepository.save(
        PersonContactMapping(
          dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisId = nomisPersonContactId,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact/nomis-contact-id/{contactId}", nomisPersonContactId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact/nomis-contact-id/{contactId}", nomisPersonContactId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact/nomis-contact-id/{contactId}", nomisPersonContactId)
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
          .uri("/mapping/contact-person/contact/nomis-contact-id/{contactId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact/nomis-contact-id/{contactId}", nomisPersonContactId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo("edcd118c-41ba-42ea-b5c4-404b453ad58b")
          .jsonPath("nomisId").isEqualTo(nomisPersonContactId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/contact-person/contact/nomis-contact-id/{contactId}")
  inner class DeletePersonContactByNomisId {
    private val nomisPersonContactId = 12345L
    private lateinit var personContactMapping: PersonContactMapping

    @BeforeEach
    fun setUp() = runTest {
      personContactMapping = personContactMappingRepository.save(
        PersonContactMapping(
          dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisId = nomisPersonContactId,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/contact/nomis-contact-id/{contactId}", nomisPersonContactId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/contact/nomis-contact-id/{contactId}", nomisPersonContactId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/contact/nomis-contact-id/{contactId}", nomisPersonContactId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping does not exist`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/contact/nomis-contact-id/{contactId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping data`() = runTest {
        assertThat(personContactMappingRepository.findOneByNomisId(nomisPersonContactId)).isNotNull
        webTestClient.delete()
          .uri("/mapping/contact-person/contact/nomis-contact-id/{contactId}", nomisPersonContactId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
        assertThat(personContactMappingRepository.findOneByNomisId(nomisPersonContactId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/contact/dps-prisoner-contact-id/{prisonerContactId}")
  inner class GetPersonContactByDpsId {
    private val dpsPrisonerContactId = "1234567"
    private lateinit var personContactMapping: PersonContactMapping

    @BeforeEach
    fun setUp() = runTest {
      personContactMapping = personContactMappingRepository.save(
        PersonContactMapping(
          dpsId = dpsPrisonerContactId,
          nomisId = 123456,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact/dps-prisoner-contact-id/{prisonerContactId}", dpsPrisonerContactId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact/dps-prisoner-contact-id/{prisonerContactId}", dpsPrisonerContactId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact/dps-prisoner-contact-id/{prisonerContactId}", dpsPrisonerContactId)
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
          .uri("/mapping/contact-person/contact/dps-prisoner-contact-id/{prisonerContactId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact/dps-prisoner-contact-id/{prisonerContactId}", dpsPrisonerContactId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsPrisonerContactId)
          .jsonPath("nomisId").isEqualTo(123456)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("POST mapping/contact-person/contact")
  inner class CreatePersonContactMapping {

    @Nested
    inner class Security {
      val mapping = PersonContactMappingDto(
        dpsId = UUID.randomUUID().toString(),
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/contact")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/contact")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/contact")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingPersonContactMapping: PersonContactMapping

      val mapping = PersonContactMappingDto(
        dpsId = UUID.randomUUID().toString(),
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingPersonContactMapping = personContactMappingRepository.save(
          PersonContactMapping(
            dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
            nomisId = 12345L,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same person contact to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/contact-person/contact")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/contact-person/contact")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
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
            .containsEntry("nomisId", existingPersonContactMapping.nomisId.toInt())
            .containsEntry("dpsId", existingPersonContactMapping.dpsId)
            .containsEntry("mappingType", existingPersonContactMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mapping.nomisId.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = PersonContactMappingDto(
        dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/contact")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the person contact mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/contact")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val personContactMapping =
          personContactMappingRepository.findOneByNomisId(mapping.nomisId)!!

        assertThat(personContactMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(personContactMapping.nomisId).isEqualTo(mapping.nomisId)
        assertThat(personContactMapping.label).isNull()
        assertThat(personContactMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(personContactMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/address/nomis-address-id/{nomisAddressId}")
  inner class GetPersonAddressByNomisId {
    private val nomisPersonAddressId = 12345L
    private val dpsContactAddressId = "54321"
    private lateinit var personAddressMapping: PersonAddressMapping

    @BeforeEach
    fun setUp() = runTest {
      personAddressMapping = personAddressMappingRepository.save(
        PersonAddressMapping(
          dpsId = dpsContactAddressId,
          nomisId = nomisPersonAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/address/nomis-address-id/{nomisPersonAddressId}", nomisPersonAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/address/nomis-address-id/{nomisPersonAddressId}", nomisPersonAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/address/nomis-address-id/{nomisPersonAddressId}", nomisPersonAddressId)
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
          .uri("/mapping/contact-person/address/nomis-address-id/{nomisPersonAddressId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/address/nomis-address-id/{nomisPersonAddressId}", nomisPersonAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsContactAddressId)
          .jsonPath("nomisId").isEqualTo(nomisPersonAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/contact-person/address/nomis-address-id/{nomisAddressId}")
  inner class DeletePersonAddressByNomisId {
    private val nomisPersonAddressId = 12345L
    private val dpsContactAddressId = "54321"
    private lateinit var personAddressMapping: PersonAddressMapping

    @BeforeEach
    fun setUp() = runTest {
      personAddressMapping = personAddressMappingRepository.save(
        PersonAddressMapping(
          dpsId = dpsContactAddressId,
          nomisId = nomisPersonAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/address/nomis-address-id/{nomisPersonAddressId}", nomisPersonAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/address/nomis-address-id/{nomisPersonAddressId}", nomisPersonAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/address/nomis-address-id/{nomisPersonAddressId}", nomisPersonAddressId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping does not exist`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/address/nomis-address-id/{nomisPersonAddressId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping data`() = runTest {
        assertThat(personAddressMappingRepository.findOneByNomisId(nomisPersonAddressId)).isNotNull
        webTestClient.delete()
          .uri("/mapping/contact-person/address/nomis-address-id/{nomisPersonAddressId}", nomisPersonAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
        assertThat(personAddressMappingRepository.findOneByNomisId(nomisPersonAddressId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/address/dps-contact-address-id/{dpsContactAddressId}")
  inner class GetPersonAddressByDpsId {
    private val nomisPersonAddressId = 7654321L
    private val dpsContactAddressId = "1234567"
    private lateinit var personAddressMapping: PersonAddressMapping

    @BeforeEach
    fun setUp() = runTest {
      personAddressMapping = personAddressMappingRepository.save(
        PersonAddressMapping(
          dpsId = dpsContactAddressId,
          nomisId = nomisPersonAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/address/dps-contact-address-id/{dpsContactAddressId}", dpsContactAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/address/dps-contact-address-id/{dpsContactAddressId}", dpsContactAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/address/dps-contact-address-id/{dpsContactAddressId}", dpsContactAddressId)
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
          .uri("/mapping/contact-person/address/dps-contact-address-id/{dpsContactAddressId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/address/dps-contact-address-id/{dpsContactAddressId}", dpsContactAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsContactAddressId)
          .jsonPath("nomisId").isEqualTo(nomisPersonAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("POST mapping/contact-person/address")
  inner class CreatePersonAddressMapping {

    @Nested
    inner class Security {
      val mapping = PersonAddressMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/address")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/address")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/address")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingPersonAddressMapping: PersonAddressMapping

      val mapping = PersonAddressMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingPersonAddressMapping = personAddressMappingRepository.save(
          PersonAddressMapping(
            dpsId = "765432",
            nomisId = 12345L,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same person address to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/contact-person/address")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/contact-person/address")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
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
            .containsEntry("nomisId", existingPersonAddressMapping.nomisId.toInt())
            .containsEntry("dpsId", existingPersonAddressMapping.dpsId)
            .containsEntry("mappingType", existingPersonAddressMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mapping.nomisId.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = PersonAddressMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/address")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the person address mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/address")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val personAddressMapping =
          personAddressMappingRepository.findOneByNomisId(mapping.nomisId)!!

        assertThat(personAddressMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(personAddressMapping.nomisId).isEqualTo(mapping.nomisId)
        assertThat(personAddressMapping.label).isNull()
        assertThat(personAddressMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(personAddressMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/email/nomis-internet-address-id/{nomisInternetAddressId}")
  inner class GetPersonEmailByNomisId {
    private val nomisInternetAddressId = 12345L
    private val dpsContactEmailId = "54321"
    private lateinit var personEmailMapping: PersonEmailMapping

    @BeforeEach
    fun setUp() = runTest {
      personEmailMapping = personEmailMappingRepository.save(
        PersonEmailMapping(
          dpsId = dpsContactEmailId,
          nomisId = nomisInternetAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/email/nomis-internet-address-id/{nomisInternetAddressId}", nomisInternetAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/email/nomis-internet-address-id/{nomisInternetAddressId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/email/nomis-internet-address-id/{nomisInternetAddressId}", nomisInternetAddressId)
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
          .uri("/mapping/contact-person/email/nomis-internet-address-id/{nomisInternetAddressId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/email/nomis-internet-address-id/{nomisInternetAddressId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsContactEmailId)
          .jsonPath("nomisId").isEqualTo(nomisInternetAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/contact-person/email/nomis-internet-address-id/{nomisInternetAddressId}")
  inner class DeletePersonEmailByNomisId {
    private val nomisInternetAddressId = 12345L
    private val dpsContactEmailId = "54321"
    private lateinit var personEmailMapping: PersonEmailMapping

    @BeforeEach
    fun setUp() = runTest {
      personEmailMapping = personEmailMappingRepository.save(
        PersonEmailMapping(
          dpsId = dpsContactEmailId,
          nomisId = nomisInternetAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/email/nomis-internet-address-id/{nomisInternetAddressId}", nomisInternetAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/email/nomis-internet-address-id/{nomisInternetAddressId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/email/nomis-internet-address-id/{nomisInternetAddressId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping does not exist`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/email/nomis-internet-address-id/{nomisInternetAddressId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping data`() = runTest {
        assertThat(personEmailMappingRepository.findOneByNomisId(nomisInternetAddressId)).isNotNull
        webTestClient.delete()
          .uri("/mapping/contact-person/email/nomis-internet-address-id/{nomisInternetAddressId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
        assertThat(personEmailMappingRepository.findOneByNomisId(nomisInternetAddressId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/email/dps-contact-email-id/{dpsContactEmailId}")
  inner class GetPersonEmailByDpsId {
    private val nomisInternetAddressId = 7654321L
    private val dpsContactEmailId = "1234567"
    private lateinit var personEmailMapping: PersonEmailMapping

    @BeforeEach
    fun setUp() = runTest {
      personEmailMapping = personEmailMappingRepository.save(
        PersonEmailMapping(
          dpsId = dpsContactEmailId,
          nomisId = nomisInternetAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/email/dps-contact-email-id/{dpsContactEmailId}", dpsContactEmailId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/email/dps-contact-email-id/{dpsContactEmailId}", dpsContactEmailId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/email/dps-contact-email-id/{dpsContactEmailId}", dpsContactEmailId)
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
          .uri("/mapping/contact-person/email/dps-contact-email-id/{dpsContactEmailId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/email/dps-contact-email-id/{dpsContactEmailId}", dpsContactEmailId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsContactEmailId)
          .jsonPath("nomisId").isEqualTo(nomisInternetAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("POST mapping/contact-person/email")
  inner class CreatePersonEmailMapping {

    @Nested
    inner class Security {
      val mapping = PersonEmailMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/email")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/email")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/email")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingPersonEmailMapping: PersonEmailMapping

      val mapping = PersonEmailMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingPersonEmailMapping = personEmailMappingRepository.save(
          PersonEmailMapping(
            dpsId = "765432",
            nomisId = 12345L,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same person email to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/contact-person/email")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/contact-person/email")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
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
            .containsEntry("nomisId", existingPersonEmailMapping.nomisId.toInt())
            .containsEntry("dpsId", existingPersonEmailMapping.dpsId)
            .containsEntry("mappingType", existingPersonEmailMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mapping.nomisId.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = PersonEmailMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/email")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the person email mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/email")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val personEmailMapping =
          personEmailMappingRepository.findOneByNomisId(mapping.nomisId)!!

        assertThat(personEmailMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(personEmailMapping.nomisId).isEqualTo(mapping.nomisId)
        assertThat(personEmailMapping.label).isNull()
        assertThat(personEmailMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(personEmailMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/phone/nomis-phone-id/{nomisPhoneId}")
  inner class GetPersonPhoneByNomisId {
    private val nomisPhoneId = 12345L
    private val dpsContactPhoneId = "54321"
    private lateinit var personPhoneMapping: PersonPhoneMapping

    @BeforeEach
    fun setUp() = runTest {
      personPhoneMapping = personPhoneMappingRepository.save(
        PersonPhoneMapping(
          dpsId = dpsContactPhoneId,
          nomisId = nomisPhoneId,
          label = "2023-01-01T12:45:12",
          dpsPhoneType = DpsPersonPhoneType.PERSON,
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
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
          .uri("/mapping/contact-person/phone/nomis-phone-id/{nomisPhoneId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/phone/nomis-phone-id/{nomisPhoneId}", nomisPhoneId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsContactPhoneId)
          .jsonPath("nomisId").isEqualTo(nomisPhoneId)
          .jsonPath("dpsPhoneType").isEqualTo("PERSON")
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/phone/dps-contact-phone-id/{dpsContactPhoneId}")
  inner class GetPersonPhoneByDpsId {
    private val nomisInternetAddressId = 7654321L
    private val dpsContactPhoneId = "1234567"
    private lateinit var personPhoneMapping: PersonPhoneMapping

    @BeforeEach
    fun setUp() = runTest {
      personPhoneMapping = personPhoneMappingRepository.save(
        PersonPhoneMapping(
          dpsId = dpsContactPhoneId,
          nomisId = nomisInternetAddressId,
          dpsPhoneType = DpsPersonPhoneType.PERSON,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/phone/dps-contact-phone-id/{dpsContactPhoneId}", dpsContactPhoneId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/phone/dps-contact-phone-id/{dpsContactPhoneId}", dpsContactPhoneId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/phone/dps-contact-phone-id/{dpsContactPhoneId}", dpsContactPhoneId)
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
          .uri("/mapping/contact-person/phone/dps-contact-phone-id/{dpsContactPhoneId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 when mapping not found even when address phone with same ID exists`() {
        webTestClient.get()
          .uri("/mapping/contact-person/phone/dps-contact-address-phone-id/{dpsContactAddressPhoneId}", dpsContactPhoneId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/phone/dps-contact-phone-id/{dpsContactPhoneId}", dpsContactPhoneId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsContactPhoneId)
          .jsonPath("nomisId").isEqualTo(nomisInternetAddressId)
          .jsonPath("dpsPhoneType").isEqualTo("PERSON")
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/phone/dps-contact-address-phone-id/{dpsContactAddressPhoneId}")
  inner class GetPersonAddressPhoneByDpsId {
    private val nomisInternetAddressId = 7654321L
    private val dpsContactPhoneId = "1234567"
    private lateinit var personPhoneMapping: PersonPhoneMapping

    @BeforeEach
    fun setUp() = runTest {
      personPhoneMapping = personPhoneMappingRepository.save(
        PersonPhoneMapping(
          dpsId = dpsContactPhoneId,
          nomisId = nomisInternetAddressId,
          dpsPhoneType = DpsPersonPhoneType.ADDRESS,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/phone/dps-contact-address-phone-id/{dpsContactAddressPhoneId}", dpsContactPhoneId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/phone/dps-contact-address-phone-id/{dpsContactAddressPhoneId}", dpsContactPhoneId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/phone/dps-contact-address-phone-id/{dpsContactAddressPhoneId}", dpsContactPhoneId)
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
          .uri("/mapping/contact-person/phone/dps-contact-address-phone-id/{dpsContactAddressPhoneId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 when mapping not found even though global phone has same id`() {
        webTestClient.get()
          .uri("/mapping/contact-person/phone/dps-contact-phone-id/{dpsContactPhoneId}", dpsContactPhoneId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/phone/dps-contact-address-phone-id/{dpsContactAddressPhoneId}", dpsContactPhoneId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsContactPhoneId)
          .jsonPath("nomisId").isEqualTo(nomisInternetAddressId)
          .jsonPath("dpsPhoneType").isEqualTo("ADDRESS")
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("POST mapping/contact-person/phone")
  inner class CreatePersonPhoneMapping {

    @Nested
    inner class Security {
      val mapping = PersonPhoneMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        dpsPhoneType = DpsPersonPhoneType.PERSON,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/phone")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/phone")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/phone")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingPersonPhoneMapping: PersonPhoneMapping

      val mapping = PersonPhoneMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        dpsPhoneType = DpsPersonPhoneType.PERSON,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingPersonPhoneMapping = personPhoneMappingRepository.save(
          PersonPhoneMapping(
            dpsId = "765432",
            nomisId = 12345L,
            dpsPhoneType = DpsPersonPhoneType.PERSON,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same person phone to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/contact-person/phone")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/contact-person/phone")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
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
            .containsEntry("nomisId", existingPersonPhoneMapping.nomisId.toInt())
            .containsEntry("dpsId", existingPersonPhoneMapping.dpsId)
            .containsEntry("mappingType", existingPersonPhoneMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mapping.nomisId.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = PersonPhoneMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        dpsPhoneType = DpsPersonPhoneType.PERSON,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        // also save an address phone mapping that coincidentally has the same dpsId
        // and show this is not considered a duplicate
        personPhoneMappingRepository.save(
          PersonPhoneMapping(
            dpsId = mapping.dpsId,
            nomisId = 654345,
            dpsPhoneType = DpsPersonPhoneType.ADDRESS,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/phone")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the person phone mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/phone")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val personPhoneMapping =
          personPhoneMappingRepository.findOneByNomisId(mapping.nomisId)!!

        assertThat(personPhoneMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(personPhoneMapping.nomisId).isEqualTo(mapping.nomisId)
        assertThat(personPhoneMapping.dpsPhoneType).isEqualTo(mapping.dpsPhoneType)
        assertThat(personPhoneMapping.label).isNull()
        assertThat(personPhoneMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(personPhoneMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}")
  inner class GetPersonIdentifierByNomisIds {
    private val nomisPersonId = 12345L
    private val nomisSequenceNumber = 4L
    private val dpsContactIdentifierId = "54321"
    private lateinit var personIdentifierMapping: PersonIdentifierMapping

    @BeforeEach
    fun setUp() = runTest {
      personIdentifierMapping = personIdentifierMappingRepository.save(
        PersonIdentifierMapping(
          dpsId = dpsContactIdentifierId,
          nomisPersonId = nomisPersonId,
          nomisSequenceNumber = nomisSequenceNumber,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
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
          .uri("/mapping/contact-person/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, 999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound

        webTestClient.get()
          .uri("/mapping/contact-person/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", 999, nomisSequenceNumber)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsContactIdentifierId)
          .jsonPath("nomisPersonId").isEqualTo(nomisPersonId)
          .jsonPath("nomisSequenceNumber").isEqualTo(nomisSequenceNumber)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/contact-person/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}")
  inner class DeletePersonIdentifierByNomisIds {
    private val nomisPersonId = 12345L
    private val nomisSequenceNumber = 4L
    private val dpsContactIdentifierId = "54321"
    private lateinit var personIdentifierMapping: PersonIdentifierMapping

    @BeforeEach
    fun setUp() = runTest {
      personIdentifierMapping = personIdentifierMappingRepository.save(
        PersonIdentifierMapping(
          dpsId = dpsContactIdentifierId,
          nomisPersonId = nomisPersonId,
          nomisSequenceNumber = nomisSequenceNumber,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping does not exist`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, 999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.delete()
          .uri("/mapping/contact-person/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", 999, nomisSequenceNumber)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping data`() = runTest {
        assertThat(personIdentifierMappingRepository.findOneByNomisPersonIdAndNomisSequenceNumber(nomisPersonId, nomisSequenceNumber)).isNotNull
        webTestClient.delete()
          .uri("/mapping/contact-person/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
        assertThat(personIdentifierMappingRepository.findOneByNomisPersonIdAndNomisSequenceNumber(nomisPersonId, nomisSequenceNumber)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/employment/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}")
  inner class GetPersonEmploymentByNomisIds {
    private val nomisPersonId = 12345L
    private val nomisSequenceNumber = 4L
    private val dpsContactEmploymentId = "54321"
    private lateinit var personEmploymentMapping: PersonEmploymentMapping

    @BeforeEach
    fun setUp() = runTest {
      personEmploymentMapping = personEmploymentMappingRepository.save(
        PersonEmploymentMapping(
          dpsId = dpsContactEmploymentId,
          nomisPersonId = nomisPersonId,
          nomisSequenceNumber = nomisSequenceNumber,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/employment/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/employment/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/employment/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
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
          .uri("/mapping/contact-person/employment/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, 999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound

        webTestClient.get()
          .uri("/mapping/contact-person/employment/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", 999, nomisSequenceNumber)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/employment/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsContactEmploymentId)
          .jsonPath("nomisPersonId").isEqualTo(nomisPersonId)
          .jsonPath("nomisSequenceNumber").isEqualTo(nomisSequenceNumber)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/employment/dps-contact-employment-id/{dpsContactEmploymentId}")
  inner class GetPersonEmploymentByDpsId {
    private val nomisPersonId = 7654321L
    private val dpsContactEmploymentId = "1234567"
    private val nomisSequenceNumber = 4L
    private lateinit var personEmploymentMapping: PersonEmploymentMapping

    @BeforeEach
    fun setUp() = runTest {
      personEmploymentMapping = personEmploymentMappingRepository.save(
        PersonEmploymentMapping(
          dpsId = dpsContactEmploymentId,
          nomisPersonId = nomisPersonId,
          nomisSequenceNumber = nomisSequenceNumber,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/employment/dps-contact-employment-id/{dpsContactEmploymentId}", dpsContactEmploymentId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/employment/dps-contact-employment-id/{dpsContactEmploymentId}", dpsContactEmploymentId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/employment/dps-contact-employment-id/{dpsContactEmploymentId}", dpsContactEmploymentId)
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
          .uri("/mapping/contact-person/employment/dps-contact-employment-id/{dpsContactEmploymentId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/employment/dps-contact-employment-id/{dpsContactEmploymentId}", dpsContactEmploymentId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsContactEmploymentId)
          .jsonPath("nomisPersonId").isEqualTo(nomisPersonId)
          .jsonPath("nomisSequenceNumber").isEqualTo(nomisSequenceNumber)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("POST mapping/contact-person/employment")
  inner class CreatePersonEmploymentMapping {

    @Nested
    inner class Security {
      val mapping = PersonEmploymentMappingDto(
        dpsId = "54321",
        nomisPersonId = 12345L,
        nomisSequenceNumber = 4L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/employment")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/employment")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/employment")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingPersonEmploymentMapping: PersonEmploymentMapping

      val mapping = PersonEmploymentMappingDto(
        dpsId = "54321",
        nomisPersonId = 12345L,
        nomisSequenceNumber = 4L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingPersonEmploymentMapping = personEmploymentMappingRepository.save(
          PersonEmploymentMapping(
            dpsId = "765432",
            nomisPersonId = 12345L,
            nomisSequenceNumber = 4L,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same person employment to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/contact-person/employment")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/contact-person/employment")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
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
            .containsEntry("nomisPersonId", existingPersonEmploymentMapping.nomisPersonId.toInt())
            .containsEntry("nomisSequenceNumber", existingPersonEmploymentMapping.nomisSequenceNumber.toInt())
            .containsEntry("dpsId", existingPersonEmploymentMapping.dpsId)
            .containsEntry("mappingType", existingPersonEmploymentMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisPersonId", mapping.nomisPersonId.toInt())
            .containsEntry("nomisSequenceNumber", mapping.nomisSequenceNumber.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = PersonEmploymentMappingDto(
        dpsId = "54321",
        nomisPersonId = 12345L,
        nomisSequenceNumber = 4L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/employment")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the person employment mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/employment")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val personEmploymentMapping =
          personEmploymentMappingRepository.findOneByNomisPersonIdAndNomisSequenceNumber(
            nomisPersonId = mapping.nomisPersonId,
            nomisSequenceNumber = mapping.nomisSequenceNumber,
          )!!

        assertThat(personEmploymentMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(personEmploymentMapping.nomisPersonId).isEqualTo(mapping.nomisPersonId)
        assertThat(personEmploymentMapping.nomisSequenceNumber).isEqualTo(mapping.nomisSequenceNumber)
        assertThat(personEmploymentMapping.label).isNull()
        assertThat(personEmploymentMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(personEmploymentMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/contact-person/employment/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}")
  inner class DeletePersonEmploymentByNomisIds {
    private val nomisPersonId = 12345L
    private val nomisSequenceNumber = 4L
    private val dpsContactEmploymentId = "54321"
    private lateinit var personEmploymentMapping: PersonEmploymentMapping

    @BeforeEach
    fun setUp() = runTest {
      personEmploymentMapping = personEmploymentMappingRepository.save(
        PersonEmploymentMapping(
          dpsId = dpsContactEmploymentId,
          nomisPersonId = nomisPersonId,
          nomisSequenceNumber = nomisSequenceNumber,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/employment/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/employment/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/employment/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping does not exist`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/employment/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, 999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.delete()
          .uri("/mapping/contact-person/employment/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", 999, nomisSequenceNumber)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() = runTest {
        assertThat(personEmploymentMappingRepository.findOneByNomisPersonIdAndNomisSequenceNumber(nomisPersonId, nomisSequenceNumber)).isNotNull
        webTestClient.delete()
          .uri("/mapping/contact-person/employment/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}", nomisPersonId, nomisSequenceNumber)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
        assertThat(personEmploymentMappingRepository.findOneByNomisPersonIdAndNomisSequenceNumber(nomisPersonId, nomisSequenceNumber)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/identifier/dps-contact-identifier-id/{dpsContactIdentifierId}")
  inner class GetPersonIdentifierByDpsId {
    private val nomisPersonId = 7654321L
    private val dpsContactIdentifierId = "1234567"
    private val nomisSequenceNumber = 4L
    private lateinit var personIdentifierMapping: PersonIdentifierMapping

    @BeforeEach
    fun setUp() = runTest {
      personIdentifierMapping = personIdentifierMappingRepository.save(
        PersonIdentifierMapping(
          dpsId = dpsContactIdentifierId,
          nomisPersonId = nomisPersonId,
          nomisSequenceNumber = nomisSequenceNumber,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/identifier/dps-contact-identifier-id/{dpsContactIdentifierId}", dpsContactIdentifierId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/identifier/dps-contact-identifier-id/{dpsContactIdentifierId}", dpsContactIdentifierId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/identifier/dps-contact-identifier-id/{dpsContactIdentifierId}", dpsContactIdentifierId)
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
          .uri("/mapping/contact-person/identifier/dps-contact-identifier-id/{dpsContactIdentifierId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/identifier/dps-contact-identifier-id/{dpsContactIdentifierId}", dpsContactIdentifierId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsContactIdentifierId)
          .jsonPath("nomisPersonId").isEqualTo(nomisPersonId)
          .jsonPath("nomisSequenceNumber").isEqualTo(nomisSequenceNumber)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("POST mapping/contact-person/identifier")
  inner class CreatePersonIdentifierMapping {

    @Nested
    inner class Security {
      val mapping = PersonIdentifierMappingDto(
        dpsId = "54321",
        nomisPersonId = 12345L,
        nomisSequenceNumber = 4L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/identifier")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/identifier")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/identifier")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingPersonIdentifierMapping: PersonIdentifierMapping

      val mapping = PersonIdentifierMappingDto(
        dpsId = "54321",
        nomisPersonId = 12345L,
        nomisSequenceNumber = 4L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingPersonIdentifierMapping = personIdentifierMappingRepository.save(
          PersonIdentifierMapping(
            dpsId = "765432",
            nomisPersonId = 12345L,
            nomisSequenceNumber = 4L,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same person identifier to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/contact-person/identifier")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/contact-person/identifier")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
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
            .containsEntry("nomisPersonId", existingPersonIdentifierMapping.nomisPersonId.toInt())
            .containsEntry("nomisSequenceNumber", existingPersonIdentifierMapping.nomisSequenceNumber.toInt())
            .containsEntry("dpsId", existingPersonIdentifierMapping.dpsId)
            .containsEntry("mappingType", existingPersonIdentifierMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisPersonId", mapping.nomisPersonId.toInt())
            .containsEntry("nomisSequenceNumber", mapping.nomisSequenceNumber.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = PersonIdentifierMappingDto(
        dpsId = "54321",
        nomisPersonId = 12345L,
        nomisSequenceNumber = 4L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/identifier")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the person identifier mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/identifier")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val personIdentifierMapping =
          personIdentifierMappingRepository.findOneByNomisPersonIdAndNomisSequenceNumber(
            nomisPersonId = mapping.nomisPersonId,
            nomisSequenceNumber = mapping.nomisSequenceNumber,
          )!!

        assertThat(personIdentifierMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(personIdentifierMapping.nomisPersonId).isEqualTo(mapping.nomisPersonId)
        assertThat(personIdentifierMapping.nomisSequenceNumber).isEqualTo(mapping.nomisSequenceNumber)
        assertThat(personIdentifierMapping.label).isNull()
        assertThat(personIdentifierMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(personIdentifierMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/contact-restriction/nomis-contact-restriction-id/{contactRestrictionId}")
  inner class GetContactRestrictionByNomisId {
    private val nomisContactRestrictionId = 12345L
    private lateinit var personContactRestrictionMapping: PersonContactRestrictionMapping

    @BeforeEach
    fun setUp() = runTest {
      personContactRestrictionMapping = personContactRestrictionMappingRepository.save(
        PersonContactRestrictionMapping(
          dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisId = nomisContactRestrictionId,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact-restriction/nomis-contact-restriction-id/{contactRestrictionId}", nomisContactRestrictionId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact-restriction/nomis-contact-restriction-id/{contactRestrictionId}", nomisContactRestrictionId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact-restriction/nomis-contact-restriction-id/{contactRestrictionId}", nomisContactRestrictionId)
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
          .uri("/mapping/contact-person/contact-restriction/nomis-contact-restriction-id/{contactRestrictionId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact-restriction/nomis-contact-restriction-id/{contactRestrictionId}", nomisContactRestrictionId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo("edcd118c-41ba-42ea-b5c4-404b453ad58b")
          .jsonPath("nomisId").isEqualTo(nomisContactRestrictionId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/contact-person/contact-restriction/nomis-contact-restriction-id/{contactRestrictionId}")
  inner class DeleteContactRestrictionByNomisId {
    private val nomisContactRestrictionId = 12345L
    private lateinit var personContactRestrictionMapping: PersonContactRestrictionMapping

    @BeforeEach
    fun setUp() = runTest {
      personContactRestrictionMapping = personContactRestrictionMappingRepository.save(
        PersonContactRestrictionMapping(
          dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisId = nomisContactRestrictionId,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/contact-restriction/nomis-contact-restriction-id/{contactRestrictionId}", nomisContactRestrictionId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/contact-restriction/nomis-contact-restriction-id/{contactRestrictionId}", nomisContactRestrictionId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/contact-restriction/nomis-contact-restriction-id/{contactRestrictionId}", nomisContactRestrictionId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping does not exist`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/contact-restriction/nomis-contact-restriction-id/{contactRestrictionId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping data`() = runTest {
        assertThat(personContactRestrictionMappingRepository.findOneByNomisId(nomisContactRestrictionId)).isNotNull
        webTestClient.delete()
          .uri("/mapping/contact-person/contact-restriction/nomis-contact-restriction-id/{contactRestrictionId}", nomisContactRestrictionId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
        assertThat(personContactRestrictionMappingRepository.findOneByNomisId(nomisContactRestrictionId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/contact-restriction/dps-prisoner-contact-restriction-id/{prisonerContactRestrictionId}")
  inner class GetContactRestrictionByDpsId {
    private val dpsPrisonerContactRestrictionId = "1234567"
    private lateinit var personContactRestrictionMapping: PersonContactRestrictionMapping

    @BeforeEach
    fun setUp() = runTest {
      personContactRestrictionMapping = personContactRestrictionMappingRepository.save(
        PersonContactRestrictionMapping(
          dpsId = dpsPrisonerContactRestrictionId,
          nomisId = 123456,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact-restriction/dps-prisoner-contact-restriction-id/{prisonerContactRestrictionId}}", dpsPrisonerContactRestrictionId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact-restriction/dps-prisoner-contact-restriction-id/{prisonerContactRestrictionId}", dpsPrisonerContactRestrictionId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact-restriction/dps-prisoner-contact-restriction-id/{prisonerContactRestrictionId}", dpsPrisonerContactRestrictionId)
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
          .uri("/mapping/contact-person/contact-restriction/dps-prisoner-contact-restriction-id/{prisonerContactRestrictionId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/contact-restriction/dps-prisoner-contact-restriction-id/{prisonerContactRestrictionId}", dpsPrisonerContactRestrictionId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsPrisonerContactRestrictionId)
          .jsonPath("nomisId").isEqualTo(123456)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("POST mapping/contact-person/contact-restriction")
  inner class CreateContactRestrictionMapping {

    @Nested
    inner class Security {
      val mapping = PersonContactRestrictionMappingDto(
        dpsId = UUID.randomUUID().toString(),
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/contact-restriction")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/contact-restriction")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/contact-restriction")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingPersonContactRestrictionMapping: PersonContactRestrictionMapping

      val mapping = PersonContactRestrictionMappingDto(
        dpsId = UUID.randomUUID().toString(),
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingPersonContactRestrictionMapping = personContactRestrictionMappingRepository.save(
          PersonContactRestrictionMapping(
            dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
            nomisId = 12345L,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same person contact to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/contact-person/contact-restriction")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/contact-person/contact-restriction")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
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
            .containsEntry("nomisId", existingPersonContactRestrictionMapping.nomisId.toInt())
            .containsEntry("dpsId", existingPersonContactRestrictionMapping.dpsId)
            .containsEntry("mappingType", existingPersonContactRestrictionMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mapping.nomisId.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = PersonContactRestrictionMappingDto(
        dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/contact-restriction")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the person contact mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/contact-restriction")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val personContactRestrictionMapping =
          personContactRestrictionMappingRepository.findOneByNomisId(mapping.nomisId)!!

        assertThat(personContactRestrictionMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(personContactRestrictionMapping.nomisId).isEqualTo(mapping.nomisId)
        assertThat(personContactRestrictionMapping.label).isNull()
        assertThat(personContactRestrictionMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(personContactRestrictionMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/person-restriction/nomis-person-restriction-id/{personRestrictionId}")
  inner class GetPersonRestrictionByNomisId {
    private val nomisPersonRestrictionId = 12345L
    private lateinit var personRestrictionMapping: PersonRestrictionMapping

    @BeforeEach
    fun setUp() = runTest {
      personRestrictionMapping = personRestrictionMappingRepository.save(
        PersonRestrictionMapping(
          dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisId = nomisPersonRestrictionId,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person-restriction/nomis-person-restriction-id/{personRestrictionId}", nomisPersonRestrictionId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person-restriction/nomis-person-restriction-id/{personRestrictionId}", nomisPersonRestrictionId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person-restriction/nomis-person-restriction-id/{personRestrictionId}", nomisPersonRestrictionId)
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
          .uri("/mapping/contact-person/person-restriction/nomis-person-restriction-id/{personRestrictionId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person-restriction/nomis-person-restriction-id/{personRestrictionId}", nomisPersonRestrictionId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo("edcd118c-41ba-42ea-b5c4-404b453ad58b")
          .jsonPath("nomisId").isEqualTo(nomisPersonRestrictionId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/contact-person/person-restriction/nomis-person-restriction-id/{personRestrictionId}")
  inner class DeletePersonRestrictionByNomisId {
    private val nomisPersonRestrictionId = 12345L
    private lateinit var personRestrictionMapping: PersonRestrictionMapping

    @BeforeEach
    fun setUp() = runTest {
      personRestrictionMapping = personRestrictionMappingRepository.save(
        PersonRestrictionMapping(
          dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisId = nomisPersonRestrictionId,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/person-restriction/nomis-person-restriction-id/{personRestrictionId}", nomisPersonRestrictionId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/person-restriction/nomis-person-restriction-id/{personRestrictionId}", nomisPersonRestrictionId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/person-restriction/nomis-person-restriction-id/{personRestrictionId}", nomisPersonRestrictionId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping does not exist`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/person-restriction/nomis-person-restriction-id/{personRestrictionId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping data`() = runTest {
        assertThat(personRestrictionMappingRepository.findOneByNomisId(nomisPersonRestrictionId)).isNotNull
        webTestClient.delete()
          .uri("/mapping/contact-person/person-restriction/nomis-person-restriction-id/{personRestrictionId}", nomisPersonRestrictionId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
        assertThat(personRestrictionMappingRepository.findOneByNomisId(nomisPersonRestrictionId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/contact-person/prisoner-restriction/nomis-prisoner-restriction-id/{nomisPrisonerRestrictionId}")
  inner class DeletePrisonerRestrictionByNomisId {
    private val nomisPrisonerRestrictionId = 12345L
    private lateinit var prisonerRestrictionMapping: PrisonerRestrictionMapping

    @BeforeEach
    fun setUp() = runTest {
      prisonerRestrictionMapping = prisonerRestrictionMappingRepository.save(
        PrisonerRestrictionMapping(
          dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisId = nomisPrisonerRestrictionId,
          offenderNo = "A1234BC",
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/prisoner-restriction/nomis-prisoner-restriction-id/{nomisPrisonerRestrictionId}", nomisPrisonerRestrictionId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/prisoner-restriction/nomis-prisoner-restriction-id/{nomisPrisonerRestrictionId}", nomisPrisonerRestrictionId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/prisoner-restriction/nomis-prisoner-restriction-id/{nomisPrisonerRestrictionId}", nomisPrisonerRestrictionId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping does not exist`() {
        webTestClient.delete()
          .uri("/mapping/contact-person/prisoner-restriction/nomis-prisoner-restriction-id/{nomisPrisonerRestrictionId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping data`() = runTest {
        assertThat(prisonerRestrictionMappingRepository.findOneByNomisId(nomisPrisonerRestrictionId)).isNotNull
        webTestClient.delete()
          .uri("/mapping/contact-person/prisoner-restriction/nomis-prisoner-restriction-id/{nomisPrisonerRestrictionId}", nomisPrisonerRestrictionId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
        assertThat(prisonerRestrictionMappingRepository.findOneByNomisId(nomisPrisonerRestrictionId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/prisoner-restrictions/prisoners/{offenderNo}")
  inner class GetPrisonerRestrictionMappingsByOffenderNo {
    private val offenderNo = "A1234BC"

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/contact-person/prisoner-restrictions/prisoners/$offenderNo")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/contact-person/prisoner-restrictions/prisoners/$offenderNo")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/contact-person/prisoner-restrictions/prisoners/$offenderNo")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can retrieve all mappings by offender number`() = runTest {
        // Create mappings for the test offender number
        (1L..3L).forEach {
          prisonerRestrictionMappingRepository.save(
            PrisonerRestrictionMapping(
              dpsId = "edcd118c-${it}1ba-42ea-b5c4-404b453ad58b",
              nomisId = it,
              offenderNo = offenderNo,
              label = null,
              mappingType = ContactPersonMappingType.DPS_CREATED,
            ),
          )
        }

        // Create a mapping for a different offender number
        prisonerRestrictionMappingRepository.save(
          PrisonerRestrictionMapping(
            dpsId = "edcd118c-91ba-42ea-b5c4-404b453ad58b",
            nomisId = 54321L,
            offenderNo = "Z9876YX",
            label = null,
            mappingType = ContactPersonMappingType.DPS_CREATED,
          ),
        )

        webTestClient.get().uri("/mapping/contact-person/prisoner-restrictions/prisoners/$offenderNo")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$[0].nomisId").isEqualTo(1)
          .jsonPath("$[1].nomisId").isEqualTo(2)
          .jsonPath("$[2].nomisId").isEqualTo(3)
          .jsonPath("$[0].offenderNo").isEqualTo(offenderNo)
          .jsonPath("$[1].offenderNo").isEqualTo(offenderNo)
          .jsonPath("$[2].offenderNo").isEqualTo(offenderNo)
          .jsonPath("$[0].dpsId").isEqualTo("edcd118c-11ba-42ea-b5c4-404b453ad58b")
          .jsonPath("$[1].dpsId").isEqualTo("edcd118c-21ba-42ea-b5c4-404b453ad58b")
          .jsonPath("$[2].dpsId").isEqualTo("edcd118c-31ba-42ea-b5c4-404b453ad58b")
      }

      @Test
      fun `returns empty list when no mappings found`() {
        webTestClient.get().uri("/mapping/contact-person/prisoner-restrictions/prisoners/UNKNOWN")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$").isArray
          .jsonPath("$.length()").isEqualTo(0)
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/person-restriction/dps-contact-restriction-id/{contactRestrictionId}")
  inner class GetPersonRestrictionByDpsId {
    private val dpsContactRestrictionId = "1234567"
    private lateinit var personRestrictionMapping: PersonRestrictionMapping

    @BeforeEach
    fun setUp() = runTest {
      personRestrictionMapping = personRestrictionMappingRepository.save(
        PersonRestrictionMapping(
          dpsId = dpsContactRestrictionId,
          nomisId = 123456,
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person-restriction/dps-contact-restriction-id/{contactRestrictionId}", dpsContactRestrictionId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person-restriction/dps-contact-restriction-id/{contactRestrictionId}", dpsContactRestrictionId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person-restriction/dps-contact-restriction-id/{contactRestrictionId}", dpsContactRestrictionId)
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
          .uri("/mapping/contact-person/person-restriction/dps-contact-restriction-id/{contactRestrictionId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/person-restriction/dps-contact-restriction-id/{contactRestrictionId}", dpsContactRestrictionId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsContactRestrictionId)
          .jsonPath("nomisId").isEqualTo(123456)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("POST mapping/contact-person/person-restriction")
  inner class CreateRestrictionMapping {

    @Nested
    inner class Security {
      val mapping = PersonRestrictionMappingDto(
        dpsId = UUID.randomUUID().toString(),
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/person-restriction")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/person-restriction")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/person-restriction")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingPersonRestrictionMapping: PersonRestrictionMapping

      val mapping = PersonRestrictionMappingDto(
        dpsId = UUID.randomUUID().toString(),
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingPersonRestrictionMapping = personRestrictionMappingRepository.save(
          PersonRestrictionMapping(
            dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
            nomisId = 12345L,
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same person  to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/contact-person/person-restriction")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/contact-person/person-restriction")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
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
            .containsEntry("nomisId", existingPersonRestrictionMapping.nomisId.toInt())
            .containsEntry("dpsId", existingPersonRestrictionMapping.dpsId)
            .containsEntry("mappingType", existingPersonRestrictionMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mapping.nomisId.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = PersonRestrictionMappingDto(
        dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
        nomisId = 12345L,
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/person-restriction")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the person restriction mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/person-restriction")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val personRestrictionMapping =
          personRestrictionMappingRepository.findOneByNomisId(mapping.nomisId)!!

        assertThat(personRestrictionMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(personRestrictionMapping.nomisId).isEqualTo(mapping.nomisId)
        assertThat(personRestrictionMapping.label).isNull()
        assertThat(personRestrictionMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(personRestrictionMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("POST mapping/contact-person/prisoner-restriction")
  inner class CreatePrisonerRestrictionMapping {

    @Nested
    inner class Security {
      val mapping = PrisonerRestrictionMappingDto(
        dpsId = UUID.randomUUID().toString(),
        nomisId = 12345L,
        offenderNo = "A1234BC",
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/contact-person/prisoner-restriction")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/prisoner-restriction")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/contact-person/prisoner-restriction")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingPrisonerRestrictionMapping: PrisonerRestrictionMapping

      val mapping = PrisonerRestrictionMappingDto(
        dpsId = UUID.randomUUID().toString(),
        nomisId = 12345L,
        offenderNo = "A1234BC",
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingPrisonerRestrictionMapping = prisonerRestrictionMappingRepository.save(
          PrisonerRestrictionMapping(
            dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
            nomisId = 12345L,
            offenderNo = "A1234BC",
            label = "2023-01-01T12:45:12",
            mappingType = ContactPersonMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same prisoner to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/contact-person/prisoner-restriction")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/contact-person/prisoner-restriction")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
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
            .containsEntry("nomisId", existingPrisonerRestrictionMapping.nomisId.toInt())
            .containsEntry("dpsId", existingPrisonerRestrictionMapping.dpsId)
            .containsEntry("mappingType", existingPrisonerRestrictionMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mapping.nomisId.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = PrisonerRestrictionMappingDto(
        dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
        nomisId = 12345L,
        offenderNo = "A1234BC",
        label = null,
        mappingType = ContactPersonMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/prisoner-restriction")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the prisoner restriction mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/prisoner-restriction")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val prisonerRestrictionMapping =
          prisonerRestrictionMappingRepository.findOneByNomisId(mapping.nomisId)!!

        assertThat(prisonerRestrictionMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(prisonerRestrictionMapping.nomisId).isEqualTo(mapping.nomisId)
        assertThat(prisonerRestrictionMapping.offenderNo).isEqualTo(mapping.offenderNo)
        assertThat(prisonerRestrictionMapping.label).isNull()
        assertThat(prisonerRestrictionMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(prisonerRestrictionMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/contact-person/prisoner-restriction/nomis-prisoner-restriction-id/{nomisPrisonerRestrictionId}")
  inner class GetPrisonerRestrictionByNomisId {
    private val nomisPrisonerRestrictionId = 12345L
    private lateinit var prisonerRestrictionMapping: PrisonerRestrictionMapping

    @BeforeEach
    fun setUp() = runTest {
      prisonerRestrictionMapping = prisonerRestrictionMappingRepository.save(
        PrisonerRestrictionMapping(
          dpsId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisId = nomisPrisonerRestrictionId,
          offenderNo = "A1234BC",
          label = "2023-01-01T12:45:12",
          mappingType = ContactPersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/contact-person/prisoner-restriction/nomis-prisoner-restriction-id/{nomisPrisonerRestrictionId}", nomisPrisonerRestrictionId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/prisoner-restriction/nomis-prisoner-restriction-id/{nomisPrisonerRestrictionId}", nomisPrisonerRestrictionId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/contact-person/prisoner-restriction/nomis-prisoner-restriction-id/{nomisPrisonerRestrictionId}", nomisPrisonerRestrictionId)
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
          .uri("/mapping/contact-person/prisoner-restriction/nomis-prisoner-restriction-id/{nomisPrisonerRestrictionId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/contact-person/prisoner-restriction/nomis-prisoner-restriction-id/{nomisPrisonerRestrictionId}", nomisPrisonerRestrictionId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo("edcd118c-41ba-42ea-b5c4-404b453ad58b")
          .jsonPath("nomisId").isEqualTo(nomisPrisonerRestrictionId)
          .jsonPath("offenderNo").isEqualTo("A1234BC")
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }
}
