package uk.gov.justice.digital.hmpps.nomismappingservice.contactperson

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.contactperson.profiledetails.ContactPersonProfileDetailMigrationMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.contactperson.profiledetails.ContactPersonProfileDetailMigrationMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.contactperson.profiledetails.ContactPersonProfileDetailsMigrationMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ContactPersonProfileDetailsMigrationMappingIntTest(
  @Autowired private val repository: ContactPersonProfileDetailMigrationMappingRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("PUT /mapping/contact-person/profile-details/migration")
  inner class CreateMigrationMapping {
    @BeforeEach
    fun tearDown() = runTest {
      repository.deleteAll()
    }

    @Nested
    inner class Security {

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/contact-person/profile-details/migration")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request()))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/contact-person/profile-details/migration")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(request()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/contact-person/profile-details/migration")
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
      fun `should insert mapping`() = runTest {
        val migrationId = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
        webTestClient.upsertMapping("A1234BC", migrationId, "1,2,3", "4,5,6")
          .expectBody()
          .jsonPath("$.prisonerNumber").isEqualTo("A1234BC")
          .jsonPath("$.migrationId").isEqualTo(migrationId)
          .jsonPath("$.domesticStatusDpsIds").isEqualTo("1,2,3")
          .jsonPath("$.numberOfChildrenDpsIds").isEqualTo("4,5,6")

        with(repository.findByNomisPrisonerNumberAndLabel("A1234BC", migrationId)!!) {
          assertThat(nomisPrisonerNumber).isEqualTo("A1234BC")
          assertThat(label).isEqualTo(migrationId)
          assertThat(domesticStatusDpsIds).isEqualTo("1,2,3")
          assertThat(numberOfChildrenDpsIds).isEqualTo("4,5,6")
          assertThat("$whenCreated").startsWith("${LocalDate.now()}")
        }
      }

      @Test
      fun `should update mapping`() = runTest {
        val migrationId = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
        webTestClient.upsertMapping("A1234BC", migrationId, "1,2,3", "4,5,6")

        with(repository.findByNomisPrisonerNumberAndLabel("A1234BC", migrationId)!!) {
          assertThat(domesticStatusDpsIds).isEqualTo("1,2,3")
          assertThat(numberOfChildrenDpsIds).isEqualTo("4,5,6")
        }

        webTestClient.upsertMapping("A1234BC", migrationId, "7,8,9", "10,11,12")

        with(repository.findByNomisPrisonerNumberAndLabel("A1234BC", migrationId)!!) {
          assertThat(domesticStatusDpsIds).isEqualTo("7,8,9")
          assertThat(numberOfChildrenDpsIds).isEqualTo("10,11,12")
        }
      }

      @Test
      fun `should allow multiple mappings for same prisoner number`() = runTest {
        val firstMigrationId = LocalDateTime.now().minusDays(2).truncatedTo(ChronoUnit.SECONDS).toString()
        webTestClient.upsertMapping("A1234BC", firstMigrationId, "1,2,3", "4,5,6")
        val secondMigrationId = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS).toString()
        webTestClient.upsertMapping("A1234BC", secondMigrationId, "7,8,9", "10,11,12")

        with(repository.findByNomisPrisonerNumberAndLabel("A1234BC", firstMigrationId)!!) {
          assertThat(domesticStatusDpsIds).isEqualTo("1,2,3")
          assertThat(numberOfChildrenDpsIds).isEqualTo("4,5,6")
        }
        with(repository.findByNomisPrisonerNumberAndLabel("A1234BC", secondMigrationId)!!) {
          assertThat(domesticStatusDpsIds).isEqualTo("7,8,9")
          assertThat(numberOfChildrenDpsIds).isEqualTo("10,11,12")
        }
      }
    }

    private fun WebTestClient.upsertMapping(
      prisonerNumber: String,
      migrationId: String,
      domesticStatusDpsIds: String,
      numberOfChildrenDpsIds: String,
    ) = webTestClient.put()
      .uri("/mapping/contact-person/profile-details/migration")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(request(prisonerNumber, migrationId, domesticStatusDpsIds, numberOfChildrenDpsIds)))
      .exchange()
      .expectStatus().isOk

    private fun request(
      prisonerNumber: String = "A1234BC",
      migrationId: String = "2025-03-01T12:45:12",
      domesticStatusDpsIds: String = "1, 2, 3",
      numberOfChildrenDpsIds: String = "4, 5, 6",
    ) = ContactPersonProfileDetailsMigrationMappingDto(
      prisonerNumber,
      migrationId,
      domesticStatusDpsIds,
      numberOfChildrenDpsIds,
    )
  }

  @DisplayName("GET /mapping/contact-person/profile-details/migration/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationId {
    @BeforeEach
    fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get().uri("/mapping/contact-person/profile-details/migration/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/contact-person/profile-details/migration/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/contact-person/profile-details/migration/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should return only the migration requested`() {
        saveMapping("A1234BC")
        saveMapping("B2345CD")
        saveMapping("C3456DE", label = "wrong-migration")

        webTestClient.get().uri("/mapping/contact-person/profile-details/migration/migration-id/some_migration_id")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(2)
          .jsonPath("content.size()").isEqualTo(2)
          .jsonPath("content[0].prisonerNumber").isEqualTo("A1234BC")
          .jsonPath("content[0].domesticStatusDpsIds").isEqualTo("1,2")
          .jsonPath("content[0].numberOfChildrenDpsIds").isEqualTo("3,4")
          .jsonPath("content[0].whenCreated").isNotEmpty
          .jsonPath("content[1].prisonerNumber").isEqualTo("B2345CD")
          .jsonPath("content[1].domesticStatusDpsIds").isEqualTo("1,2")
          .jsonPath("content[1].numberOfChildrenDpsIds").isEqualTo("3,4")
          .jsonPath("content[1].whenCreated").isNotEmpty
      }

      @Test
      fun `should return an empty list`() {
        saveMapping(label = "wrong-migration")

        webTestClient.get().uri("/mapping/contact-person/profile-details/migration/migration-id/some_migration_id")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
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
          it.path("/mapping/contact-person/profile-details/migration/migration-id/some_migration_id")
            .queryParam("size", "$pageSize")
            .queryParam("page", "0")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(4)
          .jsonPath("numberOfElements").isEqualTo(pageSize)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(2)
          .jsonPath("size").isEqualTo(pageSize)
          .jsonPath("content.size()").isEqualTo(pageSize)
          .jsonPath("content[0].prisonerNumber").isEqualTo("A0001BC")
          .jsonPath("content[1].prisonerNumber").isEqualTo("A0002BC")
          .jsonPath("content[2].prisonerNumber").isEqualTo("A0003BC")

        webTestClient.get().uri {
          it.path("/mapping/contact-person/profile-details/migration/migration-id/some_migration_id")
            .queryParam("size", "$pageSize")
            .queryParam("page", "1")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(4)
          .jsonPath("numberOfElements").isEqualTo(1)
          .jsonPath("number").isEqualTo(1)
          .jsonPath("totalPages").isEqualTo(2)
          .jsonPath("size").isEqualTo(pageSize)
          .jsonPath("content.size()").isEqualTo(1)
          .jsonPath("content[0].prisonerNumber").isEqualTo("A0004BC")
      }
    }

    private fun saveMapping(offset: Int, label: String = "some_migration_id") = saveMapping("A${offset.toString().padStart(4, '0')}BC", label, "[$offset]", "[$offset]")

    private fun saveMapping(
      nomisPrisonerNumber: String = "A1234BC",
      label: String = "some_migration_id",
      domesticStatusDpsIds: String = "1,2",
      numberOfChildrenDpsIds: String = "3,4",
    ) = runTest {
      repository.save(
        ContactPersonProfileDetailMigrationMapping(nomisPrisonerNumber, label, domesticStatusDpsIds, numberOfChildrenDpsIds),
      )
    }
  }
}
