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
  private lateinit var personContactMappingRepository: PersonContactMappingRepository

  @Autowired
  private lateinit var personContactRestrictionMappingRepository: PersonContactRestrictionMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    personContactRestrictionMappingRepository.deleteAll()
    personContactMappingRepository.deleteAll()
    personRestrictionMappingRepository.deleteAll()
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
                  ContactPersonSimpleMappingIdDto(dpsId = "0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6", nomisId = 1),
                  ContactPersonSimpleMappingIdDto(dpsId = "e96babce-4a24-49d7-8447-b45f8768f6c1", nomisId = 2),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(personPhoneMappingRepository.findOneByNomisId(1)!!) {
          assertThat(dpsId).isEqualTo("0dcdd1cf-6a40-47d9-9c7e-f8c92452f1a6")
          assertThat(nomisId).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(personPhoneMappingRepository.findOneByDpsId("e96babce-4a24-49d7-8447-b45f8768f6c1")!!) {
          assertThat(dpsId).isEqualTo("e96babce-4a24-49d7-8447-b45f8768f6c1")
          assertThat(nomisId).isEqualTo(2L)
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
          ContactPersonSimpleMappingIdDto(
            dpsId = "c5a02cec-4aa3-4aa7-9871-41e9c9af50f7",
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
}
