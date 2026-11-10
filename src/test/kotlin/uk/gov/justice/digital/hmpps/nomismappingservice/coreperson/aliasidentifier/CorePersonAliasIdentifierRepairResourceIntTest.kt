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

  private suspend fun assertAliasesForPrisonNumber(prisonNumber: String) = assertThat(offenderAliasMappingRepository.findAll().toList().filter { it.nomisPrisonNumber == prisonNumber })
  private suspend fun assertIdentifiersForPrisonNumber(prisonNumber: String) = assertThat(offenderIdentifierMappingRepository.findAll().toList().filter { it.nomisPrisonNumber == prisonNumber })
}
