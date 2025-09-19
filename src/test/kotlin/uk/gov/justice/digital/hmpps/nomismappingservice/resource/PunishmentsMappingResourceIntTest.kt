@file:OptIn(ExperimentalCoroutinesApi::class)

package uk.gov.justice.digital.hmpps.nomismappingservice.resource

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.data.AdjudicationPunishmentBatchMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.data.AdjudicationPunishmentBatchUpdateMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.data.AdjudicationPunishmentMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.data.AdjudicationPunishmentNomisIdDto
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.AdjudicationMappingType.ADJUDICATION_CREATED
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository.AdjudicationPunishmentMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.service.AdjudicationMappingService
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

const val DPS_PUNISHMENT_ID = "876"
const val NOMIS_BOOKING_ID = 5434220000
const val NOMIS_SANCTION_SEQUENCE = 2

class PunishmentsMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var realRepository: AdjudicationPunishmentMappingRepository
  private lateinit var repository: AdjudicationPunishmentMappingRepository

  @Autowired
  private lateinit var adjudicationMappingService: AdjudicationMappingService

  @BeforeEach
  fun setup() {
    repository = mock(defaultAnswer = AdditionalAnswers.delegatesTo(realRepository))
    ReflectionTestUtils.setField(adjudicationMappingService, "adjudicationPunishmentMappingRepository", repository)
  }

  private fun createMapping(
    dpsPunishmentId: String = DPS_PUNISHMENT_ID,
    nomisBookingId: Long = NOMIS_BOOKING_ID,
    nomisSanctionSequence: Int = NOMIS_SANCTION_SEQUENCE,
  ): AdjudicationPunishmentMappingDto = AdjudicationPunishmentMappingDto(
    dpsPunishmentId = dpsPunishmentId,
    nomisBookingId = nomisBookingId,
    nomisSanctionSequence = nomisSanctionSequence,
    mappingType = ADJUDICATION_CREATED.name,
  )

  private fun createMappingBatch(mapping: AdjudicationPunishmentMappingDto = createMapping()): AdjudicationPunishmentBatchMappingDto = AdjudicationPunishmentBatchMappingDto(
    punishments = listOf(mapping),
  )

  private fun updateMappingBatch(mapping: AdjudicationPunishmentMappingDto = createMapping()): AdjudicationPunishmentBatchUpdateMappingDto = AdjudicationPunishmentBatchUpdateMappingDto(
    punishmentsToCreate = listOf(mapping),
  )

  private fun postCreateSingleMappingRequest(
    dpsPunishmentId: String = DPS_PUNISHMENT_ID,
    nomisBookingId: Long = NOMIS_BOOKING_ID,
    nomisSanctionSequence: Int = NOMIS_SANCTION_SEQUENCE,
  ) {
    webTestClient.post().uri("/mapping/punishments")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          AdjudicationPunishmentBatchMappingDto(
            punishments = listOf(
              createMapping(
                dpsPunishmentId = dpsPunishmentId,
                nomisBookingId = nomisBookingId,
                nomisSanctionSequence = nomisSanctionSequence,
              ),
            ),
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  private fun putUpdateSingleMappingRequest(
    dpsPunishmentId: String = DPS_PUNISHMENT_ID,
    nomisBookingId: Long = NOMIS_BOOKING_ID,
    nomisSanctionSequence: Int = NOMIS_SANCTION_SEQUENCE,
  ) {
    webTestClient.put().uri("/mapping/punishments")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          AdjudicationPunishmentBatchUpdateMappingDto(
            punishmentsToCreate = listOf(
              createMapping(
                dpsPunishmentId = dpsPunishmentId,
                nomisBookingId = nomisBookingId,
                nomisSanctionSequence = nomisSanctionSequence,
              ),
            ),
          ),
        ),
      )
      .exchange()
      .expectStatus().isOk
  }

  private fun postCreateMappingsRequest(
    punishments: List<AdjudicationPunishmentMappingDto>,
  ) {
    webTestClient.post().uri("/mapping/punishments")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          AdjudicationPunishmentBatchMappingDto(
            punishments = punishments,
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  private fun putUpdateMappingsRequest(
    punishments: List<AdjudicationPunishmentMappingDto>,
    punishmentsToDelete: List<AdjudicationPunishmentNomisIdDto> = emptyList(),
  ) {
    webTestClient.put().uri("/mapping/punishments")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          AdjudicationPunishmentBatchUpdateMappingDto(
            punishmentsToCreate = punishments,
            punishmentsToDelete = punishmentsToDelete,
          ),
        ),
      )
      .exchange()
      .expectStatus().isOk
  }

  @BeforeEach
  fun deleteData() = runBlocking {
    repository.deleteAll()
  }

  @DisplayName("POST /mapping/punishments")
  @Nested
  inner class CreateMappingTest {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.post().uri("/mapping/punishments")
          .body(BodyInserters.fromValue(createMappingBatch()))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/mapping/punishments")
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(createMappingBatch()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `create forbidden with wrong role`() {
        webTestClient.post().uri("/mapping/punishments")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(createMappingBatch()))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can create a single punishment mapping`() = runTest {
        postCreateSingleMappingRequest(
          dpsPunishmentId = DPS_PUNISHMENT_ID,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE,
        )

        val mappings = repository.findAll().toList()

        assertThat(mappings).hasSize(1)
        val mapping = mappings[0]
        assertThat(mapping.dpsPunishmentId).isEqualTo(DPS_PUNISHMENT_ID)
        assertThat(mapping.nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
        assertThat(mapping.nomisSanctionSequence).isEqualTo(NOMIS_SANCTION_SEQUENCE)
        assertThat(mapping.mappingType).isEqualTo(ADJUDICATION_CREATED)
        assertThat(mapping.label).isNull()
        assertThat(mapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }

      @Test
      fun `can create a multiple punishment mappings`() = runTest {
        postCreateMappingsRequest(
          listOf(
            createMapping(dpsPunishmentId = "10", nomisBookingId = 101, nomisSanctionSequence = 1),
            createMapping(dpsPunishmentId = "11", nomisBookingId = 101, nomisSanctionSequence = 2),
          ),
        )

        val mappings = repository.findAll().toList()

        assertThat(mappings).hasSize(2)

        assertThat(mappings[0].dpsPunishmentId).isEqualTo("10")
        assertThat(mappings[0].nomisBookingId).isEqualTo(101)
        assertThat(mappings[0].nomisSanctionSequence).isEqualTo(1)
        assertThat(mappings[0].mappingType).isEqualTo(ADJUDICATION_CREATED)
        assertThat(mappings[0].label).isNull()
        assertThat(mappings[0].whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))

        assertThat(mappings[1].dpsPunishmentId).isEqualTo("11")
        assertThat(mappings[1].nomisBookingId).isEqualTo(101)
        assertThat(mappings[1].nomisSanctionSequence).isEqualTo(2)
        assertThat(mappings[1].mappingType).isEqualTo(ADJUDICATION_CREATED)
        assertThat(mappings[1].label).isNull()
        assertThat(mappings[1].whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }

    @Nested
    inner class Failures {
      @Test
      fun `create mapping failure - punishment exists`() {
        postCreateSingleMappingRequest(
          dpsPunishmentId = DPS_PUNISHMENT_ID,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE,
        )

        val response1 =
          webTestClient.post().uri("/mapping/punishments")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                createMappingBatch(
                  createMapping(
                    dpsPunishmentId = DPS_PUNISHMENT_ID,
                    nomisBookingId = NOMIS_BOOKING_ID,
                    nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE,
                  ),
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

        with(response1!!) {
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisBookingId", NOMIS_BOOKING_ID)
            .containsEntry("nomisSanctionSequence", NOMIS_SANCTION_SEQUENCE)
            .containsEntry("dpsPunishmentId", DPS_PUNISHMENT_ID)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisBookingId", NOMIS_BOOKING_ID)
            .containsEntry("nomisSanctionSequence", NOMIS_SANCTION_SEQUENCE)
            .containsEntry("dpsPunishmentId", DPS_PUNISHMENT_ID)
        }

        val response2 =
          webTestClient.post().uri("/mapping/punishments")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                createMappingBatch(
                  createMapping(
                    dpsPunishmentId = DPS_PUNISHMENT_ID,
                    nomisBookingId = 54342310000,
                    nomisSanctionSequence = 99,
                  ),
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

        with(response2!!) {
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisBookingId", NOMIS_BOOKING_ID)
            .containsEntry("nomisSanctionSequence", NOMIS_SANCTION_SEQUENCE)
            .containsEntry("dpsPunishmentId", DPS_PUNISHMENT_ID)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisBookingId", 54342310000)
            .containsEntry("nomisSanctionSequence", 99)
            .containsEntry("dpsPunishmentId", DPS_PUNISHMENT_ID)
        }

        val response3 =
          webTestClient.post().uri("/mapping/punishments")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                createMappingBatch(
                  createMapping(
                    dpsPunishmentId = "7656543",
                    nomisBookingId = NOMIS_BOOKING_ID,
                    nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE,
                  ),
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

        with(response3!!) {
          // not supplied in this scenario since this happens for duplicate key rather than look up
          // so returns ugly list on punishments supplied
          assertThat(this.moreInfo.existing)
            .isNull()
          assertThat(this.moreInfo.duplicate)
            .containsKeys("punishments")
          @Suppress("UNCHECKED_CAST")
          val existingPunishments = this.moreInfo.duplicate["punishments"] as List<Map<String, *>>
          assertThat(existingPunishments).hasSize(1)
          assertThat(existingPunishments[0])
            .containsEntry("nomisBookingId", NOMIS_BOOKING_ID)
            .containsEntry("nomisSanctionSequence", NOMIS_SANCTION_SEQUENCE)
            .containsEntry("dpsPunishmentId", "7656543")
        }
      }

      @Test
      fun `create mapping failure - punishment added at same time`() = runTest {
        postCreateSingleMappingRequest(
          dpsPunishmentId = DPS_PUNISHMENT_ID,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE,
        )

        // Emulate calling service simultaneously twice by disabling the duplicate check
        // Note: the spy is automatically reset by ResetMocksTestExecutionListener
        whenever(repository.findById(DPS_PUNISHMENT_ID)).thenReturn(null)

        val responseBody =
          webTestClient.post().uri("/mapping/punishments")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                // language=json
                createMappingBatch(
                  createMapping(
                    dpsPunishmentId = DPS_PUNISHMENT_ID,
                    nomisBookingId = NOMIS_BOOKING_ID,
                    nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE + 1,
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(
              object :
                ParameterizedTypeReference<DuplicateMappingErrorResponse<AdjudicationPunishmentBatchMappingDto>>() {},
            )
            .returnResult().responseBody

        with(responseBody!!) {
          assertThat(userMessage)
            .contains("Conflict: Adjudication punishment mapping already exists, detected by org.springframework.dao.DuplicateKeyException")
          assertThat(errorCode).isEqualTo(1409)
        }
      }
    }
  }

  @DisplayName("PUT /mapping/punishments")
  @Nested
  inner class UpdateMappingTest {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.put().uri("/mapping/punishments")
          .body(BodyInserters.fromValue(updateMappingBatch()))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/mapping/punishments")
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(updateMappingBatch()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `create forbidden with wrong role`() {
        webTestClient.put().uri("/mapping/punishments")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(updateMappingBatch()))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can create a single punishment mapping`() = runTest {
        putUpdateSingleMappingRequest(
          dpsPunishmentId = DPS_PUNISHMENT_ID,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE,
        )

        val mappings = repository.findAll().toList()

        assertThat(mappings).hasSize(1)
        val mapping = mappings[0]
        assertThat(mapping.dpsPunishmentId).isEqualTo(DPS_PUNISHMENT_ID)
        assertThat(mapping.nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
        assertThat(mapping.nomisSanctionSequence).isEqualTo(NOMIS_SANCTION_SEQUENCE)
        assertThat(mapping.mappingType).isEqualTo(ADJUDICATION_CREATED)
        assertThat(mapping.label).isNull()
        assertThat(mapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }

      @Test
      fun `can create and delete punishment mapping`() = runTest {
        putUpdateSingleMappingRequest(
          dpsPunishmentId = DPS_PUNISHMENT_ID,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE,
        )

        val mappings = repository.findAll().toList()

        assertThat(mappings).hasSize(1)
        assertThat(mappings[0].dpsPunishmentId).isEqualTo(DPS_PUNISHMENT_ID)
        assertThat(mappings[0].nomisBookingId).isEqualTo(NOMIS_BOOKING_ID)
        assertThat(mappings[0].nomisSanctionSequence).isEqualTo(NOMIS_SANCTION_SEQUENCE)

        putUpdateMappingsRequest(
          punishments = listOf(
            createMapping(dpsPunishmentId = "10", nomisBookingId = 101, nomisSanctionSequence = 1),
          ),
          punishmentsToDelete = listOf(
            AdjudicationPunishmentNomisIdDto(
              nomisBookingId = NOMIS_BOOKING_ID,
              nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE,
            ),
          ),
        )

        val updatedMappings = repository.findAll().toList()

        assertThat(updatedMappings).hasSize(1)
        assertThat(updatedMappings[0].dpsPunishmentId).isEqualTo("10")
        assertThat(updatedMappings[0].nomisBookingId).isEqualTo(101)
        assertThat(updatedMappings[0].nomisSanctionSequence).isEqualTo(1)
      }

      @Test
      fun `can create a multiple punishment mappings`() = runTest {
        putUpdateMappingsRequest(
          listOf(
            createMapping(dpsPunishmentId = "10", nomisBookingId = 101, nomisSanctionSequence = 1),
            createMapping(dpsPunishmentId = "11", nomisBookingId = 101, nomisSanctionSequence = 2),
          ),
        )

        val mappings = repository.findAll().toList()

        assertThat(mappings).hasSize(2)

        assertThat(mappings[0].dpsPunishmentId).isEqualTo("10")
        assertThat(mappings[0].nomisBookingId).isEqualTo(101)
        assertThat(mappings[0].nomisSanctionSequence).isEqualTo(1)
        assertThat(mappings[0].mappingType).isEqualTo(ADJUDICATION_CREATED)
        assertThat(mappings[0].label).isNull()
        assertThat(mappings[0].whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))

        assertThat(mappings[1].dpsPunishmentId).isEqualTo("11")
        assertThat(mappings[1].nomisBookingId).isEqualTo(101)
        assertThat(mappings[1].nomisSanctionSequence).isEqualTo(2)
        assertThat(mappings[1].mappingType).isEqualTo(ADJUDICATION_CREATED)
        assertThat(mappings[1].label).isNull()
        assertThat(mappings[1].whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }

    @Nested
    inner class Failures {
      @Test
      fun `create mapping failure - punishment exists`() {
        putUpdateSingleMappingRequest(
          dpsPunishmentId = DPS_PUNISHMENT_ID,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE,
        )

        webTestClient.put().uri("/mapping/punishments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              updateMappingBatch(
                createMapping(
                  dpsPunishmentId = DPS_PUNISHMENT_ID,
                  nomisBookingId = 5434231,
                  nomisSanctionSequence = 99,
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isDuplicateMapping

        webTestClient.put().uri("/mapping/punishments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              updateMappingBatch(
                createMapping(
                  dpsPunishmentId = "7656543",
                  nomisBookingId = NOMIS_BOOKING_ID,
                  nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE,
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `create mapping failure - punishment added at same time`() = runTest {
        putUpdateSingleMappingRequest(
          dpsPunishmentId = DPS_PUNISHMENT_ID,
          nomisBookingId = NOMIS_BOOKING_ID,
          nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE,
        )

        // Emulate calling service simultaneously twice by disabling the duplicate check
        // Note: the spy is automatically reset by ResetMocksTestExecutionListener
        whenever(repository.findById(DPS_PUNISHMENT_ID)).thenReturn(null)

        val responseBody =
          webTestClient.put().uri("/mapping/punishments")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                // language=json
                updateMappingBatch(
                  createMapping(
                    dpsPunishmentId = DPS_PUNISHMENT_ID,
                    nomisBookingId = NOMIS_BOOKING_ID,
                    nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE + 1,
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(
              object :
                ParameterizedTypeReference<DuplicateMappingErrorResponse<AdjudicationPunishmentBatchMappingDto>>() {},
            )
            .returnResult().responseBody

        with(responseBody!!) {
          assertThat(userMessage)
            .contains("Conflict: Adjudication punishment mapping already exists, detected by org.springframework.dao.DuplicateKeyException")
          assertThat(errorCode).isEqualTo(1409)
        }
      }
    }
  }

  @DisplayName("GET /mapping/punishments/:dpsPunishmentId")
  @Nested
  inner class GetByDpsIdMappingTest {
    @BeforeEach
    fun setUp() {
      postCreateSingleMappingRequest(
        dpsPunishmentId = DPS_PUNISHMENT_ID,
        nomisBookingId = NOMIS_BOOKING_ID,
        nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE,
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get().uri("/mapping/punishments/$DPS_PUNISHMENT_ID")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/punishments/$DPS_PUNISHMENT_ID")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `create forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/punishments/$DPS_PUNISHMENT_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class NotFound {
      @Test
      fun `not found when mapping does not exist`() = runTest {
        webTestClient.get().uri("/mapping/punishments/9940235")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can read punishment mapping`() = runTest {
        webTestClient.get().uri("/mapping/punishments/$DPS_PUNISHMENT_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisBookingId").isEqualTo("$NOMIS_BOOKING_ID")
          .jsonPath("nomisSanctionSequence").isEqualTo("$NOMIS_SANCTION_SEQUENCE")
          .jsonPath("dpsPunishmentId").isEqualTo(DPS_PUNISHMENT_ID)
      }
    }
  }

  @DisplayName("GET /mapping/punishments/nomis-booking-id/:nomisBookingId/nomis-sanction-sequence/:nomisSanctionSequence")
  @Nested
  inner class GetByNomisIdMappingTest {
    @BeforeEach
    fun setUp() {
      postCreateSingleMappingRequest(
        dpsPunishmentId = DPS_PUNISHMENT_ID,
        nomisBookingId = NOMIS_BOOKING_ID,
        nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE,
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get().uri("mapping/punishments/nomis-booking-id/$NOMIS_BOOKING_ID/nomis-sanction-sequence/$NOMIS_SANCTION_SEQUENCE")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("mapping/punishments/nomis-booking-id/$NOMIS_BOOKING_ID/nomis-sanction-sequence/$NOMIS_SANCTION_SEQUENCE")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `create forbidden with wrong role`() {
        webTestClient.get().uri("mapping/punishments/nomis-booking-id/$NOMIS_BOOKING_ID/nomis-sanction-sequence/$NOMIS_SANCTION_SEQUENCE")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class NotFound {
      @Test
      fun `not found when mapping does not exist`() = runTest {
        webTestClient.get().uri("mapping/punishments/nomis-booking-id/$NOMIS_BOOKING_ID/nomis-sanction-sequence/9999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
        webTestClient.get().uri("mapping/punishments/nomis-booking-id/9999/nomis-sanction-sequence/$NOMIS_SANCTION_SEQUENCE")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can read punishment mapping`() = runTest {
        webTestClient.get().uri("mapping/punishments/nomis-booking-id/$NOMIS_BOOKING_ID/nomis-sanction-sequence/$NOMIS_SANCTION_SEQUENCE")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisBookingId").isEqualTo("$NOMIS_BOOKING_ID")
          .jsonPath("nomisSanctionSequence").isEqualTo("$NOMIS_SANCTION_SEQUENCE")
          .jsonPath("dpsPunishmentId").isEqualTo(DPS_PUNISHMENT_ID)
      }
    }
  }

  @DisplayName("DELETE /mapping/punishments/:dpsPunishmentId")
  @Nested
  inner class DeleteMappingTest {
    @BeforeEach
    fun setUp() {
      postCreateSingleMappingRequest(
        dpsPunishmentId = DPS_PUNISHMENT_ID,
        nomisBookingId = NOMIS_BOOKING_ID,
        nomisSanctionSequence = NOMIS_SANCTION_SEQUENCE,
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.delete().uri("/mapping/punishments/$DPS_PUNISHMENT_ID")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/mapping/punishments/$DPS_PUNISHMENT_ID")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `create forbidden with wrong role`() {
        webTestClient.delete().uri("/mapping/punishments/$DPS_PUNISHMENT_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can delete punishment mapping`() = runTest {
        webTestClient.get().uri("/mapping/punishments/$DPS_PUNISHMENT_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
        webTestClient.delete().uri("/mapping/punishments/$DPS_PUNISHMENT_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNoContent
        webTestClient.get().uri("/mapping/punishments/$DPS_PUNISHMENT_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }
}
