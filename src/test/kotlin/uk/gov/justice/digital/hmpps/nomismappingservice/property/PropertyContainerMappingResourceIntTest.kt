package uk.gov.justice.digital.hmpps.nomismappingservice.property

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
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
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val DPS_ID = "e52d7268-6e10-41a8-a0b9-2319b32520d6"
private const val DPS_ID2 = "edcd118c-41ba-42ea-b5c4-404b453ad58b"
private const val NOMIS_ID = 1234567L
private const val NOMIS_ID2 = 1234568L
private const val OFFENDER_NO = "A1234AA"
private const val BOOKING_ID = 12345L

class PropertyContainerMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: PropertyContainerMappingRepository

  private fun generateUUIDs(n: Long) = "de91dfa7-821f-4552-a427-000000${n.toString().padStart(6, '0')}"

  private fun postCreateMappingRequest(
    bookingId: Long = BOOKING_ID,
    nomisPropertyContainerId: Long,
    dpsPropertyContainerId: String = DPS_ID,
    offenderNo: String = OFFENDER_NO,
    label: String = "2022-01-01",
    mappingType: PropertyContainerMappingType = PropertyContainerMappingType.DPS_CREATED,
  ) {
    webTestClient.post().uri("/mapping/property")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          PropertyContainerMappingDto(
            bookingId = bookingId,
            nomisPropertyContainerId = nomisPropertyContainerId,
            dpsPropertyContainerId = dpsPropertyContainerId,
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
  @DisplayName("POST /mapping/property")
  inner class CreateMapping {
    private lateinit var existingMapping: PropertyContainerMapping
    private val mapping = PropertyContainerMappingDto(
      dpsPropertyContainerId = DPS_ID,
      bookingId = BOOKING_ID,
      nomisPropertyContainerId = NOMIS_ID,
      offenderNo = OFFENDER_NO,
      label = "2024-02-01T12:45:12",
      mappingType = PropertyContainerMappingType.MIGRATED,
    )

    @BeforeEach
    fun setUp() = runTest {
      existingMapping = repository.save(
        PropertyContainerMapping(
          dpsPropertyContainerId = UUID.fromString(DPS_ID2),
          bookingId = 3456L,
          nomisPropertyContainerId = 1,
          offenderNo = OFFENDER_NO,
          label = "2024-01-01T12:45:12",
          mappingType = PropertyContainerMappingType.DPS_CREATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/property")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/property")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/property")
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
          .uri("/mapping/property")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val createdMapping = repository.findById(UUID.fromString(mapping.dpsPropertyContainerId))!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.bookingId).isEqualTo(mapping.bookingId)
        assertThat(createdMapping.nomisPropertyContainerId).isEqualTo(mapping.nomisPropertyContainerId)
        assertThat(createdMapping.dpsPropertyContainerId.toString()).isEqualTo(mapping.dpsPropertyContainerId)
        assertThat(createdMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(createdMapping.label).isEqualTo(mapping.label)
      }

      @Test
      fun `can create with minimal data`() = runTest {
        webTestClient.post()
          .uri("/mapping/property")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                {
                  "bookingId": $BOOKING_ID,
                  "nomisPropertyContainerId": $NOMIS_ID,
                  "dpsPropertyContainerId": "$DPS_ID",
                  "offenderNo": "A1234AA"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        val createdMapping = repository.findById(UUID.fromString(DPS_ID))!!

        assertThat(createdMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        assertThat(createdMapping.bookingId).isEqualTo(BOOKING_ID)
        assertThat(createdMapping.nomisPropertyContainerId).isEqualTo(NOMIS_ID)
        assertThat(createdMapping.dpsPropertyContainerId).isEqualTo(UUID.fromString(DPS_ID))
        assertThat(createdMapping.mappingType).isEqualTo(PropertyContainerMappingType.DPS_CREATED)
        assertThat(createdMapping.label).isNull()
      }

      @Test
      fun `can post and then get mapping`() {
        webTestClient.post()
          .uri("/mapping/property")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                {
                  "bookingId": $BOOKING_ID,
                  "nomisPropertyContainerId": $NOMIS_ID,
                  "dpsPropertyContainerId": "$DPS_ID",
                  "offenderNo": "$OFFENDER_NO"
                }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        webTestClient.get()
          .uri("/mapping/property/nomis-id/$NOMIS_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/property/dps-id/$DPS_ID")
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
          .uri("/mapping/property")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                {
                  "bookingId": $BOOKING_ID,
                  "nomisPropertyContainerId": $NOMIS_ID,
                  "dpsPropertyContainerId": "$DPS_ID",
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
          .uri("/mapping/property")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=JSON
              """
                {
                  "bookingId": $BOOKING_ID,
                  "nomisPropertyContainerId": $NOMIS_ID,
                  
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
          .uri("/mapping/property")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                {
                  "dpsPropertyContainerId": "$DPS_ID",
                  "bookingId": $BOOKING_ID,
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
        val dpsPropertyContainerId = UUID.randomUUID().toString()
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/property")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              PropertyContainerMappingDto(
                bookingId = existingMapping.bookingId,
                nomisPropertyContainerId = existingMapping.nomisPropertyContainerId,
                dpsPropertyContainerId = dpsPropertyContainerId,
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
            .containsEntry("bookingId", existingMapping.bookingId.toInt())
            .containsEntry("nomisPropertyContainerId", existingMapping.nomisPropertyContainerId.toInt())
            .containsEntry("dpsPropertyContainerId", existingMapping.dpsPropertyContainerId.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("bookingId", existingMapping.bookingId.toInt())
            .containsEntry("nomisPropertyContainerId", existingMapping.nomisPropertyContainerId.toInt())
            .containsEntry("dpsPropertyContainerId", dpsPropertyContainerId)
        }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/property/nomis-id/{id}")
  inner class GetMappingByNomisId {
    lateinit var mapping: PropertyContainerMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        PropertyContainerMapping(
          dpsPropertyContainerId = UUID.fromString(DPS_ID2),
          bookingId = BOOKING_ID,
          nomisPropertyContainerId = NOMIS_ID,
          offenderNo = OFFENDER_NO,
          label = "2023-01-01T12:45:12",
          mappingType = PropertyContainerMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/property/nomis-id/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/property/nomis-id/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/property/nomis-id/1")
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
          .uri("/mapping/property/nomis-id/99")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/property/nomis-id/$NOMIS_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(mapping.bookingId)
          .jsonPath("nomisPropertyContainerId").isEqualTo(mapping.nomisPropertyContainerId)
          .jsonPath("dpsPropertyContainerId").isEqualTo(mapping.dpsPropertyContainerId.toString())
          .jsonPath("mappingType").isEqualTo(mapping.mappingType.name)
          .jsonPath("label").isEqualTo(mapping.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/property/dps-id/{dpsPropertyContainerId}")
  inner class GetMappingsByDpsId {
    lateinit var mapping1: PropertyContainerMapping
    lateinit var mapping2: PropertyContainerMapping

    @BeforeEach
    fun setUp() {
      runTest {
        mapping1 = repository.save(
          PropertyContainerMapping(
            dpsPropertyContainerId = UUID.fromString(DPS_ID),
            bookingId = BOOKING_ID,
            nomisPropertyContainerId = NOMIS_ID,
            offenderNo = OFFENDER_NO,
            label = "2023-01-01T12:45:12",
            mappingType = PropertyContainerMappingType.MIGRATED,
          ),
        )
        mapping2 = repository.save(
          PropertyContainerMapping(
            dpsPropertyContainerId = UUID.fromString(DPS_ID2),
            nomisPropertyContainerId = NOMIS_ID2,
            offenderNo = OFFENDER_NO,
            bookingId = BOOKING_ID,
            label = "2023-06-01T12:45:12",
            mappingType = PropertyContainerMappingType.DPS_CREATED,
          ),
        )
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/property/dps-id/${mapping1.dpsPropertyContainerId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/property/dps-id/${mapping1.dpsPropertyContainerId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/property/dps-id/${mapping1.dpsPropertyContainerId}")
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
          .uri("/mapping/property/dps-id/00001111-0000-0000-0000-000011112222")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 200 when mapping does exist`() {
        webTestClient.get()
          .uri("/mapping/property/dps-id/${mapping1.dpsPropertyContainerId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsPropertyContainerId").isEqualTo(mapping1.dpsPropertyContainerId.toString())
          .jsonPath("bookingId").isEqualTo(mapping1.bookingId)
          .jsonPath("nomisPropertyContainerId").isEqualTo(mapping1.nomisPropertyContainerId)
          .jsonPath("offenderNo").isEqualTo(mapping1.offenderNo)
          .jsonPath("mappingType").isEqualTo(mapping1.mappingType.name)
          .jsonPath("label").isEqualTo(mapping1.label!!)
          .jsonPath("whenCreated").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          }
      }
    }
  }

  @DisplayName("GET /mapping/property/migration-id/{migrationId}/count")
  @Nested
  inner class GetMappingCountByMigrationIdTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/property/migration-id/2022-01-01T00:00:00/count")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/property/migration-id/2022-01-01T00:00:00/count")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/property/migration-id/2022-01-01T00:00:00/count")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping count success`() {
      (1L..5L).forEach {
        postCreateMappingRequest(
          it,
          it,
          generateUUIDs(it),
          OFFENDER_NO,
          label = "2022-01-01",
          mappingType = PropertyContainerMappingType.MIGRATED,
        )
      }

      webTestClient.get().uri("/mapping/property/migration-id/2022-01-01/count")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").isEqualTo(5)
    }
  }

  @DisplayName("GET /mapping/property/migrated/latest")
  @Nested
  inner class GeMappingMigratedLatestTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/property/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/property/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/property/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {
      webTestClient.post().uri("/mapping/property")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            PropertyContainerMappingDto(
              nomisPropertyContainerId = 10,
              dpsPropertyContainerId = generateUUIDs(10),
              offenderNo = OFFENDER_NO,
              bookingId = 10,
              label = "2022-01-01T00:00:00",
              mappingType = PropertyContainerMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/property")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            PropertyContainerMappingDto(
              nomisPropertyContainerId = 20,
              dpsPropertyContainerId = generateUUIDs(20),
              offenderNo = OFFENDER_NO,
              bookingId = BOOKING_ID,
              label = "2022-01-02T00:00:00",
              mappingType = PropertyContainerMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/property")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            PropertyContainerMappingDto(
              nomisPropertyContainerId = 1,
              dpsPropertyContainerId = generateUUIDs(1),
              offenderNo = OFFENDER_NO,
              bookingId = 2,
              label = "2022-01-02T10:00:00",
              mappingType = PropertyContainerMappingType.MIGRATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/property")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            PropertyContainerMappingDto(
              nomisPropertyContainerId = 99,
              dpsPropertyContainerId = generateUUIDs(199),
              offenderNo = OFFENDER_NO,
              bookingId = BOOKING_ID,
              label = "whatever",
              mappingType = PropertyContainerMappingType.DPS_CREATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/property/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody<PropertyContainerMappingDto>()
        .returnResult().responseBody!!

      assertThat(mapping.nomisPropertyContainerId).isEqualTo(1)
      assertThat(mapping.dpsPropertyContainerId).isEqualTo(generateUUIDs(1))
      assertThat(mapping.offenderNo).isEqualTo(OFFENDER_NO)
      assertThat(mapping.bookingId).isEqualTo(2)
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo(PropertyContainerMappingType.MIGRATED)
      assertThat(mapping.whenCreated)
        .isCloseTo(LocalDateTime.now(), Assertions.byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/property")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            PropertyContainerMappingDto(
              nomisPropertyContainerId = 77,
              dpsPropertyContainerId = generateUUIDs(77),
              offenderNo = OFFENDER_NO,
              bookingId = BOOKING_ID,
              label = "whatever",
              mappingType = PropertyContainerMappingType.DPS_CREATED,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/property/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/property/nomis-id/{id}")
  inner class DeleteMappingByNomisId {
    lateinit var mapping: PropertyContainerMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        PropertyContainerMapping(
          dpsPropertyContainerId = UUID.fromString(DPS_ID2),
          bookingId = BOOKING_ID,
          nomisPropertyContainerId = NOMIS_ID,
          offenderNo = OFFENDER_NO,
          label = "2023-01-01T12:45:12",
          mappingType = PropertyContainerMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/property/nomis-id/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/property/nomis-id/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/property/nomis-id/1")
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
          .uri("/mapping/property/nomis-id/99")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/property/nomis-id/$NOMIS_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/property/nomis-id/$NOMIS_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/property/nomis-id/$NOMIS_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/property/dps-id/{dpsId}")
  inner class DeleteMappingByDpsId {
    lateinit var mapping: PropertyContainerMapping

    @BeforeEach
    fun setUp() = runTest {
      mapping = repository.save(
        PropertyContainerMapping(
          dpsPropertyContainerId = UUID.fromString(DPS_ID2),
          bookingId = BOOKING_ID,
          nomisPropertyContainerId = NOMIS_ID,
          offenderNo = OFFENDER_NO,
          label = "2023-01-01T12:45:12",
          mappingType = PropertyContainerMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/property/dps-id/${mapping.dpsPropertyContainerId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/property/dps-id/${mapping.dpsPropertyContainerId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/property/dps-id/${mapping.dpsPropertyContainerId}")
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
          .uri("/mapping/property/dps-id/00001111-0000-0000-0000-000011112222")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `will return 204 when mapping does exist and is deleted`() {
        webTestClient.get()
          .uri("/mapping/property/dps-id/${mapping.dpsPropertyContainerId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.delete()
          .uri("/mapping/property/dps-id/${mapping.dpsPropertyContainerId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        webTestClient.get()
          .uri("/mapping/property/dps-id/${mapping.dpsPropertyContainerId}")
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
          .uri("/mapping/property/merge/from/A1234AA/to/A1234BB")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/property/merge/from/A1234AA/to/A1234BB")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/property/merge/from/A1234AA/to/A1234BB")
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
            PropertyContainerMapping(
              dpsPropertyContainerId = UUID.fromString(dps1),
              bookingId = BOOKING_ID,
              nomisPropertyContainerId = 54321,
              offenderNo = "A1234AA",
              mappingType = PropertyContainerMappingType.MIGRATED,
            ),
          )
          repository.save(
            PropertyContainerMapping(
              dpsPropertyContainerId = UUID.fromString(dps2),
              bookingId = 2,
              nomisPropertyContainerId = 54322,
              offenderNo = "A1234BB",
              mappingType = PropertyContainerMappingType.NOMIS_CREATED,
            ),
          )
          repository.save(
            PropertyContainerMapping(
              dpsPropertyContainerId = UUID.fromString(dps3),
              bookingId = 2,
              nomisPropertyContainerId = 54323,
              offenderNo = "A1234BB",
              mappingType = PropertyContainerMappingType.NOMIS_CREATED,
            ),
          )
        }
      }

      @Test
      fun `Merge success`() = runTest {
        webTestClient.put().uri("/mapping/property/merge/from/A1234AA/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        // first record has changed
        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo("B5678BB")

        // second has not
        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo("A1234BB")
      }

      @Test
      fun `Nothing happens if not found`() = runTest {
        webTestClient.put().uri("/mapping/property/merge/from/A9999AA/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisPropertyContainerId").isEqualTo(54321)
          .jsonPath("offenderNo").isEqualTo("A1234AA")
          .jsonPath("bookingId").isEqualTo(BOOKING_ID)

        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisPropertyContainerId").isEqualTo(54322)
          .jsonPath("offenderNo").isEqualTo("A1234BB")
          .jsonPath("bookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisPropertyContainerId").isEqualTo(54323)
          .jsonPath("offenderNo").isEqualTo("A1234BB")
          .jsonPath("bookingId").isEqualTo(2)
      }

      @Test
      fun `Merge success - multiple candidates`() = runTest {
        webTestClient.put().uri("/mapping/property/merge/from/A1234BB/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisPropertyContainerId").isEqualTo(54321)
          .jsonPath("offenderNo").isEqualTo("A1234AA")
          .jsonPath("bookingId").isEqualTo(BOOKING_ID)

        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisPropertyContainerId").isEqualTo(54322)
          .jsonPath("offenderNo").isEqualTo("B5678BB")
          .jsonPath("bookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisPropertyContainerId").isEqualTo(54323)
          .jsonPath("offenderNo").isEqualTo("B5678BB")
          .jsonPath("bookingId").isEqualTo(2)
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
          .uri("/mapping/property/merge/booking-id/333/to/A1234BB")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/property/merge/booking-id/333/to/A1234BB")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/property/merge/booking-id/333/to/A1234BB")
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
            PropertyContainerMapping(
              dpsPropertyContainerId = UUID.fromString(dps1),
              bookingId = BOOKING_ID,
              nomisPropertyContainerId = 54321,
              offenderNo = "A1234AA",
              mappingType = PropertyContainerMappingType.MIGRATED,
            ),
          )
          repository.save(
            PropertyContainerMapping(
              dpsPropertyContainerId = UUID.fromString(dps2),
              bookingId = 2,
              nomisPropertyContainerId = 54322,
              offenderNo = "A1234BB",
              mappingType = PropertyContainerMappingType.NOMIS_CREATED,
            ),
          )
          repository.save(
            PropertyContainerMapping(
              dpsPropertyContainerId = UUID.fromString(dps3),
              bookingId = 2,
              nomisPropertyContainerId = 54323,
              offenderNo = "A1234BB",
              mappingType = PropertyContainerMappingType.NOMIS_CREATED,
            ),
          )
        }
      }

      @Test
      fun `Move success`() = runTest {
        webTestClient.put().uri("/mapping/property/merge/booking-id/$BOOKING_ID/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  { 
    "nomisPropertyContainerId": 54321,
    "dpsPropertyContainerId": "$dps1",
    "bookingId": $BOOKING_ID
  }
]""",
          )

        // Check first record has changed
        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo("B5678BB")

        // second has not
        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo("A1234BB")
      }

      @Test
      fun `Nothing happens if not found`() = runTest {
        webTestClient.put().uri("/mapping/property/merge/booking-id/999/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("[]")

        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisPropertyContainerId").isEqualTo(54321)
          .jsonPath("offenderNo").isEqualTo("A1234AA")
          .jsonPath("bookingId").isEqualTo(BOOKING_ID)

        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisPropertyContainerId").isEqualTo(54322)
          .jsonPath("offenderNo").isEqualTo("A1234BB")
          .jsonPath("bookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisPropertyContainerId").isEqualTo(54323)
          .jsonPath("offenderNo").isEqualTo("A1234BB")
          .jsonPath("bookingId").isEqualTo(2)
      }

      @Test
      fun `Move success - multiple candidates`() = runTest {
        webTestClient.put().uri("/mapping/property/merge/booking-id/2/to/B5678BB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  { 
    "nomisPropertyContainerId": 54322,
    "dpsPropertyContainerId": "$dps2",
    "bookingId": 2
  },
  { 
    "nomisPropertyContainerId": 54323,
    "dpsPropertyContainerId": "$dps3",
    "bookingId": 2
  }
]""",
          )

        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisPropertyContainerId").isEqualTo(54321)
          .jsonPath("offenderNo").isEqualTo("A1234AA")
          .jsonPath("bookingId").isEqualTo(BOOKING_ID)

        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisPropertyContainerId").isEqualTo(54322)
          .jsonPath("offenderNo").isEqualTo("B5678BB")
          .jsonPath("bookingId").isEqualTo(2)

        webTestClient.get()
          .uri("/mapping/property/dps-id/$dps3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("nomisPropertyContainerId").isEqualTo(54323)
          .jsonPath("offenderNo").isEqualTo("B5678BB")
          .jsonPath("bookingId").isEqualTo(2)
      }
    }
  }
}
