package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.religion

import kotlinx.coroutines.flow.collect
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
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ReligionResourceIntTest(
  @Autowired private val religionMappingRepository: ReligionMappingRepository,
) : IntegrationTestBase() {

  @AfterEach
  fun tearDown() = runTest {
    religionMappingRepository.deleteAll()
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
          mappingType = StandardMappingType.NOMIS_CREATED,
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
          .jsonPath("mappingType").isEqualTo("NOMIS_CREATED")
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
  @DisplayName("GET /mapping/core-person-religion/religion/cpr-ids")
  inner class GetReligionMappingsByCprId {
    private val ids = arrayOf("61f19a0a-dc46-4771-b6d2-e920c6bd151f", "a16877a5-f5fc-4a58-a08b-cfe84d653f5b")

    @BeforeEach
    fun setUp() = runTest {
      religionMappingRepository.saveAll(
        listOf(
          CorePersonReligionMapping(
            cprId = ids[0],
            nomisId = 9831302L,
            nomisPrisonNumber = "A1234BC",
            mappingType = StandardMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2021-02-01T12:35"),
          ),
          CorePersonReligionMapping(
            cprId = ids[1],
            nomisId = 6837812L,
            nomisPrisonNumber = "B1234CD",
            mappingType = StandardMappingType.MIGRATED,
            whenCreated = LocalDateTime.parse("2020-01-01T10:15"),
          ),
        ),
      ).collect()
    }

    @Nested
    inner class Security {
      @Test
      fun `access unauthorised when no authority`() {
        webTestClient.get()
          .uri {
            it.path("/mapping/core-person-religion/religion/cpr-ids")
            it.queryParam("ids", *ids).build()
          }
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get()
        .uri {
          it.path("/mapping/core-person-religion/religion/cpr-ids")
          it.queryParam("ids", *ids).build()
        }
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get()
        .uri {
          it.path("/mapping/core-person-religion/religion/cpr-ids")
          it.queryParam("ids", *ids).build()
        }
        .headers(setAuthorisation(roles = listOf("BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will retrieve mapping by cprId`() {
        webTestClient.get()
          .uri {
            it.path("/mapping/core-person-religion/religion/cpr-ids")
            it.queryParam("ids", *ids).build()
          }
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<List<ReligionMappingDto>>()
          .returnResult().responseBody!!.apply {
          assertThat(size).isEqualTo(2)
          assertThat(this.map { it.nomisId }).containsExactlyInAnyOrder(9831302L, 6837812L)
        }
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/core-person-religion/replace")
  inner class ReplaceReligionMappings {
    val nomisPrisonNumber = "A1234BC"
    val cprId = "123456789"

    @Nested
    inner class Security {
      val mapping = ReligionsMigrationMappingDto(
        cprId = cprId,
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2020-01-01T10:00",
        religions = listOf(),
        mappingType = StandardMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person-religion/replace")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person-religion/replace")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person-religion/replace")
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
        mappingType = StandardMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      val individualMapping = CorePersonReligionMapping(
        cprId = "tobereplaced",
        nomisId = 99991L,
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2026-01-01T10:00",
        mappingType = StandardMappingType.NOMIS_CREATED,
      )

      @BeforeEach
      fun setUp() = runTest {
        religionMappingRepository.save(individualMapping)
      }

      @Test
      fun `will save individual religion mappings even if top-level mapping does not exist and mappings are empty`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person-religion/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping.copy(cprId = "96969")))
          .exchange()
          .expectStatus().isOk

        assertThat(religionMappingRepository.findByNomisPrisonNumber(nomisPrisonNumber)).isEmpty()
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
        mappingType = StandardMappingType.NOMIS_CREATED,
      )

      val existingMapping = CorePersonReligionsMapping(
        cprId = cprId,
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = StandardMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
      )

      val individualMapping = CorePersonReligionMapping(
        cprId = "tobereplaced",
        nomisId = 99991L,
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2026-01-01T10:00",
        mappingType = StandardMappingType.NOMIS_CREATED,
      )

      val mappingForDifferentPrisoner = CorePersonReligionMapping(
        cprId = "tobereplaced2",
        nomisId = 99998L,
        nomisPrisonNumber = "ZZ1234Z",
        label = "2026-01-01T10:00",
        mappingType = StandardMappingType.NOMIS_CREATED,
      )

      @BeforeEach
      fun setUp() = runTest {
        religionMappingRepository.save(individualMapping)
        religionMappingRepository.save(mappingForDifferentPrisoner)
      }

      @Test
      fun `returns 200 when mappings replaced`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person-religion/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk
      }

      @Test
      fun `will persist the religion mappings`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person-religion/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk

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

      @Test
      fun `will delete any religion mappings persisted before the replace`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person-religion/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isOk

        assertThat(religionMappingRepository.findByNomisPrisonNumber(nomisPrisonNumber)).size().isEqualTo(2)
        assertThat(religionMappingRepository.findOneByCprId(mappingForDifferentPrisoner.cprId)).isNull()
      }
    }
  }

  @DisplayName("GET /mapping/core-person-religion/religion/nomis-prison-number/{nomisPrisonNumber}/exists")
  @Nested
  inner class ExistsReligionMappingByNomisPrisonNumber {
    val nomisPrisonNumber = "A1234BC"

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/core-person-religion/religion/nomis-prison-number/$nomisPrisonNumber/exists")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/core-person-religion/religion/nomis-prison-number/$nomisPrisonNumber/exists")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/core-person-religion/religion/nomis-prison-number/$nomisPrisonNumber/exists")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `returns true when there exists a mapping by nomis prison number`() = runTest {
        religionMappingRepository.save(
          CorePersonReligionMapping(
            cprId = "123456789",
            nomisId = 123456789,
            nomisPrisonNumber = "A1234BC",
            label = "2020-01-01T10:00",
            mappingType = StandardMappingType.MIGRATED,
          ),
        )

        webTestClient.get().uri("/mapping/core-person-religion/religion/nomis-prison-number/A1234BC/exists")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody().jsonPath(".exists").isEqualTo(true)
      }

      @Test
      fun `returns false when there does not exists a mapping by nomis prison number`() = runTest {
        webTestClient.get().uri("/mapping/core-person-religion/religion/nomis-prison-number/A1234BC/exists")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody().jsonPath(".exists").isEqualTo(false)
      }
    }
  }
}
