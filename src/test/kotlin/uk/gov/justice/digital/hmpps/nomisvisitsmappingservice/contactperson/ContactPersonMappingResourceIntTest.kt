package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

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

  @Nested
  @DisplayName("POST mapping/contact-person/migrate")
  inner class CreateMappings {

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
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
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
      }

      @Test
      fun `will not allow the same person to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
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
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mappings.personMapping.nomisId.toInt())
            .containsEntry("dpsId", mappings.personMapping.dpsId)
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the person mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/contact-person/migrate")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
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
}