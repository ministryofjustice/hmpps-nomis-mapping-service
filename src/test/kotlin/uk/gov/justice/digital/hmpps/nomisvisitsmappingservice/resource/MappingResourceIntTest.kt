package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.byLessThan
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.MappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.RoomMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val nomisId = 1234L
private const val vsipId = "12345678"

private fun createMapping(
  nomisIdOverride: Long = nomisId,
  vsipIdOverride: String = vsipId,
  label: String = "2022-01-01",
  mappingType: String = "ONLINE"
): MappingDto = MappingDto(
  nomisId = nomisIdOverride,
  vsipId = vsipIdOverride,
  label = label,
  mappingType = mappingType,
)

class MappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var repository: Repository

  private fun postCreateMappingRequest(
    nomisIdOverride: Long = nomisId,
    vsipIdOverride: String = vsipId,
    label: String = "2022-01-01",
    mappingType: String = "ONLINE"
  ) {
    webTestClient.post().uri("/mapping")
      .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createMapping(
            vsipIdOverride = vsipIdOverride,
            nomisIdOverride = nomisIdOverride,
            label = label,
            mappingType = mappingType
          )
        )
      )
      .exchange()
      .expectStatus().isCreated
  }

  @DisplayName("Create id mapping")
  @Nested
  inner class CreateMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping")
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create visit forbidden with wrong role`() {
      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create when mapping already exists`() {
      postCreateMappingRequest()

      assertThat(
        webTestClient.post().uri("/mapping")
          .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createMapping().copy(vsipId = "other")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Validation failure: Nomis visit id = 1234 already exists")
    }

    @Test
    fun `create mapping success`() {
      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisId"     : $nomisId,
            "vsipId"      : "$vsipId",
            "label"       : "2022-01-01",
            "mappingType" : "ONLINE"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated

      val mapping1 = webTestClient.get().uri("/mapping/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
        .exchange()
        .expectStatus().isOk
        .expectBody(MappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping1.nomisId).isEqualTo(nomisId)
      assertThat(mapping1.vsipId).isEqualTo(vsipId)
      assertThat(mapping1.label).isEqualTo("2022-01-01")
      assertThat(mapping1.mappingType).isEqualTo("ONLINE")

      val mapping2 = webTestClient.get().uri("/mapping/vsipId/$vsipId")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
        .exchange()
        .expectStatus().isOk
        .expectBody(MappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.nomisId).isEqualTo(nomisId)
      assertThat(mapping2.vsipId).isEqualTo(vsipId)
      assertThat(mapping2.label).isEqualTo("2022-01-01")
      assertThat(mapping2.mappingType).isEqualTo("ONLINE")
    }
  }

  @DisplayName("get NOMIS id mapping")
  @Nested
  inner class GetNomisMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/nomisId/$nomisId")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {

      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
        .exchange()
        .expectStatus().isOk
        .expectBody(MappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisId).isEqualTo(nomisId)
      assertThat(mapping.vsipId).isEqualTo(vsipId)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo("ONLINE")
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/nomisId/99999")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: NOMIS visit id=99999")
    }

    @Test
    fun `get mapping success with update role`() {

      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /mapping/migrated/latest")
  @Nested
  inner class GeMappingMigratedLatestTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {

      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              vsipIdOverride = "10",
              nomisIdOverride = 10,
              label = "2022-01-01T00:00:00",
              mappingType = "MIGRATED"
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              vsipIdOverride = "20",
              nomisIdOverride = 20,
              label = "2022-01-02T00:00:00",
              mappingType = "MIGRATED"
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              vsipIdOverride = "1",
              nomisIdOverride = 1,
              label = "2022-01-02T10:00:00",
              mappingType = "MIGRATED"
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              vsipIdOverride = "99",
              nomisIdOverride = 199,
              label = "whatever",
              mappingType = "ONLINE"
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
        .exchange()
        .expectStatus().isOk
        .expectBody(MappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisId).isEqualTo(1)
      assertThat(mapping.vsipId).isEqualTo("1")
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo("MIGRATED")
      assertThat(mapping.whenCreated).isCloseTo(LocalDateTime.now(), byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              vsipIdOverride = "99",
              nomisIdOverride = 199,
              label = "whatever",
              mappingType = "ONLINE"
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }

  @DisplayName("get VSIP id mapping")
  @Nested
  inner class GetVsipMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/vsipId/$vsipId")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/vsipId/$vsipId")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/vsipId/$vsipId")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {

      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/vsipId/$vsipId")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
        .exchange()
        .expectStatus().isOk
        .expectBody(MappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisId).isEqualTo(nomisId)
      assertThat(mapping.vsipId).isEqualTo(vsipId)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo("ONLINE")
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/vsipId/NOT_THERE")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: VSIP visit id=NOT_THERE")
    }

    @Test
    fun `get mapping success with update role`() {

      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/vsipId/$vsipId")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("get room mapping")
  @Nested
  inner class GetRoomMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/prison/HEI/room/nomis-room-id/HEI-VISITS-SOC_VIS")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/prison/HEI/room/nomis-room-id/HEI-VISITS-SOC_VIS")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/prison/HEI/room/nomis-room-id/HEI-VISITS-SOC_VIS")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get room mapping success`() {
      val mapping1 = webTestClient.get().uri("/prison/HEI/room/nomis-room-id/HEI-VISITS-SOC_VIS")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
        .exchange()
        .expectStatus().isOk
        .expectBody(RoomMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping1.nomisRoomDescription).isEqualTo("HEI-VISITS-SOC_VIS")
      assertThat(mapping1.vsipId).isEqualTo("VSIP_SOC_VIS")
      assertThat(mapping1.isOpen).isEqualTo(true)
      assertThat(mapping1.prisonId).isEqualTo("HEI")
    }

    @Test
    fun `room mapping not found`() {
      val error = webTestClient.get().uri("/prison/HEI/room/nomis-room-id/HEI-NOT_THERE")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: prison id=HEI, nomis room id=HEI-NOT_THERE")
    }
  }

  @DisplayName("delete visit migration mapping")
  @Nested
  inner class DeleteMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete visit mapping success`() {
      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisId"     : $nomisId,
            "vsipId"      : "$vsipId",
            "label"       : "2022-01-01",
            "mappingType" : "ONLINE"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_ADMIN_MAPPING")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/mapping/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
        .exchange()
        .expectStatus().isNotFound
    }

    @DisplayName("get visit mapping by migration id")
    @Nested
    inner class GetVisitMappingByMigrationIdTest {

      @AfterEach
      internal fun deleteData() = runBlocking {
        repository.deleteAll()
      }

      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get().uri("/mapping/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `get visit mappings by migration id forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `get visit mappings by migration id success`() {

        (1L..4L).forEach {
          postCreateMappingRequest(
            vsipIdOverride = it.toString(),
            nomisIdOverride = it,
            label = "2022-01-01",
            mappingType = "MIGRATED"
          )
        }
        (5L..9L).forEach {
          postCreateMappingRequest(
            vsipIdOverride = it.toString(),
            nomisIdOverride = it,
            label = "2099-01-01",
            mappingType = "MIGRATED"
          )
        }
        postCreateMappingRequest(nomisIdOverride = 12, vsipIdOverride = "12", mappingType = "ONLINE")

        webTestClient.get().uri("/mapping/migration-id/2022-01-01")
          .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(4)
          .jsonPath("$.content..nomisId").value(
            Matchers.contains(
              1, 2, 3, 4
            )
          )
      }

      @Test
      fun `get visit mappings by migration id - no records exist`() {

        (1L..4L).forEach {
          postCreateMappingRequest(
            vsipIdOverride = it.toString(),
            nomisIdOverride = it,
            label = "2022-01-01",
            mappingType = "MIGRATED"
          )
        }

        webTestClient.get().uri("/mapping/migration-id/2044-01-01")
          .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(0)
          .jsonPath("content").isEmpty
      }

      @Test
      fun `can request a different page size`() {

        (1L..6L).forEach {
          postCreateMappingRequest(
            vsipIdOverride = it.toString(),
            nomisIdOverride = it,
            label = "2022-01-01",
            mappingType = "MIGRATED"
          )
        }
        webTestClient.get().uri {
          it.path("/mapping/migration-id/2022-01-01")
            .queryParam("size", "2")
            .queryParam("sort", "nomisId,asc")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
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
          postCreateMappingRequest(
            vsipIdOverride = it.toString(),
            nomisIdOverride = it,
            label = "2022-01-01",
            mappingType = "MIGRATED"
          )
        }
        webTestClient.get().uri {
          it.path("/mapping/migration-id/2022-01-01")
            .queryParam("size", "2")
            .queryParam("page", "1")
            .queryParam("sort", "nomisId,asc")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_READ_MAPPING")))
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
}
