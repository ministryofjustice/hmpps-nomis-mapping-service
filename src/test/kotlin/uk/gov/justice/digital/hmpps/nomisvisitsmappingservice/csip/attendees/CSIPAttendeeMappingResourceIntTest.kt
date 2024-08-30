package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees

import kotlinx.coroutines.runBlocking
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.CSIPAttendeeMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class CSIPAttendeeMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: CSIPAttendeeMappingRepository

  @Autowired
  private lateinit var csipReportRepository: CSIPMappingRepository

  @Nested
  @DisplayName("POST /mapping/csip/attendees")
  inner class CreateMapping {
    private lateinit var existingMapping: CSIPAttendeeMapping
    private val mapping = CSIPAttendeeMappingDto(
      dpsCSIPAttendeeId = "a018f95e-459d-4d0d-9ccd-1fddf4315b2a",
      nomisCSIPAttendeeId = 54321L,
      dpsCSIPReportId = "987",
      label = "2023-01-01T12:45:12",
      mappingType = MIGRATED,
    )

    @BeforeEach
    fun setUp() = runTest {
      csipReportRepository.save(
        CSIPMapping(
          dpsCSIPId = "987",
          nomisCSIPId = 654,
          label = "TIMESTAMP",
          mappingType = CSIPMappingType.MIGRATED,
        ),
      )

      existingMapping = repository.save(
        CSIPAttendeeMapping(
          dpsCSIPAttendeeId = "c5e56441-04c9-40e1-bd37-553ec1abcdef",
          nomisCSIPAttendeeId = 12345L,
          dpsCSIPReportId = "987",
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
      csipReportRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/csip/attendees")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/csip/attendees")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/csip/attendees")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns 201 when mapping created`() = runTest {
        webTestClient.post()
          .uri("/mapping/csip/attendees")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMapping =
          repository.findOneByNomisCSIPAttendeeId(nomisCSIPAttendeeId = mapping.nomisCSIPAttendeeId)!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.dpsCSIPAttendeeId).isEqualTo(mapping.dpsCSIPAttendeeId)
        assertThat(createdMapping.dpsCSIPAttendeeId).isEqualTo(mapping.dpsCSIPAttendeeId)
        assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(createdMapping.label).isEqualTo(mapping.label)
      }

      @Test
      fun `can post and then get new and existing mapping`() {
        webTestClient.post()
          .uri("/mapping/csip/attendees")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              // language=JSON
              """
                {
                  "nomisCSIPAttendeeId": 54321,
                  "dpsCSIPAttendeeId": "018f95e-459d-4d0d-9ccd-1fddf4315b2a",
                  "dpsCSIPReportId": "987"
 }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        webTestClient.get()
          .uri("/mapping/csip/attendees/nomis-csip-attendee-id/54321")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/csip/attendees/nomis-csip-attendee-id/${existingMapping.nomisCSIPAttendeeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when mapping type is invalid`() {
        webTestClient.post()
          .uri("/mapping/csip/attendees")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCSIPAttendeeId": 54321,
                  "dpsCSIPAttendeeId": "e52d7268-6e10-41a8-a0b9-2319b32520d6",
                  "dpsCSIPReportId": "e52d7268-6e10-41a8-a0b9-2319b32520d6",
                  "mappingType": "INVALID_TYPE"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when DPS id is missing`() {
        webTestClient.post()
          .uri("/mapping/csip/attendees")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCSIPAttendeeId": 54321,
                  "dpsCSIPReportId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when NOMIS id is missing`() {
        webTestClient.post()
          .uri("/mapping/csip/attendees")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "dpsCSIPAttendeeId": "5f70a789-7f36-4bec-87dd-fde1a9a995d8",
                  "dpsCSIPReportId": "e52d7268-6e10-41a8-a0b9-2319b32520d6"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 400 when DPS CSIP Report id is missing`() {
        webTestClient.post()
          .uri("/mapping/csip/attendees")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisCSIPAttendeeId": 54321,
                  "dpsCSIPAttendeeId": "5f70a789-7f36-4bec-87dd-fde1a9a995d8"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 409 if nomis ids already exist`() {
        val dpsCSIPAttendeeId = UUID.randomUUID().toString()
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/csip/attendees")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CSIPAttendeeMappingDto(
                nomisCSIPAttendeeId = existingMapping.nomisCSIPAttendeeId,
                dpsCSIPAttendeeId = dpsCSIPAttendeeId,
                dpsCSIPReportId = existingMapping.dpsCSIPReportId,
              ),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          // since this is an untyped map an int will be assumed for such small numbers
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisCSIPAttendeeId", existingMapping.nomisCSIPAttendeeId.toInt())
            .containsEntry("dpsCSIPAttendeeId", existingMapping.dpsCSIPAttendeeId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCSIPAttendeeId", existingMapping.nomisCSIPAttendeeId.toInt())
            .containsEntry("dpsCSIPAttendeeId", dpsCSIPAttendeeId)
        }
      }

      @Test
      fun `returns 409 if dps id already exist`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/csip/attendees")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CSIPAttendeeMappingDto(
                nomisCSIPAttendeeId = 123123,
                dpsCSIPAttendeeId = existingMapping.dpsCSIPAttendeeId,
                dpsCSIPReportId = existingMapping.dpsCSIPReportId,
              ),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          // since this is an untyped map an int will be assumed for such small numbers
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisCSIPAttendeeId", existingMapping.nomisCSIPAttendeeId.toInt())
            .containsEntry("dpsCSIPAttendeeId", existingMapping.dpsCSIPAttendeeId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisCSIPAttendeeId", 123123)
            .containsEntry("dpsCSIPAttendeeId", existingMapping.dpsCSIPAttendeeId)
        }
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/csip/attendees/dps-csip-attendee-id/{dpsCSIPAttendeeId}")
  inner class DeleteMapping {
    lateinit var mapping: CSIPAttendeeMapping

    @BeforeEach
    fun setUp() = runTest {
      csipReportRepository.save(
        CSIPMapping(
          dpsCSIPId = "987",
          nomisCSIPId = 654,
          label = "TIMESTAMP",
          mappingType = CSIPMappingType.MIGRATED,
        ),
      )
      mapping = repository.save(
        CSIPAttendeeMapping(
          dpsCSIPAttendeeId = "edcd118c-41ba-42ea-b5c4-404b453ad5aa",
          nomisCSIPAttendeeId = 8912L,
          dpsCSIPReportId = "987",
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
      csipReportRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/csip/attendees/dps-csip-attendee-id/${mapping.dpsCSIPAttendeeId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/csip/attendees/dps-csip-attendee-id/${mapping.dpsCSIPAttendeeId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/csip/attendees/dps-csip-attendee-id/${mapping.dpsCSIPAttendeeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return 204 even when mapping does not exist`() {
        webTestClient.delete()
          .uri("/mapping/csip/attendees/dps-csip-attendee-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/csip/attendees/dps-csip-attendee-id/${mapping.dpsCSIPAttendeeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/csip/attendees/dps-csip-attendee-id/${mapping.dpsCSIPAttendeeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/csip/attendees/dps-csip-attendee-id/${mapping.dpsCSIPAttendeeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/csip/attendees/nomis-csip-attendee-id/{nomisCSIPAttendeeId}")
  inner class GetMappingByNomisId {
    lateinit var mapping: CSIPAttendeeMapping

    @BeforeEach
    fun setUp() = runTest {
      csipReportRepository.save(
        CSIPMapping(
          dpsCSIPId = "765",
          nomisCSIPId = 654,
          label = "TIMESTAMP",
          mappingType = CSIPMappingType.MIGRATED,
        ),
      )
      mapping = repository.save(
        CSIPAttendeeMapping(
          dpsCSIPAttendeeId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisCSIPAttendeeId = 2345L,
          dpsCSIPReportId = "765",
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
      csipReportRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/csip/attendees/nomis-csip-attendee-id/${mapping.nomisCSIPAttendeeId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/csip/attendees/nomis-csip-attendee-id/${mapping.nomisCSIPAttendeeId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/csip/attendees/nomis-csip-attendee-id/${mapping.nomisCSIPAttendeeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return 404 when mapping does not exist`() {
        webTestClient.get()
          .uri("/mapping/csip/attendees/nomis-csip-attendee-id/99999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/csip/attendees/nomis-csip-attendee-id/${mapping.nomisCSIPAttendeeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCSIPAttendeeId").isEqualTo(mapping.nomisCSIPAttendeeId)
          .jsonPath("dpsCSIPAttendeeId").isEqualTo(mapping.dpsCSIPAttendeeId)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/csip/attendees/dps-csip-attendee-id/{dpsCSIPAttendeeId}")
  inner class GetMappingByDpsId {
    lateinit var mapping: CSIPAttendeeMapping

    @BeforeEach
    fun setUp() = runTest {
      csipReportRepository.save(
        CSIPMapping(
          dpsCSIPId = "765",
          nomisCSIPId = 654,
          label = "TIMESTAMP",
          mappingType = CSIPMappingType.MIGRATED,
        ),
      )
      mapping = repository.save(
        CSIPAttendeeMapping(
          dpsCSIPAttendeeId = "edcd118c-41ba-42ea-b5c4-404b453ad58b",
          nomisCSIPAttendeeId = 54321L,
          dpsCSIPReportId = "765",
          label = "2023-01-01T12:45:12",
          mappingType = MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
      csipReportRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/csip/attendees/dps-csip-attendee-id/${mapping.dpsCSIPAttendeeId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/csip/attendees/dps-csip-attendee-id/${mapping.dpsCSIPAttendeeId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/csip/attendees/dps-csip-attendee-id/${mapping.dpsCSIPAttendeeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return 404 when mapping does not exist`() {
        webTestClient.get()
          .uri("/mapping/csip/attendees/dps-csip-attendee-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/csip/attendees/dps-csip-attendee-id/${mapping.dpsCSIPAttendeeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCSIPAttendeeId").isEqualTo(mapping.nomisCSIPAttendeeId)
          .jsonPath("dpsCSIPAttendeeId").isEqualTo(mapping.dpsCSIPAttendeeId)
          .jsonPath("dpsCSIPReportId").isEqualTo(mapping.dpsCSIPReportId)
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }
}
