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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AppointmentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AppointmentMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.AppointmentMappingRepository

private const val NOMIS_EVENT_ID = 1234L
private const val APPOINTMENT_INSTANCE_ID = 4444L

class AppointmentMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var repository: AppointmentMappingRepository

  private fun createMapping(
    nomisId: Long = NOMIS_EVENT_ID,
    appointmentId: Long = APPOINTMENT_INSTANCE_ID,
    mappingType: String = AppointmentMappingType.NOMIS_CREATED.name
  ): AppointmentMappingDto = AppointmentMappingDto(
    nomisEventId = nomisId,
    appointmentInstanceId = appointmentId,
    mappingType = mappingType
  )

  private fun postCreateMappingRequest(
    nomisId: Long = NOMIS_EVENT_ID,
    appointmentId: Long = APPOINTMENT_INSTANCE_ID,
    mappingType: String = AppointmentMappingType.NOMIS_CREATED.name
  ) {
    webTestClient.post().uri("/mapping/appointments")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createMapping(
            nomisId = nomisId,
            appointmentId = appointmentId,
            mappingType = mappingType
          )
        )
      )
      .exchange()
      .expectStatus().isCreated
  }

  @DisplayName("POST /mapping/appointments")
  @Nested
  inner class CreateMappingTest {

    @AfterEach
    fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/appointments")
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create visit forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create when mapping for appointment id already exists for another appointment instance`() {
      postCreateMappingRequest()

      assertThat(
        webTestClient.post().uri("/mapping/appointments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createMapping().copy(nomisEventId = 21)))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Validation failure: Appointment mapping id = 4444 already exists")
    }

    @Test
    fun `create mapping succeeds when the same mapping already exists for the same appointment schedule`() {
      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisEventId"          : $NOMIS_EVENT_ID,
            "appointmentInstanceId" : $APPOINTMENT_INSTANCE_ID,
            "mappingType"           : "APPOINTMENT_CREATED"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisEventId"          : $NOMIS_EVENT_ID,
            "appointmentInstanceId" : $APPOINTMENT_INSTANCE_ID,
            "mappingType"           : "APPOINTMENT_CREATED"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    fun `create when mapping for nomis ids already exists`() {
      postCreateMappingRequest()

      assertThat(
        webTestClient.post().uri("/mapping/appointments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createMapping().copy(appointmentInstanceId = 99)))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Validation failure: Appointment with Nomis id=1234 already exists")
    }

    @Test
    fun `create mapping success`() {
      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisEventId"          : $NOMIS_EVENT_ID,
            "appointmentInstanceId" : $APPOINTMENT_INSTANCE_ID,
            "mappingType"           : "APPOINTMENT_CREATED"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated

      val mapping2 = webTestClient.get().uri("/mapping/appointments/appointment-instance-id/$APPOINTMENT_INSTANCE_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AppointmentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.nomisEventId).isEqualTo(NOMIS_EVENT_ID)
      assertThat(mapping2.appointmentInstanceId).isEqualTo(APPOINTMENT_INSTANCE_ID)
      assertThat(mapping2.mappingType).isEqualTo("APPOINTMENT_CREATED")
    }
  }

  @DisplayName("GET /mapping/appointments/appointment-instance-id/{appointmentInstanceId}")
  @Nested
  inner class GetMappingTest {

    @AfterEach
    fun deleteData() {
      runBlocking {
        repository.deleteAll()
      }
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/appointments/appointment-instance-id/$APPOINTMENT_INSTANCE_ID")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/appointments/appointment-instance-id/$APPOINTMENT_INSTANCE_ID")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/appointments/appointment-instance-id/$APPOINTMENT_INSTANCE_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {

      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/appointments/appointment-instance-id/$APPOINTMENT_INSTANCE_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AppointmentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisEventId).isEqualTo(NOMIS_EVENT_ID)
      assertThat(mapping.appointmentInstanceId).isEqualTo(APPOINTMENT_INSTANCE_ID)
      assertThat(mapping.mappingType).isEqualTo(AppointmentMappingType.NOMIS_CREATED.name)
    }

    @Test
    fun `mapping not found`() {
      webTestClient.get().uri("/mapping/appointments/appointment-instance-id/765")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).isEqualTo("Not Found: appointmentInstanceId=765")
        }
    }

    @Test
    fun `get mapping success with update role`() {

      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/appointments/appointment-instance-id/$APPOINTMENT_INSTANCE_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("DELETE /mapping/appointments/appointment-instance-id/{appointmentInstanceId}")
  @Nested
  inner class DeleteMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/appointments/appointment-instance-id/999")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/appointments/appointment-instance-id/999")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/appointments/appointment-instance-id/999")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete specific mapping success`() {
      // create mapping
      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      // it is present after creation by appointment id
      webTestClient.get().uri("/mapping/appointments/appointment-instance-id/$APPOINTMENT_INSTANCE_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk

      // delete mapping
      webTestClient.delete().uri("/mapping/appointments/appointment-instance-id/$APPOINTMENT_INSTANCE_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isNoContent

      // no longer present by appointment id
      webTestClient.get().uri("/mapping/appointments/appointment-instance-id/$APPOINTMENT_INSTANCE_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `delete is idempotent`() {
      // create mapping
      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      // delete mapping
      webTestClient.delete().uri("/mapping/appointments/appointment-instance-id/$APPOINTMENT_INSTANCE_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isNoContent

      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/appointments/appointment-instance-id/$APPOINTMENT_INSTANCE_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isNoContent
    }
  }
}
