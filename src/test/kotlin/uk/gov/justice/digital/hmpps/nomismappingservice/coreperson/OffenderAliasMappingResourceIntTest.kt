package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
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

class OffenderAliasMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var offenderAliasMappingRepository: OffenderAliasMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    offenderAliasMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /mapping/offender-alias/nomis-offender-id/{nomisOffenderId}")
  inner class GetByNomisId {
    private val nomisOffenderId = 12345L
    private val cprId = "ca550f8c-00f2-41d7-80f1-ff9978ea220b"

    @BeforeEach
    fun setUp() = runTest {
      offenderAliasMappingRepository.save(
        OffenderAliasMapping(
          cprId = cprId,
          nomisOffenderId = nomisOffenderId,
          nomisPrisonNumber = "A1234BC",
          mappingType = CorePersonMappingType.CPR_CREATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/offender-alias/nomis-offender-id/{nomisOffenderId}", nomisOffenderId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/offender-alias/nomis-offender-id/{nomisOffenderId}", nomisOffenderId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/offender-alias/nomis-offender-id/{nomisOffenderId}", nomisOffenderId)
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
          .uri("/mapping/offender-alias/nomis-offender-id/{nomisOffenderId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/offender-alias/nomis-offender-id/{nomisOffenderId}", nomisOffenderId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprId)
          .jsonPath("nomisOffenderId").isEqualTo(nomisOffenderId)
          .jsonPath("nomisPrisonNumber").isEqualTo("A1234BC")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/offender-alias/cpr-id/{cprId}")
  inner class GetByCprId {
    private val cprId = "c85ef4dd-0f66-41d4-a9b5-930cdd7f208f"

    @BeforeEach
    fun setUp() = runTest {
      offenderAliasMappingRepository.save(
        OffenderAliasMapping(
          cprId = cprId,
          nomisOffenderId = 6789,
          nomisPrisonNumber = "B1234CD",
          mappingType = CorePersonMappingType.CPR_CREATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/offender-alias/cpr-id/{cprId}", cprId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/offender-alias/cpr-id/{cprId}", cprId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/offender-alias/cpr-id/{cprId}", cprId)
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
          .uri("/mapping/offender-alias/cpr-id/{cprId}", "99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/offender-alias/cpr-id/{cprId}", cprId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprId)
          .jsonPath("nomisOffenderId").isEqualTo(6789)
          .jsonPath("nomisPrisonNumber").isEqualTo("B1234CD")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/offender-alias/nomis-offender-id/{nomisOffenderId}")
  inner class DeleteByNomisId {
    private val nomisOffenderId = 111L

    @BeforeEach
    fun setUp() = runTest {
      offenderAliasMappingRepository.save(
        OffenderAliasMapping(
          cprId = "b8985f10-595e-4937-8344-73a7e166e652",
          nomisOffenderId = nomisOffenderId,
          nomisPrisonNumber = "C1234DE",
          mappingType = CorePersonMappingType.CPR_CREATED,
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/offender-alias/nomis-offender-id/{nomisOffenderId}", nomisOffenderId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/offender-alias/nomis-offender-id/{nomisOffenderId}", nomisOffenderId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/offender-alias/nomis-offender-id/{nomisOffenderId}", nomisOffenderId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping`() = runTest {
        webTestClient.delete()
          .uri("/mapping/offender-alias/nomis-offender-id/{nomisOffenderId}", nomisOffenderId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(offenderAliasMappingRepository.findOneByNomisOffenderId(nomisOffenderId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/offender-alias")
  inner class CreateMapping {
    private val mappingDto = OffenderAliasMappingDto(
      cprId = "ca550f8c-00f2-41d7-80f1-ff9978ea220b",
      nomisOffenderId = 12345,
      nomisPrisonNumber = "A1234BC",
      label = null,
      mappingType = CorePersonMappingType.CPR_CREATED,
      whenCreated = null,
    )

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/offender-alias")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappingDto))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/offender-alias")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappingDto))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/offender-alias")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappingDto))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private val existingMapping = mappingDto.toMapping()

      @BeforeEach
      fun setUp() = runTest {
        offenderAliasMappingRepository.save(mappingDto.toMapping())
      }

      @Test
      fun `will not allow the same offender alias to have duplicate NOMIS ids`() {
        webTestClient.post()
          .uri("/mapping/offender-alias")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappingDto.copy(cprId = "96969")))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will not allow the same offender alias to have duplicate CPR ids`() {
        webTestClient.post()
          .uri("/mapping/offender-alias")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappingDto.copy(nomisOffenderId = 999)))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/offender-alias")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappingDto.copy(cprId = "96969")))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          assertThat(moreInfo.existing)
            .containsEntry("cprId", existingMapping.cprId)
            .containsEntry("nomisOffenderId", existingMapping.nomisOffenderId.toInt())
            .containsEntry("nomisPrisonNumber", existingMapping.nomisPrisonNumber)
          assertThat(moreInfo.duplicate)
            .containsEntry("cprId", "96969")
            .containsEntry("nomisOffenderId", mappingDto.nomisOffenderId.toInt())
            .containsEntry("nomisPrisonNumber", mappingDto.nomisPrisonNumber)
        }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns 201 when mapping created`() {
        webTestClient.post()
          .uri("/mapping/offender-alias")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappingDto))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the offender alias mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/offender-alias")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappingDto.copy(label = "2023-01-01T12:45:12")))
          .exchange()
          .expectStatus().isCreated

        with(offenderAliasMappingRepository.findOneByNomisOffenderId(mappingDto.nomisOffenderId)!!) {
          assertThat(cprId).isEqualTo(mappingDto.cprId)
          assertThat(nomisOffenderId).isEqualTo(mappingDto.nomisOffenderId)
          assertThat(nomisPrisonNumber).isEqualTo(mappingDto.nomisPrisonNumber)
          assertThat(label).isEqualTo("2023-01-01T12:45:12")
          assertThat(mappingType).isEqualTo(mappingDto.mappingType)
        }
      }
    }
  }
}
