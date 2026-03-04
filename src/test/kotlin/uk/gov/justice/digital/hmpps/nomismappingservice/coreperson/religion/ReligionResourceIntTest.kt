package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.religion

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

class ReligionResourceIntTest(
  @Autowired private val religionsMappingRepository: ReligionsMappingRepository,
  @Autowired private val religionMappingRepository: ReligionMappingRepository,
) : IntegrationTestBase() {

  @AfterEach
  fun tearDown() = runTest {
    religionsMappingRepository.deleteAll()
    religionMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /mapping/core-person-religion/religions/nomis-prison-number/{nomisPrisonNumber}")
  inner class GetReligionsMappingByNomisPrisonNumber {
    val nomisPrisonNumber = "A1234BC"
    val cprId = "123456789"

    @BeforeEach
    fun setUp() = runTest {
      religionsMappingRepository.save(
        CorePersonReligionsMapping(
          cprId = cprId,
          nomisPrisonNumber = nomisPrisonNumber,
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
          .uri("/mapping/core-person-religion/religions/nomis-prison-number/$nomisPrisonNumber")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person-religion/religions/nomis-prison-number/$nomisPrisonNumber")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person-religion/religions/nomis-prison-number/$nomisPrisonNumber")
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
          .uri("/mapping/core-person-religion/religions/nomis-prison-number/A2345BC")
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
          .uri("/mapping/core-person-religion/religions/nomis-prison-number/$nomisPrisonNumber")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprId)
          .jsonPath("nomisPrisonNumber").isEqualTo(nomisPrisonNumber)
          .jsonPath("label").isEqualTo("2020-01-01T10:00")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/core-person-religion/religion")
  inner class CreateReligionMapping {
    val nomisId = 9876543321
    val cprId = "123456789"
    val nomisPrisonNumber = "A1234BC"

    @Nested
    inner class Security {
      val mapping = ReligionMappingDto(
        cprId = cprId,
        nomisId = nomisId,
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person-religion/religion")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person-religion/religion")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person-religion/religion")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      val mapping = ReligionMappingDto(
        cprId = cprId,
        nomisId = nomisId,
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      val existingMapping = CorePersonReligionMapping(
        cprId = cprId,
        nomisId = nomisId,
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
      )

      @BeforeEach
      fun setUp() = runTest {
        religionMappingRepository.save(existingMapping)
      }

      @Test
      fun `will not allow the same religion to have duplicate NOMIS ids`() {
        webTestClient.post()
          .uri("/mapping/core-person-religion/religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(cprId = "96969")))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will not allow the same religion to have duplicate CPR ids`() {
        webTestClient.post()
          .uri("/mapping/core-person-religion/religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(nomisId = 999)))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/core-person-religion/religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(cprId = "96969")))
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
            .containsEntry("cprId", existingMapping.cprId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", existingMapping.nomisId)
            .containsEntry("cprId", "96969")
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = ReligionMappingDto(
        cprId = cprId,
        nomisId = nomisId,
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2020-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
      )

      @Test
      fun `returns 201 when mapping created`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person-religion/religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the religion mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person-religion/religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val religionMapping =
          religionMappingRepository.findOneByCprId(cprId)!!

        assertThat(religionMapping.cprId).isEqualTo(mapping.cprId)
        assertThat(religionMapping.nomisId).isEqualTo(mapping.nomisId)
        assertThat(religionMapping.label).isEqualTo(mapping.label)
        assertThat(religionMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(religionMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person-religion/religion/nomis-id/{nomisId}")
  inner class GetReligionMappingByNomisId {
    val nomisId = 9831302L
    val cprId = "123456789"
    val nomisPrisonNumber = "A1234BC"

    @BeforeEach
    fun setUp() = runTest {
      religionMappingRepository.save(
        CorePersonReligionMapping(
          cprId = cprId,
          nomisId = nomisId,
          nomisPrisonNumber = nomisPrisonNumber,
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
          .uri("/mapping/core-person-religion/religion/nomis-id/$nomisId")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person-religion/religion/nomis-id/$nomisId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person-religion/religion/nomis-id/$nomisId")
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
          .uri("/mapping/core-person-religion/religion/nomis-id/999")
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
          .uri("/mapping/core-person-religion/religion/nomis-id/$nomisId")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprId)
          .jsonPath("nomisId").isEqualTo(nomisId)
          .jsonPath("label").isEqualTo("2020-01-01T10:00")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person-religion/religion/cpr-id/{cprId}")
  inner class GetReligionMappingByCprId {
    private val nomisId = 9831302L
    private val cprId = "123456789"
    private val nomisPrisonNumber = "A1234BC"

    @BeforeEach
    fun setUp() = runTest {
      religionMappingRepository.save(
        CorePersonReligionMapping(
          cprId = cprId,
          nomisId = nomisId,
          nomisPrisonNumber = nomisPrisonNumber,
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
          .uri("/mapping/core-person-religion/religion/cpr-id/$cprId")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person-religion/religion/cpr-id/$cprId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person-religion/religion/cpr-id/$cprId")
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
          .uri("/mapping/core-person-religion/religion/cpr-id/999")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will retrieve mapping by cprId`() {
        webTestClient.get()
          .uri("/mapping/core-person-religion/religion/cpr-id/$cprId")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprId)
          .jsonPath("nomisId").isEqualTo(nomisId)
          .jsonPath("nomisPrisonNumber").isEqualTo(nomisPrisonNumber)
          .jsonPath("label").isEqualTo("2020-01-01T10:00")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person-religion/religion/nomis-id/{nomisId}")
  inner class DeleteReligionMappingByNomisId {
    val nomisId = 9831302L
    val cprId = "123456789"
    val nomisPrisonNumber = "A1234BC"

    @BeforeEach
    fun setUp() = runTest {
      religionMappingRepository.save(
        CorePersonReligionMapping(
          cprId = cprId,
          nomisId = nomisId,
          nomisPrisonNumber = nomisPrisonNumber,
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
          .uri("/mapping/core-person-religion/religion/nomis-id/$nomisId")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/core-person-religion/religion/nomis-id/$nomisId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/core-person-religion/religion/nomis-id/$nomisId")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping not found`() {
        webTestClient.delete()
          .uri("/mapping/core-person-religion/religion/nomis-id/999")
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
          .uri("/mapping/core-person-religion/religion/nomis-id/$nomisId")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
        webTestClient.delete()
          .uri("/mapping/core-person-religion/religion/nomis-id/$nomisId")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
        webTestClient.get()
          .uri("/mapping/core-person-religion/religion/nomis-id/$nomisId")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/core-person-religion")
  inner class CreateMigrationMappings {
    val nomisPrisonNumber = "A1234BC"
    val cprId = "123456789"

    @Nested
    inner class Security {
      val mapping = ReligionsMigrationMappingDto(
        cprId = cprId,
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2020-01-01T10:00",
        religions = listOf(),
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person-religion")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person-religion")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person-religion")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      val mapping = ReligionsMigrationMappingDto(
        cprId = cprId,
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2020-01-01T10:00",
        religions = listOf(),
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      val existingMapping = CorePersonReligionsMapping(
        cprId = cprId,
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = StandardMappingType.MIGRATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
      )

      @BeforeEach
      fun setUp() = runTest {
        religionsMappingRepository.save(existingMapping)
      }

      @Test
      fun `will not allow the same religion to have duplicate NOMIS ids`() {
        webTestClient.post()
          .uri("/mapping/core-person-religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(cprId = "96969")))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will not allow the same religion to have duplicate CPR ids`() {
        webTestClient.post()
          .uri("/mapping/core-person-religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(nomisPrisonNumber = "B1234BC")))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/core-person-religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(cprId = "96969")))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisPrisonNumber", existingMapping.nomisPrisonNumber)
            .containsEntry("cprId", existingMapping.cprId)
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisPrisonNumber", existingMapping.nomisPrisonNumber)
            .containsEntry("cprId", "96969")
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = ReligionsMigrationMappingDto(
        cprId = cprId,
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2020-01-01T10:00",
        religions = listOf(
          ReligionMigrationMappingDto(cprId = "99999", nomisId = 99999, nomisPrisonNumber = nomisPrisonNumber),
          ReligionMigrationMappingDto(cprId = "99998", nomisId = 99998, nomisPrisonNumber = nomisPrisonNumber),
        ),
        mappingType = StandardMappingType.MIGRATED,
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person-religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the religions mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person-religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val religionsMapping =
          religionsMappingRepository.findOneByCprId(cprId)!!

        assertThat(religionsMapping.cprId).isEqualTo(mapping.cprId)
        assertThat(religionsMapping.nomisPrisonNumber).isEqualTo(mapping.nomisPrisonNumber)
        assertThat(religionsMapping.label).isEqualTo(mapping.label)
        assertThat(religionsMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(religionsMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }

      @Test
      fun `will persist the religion mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person-religion")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        with(religionMappingRepository.findOneByCprId("99999")!!) {
          assertThat(this.cprId).isEqualTo("99999")
          assertThat(this.nomisId).isEqualTo(99999)
          assertThat(this.label).isEqualTo(mapping.label)
          assertThat(this.mappingType).isEqualTo(mapping.mappingType)
          assertThat(this.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(religionMappingRepository.findOneByCprId("99998")!!) {
          assertThat(this.cprId).isEqualTo("99998")
          assertThat(this.nomisId).isEqualTo(99998)
          assertThat(this.label).isEqualTo(mapping.label)
          assertThat(this.mappingType).isEqualTo(mapping.mappingType)
          assertThat(this.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }
    }
  }

  @DisplayName("GET /mapping/core-person-religion/migration-id/{migrationId}")
  @Nested
  inner class GetReligionsMappingsByMigrationId {
    val nomisPrisonNumber = "A1234BC"

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/core-person-religion/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/core-person-religion/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/core-person-religion/migration-id/2022-01-01T00:00:00")
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
          religionsMappingRepository.save(
            CorePersonReligionsMapping(
              cprId = "$it",
              nomisPrisonNumber = "A123${it}BD",
              label = "2023-01-01T12:45:12",
              mappingType = StandardMappingType.MIGRATED,
            ),
          )
        }

        religionsMappingRepository.save(
          CorePersonReligionsMapping(
            cprId = "999",
            nomisPrisonNumber = nomisPrisonNumber,
            label = "2022-01-01T12:43:12",
            mappingType = StandardMappingType.MIGRATED,
          ),
        )

        webTestClient.get().uri("/mapping/core-person-religion/migration-id/2023-01-01T12:45:12")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(4)
          .jsonPath("$.content..nomisPrisonNumber").value<JSONArray> {
            assertThat(it).contains(
              "A1231BD",
              "A1232BD",
              "A1233BD",
              "A1234BD",
            )
          }
          .jsonPath("$.content[0].whenCreated").isNotEmpty
      }

      @Test
      fun `200 response even when no mappings are found`() {
        webTestClient.get().uri("/mapping/core-person-religion/migration-id/2044-01-01")
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
          religionsMappingRepository.save(
            CorePersonReligionsMapping(
              cprId = "$it",
              nomisPrisonNumber = "A123${it}BC",
              label = "2023-01-01T12:45:12",
              mappingType = StandardMappingType.MIGRATED,
            ),
          )
        }
        webTestClient.get().uri {
          it.path("/mapping/core-person-religion/migration-id/2023-01-01T12:45:12")
            .queryParam("size", "2")
            .queryParam("sort", "nomisPrisonNumber,asc")
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
  @DisplayName("DELETE /mapping/core-person-religion/all")
  inner class DeleteAllMappings {

    @BeforeEach
    fun setUp() = runTest {
      religionsMappingRepository.save(
        CorePersonReligionsMapping(
          cprId = "123456789",
          nomisPrisonNumber = "A1234BC",
          label = "2020-01-01T10:00",
          mappingType = StandardMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
        ),
      )
      religionsMappingRepository.save(
        CorePersonReligionsMapping(
          cprId = "223456789",
          nomisPrisonNumber = "B2345CD",
          label = "2020-01-01T10:00",
          mappingType = StandardMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
        ),
      )
      religionMappingRepository.save(
        CorePersonReligionMapping(
          cprId = "123456789",
          nomisId = 123456789,
          nomisPrisonNumber = "A1234BC",
          label = "2020-01-01T10:00",
          mappingType = StandardMappingType.MIGRATED,
        ),
      )
      religionMappingRepository.save(
        CorePersonReligionMapping(
          cprId = "223456789",
          nomisId = 223456789,
          nomisPrisonNumber = "A1234BC",
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
          .uri("/mapping/core-person-religion/all")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/core-person-religion/all")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/core-person-religion/all")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete all mappings`() = runTest {
        assertThat(religionsMappingRepository.count()).isEqualTo(2)
        assertThat(religionMappingRepository.count()).isEqualTo(2)

        webTestClient.delete()
          .uri("/mapping/core-person-religion/all")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(religionsMappingRepository.count()).isEqualTo(0)
        assertThat(religionMappingRepository.count()).isEqualTo(0)
      }
    }
  }
}
