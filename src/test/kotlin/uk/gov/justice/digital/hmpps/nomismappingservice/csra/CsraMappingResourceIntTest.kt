package uk.gov.justice.digital.hmpps.nomismappingservice.csra

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val DPS_CSRA_ID = "e52d7268-6e10-41a8-a0b9-2319b32520d6"
private const val DPS_CSRA_ID2 = "edcd118c-41ba-42ea-b5c4-404b453ad58b"
private const val OFFENDER_NO = "A1234AA"
private const val BOOKING_ID = 12345L

class CsraMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: CsraMappingRepository

  private fun generateUUIDs(n: Long) = "de91dfa7-821f-4552-a427-000000${n.toString().padStart(6, '0')}"
  private fun generateUUID(n: Long) = UUID.fromString(generateUUIDs(n))

  private fun postCreateMappingRequest(
    nomisBookingId: Long = BOOKING_ID,
    sequence: Int = 1,
    dpsCsraId: String = DPS_CSRA_ID,
    offenderNo: String = OFFENDER_NO,
    label: String = "2022-01-01",
    mappingType: CsraMappingType = CsraMappingType.DPS_CREATED,
  ) {
    webTestClient.post().uri("/mapping/csras")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          CsraMappingDto(
            nomisBookingId = nomisBookingId,
            nomisSequence = sequence,
            dpsCsraId = dpsCsraId,
            offenderNo = offenderNo,
            label = label,
            mappingType = mappingType,
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  @AfterEach
  internal fun deleteData() = runBlocking {
    repository.deleteAll()
  }

  @Nested
  @DisplayName("POST /mapping/csras")
  inner class CreateMapping {
    private lateinit var existingMapping: CsraMapping
    private val mapping = CsraMappingDto(
      dpsCsraId = DPS_CSRA_ID,
      nomisBookingId = BOOKING_ID,
      nomisSequence = 1,
      offenderNo = OFFENDER_NO,
      label = "2024-02-01T12:45:12",
      mappingType = CsraMappingType.MIGRATED,
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        CsraMapping(
          dpsCsraId = UUID.fromString(DPS_CSRA_ID2),
          nomisBookingId = 3456L,
          nomisSequence = 1,
          offenderNo = OFFENDER_NO,
          label = "2024-01-01T12:45:12",
          mappingType = CsraMappingType.DPS_CREATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/csras")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/csras")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/csras")
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
          .uri("/mapping/csras")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMapping = repository.findById(UUID.fromString(mapping.dpsCsraId))!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisBookingId).isEqualTo(BOOKING_ID)
        assertThat(createdMapping.nomisSequence).isEqualTo(1)
        assertThat(createdMapping.dpsCsraId.toString()).isEqualTo(mapping.dpsCsraId)
        assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(createdMapping.label).isEqualTo(mapping.label)
      }

      @Test
      fun `can create with minimal data`() = runTest {
        webTestClient.post()
          .uri("/mapping/csras")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                {
                  "nomisBookingId": $BOOKING_ID,
                  "nomisSequence": 2,
                  "dpsCsraId": "$DPS_CSRA_ID",
                  "offenderNo": "A1234AA"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        val createdMapping = repository.findById(UUID.fromString(DPS_CSRA_ID))!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisBookingId).isEqualTo(BOOKING_ID)
        assertThat(createdMapping.nomisSequence).isEqualTo(2)
        assertThat(createdMapping.dpsCsraId).isEqualTo(UUID.fromString(DPS_CSRA_ID))
        assertThat(createdMapping.mappingType).isEqualTo(CsraMappingType.DPS_CREATED)
        assertThat(createdMapping.label).isNull()
      }

      @Test
      fun `can post and then get mapping`() {
        webTestClient.post()
          .uri("/mapping/csras")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                {
                  "nomisBookingId": 54555,
                  "nomisSequence": 1,
                  "dpsCsraId": "$DPS_CSRA_ID",
                  "offenderNo": "A1234AA"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        webTestClient.get()
          .uri("/mapping/csras/booking-id/54555/sequence/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$DPS_CSRA_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when mapping type is invalid`() {
        webTestClient.post()
          .uri("/mapping/csras")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                {
                  "nomisBookingId": $BOOKING_ID,
                  "nomisSequence": 1,
                  "dpsCsraId": "$DPS_CSRA_ID",
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
          .uri("/mapping/csras")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisBookingId": $BOOKING_ID,
                  "nomisSequence": 1,
                  
                  "mappingType": "MIGRATED"
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
          .uri("/mapping/csras")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                {
                  "dpsCsraId": "$DPS_CSRA_ID",
                  "mappingType": "MIGRATED"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `returns 409 if nomis ids already exist`() {
        val dpsCsraId = UUID.randomUUID().toString()
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/csras")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CsraMappingDto(
                nomisBookingId = existingMapping.nomisBookingId,
                nomisSequence = existingMapping.nomisSequence,
                dpsCsraId = dpsCsraId,
                offenderNo = OFFENDER_NO,
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
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisSequence", existingMapping.nomisSequence)
            .containsEntry("dpsCsraId", existingMapping.dpsCsraId.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisSequence", existingMapping.nomisSequence)
            .containsEntry("dpsCsraId", dpsCsraId)
        }
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/csras/batch")
  inner class CreateMappings {
    private var existingMapping: CsraMapping = CsraMapping(
      dpsCsraId = generateUUID(1),
      nomisBookingId = 50001L,
      nomisSequence = 1,
      offenderNo = OFFENDER_NO,
      label = "2023-01-01T12:45:12",
      mappingType = CsraMappingType.MIGRATED,
    )
    private val mappings = listOf(
      CsraMappingDto(
        dpsCsraId = "e52d7268-6e10-41a8-a0b9-000000000002",
        nomisBookingId = 50002L,
        nomisSequence = 1,
        offenderNo = OFFENDER_NO,
        mappingType = CsraMappingType.DPS_CREATED,
      ),
      CsraMappingDto(
        dpsCsraId = "fd4e55a8-0805-439b-9e27-000000000003",
        nomisBookingId = 50003L,
        nomisSequence = 1,
        offenderNo = OFFENDER_NO,
        mappingType = CsraMappingType.NOMIS_CREATED,
      ),
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(existingMapping)
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/csras/batch")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/csras/batch")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/csras/batch")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/csras/batch")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated

        val createdMapping1 = repository.findById(UUID.fromString(mappings[0].dpsCsraId))!!

        assertThat(createdMapping1.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping1.nomisBookingId).isEqualTo(mappings[0].nomisBookingId)
        assertThat(createdMapping1.nomisSequence).isEqualTo(mappings[0].nomisSequence)
        assertThat(createdMapping1.dpsCsraId.toString()).isEqualTo(mappings[0].dpsCsraId)
        assertThat(createdMapping1.offenderNo).isEqualTo(mappings[0].offenderNo)
        assertThat(createdMapping1.mappingType).isEqualTo(mappings[0].mappingType)
        assertThat(createdMapping1.label).isEqualTo(mappings[0].label)

        val createdMapping2 = repository.findById(UUID.fromString(mappings[1].dpsCsraId))!!

        assertThat(createdMapping2.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping2.nomisBookingId).isEqualTo(mappings[1].nomisBookingId)
        assertThat(createdMapping2.nomisSequence).isEqualTo(mappings[1].nomisSequence)
        assertThat(createdMapping2.dpsCsraId.toString()).isEqualTo(mappings[1].dpsCsraId)
        assertThat(createdMapping2.offenderNo).isEqualTo(mappings[1].offenderNo)
        assertThat(createdMapping2.mappingType).isEqualTo(mappings[1].mappingType)
        assertThat(createdMapping2.label).isEqualTo(mappings[1].label)
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when mapping type is invalid`() {
        webTestClient.post()
          .uri("/mapping/csras/batch")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                [
                    {
                      "nomisBookingId": $BOOKING_ID,
                    
                      "nomisSequence": 54321,
                      "dpsCsraId": "$DPS_CSRA_ID",
                      "mappingType": "INVALID_TYPE"
                    }
                  ]
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `will return 409 if nomis ids already exist`() {
        val dpsCsraId = UUID.randomUUID().toString()
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/csras/batch")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings + existingMapping.copy(dpsCsraId = UUID.fromString(dpsCsraId)),
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
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisSequence", existingMapping.nomisSequence)
            .containsEntry("dpsCsraId", existingMapping.dpsCsraId.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisBookingId", existingMapping.nomisBookingId.toInt())
            .containsEntry("nomisSequence", existingMapping.nomisSequence)
            .containsEntry("dpsCsraId", dpsCsraId)
        }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/csras/{offenderNo}/all")
  inner class GetMappingByPrisoner {
    val dpsId1 = "85665bb9-ab28-458a-8386-b8cc91b311f7"
    val dpsId2 = DPS_CSRA_ID2
    val dpsId3 = "e52d7268-6e10-41a8-a0b9-2319b32520d6"
    val dpsId4 = "fd4e55a8-0805-439b-9e27-647583b96e4e"

    private var mapping1: CsraMapping = CsraMapping(
      dpsCsraId = UUID.fromString(dpsId2),
      nomisBookingId = 54321L,
      nomisSequence = 2,
      offenderNo = "A1234KT",
      mappingType = CsraMappingType.DPS_CREATED,
    )
    private var mapping2: CsraMapping = CsraMapping(
      dpsCsraId = UUID.fromString(dpsId1),
      nomisBookingId = 11111L,
      nomisSequence = 1,
      offenderNo = "A1234KT",
      mappingType = CsraMappingType.DPS_CREATED,
    )
    private val prisonerMappings = listOf(
      CsraMappingDto(
        dpsCsraId = dpsId3,
        nomisSequence = 3,
        offenderNo = "A1234KT",
        nomisBookingId = 54321L,
        label = "2023-01-01T12:45:12",
        mappingType = CsraMappingType.MIGRATED,
      ),
      CsraMappingDto(
        dpsCsraId = dpsId4,
        nomisSequence = 4,
        offenderNo = "A1234KT",
        nomisBookingId = 54321L,
        label = "2023-01-01T12:45:12",
        mappingType = CsraMappingType.MIGRATED,
      ),
    )

    @BeforeEach
    fun setUp() = runTest {
      webTestClient.post()
        .uri("/mapping/csras/batch")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(prisonerMappings))
        .exchange()
        .expectStatus().isCreated
      mapping1 = repository.save(mapping1)
      mapping2 = repository.save(mapping2)
      repository.save(
        CsraMapping(
          dpsCsraId = UUID.fromString("fd4e55a8-41ba-42ea-b5c4-404b453ad99b"),
          nomisBookingId = 9999L,
          nomisSequence = 5,
          offenderNo = "A1111KT",
          mappingType = CsraMappingType.MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/csras/A1234KT/all")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/csras/A1234KT/all")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/csras/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return 200 when no mappings found for prisoner`() {
        webTestClient.get()
          .uri("/mapping/csras/A9999KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("mappings.size()").isEqualTo(0)
      }

      @Test
      fun `will return all mappings for prisoner`() {
        webTestClient.get()
          .uri("/mapping/csras/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("mappings.size()").isEqualTo(4)
          .jsonPath("mappings[0].dpsCsraId").isEqualTo(dpsId1)
          .jsonPath("mappings[0].nomisSequence").isEqualTo(1)
          .jsonPath("mappings[0].nomisBookingId").isEqualTo(11111)
          .jsonPath("mappings[0].offenderNo").isEqualTo("A1234KT")
          .jsonPath("mappings[1].dpsCsraId").isEqualTo(dpsId2)
          .jsonPath("mappings[1].nomisSequence").isEqualTo(2)
          .jsonPath("mappings[1].nomisBookingId").isEqualTo(54321L)
          .jsonPath("mappings[2].dpsCsraId").isEqualTo(dpsId3)
          .jsonPath("mappings[2].nomisSequence").isEqualTo(3)
          .jsonPath("mappings[2].nomisBookingId").isEqualTo(54321L)
          .jsonPath("mappings[3].dpsCsraId").isEqualTo(dpsId4)
          .jsonPath("mappings[3].nomisSequence").isEqualTo(4)
          .jsonPath("mappings[3].nomisBookingId").isEqualTo(54321L)
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/csras/booking-id/{bookingId}/sequence/{sequence}")
  inner class GetMappingByNomisId {
    lateinit var mapping: CsraMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CsraMapping(
          dpsCsraId = UUID.fromString(DPS_CSRA_ID2),
          nomisBookingId = BOOKING_ID,
          nomisSequence = 1,
          offenderNo = OFFENDER_NO,
          label = "2023-01-01T12:45:12",
          mappingType = CsraMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/csras/booking-id/999/sequence/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/csras/booking-id/999/sequence/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/csras/booking-id/999/sequence/1")
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
          .uri("/mapping/csras/booking-id/999/sequence/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/csras/booking-id/$BOOKING_ID/sequence/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisBookingId").isEqualTo(mapping.nomisBookingId)
          .jsonPath("nomisSequence").isEqualTo(mapping.nomisSequence)
          .jsonPath("dpsCsraId").isEqualTo(mapping.dpsCsraId.toString())
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/csras/dps-csra-id/{dpsCsraId}")
  inner class GetMappingsByDpsId {
    lateinit var mapping1: CsraMapping
    lateinit var mapping2: CsraMapping

    @BeforeEach
    fun setUp() {
      runTest {
        mapping1 = repository.save(
          CsraMapping(
            dpsCsraId = UUID.fromString(DPS_CSRA_ID),
            nomisBookingId = BOOKING_ID,
            nomisSequence = 1,
            offenderNo = OFFENDER_NO,
            label = "2023-01-01T12:45:12",
            mappingType = CsraMappingType.MIGRATED,
          ),
        )
        mapping2 = repository.save(
          CsraMapping(
            dpsCsraId = UUID.fromString(DPS_CSRA_ID2),
            nomisSequence = 2,
            offenderNo = OFFENDER_NO,
            nomisBookingId = BOOKING_ID,
            label = "2023-06-01T12:45:12",
            mappingType = CsraMappingType.DPS_CREATED,
          ),
        )
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/${mapping1.dpsCsraId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/${mapping1.dpsCsraId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/${mapping1.dpsCsraId}")
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
          .uri("/mapping/csras/dps-csra-id/00001111-0000-0000-0000-000011112222")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/${mapping1.dpsCsraId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsCsraId").isEqualTo(mapping1.dpsCsraId.toString())
          .jsonPath("nomisBookingId").isEqualTo(mapping1.nomisBookingId)
          .jsonPath("nomisSequence").isEqualTo(mapping1.nomisSequence)
          .jsonPath("offenderNo").isEqualTo(OFFENDER_NO)
          .jsonPath("mappingType").isEqualTo(mapping1.mappingType.name)
          .jsonPath("label").isEqualTo(mapping1.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @DisplayName("GET /mapping/csras/migration-id/{migrationId}/count-by-prisoner")
  @Nested
  inner class GetMappingCountByMigrationIdTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/csras/migration-id/2022-01-01T00:00:00/count-by-prisoner")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/csras/migration-id/2022-01-01T00:00:00/count-by-prisoner")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get csra mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/csras/migration-id/2022-01-01T00:00:00/count-by-prisoner")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get csra mapping count success`() {
      (1L..5L).forEach {
        postCreateMappingRequest(
          it,
          1,
          generateUUIDs(it),
          OFFENDER_NO,
          label = "2022-01-01",
          mappingType = CsraMappingType.MIGRATED,
        )
      }

      webTestClient.get().uri("/mapping/csras/migration-id/2022-01-01/count-by-prisoner")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").isEqualTo(2) // i.e. 5 rows divided by average-per-prisoner
    }

    @Test
    @Disabled
    fun `can request a different page size`() {
      (1L..6L).forEach {
        postCreateMappingRequest(
          it,
          1,
          generateUUIDs(it),
          offenderNo = "A000${it}AA",
          label = "2022-01-01",
          mappingType = CsraMappingType.MIGRATED,
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/csras/migration-id/2022-01-01/count-by-prisoner")
          .queryParam("size", "2")
          .queryParam("sort", "nomisSequence,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(6)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(3)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    @Disabled
    fun `can request a different page`() {
      (1L..3L).forEach {
        postCreateMappingRequest(it, 1, generateUUIDs(it), label = "2022-01-01", mappingType = CsraMappingType.MIGRATED)
      }
      webTestClient.get().uri {
        it.path("/mapping/csras/migration-id/2022-01-01/count-by-prisoner")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .queryParam("sort", "nomisSequence,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(1)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
    }
  }

  @DisplayName("GET /mapping/csras/migrated/latest")
  @Nested
  inner class GeMappingMigratedLatestTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/csras/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/csras/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/csras/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {
      webTestClient.post().uri("/mapping/csras")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CsraMappingDto(
              nomisSequence = 10,
              dpsCsraId = generateUUIDs(10),
              offenderNo = OFFENDER_NO,
              nomisBookingId = 10,
              label = "2022-01-01T00:00:00",
              mappingType = CsraMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/csras")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CsraMappingDto(
              nomisSequence = 20,
              dpsCsraId = generateUUIDs(20),
              offenderNo = OFFENDER_NO,
              nomisBookingId = BOOKING_ID,
              label = "2022-01-02T00:00:00",
              mappingType = CsraMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/csras")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CsraMappingDto(
              nomisSequence = 1,
              dpsCsraId = generateUUIDs(1),
              offenderNo = OFFENDER_NO,
              nomisBookingId = 2,
              label = "2022-01-02T10:00:00",
              mappingType = CsraMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/csras")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CsraMappingDto(
              nomisSequence = 99,
              dpsCsraId = generateUUIDs(199),
              offenderNo = OFFENDER_NO,
              nomisBookingId = BOOKING_ID,
              label = "whatever",
              mappingType = CsraMappingType.DPS_CREATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/csras/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody<CsraMappingDto>()
        .returnResult().responseBody!!

      assertThat(mapping.nomisSequence).isEqualTo(1)
      assertThat(mapping.dpsCsraId).isEqualTo(generateUUIDs(1))
      assertThat(mapping.offenderNo).isEqualTo(OFFENDER_NO)
      assertThat(mapping.nomisBookingId).isEqualTo(2)
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo(CsraMappingType.MIGRATED)
      assertThat(mapping.whenCreated)
        .isCloseTo(LocalDateTime.now(), Assertions.byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/csras")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            CsraMappingDto(
              nomisSequence = 77,
              dpsCsraId = generateUUIDs(77),
              offenderNo = OFFENDER_NO,
              nomisBookingId = BOOKING_ID,
              label = "whatever",
              mappingType = CsraMappingType.DPS_CREATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/csras/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/csras/booking-id/{bookingId}/sequence/{sequence}")
  inner class DeleteMappingByNomisId {
    lateinit var mapping: CsraMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CsraMapping(
          dpsCsraId = UUID.fromString(DPS_CSRA_ID2),
          nomisBookingId = BOOKING_ID,
          nomisSequence = 1,
          offenderNo = OFFENDER_NO,
          label = "2023-01-01T12:45:12",
          mappingType = CsraMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/csras/booking-id/999/sequence/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/csras/booking-id/999/sequence/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/csras/booking-id/999/sequence/1")
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
          .uri("/mapping/csras/booking-id/999/sequence/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/csras/booking-id/$BOOKING_ID/sequence/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/csras/booking-id/$BOOKING_ID/sequence/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/csras/booking-id/$BOOKING_ID/sequence/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/csras/dps-csra-id/{dpsCsraId}")
  inner class DeleteMappingByDpsId {
    lateinit var mapping: CsraMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        CsraMapping(
          dpsCsraId = UUID.fromString(DPS_CSRA_ID2),
          nomisBookingId = BOOKING_ID,
          nomisSequence = 1,
          offenderNo = OFFENDER_NO,
          label = "2023-01-01T12:45:12",
          mappingType = CsraMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/csras/dps-csra-id/${mapping.dpsCsraId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/csras/dps-csra-id/${mapping.dpsCsraId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/csras/dps-csra-id/${mapping.dpsCsraId}")
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
          .uri("/mapping/csras/dps-csra-id/00001111-0000-0000-0000-000011112222")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/${mapping.dpsCsraId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/csras/dps-csra-id/${mapping.dpsCsraId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/${mapping.dpsCsraId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("PUT /merge/from/{oldOffenderNo}/to/{newOffenderNo}")
  inner class PrisonerMergeMappings {
    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/csras/merge/from/A1234AA/to/A1234BB")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/csras/merge/from/A1234AA/to/A1234BB")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/csras/merge/from/A1234AA/to/A1234BB")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      private val dps1 = "00000000-1111-2222-3333-000088880001"
      private val dps2 = "00000000-1111-2222-3333-000088880002"
      private val dps3 = "00000000-1111-2222-3333-000088880003"

      @BeforeEach
      fun setUp() {
        runTest {
          repository.save(
            CsraMapping(
              dpsCsraId = UUID.fromString(dps1),
              nomisBookingId = BOOKING_ID,
              nomisSequence = 54321,
              offenderNo = "A1234AA",
              mappingType = CsraMappingType.MIGRATED,
            ),
          )
          repository.save(
            CsraMapping(
              dpsCsraId = UUID.fromString(dps2),
              nomisBookingId = 2,
              nomisSequence = 54322,
              offenderNo = "A1234BB",
              mappingType = CsraMappingType.NOMIS_CREATED,
            ),
          )
          repository.save(
            CsraMapping(
              dpsCsraId = UUID.fromString(dps3),
              nomisBookingId = 2,
              nomisSequence = 54323,
              offenderNo = "A1234BB",
              mappingType = CsraMappingType.NOMIS_CREATED,
            ),
          )
        }
      }

      @Test
      fun `Merge success`() = runTest {
        webTestClient.put().uri("/mapping/csras/merge/from/A1234AA/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        // first record has changed
        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo("B5678BB")

        // second has not
        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo("A1234BB")
      }

      @Test
      fun `Nothing happens if not found`() = runTest {
        webTestClient.put().uri("/mapping/csras/merge/from/A9999AA/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisSequence").isEqualTo(54321)
          .jsonPath("offenderNo").isEqualTo("A1234AA")
          .jsonPath("nomisBookingId").isEqualTo(BOOKING_ID)

        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisSequence").isEqualTo(54322)
          .jsonPath("offenderNo").isEqualTo("A1234BB")
          .jsonPath("nomisBookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisSequence").isEqualTo(54323)
          .jsonPath("offenderNo").isEqualTo("A1234BB")
          .jsonPath("nomisBookingId").isEqualTo(2)
      }

      @Test
      fun `Merge success - multiple candidates`() = runTest {
        webTestClient.put().uri("/mapping/csras/merge/from/A1234BB/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisSequence").isEqualTo(54321)
          .jsonPath("offenderNo").isEqualTo("A1234AA")
          .jsonPath("nomisBookingId").isEqualTo(BOOKING_ID)

        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisSequence").isEqualTo(54322)
          .jsonPath("offenderNo").isEqualTo("B5678BB")
          .jsonPath("nomisBookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisSequence").isEqualTo(54323)
          .jsonPath("offenderNo").isEqualTo("B5678BB")
          .jsonPath("nomisBookingId").isEqualTo(2)
      }
    }
  }

  @Nested
  @DisplayName("PUT /merge/booking-id/{bookingId}/to/{newOffenderNo}")
  inner class PrisonerMoveMappingsBookingId {
    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/csras/merge/booking-id/333/to/A1234BB")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/csras/merge/booking-id/333/to/A1234BB")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/csras/merge/booking-id/333/to/A1234BB")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      private val dps1 = "00000000-1111-2222-3333-000088880001"
      private val dps2 = "00000000-1111-2222-3333-000088880002"
      private val dps3 = "00000000-1111-2222-3333-000088880003"

      @BeforeEach
      fun setUp() {
        runTest {
          repository.save(
            CsraMapping(
              dpsCsraId = UUID.fromString(dps1),
              nomisBookingId = BOOKING_ID,
              nomisSequence = 54321,
              offenderNo = "A1234AA",
              mappingType = CsraMappingType.MIGRATED,
            ),
          )
          repository.save(
            CsraMapping(
              dpsCsraId = UUID.fromString(dps2),
              nomisBookingId = 2,
              nomisSequence = 54322,
              offenderNo = "A1234BB",
              mappingType = CsraMappingType.NOMIS_CREATED,
            ),
          )
          repository.save(
            CsraMapping(
              dpsCsraId = UUID.fromString(dps3),
              nomisBookingId = 2,
              nomisSequence = 54323,
              offenderNo = "A1234BB",
              mappingType = CsraMappingType.NOMIS_CREATED,
            ),
          )
        }
      }

      @Test
      fun `Move success`() = runTest {
        webTestClient.put().uri("/mapping/csras/merge/booking-id/$BOOKING_ID/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  { 
    "nomisSequence": 54321,
    "dpsCsraId": "$dps1",
    "nomisBookingId": $BOOKING_ID
  }
]""",
          )

        // Check first record has changed
        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo("B5678BB")

        // second has not
        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo("A1234BB")
      }

      @Test
      fun `Nothing happens if not found`() = runTest {
        webTestClient.put().uri("/mapping/csras/merge/booking-id/999/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("[]")

        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisSequence").isEqualTo(54321)
          .jsonPath("offenderNo").isEqualTo("A1234AA")
          .jsonPath("nomisBookingId").isEqualTo(BOOKING_ID)

        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisSequence").isEqualTo(54322)
          .jsonPath("offenderNo").isEqualTo("A1234BB")
          .jsonPath("nomisBookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisSequence").isEqualTo(54323)
          .jsonPath("offenderNo").isEqualTo("A1234BB")
          .jsonPath("nomisBookingId").isEqualTo(2)
      }

      @Test
      fun `Move success - multiple candidates`() = runTest {
        webTestClient.put().uri("/mapping/csras/merge/booking-id/2/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  { 
    "nomisSequence": 54322,
    "dpsCsraId": "$dps2",
    "nomisBookingId": 2
  },
  { 
    "nomisSequence": 54323,
    "dpsCsraId": "$dps3",
    "nomisBookingId": 2
  }
]""",
          )

        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisSequence").isEqualTo(54321)
          .jsonPath("offenderNo").isEqualTo("A1234AA")
          .jsonPath("nomisBookingId").isEqualTo(BOOKING_ID)

        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisSequence").isEqualTo(54322)
          .jsonPath("offenderNo").isEqualTo("B5678BB")
          .jsonPath("nomisBookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/csras/dps-csra-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisSequence").isEqualTo(54323)
          .jsonPath("offenderNo").isEqualTo("B5678BB")
          .jsonPath("nomisBookingId").isEqualTo(2)
      }
    }
  }
}
