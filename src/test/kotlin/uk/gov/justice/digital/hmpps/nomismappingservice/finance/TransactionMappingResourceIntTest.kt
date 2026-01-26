package uk.gov.justice.digital.hmpps.nomismappingservice.finance

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
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val DPS_TRANSACTION_ID = "e52d7268-6e10-41a8-a0b9-2319b32520d6"
private const val DPS_TRANSACTION_ID2 = "edcd118c-41ba-42ea-b5c4-404b453ad58b"
private const val NOMIS_TRANSACTION_ID = 543211L
private const val OFFENDER_NO = "A1234AA"

class TransactionMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: TransactionMappingRepository

  private fun generateUUIDs(n: Long) = "de91dfa7-821f-4552-a427-000000${n.toString().padStart(6, '0')}"
  private fun generateUUID(n: Long) = UUID.fromString(generateUUIDs(n))

  private fun postCreateMappingRequest(
    nomisTransactionId: Long = NOMIS_TRANSACTION_ID,
    dpsTransactionId: String = DPS_TRANSACTION_ID,
    offenderNo: String = OFFENDER_NO,
    nomisBookingId: Long = 1,
    label: String = "2022-01-01",
    mappingType: TransactionMappingType = TransactionMappingType.DPS_CREATED,
  ) {
    webTestClient.post().uri("/mapping/transactions")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          TransactionMappingDto(
            nomisTransactionId = nomisTransactionId,
            dpsTransactionId = dpsTransactionId,
            offenderNo = offenderNo,
            nomisBookingId = nomisBookingId,
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
  @DisplayName("POST /mapping/transactions")
  inner class CreateMapping {
    private lateinit var existingMapping: TransactionMapping
    private val mapping = TransactionMappingDto(
      dpsTransactionId = DPS_TRANSACTION_ID,
      nomisTransactionId = NOMIS_TRANSACTION_ID,
      offenderNo = OFFENDER_NO,
      nomisBookingId = 1,
      label = "2024-02-01T12:45:12",
      mappingType = TransactionMappingType.MIGRATED,
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        TransactionMapping(
          dpsTransactionId = UUID.fromString(DPS_TRANSACTION_ID2),
          nomisTransactionId = 543210L,
          offenderNo = OFFENDER_NO,
          nomisBookingId = 1,
          label = "2024-01-01T12:45:12",
          mappingType = TransactionMappingType.DPS_CREATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/transactions")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/transactions")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/transactions")
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
          .uri("/mapping/transactions")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMapping = repository.findById(mapping.nomisTransactionId)!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisTransactionId).isEqualTo(mapping.nomisTransactionId)
        assertThat(createdMapping.dpsTransactionId.toString()).isEqualTo(mapping.dpsTransactionId)
        assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(createdMapping.offenderNo).isEqualTo(mapping.offenderNo)
        assertThat(createdMapping.label).isEqualTo(mapping.label)
      }

      @Test
      fun `can create with minimal data`() = runTest {
        webTestClient.post()
          .uri("/mapping/transactions")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              TransactionMappingDto(nomisTransactionId = 54321, dpsTransactionId = DPS_TRANSACTION_ID),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        val createdMapping = repository.findById(54321)!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.nomisTransactionId).isEqualTo(54321L)
        assertThat(createdMapping.dpsTransactionId).isEqualTo(UUID.fromString(DPS_TRANSACTION_ID))
        assertThat(createdMapping.mappingType).isEqualTo(TransactionMappingType.DPS_CREATED)
        assertThat(createdMapping.offenderNo).isNull()
        assertThat(createdMapping.nomisBookingId).isNull()
        assertThat(createdMapping.label).isNull()
      }

      @Test
      fun `can post and then get mapping`() {
        webTestClient.post()
          .uri("/mapping/transactions")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                {
                  "nomisTransactionId": 54555,
                  "dpsTransactionId": "$DPS_TRANSACTION_ID",
                  "offenderNo": "A1234AA",
                  "nomisBookingId": 1
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        webTestClient.get()
          .uri("/mapping/transactions/nomis-transaction-id/54555")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$DPS_TRANSACTION_ID")
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
          .uri("/mapping/transactions")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                {
                  "nomisTransactionId": 54321,
                  "dpsTransactionId": "$DPS_TRANSACTION_ID",
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
          .uri("/mapping/transactions")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "nomisTransactionId": 3,
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
          .uri("/mapping/transactions")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                {
                  "dpsTransactionId": "$DPS_TRANSACTION_ID",
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
        val dpsTransactionId = UUID.randomUUID().toString()
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/transactions")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              TransactionMappingDto(
                nomisTransactionId = existingMapping.nomisTransactionId,
                dpsTransactionId = dpsTransactionId,
                offenderNo = OFFENDER_NO,
                nomisBookingId = 1,
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
            .containsEntry("nomisTransactionId", existingMapping.nomisTransactionId.toInt())
            .containsEntry("dpsTransactionId", existingMapping.dpsTransactionId.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisTransactionId", existingMapping.nomisTransactionId.toInt())
            .containsEntry("dpsTransactionId", dpsTransactionId)
        }
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/transactions/batch")
  inner class CreateMappings {
    private var existingMapping: TransactionMapping = TransactionMapping(
      dpsTransactionId = generateUUID(1),
      nomisTransactionId = 50001L,
      offenderNo = OFFENDER_NO,
      nomisBookingId = 1,
      label = "2023-01-01T12:45:12",
      mappingType = TransactionMappingType.MIGRATED,
    )
    private val mappings = listOf(
      TransactionMappingDto(
        dpsTransactionId = "e52d7268-6e10-41a8-a0b9-000000000002",
        nomisTransactionId = 50002L,
        offenderNo = OFFENDER_NO,
        nomisBookingId = 2,
        mappingType = TransactionMappingType.DPS_CREATED,
      ),
      TransactionMappingDto(
        dpsTransactionId = "fd4e55a8-0805-439b-9e27-000000000003",
        nomisTransactionId = 50003L,
        offenderNo = OFFENDER_NO,
        nomisBookingId = 3,
        mappingType = TransactionMappingType.NOMIS_CREATED,
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
          .uri("/mapping/transactions/batch")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/transactions/batch")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/transactions/batch")
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
          .uri("/mapping/transactions/batch")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated

        val createdMapping1 =
          repository.findById(mappings[0].nomisTransactionId)!!

        assertThat(createdMapping1.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping1.nomisTransactionId).isEqualTo(mappings[0].nomisTransactionId)
        assertThat(createdMapping1.dpsTransactionId.toString()).isEqualTo(mappings[0].dpsTransactionId)
        assertThat(createdMapping1.nomisBookingId).isEqualTo(mappings[0].nomisBookingId)
        assertThat(createdMapping1.offenderNo).isEqualTo(mappings[0].offenderNo)
        assertThat(createdMapping1.mappingType).isEqualTo(mappings[0].mappingType)
        assertThat(createdMapping1.label).isEqualTo(mappings[0].label)

        val createdMapping2 = repository.findById(mappings[1].nomisTransactionId)!!

        assertThat(createdMapping2.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping2.nomisTransactionId).isEqualTo(mappings[1].nomisTransactionId)
        assertThat(createdMapping2.dpsTransactionId.toString()).isEqualTo(mappings[1].dpsTransactionId)
        assertThat(createdMapping2.nomisBookingId).isEqualTo(mappings[1].nomisBookingId)
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
          .uri("/mapping/transactions/batch")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                [
                    {
                      "nomisTransactionId": 54321,
                      "dpsTransactionId": "$DPS_TRANSACTION_ID",
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
        val dpsTransactionId = UUID.randomUUID().toString()
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/transactions/batch")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings + existingMapping.copy(dpsTransactionId = UUID.fromString(dpsTransactionId)),
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
            .containsEntry("nomisTransactionId", existingMapping.nomisTransactionId.toInt())
            .containsEntry("dpsTransactionId", existingMapping.dpsTransactionId.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisTransactionId", existingMapping.nomisTransactionId.toInt())
            .containsEntry("dpsTransactionId", dpsTransactionId)
        }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/transactions/{offenderNo}")
  inner class GetMappingByPrisoner {
    val dpsId1 = "85665bb9-ab28-458a-8386-b8cc91b311f7"
    val dpsId2 = DPS_TRANSACTION_ID2
    val dpsId3 = "e52d7268-6e10-41a8-a0b9-2319b32520d6"
    val dpsId4 = "fd4e55a8-0805-439b-9e27-647583b96e4e"

    private var mapping1: TransactionMapping = TransactionMapping(
      dpsTransactionId = UUID.fromString(dpsId2),
      nomisBookingId = 54321L,
      nomisTransactionId = 2L,
      offenderNo = "A1234KT",
      mappingType = TransactionMappingType.DPS_CREATED,
    )
    private var mapping2: TransactionMapping = TransactionMapping(
      dpsTransactionId = UUID.fromString(dpsId1),
      nomisBookingId = 11111L,
      nomisTransactionId = 1L,
      offenderNo = "A1234KT",
      mappingType = TransactionMappingType.DPS_CREATED,
    )
    private val prisonerMappings = listOf(
      TransactionMappingDto(
        dpsTransactionId = dpsId3,
        nomisTransactionId = 3L,
        offenderNo = "A1234KT",
        nomisBookingId = 54321L,
        label = "2023-01-01T12:45:12",
        mappingType = TransactionMappingType.MIGRATED,
      ),
      TransactionMappingDto(
        dpsTransactionId = dpsId4,
        nomisTransactionId = 4L,
        offenderNo = "A1234KT",
        nomisBookingId = 54321L,
        label = "2023-01-01T12:45:12",
        mappingType = TransactionMappingType.MIGRATED,
      ),
    )

    @BeforeEach
    fun setUp() = runTest {
      webTestClient.post()
        .uri("/mapping/transactions/batch")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(prisonerMappings))
        .exchange()
        .expectStatus().isCreated
      mapping1 = repository.save(mapping1)
      mapping2 = repository.save(mapping2)
      repository.save(
        TransactionMapping(
          dpsTransactionId = UUID.fromString("fd4e55a8-41ba-42ea-b5c4-404b453ad99b"),
          nomisBookingId = 9999L,
          nomisTransactionId = 5L,
          offenderNo = "A1111KT",
          mappingType = TransactionMappingType.MIGRATED,
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
          .uri("/mapping/transactions/A1234KT/all")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/transactions/A1234KT/all")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/transactions/A1234KT/all")
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
          .uri("/mapping/transactions/A9999KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("mappings.size()").isEqualTo(0)
      }

      @Test
      fun `will return all mappings for prisoner`() {
        webTestClient.get()
          .uri("/mapping/transactions/A1234KT/all")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("mappings.size()").isEqualTo(4)
          .jsonPath("mappings[0].dpsTransactionId").isEqualTo(dpsId1)
          .jsonPath("mappings[0].nomisTransactionId").isEqualTo(1)
          .jsonPath("mappings[0].nomisBookingId").isEqualTo(11111)
          .jsonPath("mappings[0].offenderNo").isEqualTo("A1234KT")
          .jsonPath("mappings[1].dpsTransactionId").isEqualTo(dpsId2)
          .jsonPath("mappings[1].nomisTransactionId").isEqualTo(2)
          .jsonPath("mappings[1].nomisBookingId").isEqualTo(54321L)
          .jsonPath("mappings[2].dpsTransactionId").isEqualTo(dpsId3)
          .jsonPath("mappings[2].nomisTransactionId").isEqualTo(3)
          .jsonPath("mappings[2].nomisBookingId").isEqualTo(54321L)
          .jsonPath("mappings[3].dpsTransactionId").isEqualTo(dpsId4)
          .jsonPath("mappings[3].nomisTransactionId").isEqualTo(4)
          .jsonPath("mappings[3].nomisBookingId").isEqualTo(54321L)
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/transactions/nomis-transaction-id/{transactionId}")
  inner class GetMappingByNomisId {
    lateinit var mapping: TransactionMapping
    lateinit var minimalDataMapping: TransactionMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        TransactionMapping(
          dpsTransactionId = UUID.fromString(DPS_TRANSACTION_ID2),
          nomisTransactionId = 54321L,
          offenderNo = OFFENDER_NO,
          nomisBookingId = 1,
          label = "2023-01-01T12:45:12",
          mappingType = TransactionMappingType.MIGRATED,
        ),
      )
      minimalDataMapping = repository.save(
        TransactionMapping(
          dpsTransactionId = UUID.randomUUID(),
          nomisTransactionId = 554433L,
          mappingType = TransactionMappingType.NOMIS_CREATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/transactions/nomis-transaction-id/${mapping.nomisTransactionId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/transactions/nomis-transaction-id/${mapping.nomisTransactionId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/transactions/nomis-transaction-id/${mapping.nomisTransactionId}")
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
          .uri("/mapping/transactions/nomis-transaction-id/9999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/transactions/nomis-transaction-id/${mapping.nomisTransactionId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisTransactionId").isEqualTo(mapping.nomisTransactionId)
          .jsonPath("dpsTransactionId").isEqualTo(mapping.dpsTransactionId.toString())
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("offenderNo").isEqualTo(mapping.offenderNo!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }

      @Test
      fun `will return minimal data when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/transactions/nomis-transaction-id/${minimalDataMapping.nomisTransactionId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisTransactionId").isEqualTo(minimalDataMapping.nomisTransactionId)
          .jsonPath("dpsTransactionId").isEqualTo(minimalDataMapping.dpsTransactionId.toString())
          .jsonPath("mappingType").isEqualTo(minimalDataMapping.mappingType.name)
          .jsonPath("label").doesNotExist()
          .jsonPath("offenderNo").doesNotExist()
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/nomis-transaction-id")
  inner class GetMappingsByNomisId {
    lateinit var mapping1: TransactionMapping
    lateinit var mapping2: TransactionMapping
    val transactionIds: List<Long> = listOf(54321L, 54322L)

    @BeforeEach
    fun setUp() = runTest {
      mapping1 = repository.save(
        TransactionMapping(
          dpsTransactionId = generateUUID(1),
          nomisTransactionId = 54321L,
          offenderNo = OFFENDER_NO,
          nomisBookingId = 1,
          label = "2023-01-01T12:45:12",
          mappingType = TransactionMappingType.MIGRATED,
        ),
      )
      mapping2 = repository.save(
        TransactionMapping(
          dpsTransactionId = generateUUID(2),
          nomisTransactionId = 54322L,
          offenderNo = OFFENDER_NO,
          nomisBookingId = 1,
          label = "2023-06-01T12:45:12",
          mappingType = TransactionMappingType.DPS_CREATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/transactions/nomis-transaction-id")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(transactionIds))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/transactions/nomis-transaction-id")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(transactionIds))
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/transactions/nomis-transaction-id")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(transactionIds))
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return 200 when no mappings exist`() {
        webTestClient.post()
          .uri("/mapping/transactions/nomis-transaction-id")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(listOf(99999)))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("[]")
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.post()
          .uri("/mapping/transactions/nomis-transaction-id")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(transactionIds))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$[0].nomisTransactionId").isEqualTo(mapping1.nomisTransactionId)
          .jsonPath("$[0].dpsTransactionId").isEqualTo(mapping1.dpsTransactionId.toString())
          .jsonPath("$[0].mappingType").isEqualTo(mapping1.mappingType.name)
          .jsonPath("$[0].label").isEqualTo(mapping1.label!!)
          .jsonPath("$[0].whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
          .jsonPath("$[1].nomisTransactionId").isEqualTo(mapping2.nomisTransactionId)
          .jsonPath("$[1].dpsTransactionId").isEqualTo(mapping2.dpsTransactionId.toString())
          .jsonPath("$[1].mappingType").isEqualTo(mapping2.mappingType.name)
          .jsonPath("$[1].label").isEqualTo(mapping2.label!!)
          .jsonPath("$[1].whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/transactions/dps-transaction-id/{dpsTransactionId}")
  inner class GetMappingByDpsId {
    lateinit var mapping1: TransactionMapping
    lateinit var mapping2: TransactionMapping
    lateinit var minimalDataMapping: TransactionMapping
    val dpsTransactionId1 = generateUUID(1)
    val dpsTransactionId2 = generateUUID(2)

    @BeforeEach
    fun setUp() {
      runTest {
        mapping1 = repository.save(
          TransactionMapping(
            dpsTransactionId = dpsTransactionId1,
            nomisTransactionId = 54321L,
            offenderNo = OFFENDER_NO,
            nomisBookingId = 1,
            label = "2023-01-01T12:45:12",
            mappingType = TransactionMappingType.MIGRATED,
          ),
        )
        mapping2 = repository.save(
          TransactionMapping(
            dpsTransactionId = dpsTransactionId2,
            nomisTransactionId = 54322L,
            offenderNo = OFFENDER_NO,
            nomisBookingId = 1,
            label = "2023-06-01T12:45:12",
            mappingType = TransactionMappingType.DPS_CREATED,
          ),
        )
        minimalDataMapping = repository.save(
          TransactionMapping(
            dpsTransactionId = UUID.randomUUID(),
            nomisTransactionId = 554433L,
            mappingType = TransactionMappingType.NOMIS_CREATED,
          ),
        )
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/${mapping1.dpsTransactionId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/${mapping1.dpsTransactionId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/${mapping1.dpsTransactionId}")
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
          .uri("/mapping/transactions/dps-transaction-id/00001111-0000-0000-0000-000011112222")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/${mapping1.dpsTransactionId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.dpsTransactionId").isEqualTo(dpsTransactionId1.toString())
          .jsonPath("$.nomisTransactionId").isEqualTo(mapping1.nomisTransactionId)
          .jsonPath("$.offenderNo").isEqualTo(OFFENDER_NO)
          .jsonPath("$.nomisBookingId").isEqualTo(mapping1.nomisBookingId!!)
          .jsonPath("$.mappingType").isEqualTo(mapping1.mappingType.name)
          .jsonPath("$.label").isEqualTo(mapping1.label!!)
          .jsonPath("$.whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }

      @Test
      fun `will return minimal data when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/${minimalDataMapping.dpsTransactionId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisTransactionId").isEqualTo(minimalDataMapping.nomisTransactionId)
          .jsonPath("dpsTransactionId").isEqualTo(minimalDataMapping.dpsTransactionId.toString())
          .jsonPath("mappingType").isEqualTo(minimalDataMapping.mappingType.name)
          .jsonPath("label").doesNotExist()
          .jsonPath("offenderNo").doesNotExist()
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @DisplayName("GET /mapping/transactions/migration-id/{migrationId}/count-by-prisoner")
  @Disabled("A migration may not be needed")
  @Nested
  inner class GetMappingCountByMigrationIdTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/transactions/migration-id/2022-01-01T00:00:00/count-by-prisoner")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/transactions/migration-id/2022-01-01T00:00:00/count-by-prisoner")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get transaction mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/transactions/migration-id/2022-01-01T00:00:00/count-by-prisoner")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get transaction mapping count success`() {
      (1L..5L).forEach {
        postCreateMappingRequest(
          it,
          generateUUIDs(it),
          OFFENDER_NO,
          1,
          label = "2022-01-01",
          mappingType = TransactionMappingType.MIGRATED,
        )
      }

      webTestClient.get().uri("/mapping/transactions/migration-id/2022-01-01/count-by-prisoner")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").isEqualTo(2) // i.e. 5 rows divided by average-transactions-per-prisoner
    }
  }

  @DisplayName("GET /mapping/transactions/migrated/latest")
  @Disabled("A migration may not be needed")
  @Nested
  inner class GeMappingMigratedLatestTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/transactions/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/transactions/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/transactions/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {
      webTestClient.post().uri("/mapping/transactions")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            TransactionMappingDto(
              nomisTransactionId = 10,
              dpsTransactionId = generateUUIDs(10),
              offenderNo = OFFENDER_NO,
              nomisBookingId = 1,
              label = "2022-01-01T00:00:00",
              mappingType = TransactionMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/transactions")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            TransactionMappingDto(
              nomisTransactionId = 20,
              dpsTransactionId = generateUUIDs(20),
              offenderNo = OFFENDER_NO,
              nomisBookingId = 1,
              label = "2022-01-02T00:00:00",
              mappingType = TransactionMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/transactions")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            TransactionMappingDto(
              nomisTransactionId = 1,
              dpsTransactionId = generateUUIDs(1),
              offenderNo = OFFENDER_NO,
              nomisBookingId = 2,
              label = "2022-01-02T10:00:00",
              mappingType = TransactionMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/transactions")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            TransactionMappingDto(
              nomisTransactionId = 99,
              dpsTransactionId = generateUUIDs(199),
              offenderNo = OFFENDER_NO,
              nomisBookingId = 1,
              label = "whatever",
              mappingType = TransactionMappingType.DPS_CREATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/transactions/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody(TransactionMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisTransactionId).isEqualTo(1)
      assertThat(mapping.dpsTransactionId).isEqualTo(generateUUIDs(1))
      assertThat(mapping.offenderNo).isEqualTo(OFFENDER_NO)
      assertThat(mapping.nomisBookingId).isEqualTo(2)
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo(TransactionMappingType.MIGRATED)
      assertThat(mapping.whenCreated)
        .isCloseTo(LocalDateTime.now(), Assertions.byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/transactions")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            TransactionMappingDto(
              nomisTransactionId = 77,
              dpsTransactionId = generateUUIDs(77),
              offenderNo = OFFENDER_NO,
              nomisBookingId = 1,
              label = "whatever",
              mappingType = TransactionMappingType.DPS_CREATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/transactions/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/transactions/nomis-transaction-id/{nomisTransactionId}")
  inner class DeleteMappingByNomisId {
    lateinit var mapping: TransactionMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        TransactionMapping(
          dpsTransactionId = UUID.fromString(DPS_TRANSACTION_ID2),
          nomisTransactionId = 54321L,
          offenderNo = OFFENDER_NO,
          nomisBookingId = 1,
          label = "2023-01-01T12:45:12",
          mappingType = TransactionMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/transactions/nomis-transaction-id/${mapping.nomisTransactionId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/transactions/nomis-transaction-id/${mapping.nomisTransactionId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/transactions/nomis-transaction-id/${mapping.nomisTransactionId}")
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
          .uri("/mapping/transactions/nomis-transaction-id/99999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/transactions/nomis-transaction-id/${mapping.nomisTransactionId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/transactions/nomis-transaction-id/${mapping.nomisTransactionId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/transactions/nomis-transaction-id/${mapping.nomisTransactionId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/transactions/dps-transaction-id/{dpsTransactionId}")
  inner class DeleteMappingByDpsId {
    lateinit var mapping: TransactionMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        TransactionMapping(
          dpsTransactionId = UUID.fromString(DPS_TRANSACTION_ID2),
          nomisTransactionId = 54321L,
          offenderNo = OFFENDER_NO,
          nomisBookingId = 1,
          label = "2023-01-01T12:45:12",
          mappingType = TransactionMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/transactions/dps-transaction-id/${mapping.dpsTransactionId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/transactions/dps-transaction-id/${mapping.dpsTransactionId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/transactions/dps-transaction-id/${mapping.dpsTransactionId}")
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
          .uri("/mapping/transactions/dps-transaction-id/00001111-0000-0000-0000-000011112222")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/${mapping.dpsTransactionId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/transactions/dps-transaction-id/${mapping.dpsTransactionId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/${mapping.dpsTransactionId}")
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
          .uri("/mapping/transactions/merge/from/A1234AA/to/A1234BB")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/transactions/merge/from/A1234AA/to/A1234BB")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/transactions/merge/from/A1234AA/to/A1234BB")
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
            TransactionMapping(
              dpsTransactionId = UUID.fromString(dps1),
              nomisTransactionId = 54321L,
              offenderNo = "A1234AA",
              nomisBookingId = 1,
              mappingType = TransactionMappingType.MIGRATED,
            ),
          )
          repository.save(
            TransactionMapping(
              dpsTransactionId = UUID.fromString(dps2),
              nomisTransactionId = 54322L,
              offenderNo = "A1234BB",
              nomisBookingId = 2,
              mappingType = TransactionMappingType.NOMIS_CREATED,
            ),
          )
          repository.save(
            TransactionMapping(
              dpsTransactionId = UUID.fromString(dps3),
              nomisTransactionId = 54323L,
              offenderNo = "A1234BB",
              nomisBookingId = 2,
              mappingType = TransactionMappingType.NOMIS_CREATED,
            ),
          )
        }
      }

      @Test
      fun `Merge success`() = runTest {
        webTestClient.put().uri("/mapping/transactions/merge/from/A1234AA/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        // first record has changed
        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.offenderNo").isEqualTo("B5678BB")

        // second has not
        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.offenderNo").isEqualTo("A1234BB")
      }

      @Test
      fun `Nothing happens if not found`() = runTest {
        webTestClient.put().uri("/mapping/transactions/merge/from/A9999AA/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.nomisTransactionId").isEqualTo(54321)
          .jsonPath("$.offenderNo").isEqualTo("A1234AA")
          .jsonPath("$.nomisBookingId").isEqualTo(1)

        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.nomisTransactionId").isEqualTo(54322)
          .jsonPath("$.offenderNo").isEqualTo("A1234BB")
          .jsonPath("$.nomisBookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.nomisTransactionId").isEqualTo(54323)
          .jsonPath("$.offenderNo").isEqualTo("A1234BB")
          .jsonPath("$.nomisBookingId").isEqualTo(2)
      }

//      @Test
//      fun `Merge success - multiple candidates`() = runTest {
//        webTestClient.put().uri("/mapping/transactions/merge/from/A1234BB/to/B5678BB")
//          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
//          .exchange()
//          .expectStatus().isOk
//
//        webTestClient.get()
//          .uri("/mapping/transactions/dps-transaction-id/$dps1")
//          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
//          .exchange()
//          .expectStatus().isOk
//          .expectBody()
//          .jsonPath("$[0].nomisTransactionId").isEqualTo(54321)
//          .jsonPath("$[0].offenderNo").isEqualTo("A1234AA")
//          .jsonPath("$[0].nomisBookingId").isEqualTo(1)
//
//        webTestClient.get()
//          .uri("/mapping/transactions/dps-transaction-id/$dps2")
//          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
//          .exchange()
//          .expectStatus().isOk
//          .expectBody()
//          .jsonPath("$[0].nomisTransactionId").isEqualTo(54322)
//          .jsonPath("$[0].offenderNo").isEqualTo("B5678BB")
//          .jsonPath("$[0].nomisBookingId").isEqualTo(2)
//
//        webTestClient.get()
//          .uri("/mapping/transactions/dps-transaction-id/$dps3")
//          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
//          .exchange()
//          .expectStatus().isOk
//          .expectBody()
//          .jsonPath("$[0].nomisTransactionId").isEqualTo(54323)
//          .jsonPath("$[0].offenderNo").isEqualTo("B5678BB")
//          .jsonPath("$[0].nomisBookingId").isEqualTo(2)
//      }
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
          .uri("/mapping/transactions/merge/booking-id/333/to/A1234BB")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/transactions/merge/booking-id/333/to/A1234BB")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/transactions/merge/booking-id/333/to/A1234BB")
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
            TransactionMapping(
              dpsTransactionId = UUID.fromString(dps1),
              nomisTransactionId = 54321L,
              offenderNo = "A1234AA",
              nomisBookingId = 1,
              mappingType = TransactionMappingType.MIGRATED,
            ),
          )
          repository.save(
            TransactionMapping(
              dpsTransactionId = UUID.fromString(dps2),
              nomisTransactionId = 54322L,
              offenderNo = "A1234BB",
              nomisBookingId = 2,
              mappingType = TransactionMappingType.NOMIS_CREATED,
            ),
          )
          repository.save(
            TransactionMapping(
              dpsTransactionId = UUID.fromString(dps3),
              nomisTransactionId = 54323L,
              offenderNo = "A1234BB",
              nomisBookingId = 2,
              mappingType = TransactionMappingType.NOMIS_CREATED,
            ),
          )
        }
      }

      @Test
      fun `Move success`() = runTest {
        webTestClient.put().uri("/mapping/transactions/merge/booking-id/1/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  { 
    "nomisTransactionId": 54321,
    "dpsTransactionId": "$dps1",
    "nomisBookingId": 1
  }
]""",
          )

        // Check first record has changed
        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.offenderNo").isEqualTo("B5678BB")

        // second has not
        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.offenderNo").isEqualTo("A1234BB")
      }

      @Test
      fun `Nothing happens if not found`() = runTest {
        webTestClient.put().uri("/mapping/transactions/merge/booking-id/999/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("[]")

        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.nomisTransactionId").isEqualTo(54321)
          .jsonPath("$.offenderNo").isEqualTo("A1234AA")
          .jsonPath("$.nomisBookingId").isEqualTo(1)

        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.nomisTransactionId").isEqualTo(54322)
          .jsonPath("$.offenderNo").isEqualTo("A1234BB")
          .jsonPath("$.nomisBookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.nomisTransactionId").isEqualTo(54323)
          .jsonPath("$.offenderNo").isEqualTo("A1234BB")
          .jsonPath("$.nomisBookingId").isEqualTo(2)
      }

      @Test
      fun `Move success - multiple candidates`() = runTest {
        webTestClient.put().uri("/mapping/transactions/merge/booking-id/2/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  { 
    "nomisTransactionId": 54322,
    "dpsTransactionId": "$dps2",
    "nomisBookingId": 2
  },
  { 
    "nomisTransactionId": 54323,
    "dpsTransactionId": "$dps3",
    "nomisBookingId": 2
  }
]""",
          )

        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.nomisTransactionId").isEqualTo(54321)
          .jsonPath("$.offenderNo").isEqualTo("A1234AA")
          .jsonPath("$.nomisBookingId").isEqualTo(1)

        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.nomisTransactionId").isEqualTo(54322)
          .jsonPath("$.offenderNo").isEqualTo("B5678BB")
          .jsonPath("$.nomisBookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.nomisTransactionId").isEqualTo(54323)
          .jsonPath("$.offenderNo").isEqualTo("B5678BB")
          .jsonPath("$.nomisBookingId").isEqualTo(2)
      }
    }

    @Nested
    inner class HappyPathWithPreviousMerge {
      private val dps1 = "00000000-1111-2222-3333-000088880001"
      private val dps2 = "00000000-1111-2222-3333-000088880002"
      private val dps3 = "00000000-1111-2222-3333-000088880003"

      @BeforeEach
      fun setUp() {
        runTest {
          repository.save(
            TransactionMapping(
              dpsTransactionId = UUID.fromString(dps1),
              nomisTransactionId = 54321L,
              offenderNo = "A1234AA",
              nomisBookingId = 1,
              mappingType = TransactionMappingType.MIGRATED,
            ),
          )
          repository.save(
            TransactionMapping(
              dpsTransactionId = UUID.fromString(dps2),
              nomisTransactionId = 54322L,
              offenderNo = "A1234BB",
              nomisBookingId = 2,
              mappingType = TransactionMappingType.NOMIS_CREATED,
            ),
          )
          repository.save(
            TransactionMapping(
              dpsTransactionId = UUID.fromString(dps3),
              nomisTransactionId = 54323L,
              offenderNo = "A1234BB",
              nomisBookingId = 2,
              mappingType = TransactionMappingType.NOMIS_CREATED,
            ),
          )
        }
      }

      @Test
      fun `Move success`() = runTest {
        webTestClient.put().uri("/mapping/transactions/merge/booking-id/1/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  { 
    "nomisTransactionId": 54321,
    "dpsTransactionId": "$dps1",
    "nomisBookingId": 1
  }
]""",
          )

        // Check dps1 record has changed
        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.offenderNo").isEqualTo("B5678BB")

        // dps2 has not
        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.offenderNo").isEqualTo("A1234BB")
      }

      @Test
      fun `Nothing happens if not found`() = runTest {
        webTestClient.put().uri("/mapping/transactions/merge/booking-id/999/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("[]")

        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.nomisTransactionId").isEqualTo(54321)
          .jsonPath("$.offenderNo").isEqualTo("A1234AA")
          .jsonPath("$.nomisBookingId").isEqualTo(1)

        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.nomisTransactionId").isEqualTo(54322)
          .jsonPath("$.offenderNo").isEqualTo("A1234BB")
          .jsonPath("$.nomisBookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/transactions/dps-transaction-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.nomisTransactionId").isEqualTo(54323)
          .jsonPath("$.offenderNo").isEqualTo("A1234BB")
          .jsonPath("$.nomisBookingId").isEqualTo(2)
      }
    }
  }
}
