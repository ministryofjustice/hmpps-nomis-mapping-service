package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson.profiledetails.ContactPersonProfileDetailMigrationMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson.profiledetails.ContactPersonProfileDetailsMigrationMappingRequest
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ContactPersonProfileDetailsMigrationMappingIntTest(
  @Autowired private val repository: ContactPersonProfileDetailMigrationMappingRepository,
) : IntegrationTestBase() {

  @Nested
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
          .jsonPath("$.nomisPrisonerNumber").isEqualTo("A1234BC")
          .jsonPath("$.label").isEqualTo(migrationId)
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
    ) = ContactPersonProfileDetailsMigrationMappingRequest(
      prisonerNumber,
      migrationId,
      domesticStatusDpsIds,
      numberOfChildrenDpsIds,
    )
  }
}
