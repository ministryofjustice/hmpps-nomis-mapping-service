package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.isDuplicateMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.AdjudicationMappingRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val ADJUDICATION_NUMBER = 4444L

class AdjudicationMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var repository: AdjudicationMappingRepository

  private fun createMapping(
    adjudicationNumber: Long = ADJUDICATION_NUMBER,
    label: String = "2022-01-01",
    mappingType: String = AdjudicationMappingType.ADJUDICATION_CREATED.name,
  ): AdjudicationMappingDto = AdjudicationMappingDto(
    adjudicationNumber = adjudicationNumber,
    label = label,
    mappingType = mappingType,
  )

  private fun postCreateMappingRequest(
    adjudicationNumber: Long = ADJUDICATION_NUMBER,
    label: String = "2022-01-01",
    mappingType: String = AdjudicationMappingType.ADJUDICATION_CREATED.name,
  ) {
    webTestClient.post().uri("/mapping/adjudications")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createMapping(
            adjudicationNumber = adjudicationNumber,
            label = label,
            mappingType = mappingType,
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  @BeforeEach
  fun deleteData() = runBlocking {
    repository.deleteAll()
  }

  @DisplayName("POST /mapping/adjudications")
  @Nested
  inner class CreateMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/adjudications")
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create visit forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create mapping success - ADJUDICATION_CREATED`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "adjudicationNumber" : $ADJUDICATION_NUMBER
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping2 = webTestClient.get().uri("/mapping/adjudications/$ADJUDICATION_NUMBER")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AdjudicationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.adjudicationNumber).isEqualTo(ADJUDICATION_NUMBER)
      assertThat(mapping2.mappingType).isEqualTo("ADJUDICATION_CREATED")
    }

    @Test
    fun `create mapping success - MIGRATED`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "adjudicationNumber" : $ADJUDICATION_NUMBER,
            "label"                 : "2023-04-20",
            "mappingType"           : "MIGRATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping2 = webTestClient.get().uri("/mapping/adjudications/$ADJUDICATION_NUMBER")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AdjudicationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.adjudicationNumber).isEqualTo(ADJUDICATION_NUMBER)
      assertThat(mapping2.label).isEqualTo("2023-04-20")
      assertThat(mapping2.mappingType).isEqualTo("MIGRATED")
    }

    @Test
    fun `create mapping failure - adjudication exists`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "adjudicationNumber" : $ADJUDICATION_NUMBER,
            "label"                 : "2023-04-20",
            "mappingType"           : "MIGRATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "adjudicationNumber" : $ADJUDICATION_NUMBER,
            "label"                 : "2023-04-25",
            "mappingType"           : "MIGRATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isDuplicateMapping

      val mapping2 = webTestClient.get().uri("/mapping/adjudications/$ADJUDICATION_NUMBER")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AdjudicationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.adjudicationNumber).isEqualTo(ADJUDICATION_NUMBER)
      assertThat(mapping2.label).isEqualTo("2023-04-20")
      assertThat(mapping2.mappingType).isEqualTo("MIGRATED")
    }
  }

  @DisplayName("GET /mapping/adjudications/{adjudicationNumber}")
  @Nested
  inner class GetMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/adjudications/$ADJUDICATION_NUMBER")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/adjudications/$ADJUDICATION_NUMBER")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/adjudications/$ADJUDICATION_NUMBER")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/adjudications/$ADJUDICATION_NUMBER")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AdjudicationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.adjudicationNumber).isEqualTo(ADJUDICATION_NUMBER)
    }

    @Test
    fun `mapping not found`() {
      webTestClient.get().uri("/mapping/adjudications/765")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).isEqualTo("Not Found: adjudicationNumber=765")
        }
    }
  }

  @DisplayName("GET /mapping/adjudications/migrated/latest")
  @Nested
  inner class GeMappingMigratedLatestTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/adjudications/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/adjudications/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/adjudications/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              adjudicationNumber = 10,
              label = "2022-01-01T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              adjudicationNumber = 20,
              label = "2022-01-02T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              adjudicationNumber = 1,
              label = "2022-01-02T10:00:00",
              mappingType = AdjudicationMappingType.MIGRATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              adjudicationNumber = 199,
              label = "whatever",
              mappingType = AdjudicationMappingType.ADJUDICATION_CREATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/adjudications/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AdjudicationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.adjudicationNumber).isEqualTo(1)
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo("MIGRATED")
      assertThat(mapping.whenCreated)
        .isCloseTo(LocalDateTime.now(), Assertions.byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              adjudicationNumber = 77,
              label = "whatever",
              mappingType = AdjudicationMappingType.ADJUDICATION_CREATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/adjudications/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }

  @DisplayName("GET /mapping/adjudications")
  @Nested
  inner class GetAllMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/adjudications")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      postCreateMappingRequest(201)
      postCreateMappingRequest(202)

      val mapping = webTestClient.get().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody<List<AdjudicationMappingDto>>()
        .returnResult().responseBody!!

      assertThat(mapping[0].adjudicationNumber).isEqualTo(201)
      assertThat(mapping[1].adjudicationNumber).isEqualTo(202)
      assertThat(mapping).hasSize(2)
    }
  }

  @DisplayName("DELETE /mapping/adjudications/{adjudicationNumber}")
  @Nested
  inner class DeleteMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/adjudications/999")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/adjudications/999")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/adjudications/999")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete specific mapping success`() {
      // create mapping
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      // it is present after creation by adjudication id
      webTestClient.get().uri("/mapping/adjudications/$ADJUDICATION_NUMBER")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk

      // delete mapping
      webTestClient.delete().uri("/mapping/adjudications/$ADJUDICATION_NUMBER")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent

      // no longer present by adjudication id
      webTestClient.get().uri("/mapping/adjudications/$ADJUDICATION_NUMBER")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `delete is idempotent`() {
      // create mapping
      webTestClient.post().uri("/mapping/adjudications")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      // delete mapping
      webTestClient.delete().uri("/mapping/adjudications/$ADJUDICATION_NUMBER")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent

      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/adjudications/$ADJUDICATION_NUMBER")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @DisplayName("GET /mapping/adjudications/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationIdTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/adjudications/migration-id/2022-01-01T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/adjudications/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get adjudication mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/adjudications/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get adjudication mappings by migration id success`() {
      (1L..4L).forEach {
        postCreateMappingRequest(it, label = "2022-01-01", mappingType = "MIGRATED")
      }
      (5L..9L).forEach {
        postCreateMappingRequest(it, label = "2099-01-01", mappingType = "MIGRATED")
      }
      postCreateMappingRequest(12, mappingType = AdjudicationMappingType.ADJUDICATION_CREATED.name)

      webTestClient.get().uri("/mapping/adjudications/migration-id/2022-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..adjudicationNumber").value(
          Matchers.contains(1, 2, 3, 4),
        )
        .jsonPath("$.content[0].whenCreated").isNotEmpty
    }

    @Test
    fun `get adjudication mappings by migration id - no records exist`() {
      (1L..4L).forEach {
        postCreateMappingRequest(it, label = "2022-01-01", mappingType = "MIGRATED")
      }

      webTestClient.get().uri("/mapping/adjudications/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
        .jsonPath("content").isEmpty
    }

    @Test
    fun `can request a different page size`() {
      (1L..6L).forEach {
        postCreateMappingRequest(it, label = "2022-01-01", mappingType = "MIGRATED")
      }
      webTestClient.get().uri {
        it.path("/mapping/adjudications/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("sort", "adjudicationNumber,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(6)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(3)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    fun `can request a different page`() {
      (1L..3L).forEach {
        postCreateMappingRequest(it, label = "2022-01-01", mappingType = "MIGRATED")
      }
      webTestClient.get().uri {
        it.path("/mapping/adjudications/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .queryParam("sort", "adjudicationNumber,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(1)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
    }
  }
}