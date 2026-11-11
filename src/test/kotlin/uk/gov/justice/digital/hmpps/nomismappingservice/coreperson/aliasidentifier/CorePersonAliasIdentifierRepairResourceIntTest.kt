package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingIdDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.util.UUID

class CorePersonAliasIdentifierRepairResourceIntTest(
  @Autowired private val offenderAliasMappingRepository: OffenderAliasMappingRepository,
  @Autowired private val offenderIdentifierMappingRepository: OffenderIdentifierMappingRepository,
) : IntegrationTestBase() {

  @AfterEach
  fun tearDown() = runTest {
    offenderAliasMappingRepository.deleteAll()
    offenderIdentifierMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST /mapping/core-person/alias-identifier/replace")
  inner class ReplaceCorePersonAliasIdentifierMappings {
    val nomisPrisonNumber = "A1234BC"

    @Nested
    inner class Security {
      val mapping = CorePersonAliasIdentifierMappingsDto(
        personMapping = CorePersonMappingIdDto(
          cprId = UUID.randomUUID().toString(),
          nomisPrisonNumber = nomisPrisonNumber,
        ),
        label = "2020-01-01T10:00",
        aliases = emptyList(),
        identifiers = emptyList(),
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier/replace")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier/replace")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier/replace")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when person mapping is missing`() {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              // language=json
              """
              {
                "label": "2020-01-01T10:00",
                "mappingType": "NOMIS_CREATED",
                "aliases": [],
                "identifiers": []
              }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = CorePersonAliasIdentifierMappingsDto(
        personMapping = CorePersonMappingIdDto(
          cprId = "9a1313b1-5c03-4225-b6fd-dbc5ce992760",
          nomisPrisonNumber = nomisPrisonNumber,
        ),
        label = "2020-01-01T10:00",
        aliases = listOf(
          OffenderAliasMappingDto(
            cprId = "fba8a521-fc9f-4801-bb02-3f3326c760ef",
            nomisOffenderId = 10001L,
            nomisPrisonNumber = nomisPrisonNumber,
            label = "2020-01-01T10:00",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
            whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
          ),
        ),
        identifiers = listOf(
          OffenderIdentifierMappingDto(
            cprId = "27f51212-4d32-4b8b-86c2-a2e2807e3e4e",
            nomisOffenderId = 10001L,
            nomisIdentifierSequence = 1,
            nomisPrisonNumber = nomisPrisonNumber,
            label = "2020-01-01T10:00",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
            whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
          ),
          OffenderIdentifierMappingDto(
            cprId = "27f51212-4d32-4b8b-86c2-a2e2807e3e4f",
            nomisOffenderId = 10001L,
            nomisIdentifierSequence = 2,
            nomisPrisonNumber = nomisPrisonNumber,
            label = "2020-01-01T10:00",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
            whenCreated = LocalDateTime.parse("2020-01-01T10:14"),
          ),
        ),
        mappingType = CorePersonMappingType.NOMIS_CREATED,
      )

      val existingAliasMapping = OffenderAliasMapping(
        cprId = "034c070d-aff5-4464-9d1e-24c20bc6f8e1",
        nomisOffenderId = 10000L,
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
      )

      val existingIdentifierMapping = OffenderIdentifierMapping(
        cprId = "8d2c37b7-3f07-4954-b9bb-5ba61357be3f",
        nomisOffenderId = 10000L,
        nomisIdentifierSequence = 1,
        nomisPrisonNumber = nomisPrisonNumber,
        label = "2019-01-01T10:00",
        mappingType = CorePersonMappingType.NOMIS_CREATED,
        whenCreated = LocalDateTime.parse("2019-01-01T10:14"),
      )

      @BeforeEach
      fun setUp() = runTest {
        offenderAliasMappingRepository.save(existingAliasMapping)
        offenderIdentifierMappingRepository.save(existingIdentifierMapping)
      }

      @Test
      fun `returns 200 when mappings replaced`() = runTest {
        replace(mapping)
      }

      @Test
      fun `will persist the alias and identifier mappings`() = runTest {
        replace(mapping)

        with(offenderAliasMappingRepository.findOneByCprId(mapping.aliases[0].cprId)!!) {
          val expected = mapping.aliases[0]
          assertThat(nomisOffenderId).isEqualTo(expected.nomisOffenderId)
          assertThat(nomisPrisonNumber).isEqualTo(expected.nomisPrisonNumber)
          assertThat(label).isEqualTo(expected.label)
          assertThat(mappingType).isEqualTo(expected.mappingType)
          assertThat(whenCreated).isEqualTo(expected.whenCreated)
        }

        mapping.identifiers.forEach { expected ->
          with(offenderIdentifierMappingRepository.findOneByCprId(expected.cprId)!!) {
            assertThat(nomisOffenderId).isEqualTo(expected.nomisOffenderId)
            assertThat(nomisIdentifierSequence).isEqualTo(expected.nomisIdentifierSequence)
            assertThat(nomisPrisonNumber).isEqualTo(expected.nomisPrisonNumber)
            assertThat(label).isEqualTo(expected.label)
            assertThat(mappingType).isEqualTo(expected.mappingType)
            assertThat(whenCreated).isEqualTo(expected.whenCreated)
          }
        }
      }

      @Test
      fun `will delete any alias or identifier mappings persisted before the replace`() = runTest {
        assertThat(offenderAliasMappingRepository.findOneByCprId(existingAliasMapping.cprId)).isNotNull()
        assertThat(offenderIdentifierMappingRepository.findOneByCprId(existingIdentifierMapping.cprId)).isNotNull()

        replace(mapping)

        assertThat(offenderAliasMappingRepository.findOneByCprId(existingAliasMapping.cprId)).isNull()
        assertThat(offenderIdentifierMappingRepository.findOneByCprId(existingIdentifierMapping.cprId)).isNull()
      }

      @Test
      fun `will remove the existing alias and identifier mappings when passed empty lists`() = runTest {
        assertAliasesForPrisonNumber(nomisPrisonNumber).isNotEmpty()
        assertIdentifiersForPrisonNumber(nomisPrisonNumber).isNotEmpty()

        replace(
          CorePersonAliasIdentifierMappingsDto(
            label = "2021-01-01T01:00",
            mappingType = CorePersonMappingType.NOMIS_CREATED,
            whenCreated = LocalDateTime.of(2021, 1, 1, 1, 0),
            personMapping = CorePersonMappingIdDto(
              cprId = "9a1313b1-5c03-4225-b6fd-dbc5ce992760",
              nomisPrisonNumber = nomisPrisonNumber,
            ),
            aliases = emptyList(),
            identifiers = emptyList(),
          ),
        )

        assertAliasesForPrisonNumber(nomisPrisonNumber).isEmpty()
        assertIdentifiersForPrisonNumber(nomisPrisonNumber).isEmpty()
      }

      @Test
      fun `will delete dangling mappings for the same NOMIS ids held against another prison number`() = runTest {
        val oldPrisonNumber = "A9999ZZ"
        offenderAliasMappingRepository.save(
          OffenderAliasMapping(
            cprId = "11111111-aff5-4464-9d1e-24c20bc6f8e1",
            nomisOffenderId = mapping.aliases[0].nomisOffenderId,
            nomisPrisonNumber = oldPrisonNumber,
            mappingType = CorePersonMappingType.NOMIS_CREATED,
          ),
        )
        offenderIdentifierMappingRepository.save(
          OffenderIdentifierMapping(
            cprId = "22222222-aff5-4464-9d1e-24c20bc6f8e1",
            nomisOffenderId = mapping.identifiers[0].nomisOffenderId,
            nomisIdentifierSequence = mapping.identifiers[0].nomisIdentifierSequence,
            nomisPrisonNumber = oldPrisonNumber,
            mappingType = CorePersonMappingType.NOMIS_CREATED,
          ),
        )

        replace(mapping)

        assertAliasesForPrisonNumber(oldPrisonNumber).isEmpty()
        assertIdentifiersForPrisonNumber(oldPrisonNumber).isEmpty()
        assertThat(offenderAliasMappingRepository.findOneByNomisOffenderId(mapping.aliases[0].nomisOffenderId)!!.nomisPrisonNumber).isEqualTo(nomisPrisonNumber)
        assertThat(
          offenderIdentifierMappingRepository.findOneByNomisOffenderIdAndNomisIdentifierSequence(
            mapping.identifiers[0].nomisOffenderId,
            mapping.identifiers[0].nomisIdentifierSequence,
          )!!.nomisPrisonNumber,
        ).isEqualTo(nomisPrisonNumber)
      }

      @Test
      fun `will delete dangling mappings for the same CPR ids held against another prison number`() = runTest {
        val oldPrisonNumber = "A9999ZZ"
        offenderAliasMappingRepository.save(
          OffenderAliasMapping(
            cprId = mapping.aliases[0].cprId,
            nomisOffenderId = 99999L,
            nomisPrisonNumber = oldPrisonNumber,
            mappingType = CorePersonMappingType.NOMIS_CREATED,
          ),
        )
        offenderIdentifierMappingRepository.save(
          OffenderIdentifierMapping(
            cprId = mapping.identifiers[0].cprId,
            nomisOffenderId = 99999L,
            nomisIdentifierSequence = 1,
            nomisPrisonNumber = oldPrisonNumber,
            mappingType = CorePersonMappingType.NOMIS_CREATED,
          ),
        )

        replace(mapping)

        assertAliasesForPrisonNumber(oldPrisonNumber).isEmpty()
        assertIdentifiersForPrisonNumber(oldPrisonNumber).isEmpty()
        assertThat(offenderAliasMappingRepository.findOneByCprId(mapping.aliases[0].cprId)!!.nomisPrisonNumber).isEqualTo(nomisPrisonNumber)
        assertThat(offenderIdentifierMappingRepository.findOneByCprId(mapping.identifiers[0].cprId)!!.nomisPrisonNumber).isEqualTo(nomisPrisonNumber)
      }

      private fun replace(body: CorePersonAliasIdentifierMappingsDto) {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier/replace")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(body))
          .exchange()
          .expectStatus().isOk
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/core-person/alias-identifier/mappings/by-nomis-ids")
  inner class GetMappingsFromNomisIds {
    private val nomisPrisonNumber = "A1234BC"

    @BeforeEach
    fun setUp() = runTest {
      offenderAliasMappingRepository.save(
        OffenderAliasMapping(
          cprId = "a1c4d6e8-1111-4b2a-9c3d-000000000101",
          nomisOffenderId = 101L,
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      offenderAliasMappingRepository.save(
        OffenderAliasMapping(
          cprId = "a1c4d6e8-1111-4b2a-9c3d-000000000102",
          nomisOffenderId = 102L,
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      offenderIdentifierMappingRepository.save(
        OffenderIdentifierMapping(
          cprId = "b2d5e7f9-2222-4c3b-8d4e-000000000201",
          nomisOffenderId = 101L,
          nomisIdentifierSequence = 1,
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      offenderIdentifierMappingRepository.save(
        OffenderIdentifierMapping(
          cprId = "b2d5e7f9-2222-4c3b-8d4e-000000000202",
          nomisOffenderId = 101L,
          nomisIdentifierSequence = 2,
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
      offenderIdentifierMappingRepository.save(
        OffenderIdentifierMapping(
          cprId = "b2d5e7f9-2222-4c3b-8d4e-000000000203",
          nomisOffenderId = 102L,
          nomisIdentifierSequence = 1,
          nomisPrisonNumber = nomisPrisonNumber,
          label = "2023-01-01T12:45:12",
          mappingType = CorePersonMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      val request = CorePersonAliasIdentifierMappingsByNomisIdsRequest(
        aliases = listOf(101L),
        identifiers = listOf(IdentifierByNomisIdRequest(nomisId = 101L, nomisSequence = 1)),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier/mappings/by-nomis-ids")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 400 when a required list is missing`() {
        webTestClient.post()
          .uri("/mapping/core-person/alias-identifier/mappings/by-nomis-ids")
          .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("""{ "aliases": [101] }""")
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the alias mappings for the requested nomis ids`() {
        getMappings(
          CorePersonAliasIdentifierMappingsByNomisIdsRequest(
            aliases = listOf(101L, 102L, 999L),
            identifiers = emptyList(),
          ),
        )
          .jsonPath("aliases.length()").isEqualTo(2)
          .jsonPath("aliases[*].nomisOffenderId").value<List<Int>> {
            assertThat(it).containsExactlyInAnyOrder(101, 102)
          }
          .jsonPath("aliases[*].cprId").value<List<String>> {
            assertThat(it).containsExactlyInAnyOrder(
              "a1c4d6e8-1111-4b2a-9c3d-000000000101",
              "a1c4d6e8-1111-4b2a-9c3d-000000000102",
            )
          }
          .jsonPath("aliases[0].nomisPrisonNumber").isEqualTo(nomisPrisonNumber)
          .jsonPath("aliases[0].label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("aliases[0].mappingType").isEqualTo("MIGRATED")
          .jsonPath("aliases[0].whenCreated").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("identifiers").isEmpty
      }

      @Test
      fun `will only return identifier mappings where both the nomis id and sequence match`() {
        getMappings(
          CorePersonAliasIdentifierMappingsByNomisIdsRequest(
            aliases = emptyList(),
            identifiers = listOf(
              IdentifierByNomisIdRequest(nomisId = 101L, nomisSequence = 1),
              // 102 only has sequence 1 so this should not match
              IdentifierByNomisIdRequest(nomisId = 102L, nomisSequence = 2),
            ),
          ),
        )
          .jsonPath("identifiers.length()").isEqualTo(1)
          .jsonPath("identifiers[0].cprId").isEqualTo("b2d5e7f9-2222-4c3b-8d4e-000000000201")
          .jsonPath("identifiers[0].nomisOffenderId").isEqualTo(101)
          .jsonPath("identifiers[0].nomisIdentifierSequence").isEqualTo(1)
          .jsonPath("identifiers[0].nomisPrisonNumber").isEqualTo(nomisPrisonNumber)
          .jsonPath("aliases").isEmpty
      }

      @Test
      fun `will return all requested mapping types together`() {
        getMappings(
          CorePersonAliasIdentifierMappingsByNomisIdsRequest(
            aliases = listOf(101L),
            identifiers = listOf(
              IdentifierByNomisIdRequest(nomisId = 101L, nomisSequence = 1),
              IdentifierByNomisIdRequest(nomisId = 101L, nomisSequence = 2),
            ),
          ),
        )
          .jsonPath("aliases.length()").isEqualTo(1)
          .jsonPath("aliases[0].nomisOffenderId").isEqualTo(101)
          .jsonPath("identifiers.length()").isEqualTo(2)
          .jsonPath("identifiers[*].nomisIdentifierSequence").value<List<Int>> {
            assertThat(it).containsExactlyInAnyOrder(1, 2)
          }
      }

      @Test
      fun `will return empty lists when no mappings match`() {
        getMappings(
          CorePersonAliasIdentifierMappingsByNomisIdsRequest(
            aliases = listOf(999L),
            identifiers = listOf(IdentifierByNomisIdRequest(nomisId = 999L, nomisSequence = 1)),
          ),
        )
          .jsonPath("aliases").isEmpty
          .jsonPath("identifiers").isEmpty
      }

      @Test
      fun `will return empty lists when no ids are requested`() {
        getMappings(
          CorePersonAliasIdentifierMappingsByNomisIdsRequest(
            aliases = emptyList(),
            identifiers = emptyList(),
          ),
        )
          .jsonPath("aliases").isEmpty
          .jsonPath("identifiers").isEmpty
      }

      private fun getMappings(request: CorePersonAliasIdentifierMappingsByNomisIdsRequest) = webTestClient.post()
        .uri("/mapping/core-person/alias-identifier/mappings/by-nomis-ids")
        .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(request))
        .exchange()
        .expectStatus().isOk
        .expectBody()
    }
  }

  private suspend fun assertAliasesForPrisonNumber(prisonNumber: String) = assertThat(offenderAliasMappingRepository.findAll().toList().filter { it.nomisPrisonNumber == prisonNumber })
  private suspend fun assertIdentifiersForPrisonNumber(prisonNumber: String) = assertThat(offenderIdentifierMappingRepository.findAll().toList().filter { it.nomisPrisonNumber == prisonNumber })
}
