package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonerperson

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.migration.PrisonPersonMigrationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.migration.PrisonPersonMigrationMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.migration.PrisonPersonMigrationMappingRequest
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.migration.PrisonPersonMigrationType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.migration.PrisonPersonMigrationType.PHYSICAL_ATTRIBUTES
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.migration.PrisonPersonMigrationType.PROFILE_DETAILS_PHYSICAL_ATTRIBUTES
import java.time.LocalDate

class PrisonPersonResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: PrisonPersonMigrationMappingRepository

  @Nested
  inner class CreateMigrationMapping {
    @AfterEach
    fun tearDown() = runTest {
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/prisonperson/migration")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request()))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/prisonperson/migration")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/prisonperson/migration")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request()))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should create migration mapping`() = runTest {
        webTestClient.upsertMigrationMapping(request("A1234AA", PROFILE_DETAILS_PHYSICAL_ATTRIBUTES, listOf(1, 2, 3), "label"))
          .expectStatus().isOk

        val mapping = repository.findByNomisPrisonerNumberAndMigrationType("A1234AA", PROFILE_DETAILS_PHYSICAL_ATTRIBUTES)!!
        assertThat(mapping.nomisPrisonerNumber).isEqualTo("A1234AA")
        assertThat(mapping.migrationType).isEqualTo(PROFILE_DETAILS_PHYSICAL_ATTRIBUTES)
        assertThat(mapping.dpsIds).isEqualTo("[1, 2, 3]")
        assertThat(mapping.label).isEqualTo("label")
        assertThat(mapping.whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
      }

      @Test
      fun `should update migration mapping`() = runTest {
        webTestClient.upsertMigrationMapping(request("A1234AA", PROFILE_DETAILS_PHYSICAL_ATTRIBUTES, listOf(1, 2, 3), "label"))
          .expectStatus().isOk
        webTestClient.upsertMigrationMapping(request("A1234AA", PROFILE_DETAILS_PHYSICAL_ATTRIBUTES, listOf(4, 5, 6), "label"))
          .expectStatus().isOk

        val mapping = repository.findByNomisPrisonerNumberAndMigrationType("A1234AA", PROFILE_DETAILS_PHYSICAL_ATTRIBUTES)!!
        assertThat(mapping.nomisPrisonerNumber).isEqualTo("A1234AA")
        assertThat(mapping.migrationType).isEqualTo(PROFILE_DETAILS_PHYSICAL_ATTRIBUTES)
        assertThat(mapping.dpsIds).isEqualTo("[4, 5, 6]")
        assertThat(mapping.label).isEqualTo("label")
        assertThat(mapping.whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
      }
    }

    private fun WebTestClient.upsertMigrationMapping(request: PrisonPersonMigrationMappingRequest) =
      put()
        .uri("/mapping/prisonperson/migration")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(request))
        .exchange()

    private fun request(
      prisonerNumber: String = "any",
      type: PrisonPersonMigrationType = PHYSICAL_ATTRIBUTES,
      dpsIds: List<Long> = listOf(1),
      label: String = "label",
    ) =
      PrisonPersonMigrationMappingRequest(
        nomisPrisonerNumber = prisonerNumber,
        migrationType = type,
        dpsIds = dpsIds,
        label = label,
      )
  }

  @DisplayName("GET /mapping/prisonperson/migration/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationId {
    @AfterEach
    fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/prisonperson/migration/migration-id/2022-01-01T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/prisonperson/migration/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/prisonperson/migration/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return only the migration requested`() {
      saveMapping("A1234BC")
      saveMapping("B2345CD")
      saveMapping("C3456DE", label = "wrong-migration")

      webTestClient.get().uri("/mapping/prisonperson/migration/migration-id/some_migration_id")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(2)
        .jsonPath("content.size()").isEqualTo(2)
        .jsonPath("content[0].nomisPrisonerNumber").isEqualTo("A1234BC")
        .jsonPath("content[0].migrationType").isEqualTo("PHYSICAL_ATTRIBUTES")
        .jsonPath("content[0].dpsIds").isEqualTo("[1, 2]")
        .jsonPath("content[0].whenCreated").isNotEmpty
        .jsonPath("content[1].nomisPrisonerNumber").isEqualTo("B2345CD")
        .jsonPath("content[1].migrationType").isEqualTo("PHYSICAL_ATTRIBUTES")
        .jsonPath("content[1].dpsIds").isEqualTo("[1, 2]")
        .jsonPath("content[1].whenCreated").isNotEmpty
    }

    @Test
    fun `should return an empty list`() {
      saveMapping(label = "wrong-migration")

      webTestClient.get().uri("/mapping/prisonperson/migration/migration-id/some_migration_id")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
        .jsonPath("content.size()").isEqualTo(0)
    }

    @Test
    fun `should return mappings in pages`() {
      val pageSize = 3
      (1..(pageSize + 1)).forEach { saveMapping(it, "some_migration_id") }

      webTestClient.get().uri {
        it.path("/mapping/prisonperson/migration/migration-id/some_migration_id")
          .queryParam("size", "$pageSize")
          .queryParam("page", "0")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("numberOfElements").isEqualTo(pageSize)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(pageSize)
        .jsonPath("content.size()").isEqualTo(pageSize)
        .jsonPath("content[0].nomisPrisonerNumber").isEqualTo("A0001BC")
        .jsonPath("content[1].nomisPrisonerNumber").isEqualTo("A0002BC")
        .jsonPath("content[2].nomisPrisonerNumber").isEqualTo("A0003BC")

      webTestClient.get().uri {
        it.path("/mapping/prisonperson/migration/migration-id/some_migration_id")
          .queryParam("size", "$pageSize")
          .queryParam("page", "1")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONPERSON")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("numberOfElements").isEqualTo(1)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(pageSize)
        .jsonPath("content.size()").isEqualTo(1)
        .jsonPath("content[0].nomisPrisonerNumber").isEqualTo("A0004BC")
    }

    private fun saveMapping(offset: Int, label: String = "some_migration_id") =
      saveMapping("A${offset.toString().padStart(4, '0')}BC", PHYSICAL_ATTRIBUTES, "[$offset]", label)

    private fun saveMapping(
      nomisPrisonerNumber: String = "A1234BC",
      migrationType: PrisonPersonMigrationType = PHYSICAL_ATTRIBUTES,
      dpsIds: String = "[1, 2]",
      label: String = "some_migration_id",
    ) = runTest {
      repository.save(
        PrisonPersonMigrationMapping(
          nomisPrisonerNumber = nomisPrisonerNumber,
          migrationType = migrationType,
          dpsIds = dpsIds,
          label = label,
        ),
      )
    }
  }
}
