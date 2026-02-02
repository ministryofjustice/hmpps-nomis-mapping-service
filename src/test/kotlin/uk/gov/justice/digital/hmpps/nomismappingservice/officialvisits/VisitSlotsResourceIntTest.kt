package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

import kotlinx.coroutines.test.runTest
import net.minidev.json.JSONArray
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
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class VisitSlotsResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var visitTimeSlotMappingRepository: VisitTimeSlotMappingRepository

  @Autowired
  private lateinit var visitSlotMappingRepository: VisitSlotMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    visitTimeSlotMappingRepository.deleteAll()
    visitSlotMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST /mapping/visit-slots/time-slots")
  inner class CreateTimeSlotMapping {
    val nomisPrisonId = "WWI"
    val nomisDayOfWeek = "MON"
    val nomisSlotSequence = 2
    val dpsId = "123456789"

    @Nested
    inner class Security {
      val mapping = VisitTimeSlotMappingDto(
        dpsId = dpsId,
        nomisPrisonId = nomisPrisonId,
        nomisDayOfWeek = nomisDayOfWeek,
        nomisSlotSequence = nomisSlotSequence,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/visit-slots/time-slots")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/visit-slots/time-slots")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/visit-slots/time-slots")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      val mapping = VisitTimeSlotMappingDto(
        dpsId = dpsId,
        nomisPrisonId = nomisPrisonId,
        nomisDayOfWeek = nomisDayOfWeek,
        nomisSlotSequence = nomisSlotSequence,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      val existingMapping = VisitTimeSlotMapping(
        dpsId = dpsId,
        nomisPrisonId = nomisPrisonId,
        nomisDayOfWeek = nomisDayOfWeek,
        nomisSlotSequence = nomisSlotSequence,
        label = "2019-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
      )

      @BeforeEach
      fun setUp() = runTest {
        visitTimeSlotMappingRepository.save(existingMapping)
      }

      @Test
      fun `will not allow the same time slot to have duplicate NOMIS ids`() {
        webTestClient.post()
          .uri("/mapping/visit-slots/time-slots")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(dpsId = "96969")))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will not allow the same time slot to have duplicate DPS ids`() {
        webTestClient.post()
          .uri("/mapping/visit-slots/time-slots")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(nomisSlotSequence = 999)))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/visit-slots/time-slots")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(dpsId = "96969")))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisPrisonId", existingMapping.nomisPrisonId)
            .containsEntry("nomisSlotSequence", existingMapping.nomisSlotSequence)
            .containsEntry("nomisDayOfWeek", existingMapping.nomisDayOfWeek)
            .containsEntry("dpsId", existingMapping.dpsId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisPrisonId", existingMapping.nomisPrisonId)
            .containsEntry("nomisSlotSequence", existingMapping.nomisSlotSequence)
            .containsEntry("nomisDayOfWeek", existingMapping.nomisDayOfWeek)
            .containsEntry("dpsId", "96969")
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = VisitTimeSlotMappingDto(
        dpsId = dpsId,
        nomisPrisonId = nomisPrisonId,
        nomisDayOfWeek = nomisDayOfWeek,
        nomisSlotSequence = nomisSlotSequence,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
      )

      @Test
      fun `returns 201 when mapping created`() = runTest {
        webTestClient.post()
          .uri("/mapping/visit-slots/time-slots")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the time slot mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/visit-slots/time-slots")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val timeSlotMapping =
          visitTimeSlotMappingRepository.findOneByDpsId(dpsId)!!

        assertThat(timeSlotMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(timeSlotMapping.nomisPrisonId).isEqualTo(mapping.nomisPrisonId)
        assertThat(timeSlotMapping.nomisDayOfWeek).isEqualTo(mapping.nomisDayOfWeek)
        assertThat(timeSlotMapping.nomisSlotSequence).isEqualTo(mapping.nomisSlotSequence)
        assertThat(timeSlotMapping.label).isEqualTo(mapping.label)
        assertThat(timeSlotMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(timeSlotMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/visit-slots/time-slots/nomis-prison-id/{nomisPrisonId}/nomis-day-of-week/{nomisDayOfWeek}/nomis-slot-sequence/{nomisSlotSequence}")
  inner class GetVisitTimeSlotMappingByNomisIds {
    val nomisPrisonId = "WWI"
    val nomisDayOfWeek = "MON"
    val nomisSlotSequence = 2
    val dpsId = "123456789"

    @BeforeEach
    fun setUp() = runTest {
      visitTimeSlotMappingRepository.save(
        VisitTimeSlotMapping(
          dpsId = dpsId,
          nomisPrisonId = nomisPrisonId,
          nomisDayOfWeek = nomisDayOfWeek,
          nomisSlotSequence = nomisSlotSequence,
          label = "2020-01-01T10:00",
          mappingType = StandardMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access unauthorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/$nomisSlotSequence")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/$nomisSlotSequence")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/$nomisSlotSequence")
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
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/99")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will retrieve mapping`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/$nomisSlotSequence")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsId)
          .jsonPath("nomisPrisonId").isEqualTo(nomisPrisonId)
          .jsonPath("nomisDayOfWeek").isEqualTo("MON")
          .jsonPath("nomisSlotSequence").isEqualTo(nomisSlotSequence)
          .jsonPath("label").isEqualTo("2020-01-01T10:00")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/visit-slots/time-slots/nomis-prison-id/{nomisPrisonId}/nomis-day-of-week/{nomisDayOfWeek}/nomis-slot-sequence/{nomisSlotSequence}")
  inner class DeleteVisitTimeSlotMappingByNomisIds {
    val nomisPrisonId = "WWI"
    val nomisDayOfWeek = "MON"
    val nomisSlotSequence = 2
    val dpsId = "123456789"

    @BeforeEach
    fun setUp() = runTest {
      visitTimeSlotMappingRepository.save(
        VisitTimeSlotMapping(
          dpsId = dpsId,
          nomisPrisonId = nomisPrisonId,
          nomisDayOfWeek = nomisDayOfWeek,
          nomisSlotSequence = nomisSlotSequence,
          label = "2020-01-01T10:00",
          mappingType = StandardMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access unauthorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/$nomisSlotSequence")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/$nomisSlotSequence")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/$nomisSlotSequence")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping does not exist`() {
        webTestClient.delete()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/99")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete mapping`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/$nomisSlotSequence")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
        webTestClient.delete()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/$nomisSlotSequence")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
        webTestClient.get()
          .uri("/mapping/visit-slots/time-slots/nomis-prison-id/$nomisPrisonId/nomis-day-of-week/$nomisDayOfWeek/nomis-slot-sequence/$nomisSlotSequence")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/visit-slots/visit-slot")
  inner class CreateVisitSlotMapping {
    val nomisId = 9876543321
    val dpsId = "123456789"

    @Nested
    inner class Security {
      val mapping = VisitSlotMappingDto(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/visit-slots/visit-slot")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/visit-slots/visit-slot")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/visit-slots/visit-slot")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      val mapping = VisitSlotMappingDto(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      val existingMapping = VisitSlotMapping(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2019-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
      )

      @BeforeEach
      fun setUp() = runTest {
        visitSlotMappingRepository.save(existingMapping)
      }

      @Test
      fun `will not allow the same time slot to have duplicate NOMIS ids`() {
        webTestClient.post()
          .uri("/mapping/visit-slots/visit-slot")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(dpsId = "96969")))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will not allow the same time slot to have duplicate DPS ids`() {
        webTestClient.post()
          .uri("/mapping/visit-slots/visit-slot")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(nomisId = 999)))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/visit-slots/visit-slot")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(dpsId = "96969")))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisId", existingMapping.nomisId)
            .containsEntry("dpsId", existingMapping.dpsId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", existingMapping.nomisId)
            .containsEntry("dpsId", "96969")
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = VisitSlotMappingDto(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
      )

      @Test
      fun `returns 201 when mapping created`() = runTest {
        webTestClient.post()
          .uri("/mapping/visit-slots/visit-slot")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the time slot mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/visit-slots/visit-slot")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val visitSlotMapping =
          visitSlotMappingRepository.findOneByDpsId(dpsId)!!

        assertThat(visitSlotMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(visitSlotMapping.nomisId).isEqualTo(mapping.nomisId)
        assertThat(visitSlotMapping.label).isEqualTo(mapping.label)
        assertThat(visitSlotMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(visitSlotMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/visit-slots/visit-slot/nomis-id/{nomisId}")
  inner class GetVisitSlotMappingByNomisIds {
    val nomisId = 9831302L
    val dpsId = "123456789"

    @BeforeEach
    fun setUp() = runTest {
      visitSlotMappingRepository.save(
        VisitSlotMapping(
          dpsId = dpsId,
          nomisId = nomisId,
          label = "2020-01-01T10:00",
          mappingType = StandardMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access unauthorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/visit-slot/nomis-id/$nomisId")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/visit-slot/nomis-id/$nomisId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/visit-slot/nomis-id/$nomisId")
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
          .uri("/mapping/visit-slots/visit-slot/nomis-id/999")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will retrieve mapping`() {
        webTestClient.get()
          .uri("/mapping/visit-slots/visit-slot/nomis-id/$nomisId")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsId)
          .jsonPath("nomisId").isEqualTo(nomisId)
          .jsonPath("label").isEqualTo("2020-01-01T10:00")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/visit-slots")
  inner class CreateMigrationMappings {
    val nomisPrisonId = "WWI"
    val nomisDayOfWeek = "MON"
    val nomisSlotSequence = 2
    val dpsId = "123456789"

    @Nested
    inner class Security {
      val mapping = VisitTimeSlotMigrationMappingDto(
        dpsId = dpsId,
        nomisPrisonId = nomisPrisonId,
        nomisDayOfWeek = nomisDayOfWeek,
        nomisSlotSequence = nomisSlotSequence,
        label = "2020-01-01T10:00",
        visitSlots = listOf(),
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/visit-slots")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/visit-slots")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/visit-slots")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      val mapping = VisitTimeSlotMigrationMappingDto(
        dpsId = dpsId,
        nomisPrisonId = nomisPrisonId,
        nomisDayOfWeek = nomisDayOfWeek,
        nomisSlotSequence = nomisSlotSequence,
        label = "2020-01-01T10:00",
        visitSlots = listOf(),
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      val existingMapping = VisitTimeSlotMapping(
        dpsId = dpsId,
        nomisPrisonId = nomisPrisonId,
        nomisDayOfWeek = nomisDayOfWeek,
        nomisSlotSequence = nomisSlotSequence,
        label = "2019-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
      )

      @BeforeEach
      fun setUp() = runTest {
        visitTimeSlotMappingRepository.save(existingMapping)
      }

      @Test
      fun `will not allow the same time slot to have duplicate NOMIS ids`() {
        webTestClient.post()
          .uri("/mapping/visit-slots")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(dpsId = "96969")))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will not allow the same time slot to have duplicate DPS ids`() {
        webTestClient.post()
          .uri("/mapping/visit-slots")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(nomisSlotSequence = 999)))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/visit-slots")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(dpsId = "96969")))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisPrisonId", existingMapping.nomisPrisonId)
            .containsEntry("nomisSlotSequence", existingMapping.nomisSlotSequence)
            .containsEntry("nomisDayOfWeek", existingMapping.nomisDayOfWeek)
            .containsEntry("dpsId", existingMapping.dpsId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisPrisonId", existingMapping.nomisPrisonId)
            .containsEntry("nomisSlotSequence", existingMapping.nomisSlotSequence)
            .containsEntry("nomisDayOfWeek", existingMapping.nomisDayOfWeek)
            .containsEntry("dpsId", "96969")
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = VisitTimeSlotMigrationMappingDto(
        dpsId = dpsId,
        nomisPrisonId = nomisPrisonId,
        nomisDayOfWeek = nomisDayOfWeek,
        nomisSlotSequence = nomisSlotSequence,
        label = "2020-01-01T10:00",
        visitSlots = listOf(VisitSlotMigrationMappingDto(dpsId = "99999", nomisId = 99999), VisitSlotMigrationMappingDto(dpsId = "99998", nomisId = 99998)),
        mappingType = StandardMappingType.MIGRATED,
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/visit-slots")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the time slot mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/visit-slots")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val timeSlotMapping =
          visitTimeSlotMappingRepository.findOneByDpsId(dpsId)!!

        assertThat(timeSlotMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(timeSlotMapping.nomisPrisonId).isEqualTo(mapping.nomisPrisonId)
        assertThat(timeSlotMapping.nomisDayOfWeek).isEqualTo(mapping.nomisDayOfWeek)
        assertThat(timeSlotMapping.nomisSlotSequence).isEqualTo(mapping.nomisSlotSequence)
        assertThat(timeSlotMapping.label).isEqualTo(mapping.label)
        assertThat(timeSlotMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(timeSlotMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }

      @Test
      fun `will persist the visit  slot mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/visit-slots")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        with(visitSlotMappingRepository.findOneByDpsId("99999")!!) {
          assertThat(this.dpsId).isEqualTo("99999")
          assertThat(this.nomisId).isEqualTo(99999)
          assertThat(this.label).isEqualTo(mapping.label)
          assertThat(this.mappingType).isEqualTo(mapping.mappingType)
          assertThat(this.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(visitSlotMappingRepository.findOneByDpsId("99998")!!) {
          assertThat(this.dpsId).isEqualTo("99998")
          assertThat(this.nomisId).isEqualTo(99998)
          assertThat(this.label).isEqualTo(mapping.label)
          assertThat(this.mappingType).isEqualTo(mapping.mappingType)
          assertThat(this.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }
    }
  }

  @DisplayName("GET /mapping/visit-slots/migration-id/{migrationId}")
  @Nested
  inner class GetVisitTimeSlotMappingsByMigrationId {
    val nomisPrisonId = "WWI"
    val nomisDayOfWeek = "MON"

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/visit-slots/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/visit-slots/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/visit-slots/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `can retrieve all mappings by migration Id`() = runTest {
        (1L..4).forEach {
          visitTimeSlotMappingRepository.save(
            VisitTimeSlotMapping(
              dpsId = "$it",
              nomisPrisonId = nomisPrisonId,
              nomisDayOfWeek = nomisDayOfWeek,
              nomisSlotSequence = it.toInt(),
              label = "2023-01-01T12:45:12",
              mappingType = StandardMappingType.MIGRATED,
            ),
          )
        }

        visitTimeSlotMappingRepository.save(
          VisitTimeSlotMapping(
            dpsId = "999",
            nomisPrisonId = nomisPrisonId,
            nomisDayOfWeek = nomisDayOfWeek,
            nomisSlotSequence = 9999,
            label = "2022-01-01T12:43:12",
            mappingType = StandardMappingType.MIGRATED,
          ),
        )

        webTestClient.get().uri("/mapping/visit-slots/migration-id/2023-01-01T12:45:12")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(4)
          .jsonPath("$.content..nomisSlotSequence").value<JSONArray> {
            assertThat(it).contains(
              1,
              2,
              3,
              4,
            )
          }
          .jsonPath("$.content[0].whenCreated").isNotEmpty
      }

      @Test
      fun `200 response even when no mappings are found`() {
        webTestClient.get().uri("/mapping/visit-slots/migration-id/2044-01-01")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(0)
          .jsonPath("content").isEmpty
      }

      @Test
      fun `can request a different page size`() = runTest {
        (1L..6L).forEach {
          visitTimeSlotMappingRepository.save(
            VisitTimeSlotMapping(
              dpsId = "$it",
              nomisPrisonId = nomisPrisonId,
              nomisDayOfWeek = nomisDayOfWeek,
              nomisSlotSequence = it.toInt(),
              label = "2023-01-01T12:45:12",
              mappingType = StandardMappingType.MIGRATED,
            ),
          )
        }
        webTestClient.get().uri {
          it.path("/mapping/visit-slots/migration-id/2023-01-01T12:45:12")
            .queryParam("size", "2")
            .queryParam("sort", "nomisSlotSequence,asc")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
  @DisplayName("DELETE /mapping/visit-slots/all")
  inner class DeleteAllMappings {

    @BeforeEach
    fun setUp() = runTest {
      visitTimeSlotMappingRepository.save(
        VisitTimeSlotMapping(
          dpsId = "123456789",
          nomisPrisonId = "WWI",
          nomisDayOfWeek = "MON",
          nomisSlotSequence = 1,
          label = "2020-01-01T10:00",
          mappingType = StandardMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
        ),
      )
      visitTimeSlotMappingRepository.save(
        VisitTimeSlotMapping(
          dpsId = "223456789",
          nomisPrisonId = "WWI",
          nomisDayOfWeek = "MON",
          nomisSlotSequence = 2,
          label = "2020-01-01T10:00",
          mappingType = StandardMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
        ),
      )
      visitSlotMappingRepository.save(
        VisitSlotMapping(
          dpsId = "123456789",
          nomisId = 123456789,
          label = "2020-01-01T10:00",
          mappingType = StandardMappingType.MIGRATED,
        ),
      )
      visitSlotMappingRepository.save(
        VisitSlotMapping(
          dpsId = "223456789",
          nomisId = 223456789,
          label = "2020-01-01T10:00",
          mappingType = StandardMappingType.MIGRATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access unauthorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/visit-slots/all")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/visit-slots/all")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/visit-slots/all")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete all mappings`() = runTest {
        assertThat(visitTimeSlotMappingRepository.count()).isEqualTo(2)
        assertThat(visitSlotMappingRepository.count()).isEqualTo(2)

        webTestClient.delete()
          .uri("/mapping/visit-slots/all")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(visitTimeSlotMappingRepository.count()).isEqualTo(0)
        assertThat(visitSlotMappingRepository.count()).isEqualTo(0)
      }
    }
  }
}
