package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.isDuplicateMapping
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val DPS_COURT_CHARGE_ID = "DPS123"
private const val NOMIS_COURT_CHARGE_ID = 1234L
private const val DPS_COURT_CHARGE_2_ID = "DPS321"
private const val NOMIS_COURT_CHARGE_2_ID = 4321L

class CourtSentencingCourtChargeResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var courtChargeRepository: CourtChargeMappingRepository

  @Nested
  @DisplayName("GET /mapping/court-sentencing/court-charges/dps-court-charge-id/{courtChargeId}")
  inner class GetCourtChargeMappingByDpsId {
    lateinit var courtChargeMapping: CourtChargeMapping

    @BeforeEach
    fun setUp() = runTest {
      courtChargeMapping = courtChargeRepository.save(
        createMapping(
          dpsCourtChargeId = DPS_COURT_CHARGE_ID,
          nomisCourtChargeId = NOMIS_COURT_CHARGE_ID,
          label = "2023-01-01T12:45:12",
        ).toOffenderChargeMapping(),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-charges/dps-court-charge-id/${courtChargeMapping.dpsCourtChargeId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-charges/dps-court-charge-id/${courtChargeMapping.dpsCourtChargeId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-charges/dps-court-charge-id/${courtChargeMapping.dpsCourtChargeId}")
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
          .uri("/mapping/court-sentencing/court-charges/dps-court-charge-id/DOESNOTEXIST")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("DPS Court charge Id =DOESNOTEXIST")
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-charges/dps-court-charge-id/${courtChargeMapping.dpsCourtChargeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCourtChargeId").isEqualTo(courtChargeMapping.nomisCourtChargeId)
          .jsonPath("dpsCourtChargeId").isEqualTo(courtChargeMapping.dpsCourtChargeId)
          .jsonPath("mappingType").isEqualTo(courtChargeMapping.mappingType.name)
          .jsonPath("label").isEqualTo(courtChargeMapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it))
              .isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/court-sentencing/court-charges/nomis-court-charge-id/{courtChargeId}")
  inner class GetCourtChargeMappingByNomisId {
    lateinit var courtChargeMapping: CourtChargeMapping

    @BeforeEach
    fun setUp() = runTest {
      courtChargeMapping = courtChargeRepository.save(
        createMapping(
          dpsCourtChargeId = DPS_COURT_CHARGE_ID,
          nomisCourtChargeId = NOMIS_COURT_CHARGE_ID,
          label = "2023-01-01T12:45:12",
        ).toOffenderChargeMapping(),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-charges/nomis-court-charge-id/${courtChargeMapping.nomisCourtChargeId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-charges/nomis-court-charge-id/${courtChargeMapping.nomisCourtChargeId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-charges/nomis-court-charge-id/${courtChargeMapping.nomisCourtChargeId}")
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
          .uri("/mapping/court-sentencing/court-charges/nomis-court-charge-id/8888888")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("NOMIS Court charge Id =8888888")
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-charges/nomis-court-charge-id/${courtChargeMapping.nomisCourtChargeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisCourtChargeId").isEqualTo(courtChargeMapping.nomisCourtChargeId)
          .jsonPath("dpsCourtChargeId").isEqualTo(courtChargeMapping.dpsCourtChargeId)
          .jsonPath("mappingType").isEqualTo(courtChargeMapping.mappingType.name)
          .jsonPath("label").isEqualTo(courtChargeMapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it))
              .isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @DisplayName("PUT /mapping/court-sentencing/court-charges")
  @Nested
  inner class BatchUpdateMappingTest {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.put().uri("/mapping/court-sentencing/court-charges")
          .body(BodyInserters.fromValue(updateMappingBatch()))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/mapping/court-sentencing/court-charges")
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(updateMappingBatch()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `create forbidden with wrong role`() {
        webTestClient.put().uri("/mapping/court-sentencing/court-charges")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(updateMappingBatch()))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can create a single court charge mapping`() = runTest {
        putUpdateMappingsRequest(
          courtCharges = listOf(
            CourtChargeMappingDto(
              dpsCourtChargeId = DPS_COURT_CHARGE_ID,
              nomisCourtChargeId = NOMIS_COURT_CHARGE_ID,
            ),
          ),
        )

        val mappings = courtChargeRepository.findAll().toList()

        assertThat(mappings).hasSize(1)
        val mapping = mappings[0]
        assertThat(mapping.dpsCourtChargeId).isEqualTo(DPS_COURT_CHARGE_ID)
        assertThat(mapping.nomisCourtChargeId).isEqualTo(NOMIS_COURT_CHARGE_ID)
        assertThat(mapping.mappingType).isEqualTo(CourtChargeMappingType.DPS_CREATED)
        assertThat(mapping.label).isNull()
        assertThat(mapping.whenCreated)
          .isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }

      @Test
      fun `can create and delete court charge mapping`() = runTest {
        putUpdateMappingsRequest(
          courtCharges = listOf(
            CourtChargeMappingDto(
              dpsCourtChargeId = DPS_COURT_CHARGE_ID,
              nomisCourtChargeId = NOMIS_COURT_CHARGE_ID,
            ),
          ),
        )

        val mappings = courtChargeRepository.findAll().toList()

        assertThat(mappings).hasSize(1)
        assertThat(mappings[0].dpsCourtChargeId).isEqualTo(DPS_COURT_CHARGE_ID)
        assertThat(mappings[0].nomisCourtChargeId).isEqualTo(NOMIS_COURT_CHARGE_ID)

        putUpdateMappingsRequest(
          courtCharges = listOf(
            createMapping(dpsCourtChargeId = DPS_COURT_CHARGE_2_ID, nomisCourtChargeId = NOMIS_COURT_CHARGE_2_ID),
          ),
          courtChargesToDelete = listOf(
            CourtChargeNomisIdDto(
              nomisCourtChargeId = NOMIS_COURT_CHARGE_ID,
            ),
          ),
        )

        val updatedMappings = courtChargeRepository.findAll().toList()

        assertThat(updatedMappings).hasSize(1)
        assertThat(updatedMappings[0].dpsCourtChargeId).isEqualTo(DPS_COURT_CHARGE_2_ID)
        assertThat(updatedMappings[0].nomisCourtChargeId).isEqualTo(NOMIS_COURT_CHARGE_2_ID)
      }

      @Test
      fun `can create a multiple court charge mappings`() = runTest {
        putUpdateMappingsRequest(
          listOf(
            createMapping(dpsCourtChargeId = DPS_COURT_CHARGE_ID, nomisCourtChargeId = NOMIS_COURT_CHARGE_ID),
            createMapping(dpsCourtChargeId = DPS_COURT_CHARGE_2_ID, nomisCourtChargeId = NOMIS_COURT_CHARGE_2_ID),
          ),
        )

        val mappings = courtChargeRepository.findAll().toList()

        assertThat(mappings).hasSize(2)

        assertThat(mappings[0].dpsCourtChargeId).isEqualTo(DPS_COURT_CHARGE_ID)
        assertThat(mappings[0].nomisCourtChargeId).isEqualTo(NOMIS_COURT_CHARGE_ID)
        assertThat(mappings[0].mappingType).isEqualTo(CourtChargeMappingType.DPS_CREATED)
        assertThat(mappings[0].label).isNull()
        assertThat(mappings[0].whenCreated)
          .isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))

        assertThat(mappings[1].dpsCourtChargeId).isEqualTo(DPS_COURT_CHARGE_2_ID)
        assertThat(mappings[1].nomisCourtChargeId).isEqualTo(NOMIS_COURT_CHARGE_2_ID)
        assertThat(mappings[1].mappingType).isEqualTo(CourtChargeMappingType.DPS_CREATED)
        assertThat(mappings[1].label).isNull()
        assertThat(mappings[1].whenCreated)
          .isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }

    @Nested
    inner class Failures {
      @Test
      fun `create mapping failure - court charge exists`() {
        putUpdateMappingsRequest(
          courtCharges = listOf(
            CourtChargeMappingDto(
              dpsCourtChargeId = DPS_COURT_CHARGE_ID,
              nomisCourtChargeId = NOMIS_COURT_CHARGE_ID,
            ),
          ),
        )

        webTestClient.put().uri("/mapping/court-sentencing/court-charges")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              updateMappingBatch(
                createMapping(
                  dpsCourtChargeId = DPS_COURT_CHARGE_ID,
                  nomisCourtChargeId = 5434231,
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isDuplicateMapping

        webTestClient.put().uri("/mapping/court-sentencing/court-charges")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              updateMappingBatch(
                createMapping(
                  dpsCourtChargeId = "7656543",
                  nomisCourtChargeId = NOMIS_COURT_CHARGE_ID,
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isDuplicateMapping
      }
    }
  }

  @DisplayName("POST /mapping/court-sentencing/court-charges")
  @Nested
  inner class CourtChargeMappingTest {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.post().uri("/mapping/court-sentencing/court-charges")
          .body(BodyInserters.fromValue(createMapping()))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/mapping/court-sentencing/court-charges")
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(createMapping()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `create forbidden with wrong role`() {
        webTestClient.post().uri("/mapping/court-sentencing/court-charges")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(createMapping()))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can create a single court charge mapping`() = runTest {
        postCourtChargeMappingRequest()

        val mappings = courtChargeRepository.findAll().toList()

        assertThat(mappings).hasSize(1)
        val mapping = mappings[0]
        assertThat(mapping.dpsCourtChargeId).isEqualTo(DPS_COURT_CHARGE_ID)
        assertThat(mapping.nomisCourtChargeId).isEqualTo(NOMIS_COURT_CHARGE_ID)
        assertThat(mapping.mappingType).isEqualTo(CourtChargeMappingType.DPS_CREATED)
        assertThat(mapping.label).isNull()
        assertThat(mapping.whenCreated)
          .isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }

    @Nested
    inner class Failures {
      @Test
      fun `create mapping failure - court charge exists`() {
        postCourtChargeMappingRequest()

        webTestClient.post().uri("/mapping/court-sentencing/court-charges")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CourtChargeMappingDto(
                dpsCourtChargeId = DPS_COURT_CHARGE_ID,
                nomisCourtChargeId = 5434231,
              ),
            ),

          )
          .exchange()
          .expectStatus().isDuplicateMapping

        webTestClient.post().uri("/mapping/court-sentencing/court-charges")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              CourtChargeMappingDto(
                dpsCourtChargeId = "7656543",
                nomisCourtChargeId = NOMIS_COURT_CHARGE_ID,
              ),

            ),
          )
          .exchange()
          .expectStatus().isDuplicateMapping
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/court-sentencing/court-charges/nomis-court-charge-id/{courtChargeId}")
  inner class DeleteMappingByNomisId {
    lateinit var mapping: CourtChargeMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = courtChargeRepository.save(
        CourtChargeMapping(
          dpsCourtChargeId = DPS_COURT_CHARGE_ID,
          nomisCourtChargeId = NOMIS_COURT_CHARGE_ID,
          label = "2023-01-01T12:45:12",
          mappingType = CourtChargeMappingType.MIGRATED,
        ),
      )
    }

    @AfterEach
    fun tearDown() = runTest {
      courtChargeRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-charges/nomis-court-charge-id/${mapping.nomisCourtChargeId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-charges/nomis-court-charge-id/${mapping.nomisCourtChargeId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-charges/nomis-court-charge-id/${mapping.nomisCourtChargeId}")
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
          .uri("/mapping/court-sentencing/court-charges/nomis-court-charge-id/13333")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/court-sentencing/court-charges/dps-court-charge-id/${mapping.dpsCourtChargeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isOk

        // delete using nomis id
        webTestClient.delete()
          .uri("/mapping/court-sentencing/court-charges/nomis-court-charge-id/${mapping.nomisCourtChargeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/court-sentencing/court-charges/dps-court-charge-id/${mapping.dpsCourtChargeId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @AfterEach
  fun tearDown() = runTest {
    courtChargeRepository.deleteAll()
  }

  private fun updateMappingBatch(mapping: CourtChargeMappingDto = createMapping()): CourtChargeBatchUpdateMappingDto =
    CourtChargeBatchUpdateMappingDto(
      courtChargesToCreate = listOf(mapping),
    )

  private fun createMapping(
    dpsCourtChargeId: String = DPS_COURT_CHARGE_ID,
    nomisCourtChargeId: Long = NOMIS_COURT_CHARGE_ID,
    label: String? = null,
  ): CourtChargeMappingDto = CourtChargeMappingDto(
    dpsCourtChargeId = dpsCourtChargeId,
    nomisCourtChargeId = nomisCourtChargeId,
    mappingType = CourtChargeMappingType.DPS_CREATED,
    label = label,
  )

  private fun putUpdateMappingsRequest(
    courtCharges: List<CourtChargeMappingDto>,
    courtChargesToDelete: List<CourtChargeNomisIdDto> = emptyList(),
  ) {
    webTestClient.put().uri("/mapping/court-sentencing/court-charges")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          CourtChargeBatchUpdateMappingDto(
            courtChargesToCreate = courtCharges,
            courtChargesToDelete = courtChargesToDelete,
          ),
        ),
      )
      .exchange()
      .expectStatus().isOk
  }

  private fun postCourtChargeMappingRequest(
    courtCharge: CourtChargeMappingDto = CourtChargeMappingDto(
      dpsCourtChargeId = DPS_COURT_CHARGE_ID,
      nomisCourtChargeId = NOMIS_COURT_CHARGE_ID,
    ),
  ) {
    webTestClient.post().uri("/mapping/court-sentencing/court-charges")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_COURT_SENTENCING")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(courtCharge),
      )
      .exchange()
      .expectStatus().isCreated
  }
}
