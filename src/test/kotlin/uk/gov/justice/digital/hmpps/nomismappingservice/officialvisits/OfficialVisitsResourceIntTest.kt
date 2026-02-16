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

class OfficialVisitsResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var officialVisitMappingRepository: OfficialVisitMappingRepository

  @Autowired
  private lateinit var visitorMappingRepository: VisitorMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    officialVisitMappingRepository.deleteAll()
    visitorMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /mapping/official-visits/visit/nomis-id/{nomisId}")
  inner class GetOfficialVisitMappingByNomisId {
    val nomisId = 73737L
    val dpsId = "123456789"

    @BeforeEach
    fun setUp() = runTest {
      officialVisitMappingRepository.save(
        OfficialVisitMapping(
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
          .uri("/mapping/official-visits/visit/nomis-id/$nomisId")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/official-visits/visit/nomis-id/$nomisId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/official-visits/visit/nomis-id/$nomisId")
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
          .uri("/mapping/official-visits/visit/nomis-id/99")
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
          .uri("/mapping/official-visits/visit/nomis-id/$nomisId")
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
  @DisplayName("GET /mapping/official-visits/visit/dps-id/{dpsId}")
  inner class GetOfficialVisitMappingByDpsId {
    val nomisId = 73737L
    val dpsId = "123456789"

    @BeforeEach
    fun setUp() = runTest {
      officialVisitMappingRepository.save(
        OfficialVisitMapping(
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
          .uri("/mapping/official-visits/visit/dps-id/$dpsId")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/official-visits/visit/dps-id/$dpsId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/official-visits/visit/dps-id/$dpsId")
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
          .uri("/mapping/official-visits/visit/dps-id/99")
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
          .uri("/mapping/official-visits/visit/dps-id/$dpsId")
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
  @DisplayName("POST /mapping/official-visits")
  inner class CreateMigrationMappings {
    val nomisId = 123L
    val dpsId = "123456789"

    @Nested
    inner class Security {
      val mapping = OfficialVisitMigrationMappingDto(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2020-01-01T10:00",
        visitors = listOf(),
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/official-visits")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/official-visits")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/official-visits")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      val mapping = OfficialVisitMigrationMappingDto(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2020-01-01T10:00",
        visitors = listOf(),
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      val existingMapping = OfficialVisitMapping(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2019-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
      )

      @BeforeEach
      fun setUp() = runTest {
        officialVisitMappingRepository.save(existingMapping)
      }

      @Test
      fun `will not allow the same visit to have duplicate NOMIS ids`() {
        webTestClient.post()
          .uri("/mapping/official-visits")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(dpsId = "96969")))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will not allow the same visit to have duplicate DPS ids`() {
        webTestClient.post()
          .uri("/mapping/official-visits")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(nomisId = 999)))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/official-visits")
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
            .containsEntry("nomisId", existingMapping.nomisId.toInt())
            .containsEntry("dpsId", existingMapping.dpsId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", existingMapping.nomisId.toInt())
            .containsEntry("dpsId", "96969")
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = OfficialVisitMigrationMappingDto(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2020-01-01T10:00",
        visitors = listOf(VisitorMigrationMappingDto(dpsId = "99999", nomisId = 99999), VisitorMigrationMappingDto(dpsId = "99998", nomisId = 99998)),
        mappingType = StandardMappingType.MIGRATED,
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/official-visits")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the visit mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/official-visits")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val visitMapping =
          officialVisitMappingRepository.findOneByDpsId(dpsId)!!

        assertThat(visitMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(visitMapping.nomisId).isEqualTo(mapping.nomisId)
        assertThat(visitMapping.label).isEqualTo(mapping.label)
        assertThat(visitMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(visitMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }

      @Test
      fun `will persist the visitor mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/official-visits")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        with(visitorMappingRepository.findOneByDpsId("99999")!!) {
          assertThat(this.dpsId).isEqualTo("99999")
          assertThat(this.nomisId).isEqualTo(99999)
          assertThat(this.label).isEqualTo(mapping.label)
          assertThat(this.mappingType).isEqualTo(mapping.mappingType)
          assertThat(this.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(visitorMappingRepository.findOneByDpsId("99998")!!) {
          assertThat(this.dpsId).isEqualTo("99998")
          assertThat(this.nomisId).isEqualTo(99998)
          assertThat(this.label).isEqualTo(mapping.label)
          assertThat(this.mappingType).isEqualTo(mapping.mappingType)
          assertThat(this.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/official-visits/visit")
  inner class CreateVisitMapping {
    val nomisId = 123L
    val dpsId = "123456789"

    @Nested
    inner class Security {
      val mapping = OfficialVisitMappingDto(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/official-visits/visit")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/official-visits/visit")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/official-visits/visit")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      val mapping = OfficialVisitMappingDto(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      val existingMapping = OfficialVisitMapping(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2019-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
      )

      @BeforeEach
      fun setUp() = runTest {
        officialVisitMappingRepository.save(existingMapping)
      }

      @Test
      fun `will not allow the same visit to have duplicate NOMIS ids`() {
        webTestClient.post()
          .uri("/mapping/official-visits/visit")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(dpsId = "96969")))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will not allow the same visit to have duplicate DPS ids`() {
        webTestClient.post()
          .uri("/mapping/official-visits/visit")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(nomisId = 999)))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/official-visits/visit")
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
            .containsEntry("nomisId", existingMapping.nomisId.toInt())
            .containsEntry("dpsId", existingMapping.dpsId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", existingMapping.nomisId.toInt())
            .containsEntry("dpsId", "96969")
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = OfficialVisitMappingDto(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/official-visits/visit")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the visit mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/official-visits/visit")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val visitMapping =
          officialVisitMappingRepository.findOneByDpsId(dpsId)!!

        assertThat(visitMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(visitMapping.nomisId).isEqualTo(mapping.nomisId)
        assertThat(visitMapping.label).isEqualTo(mapping.label)
        assertThat(visitMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(visitMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/official-visits/visitor")
  inner class CreateVisitorMapping {
    val nomisId = 123L
    val dpsId = "123456789"

    @Nested
    inner class Security {
      val mapping = OfficialVisitorMappingDto(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/official-visits/visitor")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/official-visits/visitor")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/official-visits/visitor")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      val mapping = OfficialVisitorMappingDto(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      val existingMapping = VisitorMapping(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2019-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
      )

      @BeforeEach
      fun setUp() = runTest {
        visitorMappingRepository.save(existingMapping)
      }

      @Test
      fun `will not allow the same visitor to have duplicate NOMIS ids`() {
        webTestClient.post()
          .uri("/mapping/official-visits/visitor")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(dpsId = "96969")))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will not allow the same visitor to have duplicate DPS ids`() {
        webTestClient.post()
          .uri("/mapping/official-visits/visitor")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(nomisId = 999)))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/official-visits/visitor")
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
            .containsEntry("nomisId", existingMapping.nomisId.toInt())
            .containsEntry("dpsId", existingMapping.dpsId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", existingMapping.nomisId.toInt())
            .containsEntry("dpsId", "96969")
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = OfficialVisitorMappingDto(
        dpsId = dpsId,
        nomisId = nomisId,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/official-visits/visitor")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the visitor mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/official-visits/visitor")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val visitMapping =
          visitorMappingRepository.findOneByDpsId(dpsId)!!

        assertThat(visitMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(visitMapping.nomisId).isEqualTo(mapping.nomisId)
        assertThat(visitMapping.label).isEqualTo(mapping.label)
        assertThat(visitMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(visitMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/official-visits/visitor/nomis-id/{nomisId}")
  inner class GetOfficialVisitorMappingByNomisId {
    val nomisId = 73737L
    val dpsId = "123456789"

    @BeforeEach
    fun setUp() = runTest {
      visitorMappingRepository.save(
        VisitorMapping(
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
          .uri("/mapping/official-visits/visitor/nomis-id/$nomisId")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/official-visits/visitor/nomis-id/$nomisId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/official-visits/visitor/nomis-id/$nomisId")
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
          .uri("/mapping/official-visits/visitor/nomis-id/99")
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
          .uri("/mapping/official-visits/visitor/nomis-id/$nomisId")
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
  @DisplayName("GET /mapping/official-visits/visitor/dps-id/{dpsId}")
  inner class GetOfficialVisitorMappingByDpsId {
    val nomisId = 73737L
    val dpsId = "123456789"

    @BeforeEach
    fun setUp() = runTest {
      visitorMappingRepository.save(
        VisitorMapping(
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
          .uri("/mapping/official-visits/visitor/dps-id/$dpsId")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/official-visits/visitor/dps-id/$dpsId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/official-visits/visitor/dps-id/$dpsId")
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
          .uri("/mapping/official-visits/visitor/dps-id/99")
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
          .uri("/mapping/official-visits/visitor/dps-id/$dpsId")
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

  @DisplayName("GET /mapping/official-visits/migration-id/{migrationId}")
  @Nested
  inner class GetOfficialVisitMappingsByMigrationId {
    val nomisId = 837383L

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/official-visits/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/official-visits/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/official-visits/migration-id/2022-01-01T00:00:00")
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
          officialVisitMappingRepository.save(
            OfficialVisitMapping(
              dpsId = "$it",
              nomisId = it,
              label = "2023-01-01T12:45:12",
              mappingType = StandardMappingType.MIGRATED,
            ),
          )
        }

        officialVisitMappingRepository.save(
          OfficialVisitMapping(
            dpsId = "999",
            nomisId = nomisId,
            label = "2022-01-01T12:43:12",
            mappingType = StandardMappingType.MIGRATED,
          ),
        )

        webTestClient.get().uri("/mapping/official-visits/migration-id/2023-01-01T12:45:12")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(4)
          .jsonPath("$.content..nomisId").value<JSONArray> {
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
        webTestClient.get().uri("/mapping/official-visits/migration-id/2044-01-01")
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
          officialVisitMappingRepository.save(
            OfficialVisitMapping(
              dpsId = "$it",
              nomisId = it,
              label = "2023-01-01T12:45:12",
              mappingType = StandardMappingType.MIGRATED,
            ),
          )
        }
        webTestClient.get().uri {
          it.path("/mapping/official-visits/migration-id/2023-01-01T12:45:12")
            .queryParam("size", "2")
            .queryParam("sort", "nomisId,asc")
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
  @DisplayName("DELETE /mapping/official-visits/visit/nomis-id/{nomisVisitId}")
  inner class DeleteMappingByNomisId {

    @BeforeEach
    fun setUp() = runTest {
      officialVisitMappingRepository.save(
        OfficialVisitMapping(
          dpsId = "123456789",
          nomisId = 123,
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
          .uri("/mapping/official-visits/visit/nomis-id/123")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/official-visits/visit/nomis-id/123")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/official-visits/visit/nomis-id/123")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping`() = runTest {
        assertThat(officialVisitMappingRepository.findOneByNomisId(123)).isNotNull

        webTestClient.delete()
          .uri("/mapping/official-visits/visit/nomis-id/123")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(officialVisitMappingRepository.findOneByNomisId(123)).isNull()
      }

      @Test
      fun `will return 204 even if mapping does not exist`() = runTest {
        webTestClient.delete()
          .uri("/mapping/official-visits/visit/nomis-id/456")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/official-visits/visitor/nomis-id/{nomisVisitorId}")
  inner class DeleteVisitorMappingByNomisId {

    @BeforeEach
    fun setUp() = runTest {
      visitorMappingRepository.save(
        VisitorMapping(
          dpsId = "123456789",
          nomisId = 123,
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
          .uri("/mapping/official-visits/visitor/nomis-id/123")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/official-visits/visitor/nomis-id/123")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/official-visits/visitor/nomis-id/123")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping`() = runTest {
        assertThat(visitorMappingRepository.findOneByNomisId(123)).isNotNull

        webTestClient.delete()
          .uri("/mapping/official-visits/visitor/nomis-id/123")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(visitorMappingRepository.findOneByNomisId(123)).isNull()
      }

      @Test
      fun `will return 204 even if mapping does not exist`() = runTest {
        webTestClient.delete()
          .uri("/mapping/official-visits/visitor/nomis-id/456")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/official-visits/all")
  inner class DeleteAllMappings {

    @BeforeEach
    fun setUp() = runTest {
      officialVisitMappingRepository.save(
        OfficialVisitMapping(
          dpsId = "123456789",
          nomisId = 1,
          label = "2020-01-01T10:00",
          mappingType = StandardMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
        ),
      )
      officialVisitMappingRepository.save(
        OfficialVisitMapping(
          dpsId = "223456789",
          nomisId = 2,
          label = "2020-01-01T10:00",
          mappingType = StandardMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
        ),
      )
      visitorMappingRepository.save(
        VisitorMapping(
          dpsId = "123456789",
          nomisId = 123456789,
          label = "2020-01-01T10:00",
          mappingType = StandardMappingType.MIGRATED,
        ),
      )
      visitorMappingRepository.save(
        VisitorMapping(
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
          .uri("/mapping/official-visits/all")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/official-visits/all")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/official-visits/all")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete all mappings`() = runTest {
        assertThat(officialVisitMappingRepository.count()).isEqualTo(2)
        assertThat(visitorMappingRepository.count()).isEqualTo(2)

        webTestClient.delete()
          .uri("/mapping/official-visits/all")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(officialVisitMappingRepository.count()).isEqualTo(0)
        assertThat(visitorMappingRepository.count()).isEqualTo(0)
      }
    }
  }
}
