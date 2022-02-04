package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.MappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase

private const val nomisId = 1234L
private const val vsipId = "12345678"

private fun createMapping(): MappingDto {
  return MappingDto(
    nomisId = nomisId,
    vsipId = vsipId,
    label = "2022-01-01",
    mappingType = "ONLINE",
  )
}

class MappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var repository: Repository

  @DisplayName("Create")
  @Nested
  inner class CreateMappingTest {

    @AfterEach
    internal fun deleteData() {
      repository.delete(nomisId)
    }

    @Test
    fun `create mapping success`() {
      webTestClient.post().uri("/mapping")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_NOMIS")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NOMIS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(MappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping1.nomisId).isEqualTo(nomisId)
      assertThat(mapping1.vsipId).isEqualTo(vsipId)
      assertThat(mapping1.label).isEqualTo("2022-01-01")
      assertThat(mapping1.mappingType).isEqualTo("ONLINE")

      val mapping2 = webTestClient.get().uri("/mapping/vsipId/$vsipId")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NOMIS")))
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
}
