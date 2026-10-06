package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import java.time.LocalDateTime

class CorePersonContactMappingResourceIntTest(
  @Autowired private val corePersonMappingRepository: CorePersonMappingRepository,
  @Autowired private val corePersonContactMappingRepository: CorePersonContactMappingRepository,
) : IntegrationTestBase() {

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("GET /mapping/core-person/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}")
  inner class GetPersonContactByNomisId {
    private val nomisContactId = 12345L
    private val cprPhoneContactId = "54321"
    private val cprEmailContactId = "54322"

    @BeforeAll
    fun setUp() = runTest {
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "edcd118c-41ba-42ea-b5c4-404b453ad58c",
          nomisPrisonNumber = "B1234BB",
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      corePersonContactMappingRepository.save(
        CorePersonContactMapping(
          nomisPrisonNumber = "B1234BB",
          cprId = cprPhoneContactId,
          nomisId = nomisContactId,
          nomisContactType = NomisContactType.PHONE,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      corePersonContactMappingRepository.save(
        CorePersonContactMapping(
          nomisPrisonNumber = "B1234BB",
          cprId = cprEmailContactId,
          nomisId = nomisContactId,
          nomisContactType = NomisContactType.EMAIL,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @AfterAll
    fun tearDown() = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/core-person/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}", nomisContactId, "PHONE")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}", nomisContactId, "PHONE")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}", nomisContactId, "PHONE")
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
          .uri("/mapping/core-person/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}", 99999, "PHONE")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 when contact type is not a valid enum value`() {
        webTestClient.get()
          .uri("/mapping/core-person/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}", nomisContactId, "BANANAS")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the phone mapping data`() {
        webTestClient.get()
          .uri("/mapping/core-person/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}", nomisContactId, "PHONE")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprPhoneContactId)
          .jsonPath("nomisId").isEqualTo(nomisContactId)
          .jsonPath("nomisContactType").isEqualTo("PHONE")
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }

      @Test
      fun `will return the email mapping data for the same nomis id`() {
        webTestClient.get()
          .uri("/mapping/core-person/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}", nomisContactId, "EMAIL")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprEmailContactId)
          .jsonPath("nomisId").isEqualTo(nomisContactId)
          .jsonPath("nomisContactType").isEqualTo("EMAIL")
      }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("GET /mapping/core-person/contact/cpr-contact-id/{cprContactId}")
  inner class GetPersonContactByCprId {
    private val nomisContactId = 7654321L
    private val cprContactId = "1234567"

    @BeforeAll
    fun setUp() = runTest {
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "b6f5c52c-1d13-4f86-bf2c-4a4f4f891e69",
          nomisPrisonNumber = "B1234BB",
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      corePersonContactMappingRepository.save(
        CorePersonContactMapping(
          nomisPrisonNumber = "B1234BB",
          cprId = cprContactId,
          nomisId = nomisContactId,
          nomisContactType = NomisContactType.EMAIL,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @AfterAll
    fun tearDown() = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/core-person/contact/cpr-contact-id/{cprContactId}", cprContactId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/contact/cpr-contact-id/{cprContactId}", cprContactId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/contact/cpr-contact-id/{cprContactId}", cprContactId)
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
          .uri("/mapping/core-person/contact/cpr-contact-id/{cprContactId}", "99999")
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
          .uri("/mapping/core-person/contact/cpr-contact-id/{cprContactId}", cprContactId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprContactId)
          .jsonPath("nomisId").isEqualTo(nomisContactId)
          .jsonPath("nomisContactType").isEqualTo("EMAIL")
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("DELETE /mapping/core-person/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}")
  inner class DeletePersonContactByNomisId {
    private val nomisContactId = 22345L

    @BeforeAll
    fun setUp() = runTest {
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "c6f5c52c-1d13-4f86-bf2c-4a4f4f891e69",
          nomisPrisonNumber = "A1234AA",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      corePersonContactMappingRepository.save(
        CorePersonContactMapping(
          nomisPrisonNumber = "A1234AA",
          cprId = "654321",
          nomisId = nomisContactId,
          nomisContactType = NomisContactType.PHONE,
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      corePersonContactMappingRepository.save(
        CorePersonContactMapping(
          nomisPrisonNumber = "A1234AA",
          cprId = "654322",
          nomisId = nomisContactId,
          nomisContactType = NomisContactType.EMAIL,
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
    }

    @AfterAll
    fun tearDown() = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/core-person/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}", nomisContactId, "PHONE")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}", nomisContactId, "PHONE")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}", nomisContactId, "PHONE")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `400 when contact type is not a valid enum value`() {
        webTestClient.delete()
          .uri("/mapping/core-person/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}", nomisContactId, "BANANAS")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will only delete the mapping for the given contact type`() = runTest {
        webTestClient.delete()
          .uri("/mapping/core-person/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}", nomisContactId, "PHONE")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(corePersonContactMappingRepository.findOneByNomisIdAndNomisContactType(nomisContactId, NomisContactType.PHONE)).isNull()
        assertThat(corePersonContactMappingRepository.findOneByNomisIdAndNomisContactType(nomisContactId, NomisContactType.EMAIL)).isNotNull
      }
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("POST /mapping/core-person/contact")
  inner class CreatePersonContactMapping {
    private val mapping = CorePersonContactMappingDto(
      cprId = "754321",
      nomisId = 32345L,
      nomisContactType = NomisContactType.PHONE,
      nomisPrisonNumber = "A1234AA",
      label = null,
      mappingType = CorePersonMappingType.CPR_CREATED,
      whenCreated = null,
    )

    @BeforeAll
    fun setUp() = runTest {
      corePersonMappingRepository.save(
        CorePersonMapping(
          cprId = "d6f5c52c-1d13-4f86-bf2c-4a4f4f891e69",
          nomisPrisonNumber = "A1234AA",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
    }

    @AfterAll
    fun tearDown() = deleteAll()

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/contact")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(mapping)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/contact")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(mapping)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/contact")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(mapping)
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Validation {
      private val existingMapping = CorePersonContactMappingDto(
        cprId = "854321",
        nomisId = 42345L,
        nomisContactType = NomisContactType.PHONE,
        nomisPrisonNumber = mapping.nomisPrisonNumber,
        label = mapping.label,
        mappingType = mapping.mappingType,
        whenCreated = mapping.whenCreated,
      )

      @BeforeAll
      fun setUp() = runTest {
        corePersonContactMappingRepository.save(
          CorePersonContactMapping(
            nomisPrisonNumber = existingMapping.nomisPrisonNumber,
            cprId = existingMapping.cprId,
            nomisId = existingMapping.nomisId,
            nomisContactType = existingMapping.nomisContactType,
            mappingType = existingMapping.mappingType,
          ),
        )
      }

      @Test
      fun `will not allow a duplicate contact mapping`() {
        webTestClient.post()
          .uri("/mapping/core-person/contact")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(existingMapping)
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `400 when contact type is not a valid enum value`() {
        webTestClient.post()
          .uri("/mapping/core-person/contact")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            """
            {
              "cprId": "954321",
              "nomisId": 52345,
              "nomisContactType": "BANANAS",
              "nomisPrisonNumber": "A1234AA",
              "mappingType": "CPR_CREATED"
            }
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will persist the person contact mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/contact")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(mapping)
          .exchange()
          .expectStatus().isCreated

        val persisted = corePersonContactMappingRepository.findOneByNomisIdAndNomisContactType(mapping.nomisId, mapping.nomisContactType)!!
        assertThat(persisted.cprId).isEqualTo(mapping.cprId)
        assertThat(persisted.nomisPrisonNumber).isEqualTo(mapping.nomisPrisonNumber)
        assertThat(persisted.nomisContactType).isEqualTo(mapping.nomisContactType)
        assertThat(persisted.mappingType).isEqualTo(mapping.mappingType)
      }
    }
  }

  private fun deleteAll() = runTest {
    corePersonContactMappingRepository.deleteAll()
    corePersonMappingRepository.deleteAll()
  }
}
