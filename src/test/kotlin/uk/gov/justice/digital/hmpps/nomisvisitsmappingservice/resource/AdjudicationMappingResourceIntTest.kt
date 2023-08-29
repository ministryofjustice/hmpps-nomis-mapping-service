package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationAllMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationHearingMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationPunishmentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.isDuplicateMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationHearingMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationMappingType.ADJUDICATION_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationPunishmentMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.AdjudicationHearingMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.AdjudicationMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.AdjudicationPunishmentMappingRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val ADJUDICATION_NUMBER = 4444L
private const val CHARGE_SEQ = 2
private const val CHARGE_NUMBER = "4444-2"
private const val BOOKING_ID = 9876L
private val HEARINGS = listOf("123" to 321L, "456" to 654L, "789" to 654L)
private val PUNISHMENTS = listOf("123" to 2, "456" to 3)

@OptIn(ExperimentalCoroutinesApi::class)
class AdjudicationMappingResourceIntTest : IntegrationTestBase() {

  @SpyBean
  lateinit var repository: AdjudicationMappingRepository

  @SpyBean
  lateinit var hearingRepository: AdjudicationHearingMappingRepository

  @SpyBean
  lateinit var punishmentRepository: AdjudicationPunishmentMappingRepository

  private fun createMapping(
    adjudicationNumber: Long = ADJUDICATION_NUMBER,
    chargeSequence: Int = CHARGE_SEQ,
    chargeNumber: String = CHARGE_NUMBER,
    label: String = "2022-01-01",
    mappingType: String = ADJUDICATION_CREATED.name,
  ): AdjudicationMappingDto = AdjudicationMappingDto(
    adjudicationNumber = adjudicationNumber,
    chargeSequence = chargeSequence,
    chargeNumber = chargeNumber,
    label = label,
    mappingType = mappingType,
  )

  private fun createAllMapping(
    adjudicationNumber: Long = ADJUDICATION_NUMBER,
    chargeSequence: Int = CHARGE_SEQ,
    chargeNumber: String = CHARGE_NUMBER,
    label: String = "2022-01-01",
    mappingType: String = ADJUDICATION_CREATED.name,
    hearingIdPairs: List<Pair<String, Long>> = HEARINGS,
    bookingId: Long = BOOKING_ID,
    punishmentIdPairs: List<Pair<String, Int>> = PUNISHMENTS,
  ): AdjudicationAllMappingDto = AdjudicationAllMappingDto(
    label = label,
    mappingType = mappingType,
    adjudicationId = createMapping(
      adjudicationNumber = adjudicationNumber,
      chargeSequence = chargeSequence,
      chargeNumber = chargeNumber,
    ),
    hearings = hearingIdPairs.map {
      AdjudicationHearingMappingDto(
        dpsHearingId = it.first,
        nomisHearingId = it.second,
      )
    },
    punishments = punishmentIdPairs.map {
      AdjudicationPunishmentMappingDto(
        dpsPunishmentId = it.first,
        nomisBookingId = bookingId,
        nomisSanctionSequence = it.second,
      )
    },
  )

  private fun postCreateMappingRequest(
    adjudicationNumber: Long = ADJUDICATION_NUMBER,
    chargeSequence: Int = CHARGE_SEQ,
    chargeNumber: String = CHARGE_NUMBER,
    label: String = "2022-01-01",
    mappingType: String = ADJUDICATION_CREATED.name,
  ) {
    webTestClient.post().uri("/mapping/adjudications")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createMapping(
            adjudicationNumber = adjudicationNumber,
            chargeSequence = chargeSequence,
            chargeNumber = chargeNumber,
            label = label,
            mappingType = mappingType,
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  @BeforeEach
  fun deleteData() = runBlocking {
    repository.deleteAll()
    hearingRepository.deleteAll()
    punishmentRepository.deleteAll()
  }

  @DisplayName("POST /mapping/adjudications")
  @Nested
  inner class CreateMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/adjudications")
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create visit forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create mapping success - ADJUDICATION_CREATED`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            // language=json
            """{
            "adjudicationNumber" : $ADJUDICATION_NUMBER,
            "chargeSequence" : $CHARGE_SEQ,
            "chargeNumber": "$CHARGE_NUMBER"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val createdMappingByNomisId = webTestClient.get()
        .uri("/mapping/adjudications/adjudication-number/$ADJUDICATION_NUMBER/charge-sequence/$CHARGE_SEQ")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AdjudicationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(createdMappingByNomisId.adjudicationNumber).isEqualTo(ADJUDICATION_NUMBER)
      assertThat(createdMappingByNomisId.chargeSequence).isEqualTo(CHARGE_SEQ)
      assertThat(createdMappingByNomisId.chargeNumber).isEqualTo(CHARGE_NUMBER)
      assertThat(createdMappingByNomisId.mappingType).isEqualTo("ADJUDICATION_CREATED")

      val createdMappingByDpsId =
        webTestClient.get().uri("/mapping/adjudications/charge-number/{chargeNumber}", CHARGE_NUMBER)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(AdjudicationMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(createdMappingByDpsId.adjudicationNumber).isEqualTo(ADJUDICATION_NUMBER)
      assertThat(createdMappingByDpsId.chargeSequence).isEqualTo(CHARGE_SEQ)
      assertThat(createdMappingByDpsId.chargeNumber).isEqualTo(CHARGE_NUMBER)
      assertThat(createdMappingByDpsId.mappingType).isEqualTo("ADJUDICATION_CREATED")
    }

    @Test
    fun `create mapping success - MIGRATED`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            // language=json
            """{
                "adjudicationNumber": $ADJUDICATION_NUMBER,
                "chargeSequence": $CHARGE_SEQ,
                "chargeNumber": "$CHARGE_NUMBER",
                "label": "2023-04-20",
                "mappingType": "MIGRATED"
              }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val createdMappingByDpsId =
        webTestClient.get().uri("/mapping/adjudications/charge-number/{chargeNumber}", CHARGE_NUMBER)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(AdjudicationMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(createdMappingByDpsId.adjudicationNumber).isEqualTo(ADJUDICATION_NUMBER)
      assertThat(createdMappingByDpsId.label).isEqualTo("2023-04-20")
      assertThat(createdMappingByDpsId.mappingType).isEqualTo("MIGRATED")
    }

    @Test
    fun `create mapping failure - adjudication exists`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            // language=json
            """{
            "adjudicationNumber" : $ADJUDICATION_NUMBER,
            "chargeSequence": $CHARGE_SEQ,
            "chargeNumber": "$CHARGE_NUMBER",
            "label"                 : "2023-04-20",
            "mappingType"           : "MIGRATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            // language=json
            """{
            "adjudicationNumber" : $ADJUDICATION_NUMBER,
            "chargeSequence": $CHARGE_SEQ,
            "chargeNumber": "$CHARGE_NUMBER",
            "label"                 : "2023-04-25",
            "mappingType"           : "MIGRATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isDuplicateMapping

      val createdMappingByDpsId =
        webTestClient.get().uri("/mapping/adjudications/charge-number/{chargeNumber}", CHARGE_NUMBER)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(AdjudicationMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(createdMappingByDpsId.adjudicationNumber).isEqualTo(ADJUDICATION_NUMBER)
      assertThat(createdMappingByDpsId.label).isEqualTo("2023-04-20")
      assertThat(createdMappingByDpsId.mappingType).isEqualTo("MIGRATED")
    }

    @Test
    fun `create mapping failure - Duplicate db error`() = runTest {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            // language=json
            """{
            "adjudicationNumber" : $ADJUDICATION_NUMBER,
            "chargeSequence": $CHARGE_SEQ,
            "chargeNumber": "$CHARGE_NUMBER",
            "label"                 : "2023-04-25",
            "mappingType"           : "MIGRATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      // Emulate calling service simultaneously twice by disabling the duplicate check
      // Note: the spy is automatically reset by ResetMocksTestExecutionListener
      whenever(repository.findById(CHARGE_NUMBER)).thenReturn(null)

      val responseBody =
        webTestClient.post().uri("/mapping/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              // language=json
              """{
            "adjudicationNumber" : $ADJUDICATION_NUMBER,
            "chargeSequence": $CHARGE_SEQ,
            "chargeNumber": "$CHARGE_NUMBER",
            "label"                 : "2023-04-25",
            "mappingType"           : "MIGRATED"
          }""",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<AdjudicationMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Adjudication mapping already exists, detected by org.springframework.dao.DuplicateKeyException")
        assertThat(errorCode).isEqualTo(1409)
      }
    }
  }

  @DisplayName("POST /mapping/adjudications/all")
  @Nested
  inner class CreateAllMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/adjudications/all")
        .body(BodyInserters.fromValue(createAllMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/adjudications/all")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createAllMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create visit forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/adjudications/all")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createAllMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `will create mapping when no hearings or punishments`(): Unit = runBlocking {
      webTestClient.post().uri("/mapping/adjudications/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            // language=json
            """{
              "adjudicationId": {
                "adjudicationNumber" : $ADJUDICATION_NUMBER,
                "chargeSequence" : $CHARGE_SEQ,
                "chargeNumber": "$CHARGE_NUMBER"
              },
              "hearings": [],
              "punishments": [],
              "label": "2023-04-20",
              "mappingType": "MIGRATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val createdMappingByNomisId = webTestClient.get()
        .uri("/mapping/adjudications/adjudication-number/$ADJUDICATION_NUMBER/charge-sequence/$CHARGE_SEQ")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AdjudicationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(createdMappingByNomisId.adjudicationNumber).isEqualTo(ADJUDICATION_NUMBER)
      assertThat(createdMappingByNomisId.chargeSequence).isEqualTo(CHARGE_SEQ)
      assertThat(createdMappingByNomisId.chargeNumber).isEqualTo(CHARGE_NUMBER)
      assertThat(createdMappingByNomisId.mappingType).isEqualTo("MIGRATED")
      assertThat(createdMappingByNomisId.label).isEqualTo("2023-04-20")

      val createdMappingByDpsId =
        webTestClient.get().uri("/mapping/adjudications/charge-number/{chargeNumber}", CHARGE_NUMBER)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(AdjudicationMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(createdMappingByDpsId.adjudicationNumber).isEqualTo(ADJUDICATION_NUMBER)
      assertThat(createdMappingByDpsId.chargeSequence).isEqualTo(CHARGE_SEQ)
      assertThat(createdMappingByDpsId.chargeNumber).isEqualTo(CHARGE_NUMBER)
      assertThat(createdMappingByDpsId.mappingType).isEqualTo("MIGRATED")
      assertThat(createdMappingByDpsId.label).isEqualTo("2023-04-20")

      assertThat(hearingRepository.findAll().count()).isEqualTo(0)
      assertThat(punishmentRepository.findAll().count()).isEqualTo(0)
    }

    @Test
    fun `will create mapping with hearings and punishments`() = runBlocking {
      webTestClient.post().uri("/mapping/adjudications/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            // language=json
            """{
              "adjudicationId": {
                "adjudicationNumber" : $ADJUDICATION_NUMBER,
                "chargeSequence" : $CHARGE_SEQ,
                "chargeNumber": "$CHARGE_NUMBER"
              },
              "hearings": [
                {
                  "dpsHearingId": "123",
                  "nomisHearingId": 321
                },
                {
                  "dpsHearingId": "456",
                  "nomisHearingId": 654
                },
                {
                  "dpsHearingId": "789",
                  "nomisHearingId": 654
                }
              ],
              "punishments": [
                {
                  "dpsPunishmentId": "123",
                  "nomisBookingId": 9876,
                  "nomisSanctionSequence": 2
                },
                {
                  "dpsPunishmentId": "456",
                  "nomisBookingId": 9876,
                  "nomisSanctionSequence": 3
                }
              ],
              "label": "2023-04-20",
              "mappingType": "MIGRATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val createdMappingByNomisId = webTestClient.get()
        .uri("/mapping/adjudications/adjudication-number/$ADJUDICATION_NUMBER/charge-sequence/$CHARGE_SEQ")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AdjudicationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(createdMappingByNomisId.adjudicationNumber).isEqualTo(ADJUDICATION_NUMBER)
      assertThat(createdMappingByNomisId.chargeSequence).isEqualTo(CHARGE_SEQ)
      assertThat(createdMappingByNomisId.chargeNumber).isEqualTo(CHARGE_NUMBER)
      assertThat(createdMappingByNomisId.mappingType).isEqualTo("MIGRATED")
      assertThat(createdMappingByNomisId.label).isEqualTo("2023-04-20")

      val createdMappingByDpsId =
        webTestClient.get().uri("/mapping/adjudications/charge-number/{chargeNumber}", CHARGE_NUMBER)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(AdjudicationMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(createdMappingByDpsId.adjudicationNumber).isEqualTo(ADJUDICATION_NUMBER)
      assertThat(createdMappingByDpsId.chargeSequence).isEqualTo(CHARGE_SEQ)
      assertThat(createdMappingByDpsId.chargeNumber).isEqualTo(CHARGE_NUMBER)
      assertThat(createdMappingByDpsId.mappingType).isEqualTo("MIGRATED")
      assertThat(createdMappingByDpsId.label).isEqualTo("2023-04-20")

      assertThat(hearingRepository.findAll().count()).isEqualTo(3)
      assertThat(punishmentRepository.findAll().count()).isEqualTo(2)

      val hearings = hearingRepository.findAll().toList()

      assertThat(hearings[0].dpsHearingId).isEqualTo("123")
      assertThat(hearings[0].nomisHearingId).isEqualTo(321)
      assertThat(hearings[1].dpsHearingId).isEqualTo("456")
      assertThat(hearings[1].nomisHearingId).isEqualTo(654)
      assertThat(hearings[2].dpsHearingId).isEqualTo("789")
      assertThat(hearings[2].nomisHearingId).isEqualTo(654)

      hearingRepository.findAll().collect {
        assertThat(it.label).isEqualTo("2023-04-20")
        assertThat(it.mappingType).isEqualTo(MIGRATED)
      }

      val punishments = punishmentRepository.findAll().toList()

      assertThat(punishments[0].dpsPunishmentId).isEqualTo("123")
      assertThat(punishments[0].nomisBookingId).isEqualTo(9876)
      assertThat(punishments[0].nomisSanctionSequence).isEqualTo(2)
      assertThat(punishments[1].dpsPunishmentId).isEqualTo("456")
      assertThat(punishments[1].nomisBookingId).isEqualTo(9876)
      assertThat(punishments[1].nomisSanctionSequence).isEqualTo(3)

      punishmentRepository.findAll().collect {
        assertThat(it.label).isEqualTo("2023-04-20")
        assertThat(it.mappingType).isEqualTo(MIGRATED)
      }
    }

    @Test
    fun `create mapping failure - adjudication exists`() {
      webTestClient.post().uri("/mapping/adjudications/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            // language=json
            """{
              "adjudicationId": {
                "adjudicationNumber" : $ADJUDICATION_NUMBER,
                "chargeSequence" : $CHARGE_SEQ,
                "chargeNumber": "$CHARGE_NUMBER"
              },
              "hearings": [],
              "punishments": [],
              "label": "2023-04-20",
              "mappingType": "MIGRATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/adjudications/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            // language=json
            """{
              "adjudicationId": {
                "adjudicationNumber" : $ADJUDICATION_NUMBER,
                "chargeSequence" : $CHARGE_SEQ,
                "chargeNumber": "$CHARGE_NUMBER"
              },
              "hearings": [],
              "punishments": [],
              "label": "2023-04-21",
              "mappingType": "MIGRATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isDuplicateMapping

      val createdMappingByDpsId =
        webTestClient.get().uri("/mapping/adjudications/charge-number/{chargeNumber}", CHARGE_NUMBER)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(AdjudicationMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(createdMappingByDpsId.adjudicationNumber).isEqualTo(ADJUDICATION_NUMBER)
      assertThat(createdMappingByDpsId.label).isEqualTo("2023-04-20")
      assertThat(createdMappingByDpsId.mappingType).isEqualTo("MIGRATED")
    }
  }

  @DisplayName("GET /mapping/adjudications/adjudication-number/{adjudicationNumber}/charge-sequence/{chargeSequence}")
  @Nested
  inner class GetMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get()
        .uri("/mapping/adjudications/adjudication-number/$ADJUDICATION_NUMBER/charge-sequence/$CHARGE_SEQ")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get()
        .uri("/mapping/adjudications/adjudication-number/$ADJUDICATION_NUMBER/charge-sequence/$CHARGE_SEQ")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get()
        .uri("/mapping/adjudications/adjudication-number/$ADJUDICATION_NUMBER/charge-sequence/$CHARGE_SEQ")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get()
        .uri("/mapping/adjudications/adjudication-number/$ADJUDICATION_NUMBER/charge-sequence/$CHARGE_SEQ")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AdjudicationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.adjudicationNumber).isEqualTo(ADJUDICATION_NUMBER)
    }

    @Test
    fun `mapping not found`() {
      webTestClient.get().uri("/mapping/adjudications/adjudication-number/$ADJUDICATION_NUMBER/charge-sequence/765")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).isEqualTo("Not Found: adjudicationNumber=4444, chargeSequence=765")
        }
      webTestClient.get().uri("/mapping/adjudications/adjudication-number/765/charge-sequence/$CHARGE_SEQ")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).isEqualTo("Not Found: adjudicationNumber=765, chargeSequence=2")
        }
    }
  }

  @DisplayName("GET /mapping/adjudications/migrated/latest")
  @Nested
  inner class GeMappingMigratedLatestTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/adjudications/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/adjudications/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/adjudications/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              adjudicationNumber = 10,
              label = "2022-01-01T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              adjudicationNumber = 20,
              chargeSequence = 1,
              chargeNumber = "20/1",
              label = "2022-01-02T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              adjudicationNumber = 1,
              chargeSequence = 2,
              chargeNumber = "1/2",
              label = "2022-01-02T10:00:00",
              mappingType = AdjudicationMappingType.MIGRATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              adjudicationNumber = 199,
              chargeSequence = 2,
              chargeNumber = "199/2",
              label = "whatever",
              mappingType = ADJUDICATION_CREATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/adjudications/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AdjudicationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.adjudicationNumber).isEqualTo(1)
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo("MIGRATED")
      assertThat(mapping.whenCreated)
        .isCloseTo(LocalDateTime.now(), Assertions.byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              adjudicationNumber = 77,
              label = "whatever",
              mappingType = ADJUDICATION_CREATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/adjudications/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }

  @DisplayName("GET /mapping/adjudications")
  @Nested
  inner class GetAllMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/adjudications")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      postCreateMappingRequest(201, chargeNumber = "201/1")
      postCreateMappingRequest(202, chargeNumber = "202/1")

      val mapping = webTestClient.get().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody<List<AdjudicationMappingDto>>()
        .returnResult().responseBody!!

      assertThat(mapping[0].adjudicationNumber).isEqualTo(201)
      assertThat(mapping[1].adjudicationNumber).isEqualTo(202)
      assertThat(mapping).hasSize(2)
    }
  }

  @DisplayName("DELETE /mapping/adjudications/{adjudicationNumber}")
  @Nested
  inner class DeleteMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/adjudications/999")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/adjudications/charge-number/{chargeNumber}", CHARGE_NUMBER)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/adjudications/charge-number/{chargeNumber}", CHARGE_NUMBER)
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete specific mapping success`() {
      // create mapping
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      // it is present after creation by adjudication id
      webTestClient.get().uri("/mapping/adjudications/charge-number/{chargeNumber}", CHARGE_NUMBER)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk

      // delete mapping
      webTestClient.delete().uri("/mapping/adjudications/charge-number/{chargeNumber}", CHARGE_NUMBER)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent

      // no longer present by charge number
      webTestClient.get().uri("/mapping/adjudications/charge-number/{chargeNumber}", CHARGE_NUMBER)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `delete is idempotent`() {
      // create mapping
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      // delete mapping
      webTestClient.delete().uri("/mapping/adjudications/charge-number/{chargeNumber}", CHARGE_NUMBER)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent

      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/adjudications/charge-number/{chargeNumber}", CHARGE_NUMBER)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @DisplayName("DELETE /mapping/adjudications/all/migration-id/{migrationId}")
  @Nested
  inner class DeleteAllByMigrationIdMappingTest {
    @BeforeEach
    fun setUp() {
      webTestClient.post().uri("/mapping/adjudications/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createAllMapping(label = "2022-01-01")))
        .exchange()
        .expectStatus().isCreated
      webTestClient.post().uri("/mapping/adjudications/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createAllMapping(
              label = "2022-01-02",
              chargeNumber = "$ADJUDICATION_NUMBER-99",
              adjudicationNumber = ADJUDICATION_NUMBER,
              chargeSequence = CHARGE_SEQ + 99,
              bookingId = BOOKING_ID + 1,
              hearingIdPairs = listOf("1239" to 3219L, "4569" to 6549L, "7899" to 6549L),
              punishmentIdPairs = listOf("1239" to 98769, "4569" to 98759),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/adjudications/all/migration-id/2022-01-01")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/adjudications/all/migration-id/2022-01-01")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/adjudications/all/migration-id/2022-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete all mapping for migration`(): Unit = runBlocking {
      assertThat(repository.findAll().count()).isEqualTo(2)
      assertThat(hearingRepository.findAll().count()).isEqualTo(6)
      assertThat(punishmentRepository.findAll().count()).isEqualTo(4)

      webTestClient.delete().uri("/mapping/adjudications/all/migration-id/2022-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent

      assertThat(repository.findAll().count()).isEqualTo(1)
      assertThat(hearingRepository.findAll().count()).isEqualTo(3)
      assertThat(punishmentRepository.findAll().count()).isEqualTo(2)

      webTestClient.delete().uri("/mapping/adjudications/all/migration-id/2022-01-02")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent

      assertThat(repository.findAll().count()).isEqualTo(0)
      assertThat(hearingRepository.findAll().count()).isEqualTo(0)
      assertThat(punishmentRepository.findAll().count()).isEqualTo(0)
    }
  }

  @DisplayName("DELETE /mapping/adjudications/all")
  @Nested
  inner class DeleteAllMappingTest {
    @BeforeEach
    fun setUp() = runTest {
      repository.save(
        AdjudicationMapping(
          chargeNumber = "111111-1",
          chargeSequence = 1,
          adjudicationNumber = 111111,
          label = "2022-01-01",
          mappingType = MIGRATED,
        ),
      )
      hearingRepository.save(
        AdjudicationHearingMapping(
          dpsHearingId = "111",
          nomisHearingId = 111,
          label = "2022-01-01",
          mappingType = MIGRATED,
        ),
      )
      punishmentRepository.save(
        AdjudicationPunishmentMapping(
          dpsPunishmentId = "111",
          nomisBookingId = 111,
          nomisSanctionSequence = 1,
          label = "2022-01-01",
          mappingType = MIGRATED,
        ),
      )

      repository.save(
        AdjudicationMapping(
          chargeNumber = "222222-1",
          chargeSequence = 1,
          adjudicationNumber = 222222,
          label = "2022-02-02",
          mappingType = MIGRATED,
        ),
      )
      hearingRepository.save(
        AdjudicationHearingMapping(
          dpsHearingId = "222",
          nomisHearingId = 222,
          label = "2022-02-02",
          mappingType = MIGRATED,
        ),
      )
      punishmentRepository.save(
        AdjudicationPunishmentMapping(
          dpsPunishmentId = "222",
          nomisBookingId = 222,
          nomisSanctionSequence = 2,
          label = "2022-01-01",
          mappingType = MIGRATED,
        ),
      )
      repository.save(
        AdjudicationMapping(
          chargeNumber = "333333-1",
          chargeSequence = 1,
          adjudicationNumber = 333333,
          label = null,
          mappingType = ADJUDICATION_CREATED,
        ),
      )
      hearingRepository.save(
        AdjudicationHearingMapping(
          dpsHearingId = "333",
          nomisHearingId = 333,
          label = null,
          mappingType = ADJUDICATION_CREATED,
        ),
      )
      punishmentRepository.save(
        AdjudicationPunishmentMapping(
          dpsPunishmentId = "333",
          nomisBookingId = 333,
          nomisSanctionSequence = 3,
          label = null,
          mappingType = ADJUDICATION_CREATED,
        ),
      )

      assertThat(repository.findAll().count()).isEqualTo(3)
      assertThat(hearingRepository.findAll().count()).isEqualTo(3)
      assertThat(punishmentRepository.findAll().count()).isEqualTo(3)
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/adjudications/all")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/adjudications/all")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/adjudications/all")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete all mapping`(): Unit = runBlocking {
      webTestClient.delete().uri("/mapping/adjudications/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent

      assertThat(repository.findAll().count()).isEqualTo(0)
      assertThat(hearingRepository.findAll().count()).isEqualTo(0)
      assertThat(punishmentRepository.findAll().count()).isEqualTo(0)
    }

    @Test
    fun `delete all migration mappings`(): Unit = runBlocking {
      webTestClient.delete().uri("/mapping/adjudications/all?migrationOnly=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent

      assertThat(repository.findAll().toList()).hasSize(1).noneMatch { it.mappingType == MIGRATED }
      assertThat(hearingRepository.findAll().toList()).hasSize(1).noneMatch { it.mappingType == MIGRATED }
      assertThat(punishmentRepository.findAll().toList()).hasSize(1).noneMatch { it.mappingType == MIGRATED }
    }

    @Test
    fun `delete all synchronisation mappings`(): Unit = runBlocking {
      webTestClient.delete().uri("/mapping/adjudications/all?synchronisationOnly=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent

      assertThat(repository.findAll().toList()).hasSize(2).noneMatch { it.mappingType == ADJUDICATION_CREATED }
      assertThat(hearingRepository.findAll().toList()).hasSize(2).noneMatch { it.mappingType == ADJUDICATION_CREATED }
      assertThat(punishmentRepository.findAll().toList()).hasSize(2)
        .noneMatch { it.mappingType == ADJUDICATION_CREATED }
    }

    @Test
    fun `delete all mappings (both true which for sure makes no sense, by whatever)`(): Unit = runBlocking {
      webTestClient.delete().uri("/mapping/adjudications/all?migrationOnly=true&synchronisationOnly=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent

      assertThat(repository.findAll().count()).isEqualTo(0)
      assertThat(hearingRepository.findAll().count()).isEqualTo(0)
      assertThat(punishmentRepository.findAll().count()).isEqualTo(0)
    }

    @Test
    fun `delete all mappings (both false which is default anyway)`(): Unit = runBlocking {
      webTestClient.delete().uri("/mapping/adjudications/all?migrationOnly=false&synchronisationOnly=false")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent

      assertThat(repository.findAll().count()).isEqualTo(0)
      assertThat(hearingRepository.findAll().count()).isEqualTo(0)
      assertThat(punishmentRepository.findAll().count()).isEqualTo(0)
    }
  }

  @DisplayName("GET /mapping/adjudications/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationIdTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/adjudications/migration-id/2022-01-01T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/adjudications/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get adjudication mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/adjudications/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get adjudication mappings by migration id success`() {
      (1L..4L).forEach {
        postCreateMappingRequest(
          adjudicationNumber = it,
          chargeSequence = 1,
          chargeNumber = "$it/1",
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      (5L..9L).forEach {
        postCreateMappingRequest(
          adjudicationNumber = it,
          chargeSequence = 1,
          chargeNumber = "$it/1",
          label = "2099-01-01",
          mappingType = "MIGRATED",
        )
      }
      postCreateMappingRequest(
        12,
        chargeSequence = 1,
        chargeNumber = "12/1",
        mappingType = ADJUDICATION_CREATED.name,
      )

      webTestClient.get().uri("/mapping/adjudications/migration-id/2022-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..adjudicationNumber").value(
          Matchers.contains(1, 2, 3, 4),
        )
        .jsonPath("$.content[0].whenCreated").isNotEmpty
    }

    @Test
    fun `get adjudication mappings by migration id - no records exist`() {
      (1L..4L).forEach {
        postCreateMappingRequest(
          adjudicationNumber = it,
          chargeSequence = 1,
          chargeNumber = "$it/1",
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }

      webTestClient.get().uri("/mapping/adjudications/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
        .jsonPath("content").isEmpty
    }

    @Test
    fun `can request a different page size`() {
      (1L..6L).forEach {
        postCreateMappingRequest(
          adjudicationNumber = it,
          chargeSequence = 1,
          chargeNumber = "$it/1",
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/adjudications/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("sort", "adjudicationNumber,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
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
    fun `can request a different page`() {
      (1L..3L).forEach {
        postCreateMappingRequest(
          adjudicationNumber = it,
          chargeSequence = 1,
          chargeNumber = "$it/1",
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/adjudications/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .queryParam("sort", "adjudicationNumber,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
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
}
