package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier

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
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import java.time.LocalDateTime

class CorePersonIdentifierMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var offenderIdentifierMappingRepository: OffenderIdentifierMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    offenderIdentifierMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/nomis-identifier-sequence/{nomisIdentifierSequence}")
  inner class GetByNomisId {
    private val nomisOffenderId = 12345L
    private val nomisIdentifierSequence = 3
    private val cprId = "ca550f8c-00f2-41d7-80f1-ff9978ea220b"

    @BeforeEach
    fun setUp() = runTest {
      offenderIdentifierMappingRepository.save(
        OffenderIdentifierMapping(
          cprId = cprId,
          nomisOffenderId = nomisOffenderId,
          nomisIdentifierSequence = nomisIdentifierSequence,
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
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/nomis-identifier-sequence/{nomisIdentifierSequence}", nomisOffenderId, nomisIdentifierSequence)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/nomis-identifier-sequence/{nomisIdentifierSequence}", nomisOffenderId, nomisIdentifierSequence)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/nomis-identifier-sequence/{nomisIdentifierSequence}", nomisOffenderId, nomisIdentifierSequence)
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
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/nomis-identifier-sequence/{nomisIdentifierSequence}", 99999, 9)
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
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/nomis-identifier-sequence/{nomisIdentifierSequence}", nomisOffenderId, nomisIdentifierSequence)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprId)
          .jsonPath("nomisOffenderId").isEqualTo(nomisOffenderId)
          .jsonPath("nomisIdentifierSequence").isEqualTo(nomisIdentifierSequence)
          .jsonPath("nomisPrisonNumber").isEqualTo("A1234BC")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}")
  inner class GetAllByNomisOffenderId {
    private val nomisOffenderId = 12345L

    @BeforeEach
    fun setUp() = runTest {
      offenderIdentifierMappingRepository.save(
        OffenderIdentifierMapping(
          cprId = "1a2b3c4d-0000-4000-8000-000000000001",
          nomisOffenderId = nomisOffenderId,
          nomisIdentifierSequence = 1,
          nomisPrisonNumber = "A1234BC",
          mappingType = CorePersonMappingType.CPR_CREATED,
        ),
      )
      offenderIdentifierMappingRepository.save(
        OffenderIdentifierMapping(
          cprId = "1a2b3c4d-0000-4000-8000-000000000002",
          nomisOffenderId = nomisOffenderId,
          nomisIdentifierSequence = 2,
          nomisPrisonNumber = "A1234BC",
          mappingType = CorePersonMappingType.MIGRATED,
        ),
      )
      offenderIdentifierMappingRepository.save(
        OffenderIdentifierMapping(
          cprId = "1a2b3c4d-0000-4000-8000-000000000003",
          nomisOffenderId = 67890L,
          nomisIdentifierSequence = 1,
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
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}", nomisOffenderId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}", nomisOffenderId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}", nomisOffenderId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `400 when nomis offender id is not a number`() {
        webTestClient.get()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}", "BANANAS")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return all the mappings for the nomis offender id`() {
        webTestClient.get()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}", nomisOffenderId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("length()").isEqualTo(2)
          .jsonPath("[*].nomisOffenderId").value<List<Int>> {
            assertThat(it).containsOnly(nomisOffenderId.toInt())
          }
          .jsonPath("[*].nomisIdentifierSequence").value<List<Int>> {
            assertThat(it).containsExactlyInAnyOrder(1, 2)
          }
          .jsonPath("[*].cprId").value<List<String>> {
            assertThat(it).containsExactlyInAnyOrder(
              "1a2b3c4d-0000-4000-8000-000000000001",
              "1a2b3c4d-0000-4000-8000-000000000002",
            )
          }
          .jsonPath("[0].nomisPrisonNumber").isEqualTo("A1234BC")
      }

      @Test
      fun `will return an empty list when no mappings exist`() {
        webTestClient.get()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$").isEmpty
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/replace")
  inner class ReplaceByNomisOffenderId {
    private val nomisOffenderId = 12345L
    private val nomisPrisonNumber = "A1234BC"

    private fun identifierMapping(cprId: String, sequence: Int, offenderId: Long = nomisOffenderId) = OffenderIdentifierMappingDto(
      cprId = cprId,
      nomisOffenderId = offenderId,
      nomisIdentifierSequence = sequence,
      nomisPrisonNumber = nomisPrisonNumber,
      label = "2020-01-01T10:00",
      mappingType = CorePersonMappingType.NOMIS_CREATED,
      whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
    )

    @Nested
    inner class Security {
      private val mappings = listOf(identifierMapping(cprId = "2c3d4e5f-0000-4000-8000-000000000001", sequence = 1))

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/replace", nomisOffenderId)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/replace", nomisOffenderId)
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/replace", nomisOffenderId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `400 when a mapping is for a different nomis offender id`() = runTest {
        offenderIdentifierMappingRepository.save(identifierMapping(cprId = "2c3d4e5f-0000-4000-8000-000000000009", sequence = 1).toMapping())

        webTestClient.post()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/replace", nomisOffenderId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              listOf(
                identifierMapping(cprId = "2c3d4e5f-0000-4000-8000-000000000001", sequence = 1),
                identifierMapping(cprId = "2c3d4e5f-0000-4000-8000-000000000002", sequence = 2, offenderId = 99999L),
              ),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest

        assertThat(offenderIdentifierMappingRepository.findAllByNomisOffenderIdIn(listOf(nomisOffenderId)).map { it.cprId })
          .containsExactly("2c3d4e5f-0000-4000-8000-000000000009")
      }

      @Test
      fun `400 when body is not a list of mappings`() {
        webTestClient.post()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/replace", nomisOffenderId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("""[{ "cprId": "2c3d4e5f-0000-4000-8000-000000000001" }]""")
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      private val otherOffenderId = 67890L

      @BeforeEach
      fun setUp() = runTest {
        offenderIdentifierMappingRepository.save(identifierMapping(cprId = "2c3d4e5f-0000-4000-8000-000000000101", sequence = 1).toMapping())
        offenderIdentifierMappingRepository.save(identifierMapping(cprId = "2c3d4e5f-0000-4000-8000-000000000102", sequence = 2).toMapping())
        offenderIdentifierMappingRepository.save(identifierMapping(cprId = "2c3d4e5f-0000-4000-8000-000000000201", sequence = 1, offenderId = otherOffenderId).toMapping())
      }

      @Test
      fun `will replace all the mappings for the nomis offender id`() = runTest {
        replace(
          listOf(
            identifierMapping(cprId = "2c3d4e5f-0000-4000-8000-000000000001", sequence = 1),
            identifierMapping(cprId = "2c3d4e5f-0000-4000-8000-000000000003", sequence = 3),
          ),
        )

        val mappings = offenderIdentifierMappingRepository.findAllByNomisOffenderIdIn(listOf(nomisOffenderId))
        assertThat(mappings.map { it.cprId }).containsExactlyInAnyOrder(
          "2c3d4e5f-0000-4000-8000-000000000001",
          "2c3d4e5f-0000-4000-8000-000000000003",
        )
        with(mappings.first { it.nomisIdentifierSequence == 3 }) {
          assertThat(nomisPrisonNumber).isEqualTo(this@ReplaceByNomisOffenderId.nomisPrisonNumber)
          assertThat(label).isEqualTo("2020-01-01T10:00")
          assertThat(mappingType).isEqualTo(CorePersonMappingType.NOMIS_CREATED)
          assertThat(whenCreated).isEqualTo(LocalDateTime.parse("2020-01-01T10:14"))
        }
      }

      @Test
      fun `will not touch mappings for other nomis offender ids`() = runTest {
        replace(listOf(identifierMapping(cprId = "2c3d4e5f-0000-4000-8000-000000000001", sequence = 1)))

        assertThat(offenderIdentifierMappingRepository.findAllByNomisOffenderIdIn(listOf(otherOffenderId)).map { it.cprId })
          .containsExactly("2c3d4e5f-0000-4000-8000-000000000201")
      }

      @Test
      fun `will remove all mappings for the nomis offender id when passed an empty list`() = runTest {
        replace(emptyList())

        assertThat(offenderIdentifierMappingRepository.findAllByNomisOffenderIdIn(listOf(nomisOffenderId))).isEmpty()
        assertThat(offenderIdentifierMappingRepository.findAllByNomisOffenderIdIn(listOf(otherOffenderId))).hasSize(1)
      }

      @Test
      fun `will move a dangling mapping with the same cpr id from another nomis offender id`() = runTest {
        replace(listOf(identifierMapping(cprId = "2c3d4e5f-0000-4000-8000-000000000201", sequence = 5)))

        assertThat(offenderIdentifierMappingRepository.findAllByNomisOffenderIdIn(listOf(otherOffenderId))).isEmpty()
        with(offenderIdentifierMappingRepository.findOneByCprId("2c3d4e5f-0000-4000-8000-000000000201")!!) {
          assertThat(nomisOffenderId).isEqualTo(this@ReplaceByNomisOffenderId.nomisOffenderId)
          assertThat(nomisIdentifierSequence).isEqualTo(5)
        }
      }

      private fun replace(mappings: List<OffenderIdentifierMappingDto>) {
        webTestClient.post()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/replace", nomisOffenderId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isOk
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/core-person/identifier/cpr-id/{cprOffenderIdentifierId}")
  inner class GetByCprId {
    private val cprId = "c85ef4dd-0f66-41d4-a9b5-930cdd7f208f"

    @BeforeEach
    fun setUp() = runTest {
      offenderIdentifierMappingRepository.save(
        OffenderIdentifierMapping(
          cprId = cprId,
          nomisOffenderId = 6789,
          nomisIdentifierSequence = 9,
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
          .uri("/mapping/core-person/identifier/cpr-id/{cprOffenderIdentifierId}", cprId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/core-person/identifier/cpr-id/{cprOffenderIdentifierId}", cprId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/core-person/identifier/cpr-id/{cprOffenderIdentifierId}", cprId)
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
          .uri("/mapping/core-person/identifier/cpr-id/{cprOffenderIdentifierId}", "99999")
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
          .uri("/mapping/core-person/identifier/cpr-id/{cprOffenderIdentifierId}", cprId)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("cprId").isEqualTo(cprId)
          .jsonPath("nomisOffenderId").isEqualTo(6789)
          .jsonPath("nomisIdentifierSequence").isEqualTo(9)
          .jsonPath("nomisPrisonNumber").isEqualTo("B1234CD")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/nomis-identifier-sequence/{nomisIdentifierSequence}")
  inner class DeleteByNomisId {
    private val nomisOffenderId = 111L
    private val nomisIdentifierSequence = 2

    @BeforeEach
    fun setUp() = runTest {
      offenderIdentifierMappingRepository.save(
        OffenderIdentifierMapping(
          cprId = "b8985f10-595e-4937-8344-73a7e166e652",
          nomisOffenderId = nomisOffenderId,
          nomisIdentifierSequence = nomisIdentifierSequence,
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
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/nomis-identifier-sequence/{nomisIdentifierSequence}", nomisOffenderId, nomisIdentifierSequence)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/nomis-identifier-sequence/{nomisIdentifierSequence}", nomisOffenderId, nomisIdentifierSequence)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/nomis-identifier-sequence/{nomisIdentifierSequence}", nomisOffenderId, nomisIdentifierSequence)
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
          .uri("/mapping/core-person/identifier/nomis-offender-id/{nomisOffenderId}/nomis-identifier-sequence/{nomisIdentifierSequence}", nomisOffenderId, nomisIdentifierSequence)
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(offenderIdentifierMappingRepository.findOneByNomisOffenderIdAndNomisIdentifierSequence(nomisOffenderId, nomisIdentifierSequence)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/core-person/identifier")
  inner class CreateMapping {
    private val mappingDto = OffenderIdentifierMappingDto(
      cprId = "ca550f8c-00f2-41d7-80f1-ff9978ea220b",
      nomisOffenderId = 12345,
      nomisIdentifierSequence = 3,
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
          .uri("/mapping/core-person/identifier")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappingDto))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/identifier")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappingDto))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/identifier")
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
        offenderIdentifierMappingRepository.save(mappingDto.toMapping())
      }

      @Test
      fun `will not allow the same offender identifier to have duplicate NOMIS ids`() {
        webTestClient.post()
          .uri("/mapping/core-person/identifier")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappingDto.copy(cprId = "96969"),
            ),
          )
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will not allow the same offender identifier to have duplicate CPR ids`() {
        webTestClient.post()
          .uri("/mapping/core-person/identifier")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappingDto.copy(nomisOffenderId = 999, nomisIdentifierSequence = 1),
            ),
          )
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/core-person/identifier")
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
            .containsEntry("nomisIdentifierSequence", existingMapping.nomisIdentifierSequence)
          assertThat(moreInfo.duplicate)
            .containsEntry("cprId", "96969")
            .containsEntry("nomisOffenderId", mappingDto.nomisOffenderId.toInt())
            .containsEntry("nomisIdentifierSequence", mappingDto.nomisIdentifierSequence)
        }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns 201 when mapping created`() {
        webTestClient.post()
          .uri("/mapping/core-person/identifier")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappingDto))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the offender identifier mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/core-person/identifier")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappingDto.copy(label = "2023-01-01T12:45:12")))
          .exchange()
          .expectStatus().isCreated

        with(offenderIdentifierMappingRepository.findOneByNomisOffenderIdAndNomisIdentifierSequence(mappingDto.nomisOffenderId, mappingDto.nomisIdentifierSequence)!!) {
          assertThat(cprId).isEqualTo(mappingDto.cprId)
          assertThat(nomisOffenderId).isEqualTo(mappingDto.nomisOffenderId)
          assertThat(nomisIdentifierSequence).isEqualTo(mappingDto.nomisIdentifierSequence)
          assertThat(nomisPrisonNumber).isEqualTo(mappingDto.nomisPrisonNumber)
          assertThat(label).isEqualTo("2023-01-01T12:45:12")
          assertThat(mappingType).isEqualTo(mappingDto.mappingType)
        }
      }
    }
  }
}

private fun OffenderIdentifierMappingDto.copy(
  cprId: String = this.cprId,
  nomisOffenderId: Long = this.nomisOffenderId,
  nomisIdentifierSequence: Int = this.nomisIdentifierSequence,
  nomisPrisonNumber: String = this.nomisPrisonNumber,
  label: String? = this.label,
  mappingType: CorePersonMappingType = this.mappingType,
  whenCreated: LocalDateTime? = this.whenCreated,
) = OffenderIdentifierMappingDto(
  cprId = cprId,
  nomisOffenderId = nomisOffenderId,
  nomisIdentifierSequence = nomisIdentifierSequence,
  nomisPrisonNumber = nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
