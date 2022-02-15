package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
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

private const val nomisId = 1234L
private const val vsipId = "12345678"

private fun createMapping(): MappingDto = MappingDto(
  nomisId = nomisId,
  vsipId = vsipId,
  label = "2022-01-01",
  mappingType = "ONLINE",
)

class MappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var repository: Repository

  @DisplayName("Create")
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
      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

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
    fun `create visit forbidden with wrong role`() {
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
    fun `delete visit mappings forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_MAPPING")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete room mapping success`() {
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
  }
}
