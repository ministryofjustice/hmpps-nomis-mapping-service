package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AppointmentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AppointmentMappingType.APPOINTMENT_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AppointmentMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.AppointmentMappingRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val NOMIS_EVENT_ID = 1234L
private const val APPOINTMENT_INSTANCE_ID = 4444L

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentMappingResourceIntTest : IntegrationTestBase() {

  @SpyBean
  lateinit var repository: AppointmentMappingRepository

  private fun createMapping(
    nomisId: Long = NOMIS_EVENT_ID,
    appointmentId: Long = APPOINTMENT_INSTANCE_ID,
    label: String = "2022-01-01",
    mappingType: String = APPOINTMENT_CREATED.name,
  ): AppointmentMappingDto = AppointmentMappingDto(
    nomisEventId = nomisId,
    appointmentInstanceId = appointmentId,
    label = label,
    mappingType = mappingType,
  )

  private fun postCreateMappingRequest(
    nomisId: Long = NOMIS_EVENT_ID,
    appointmentId: Long = APPOINTMENT_INSTANCE_ID,
    label: String = "2022-01-01",
    mappingType: String = APPOINTMENT_CREATED.name,
  ) {
    webTestClient.post().uri("/mapping/appointments")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createMapping(
            nomisId = nomisId,
            appointmentId = appointmentId,
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

  @DisplayName("POST /mapping/appointments")
  @Nested
  inner class CreateMappingTest {

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

      val responseBody =
        webTestClient.post().uri("/mapping/appointments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createMapping().copy(nomisEventId = 21)))
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<AppointmentMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Appointment mapping already exists. \nExisting mapping: AppointmentMappingDto(appointmentInstanceId=4444, nomisEventId=1234, label=2022-01-01, mappingType=APPOINTMENT_CREATED, whenCreated=")
        assertThat(userMessage).contains("Duplicate mapping: AppointmentMappingDto(appointmentInstanceId=4444, nomisEventId=21, label=2022-01-01, mappingType=APPOINTMENT_CREATED, whenCreated=")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existing = responseBody.moreInfo?.existing!!
      with(existing) {
        assertThat(appointmentInstanceId).isEqualTo(4444)
        assertThat(nomisEventId).isEqualTo(1234)
      }

      val duplicate = responseBody.moreInfo?.duplicate!!
      with(duplicate) {
        assertThat(appointmentInstanceId).isEqualTo(4444)
        assertThat(nomisEventId).isEqualTo(21)
      }
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
            "appointmentInstanceId" : $APPOINTMENT_INSTANCE_ID
          }""",
          ),
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
            "appointmentInstanceId" : $APPOINTMENT_INSTANCE_ID
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    fun `create when mapping for nomis ids already exists`() {
      postCreateMappingRequest()

      val responseBody =
        webTestClient.post().uri("/mapping/appointments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createMapping().copy(appointmentInstanceId = 99)))
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<AppointmentMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Appointment mapping already exists. \nExisting mapping: AppointmentMappingDto(appointmentInstanceId=4444, nomisEventId=1234, label=2022-01-01, mappingType=APPOINTMENT_CREATED, whenCreated=")
        assertThat(userMessage).contains("Duplicate mapping: AppointmentMappingDto(appointmentInstanceId=99, nomisEventId=1234, label=2022-01-01, mappingType=APPOINTMENT_CREATED, whenCreated=")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existing = responseBody.moreInfo?.existing!!
      with(existing) {
        assertThat(appointmentInstanceId).isEqualTo(4444)
        assertThat(nomisEventId).isEqualTo(1234)
      }

      val duplicate = responseBody.moreInfo?.duplicate!!
      with(duplicate) {
        assertThat(appointmentInstanceId).isEqualTo(99)
        assertThat(nomisEventId).isEqualTo(1234)
      }
    }

    @Test
    fun `create mapping success - APPOINTMENT_CREATED`() {
      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisEventId"          : $NOMIS_EVENT_ID,
            "appointmentInstanceId" : $APPOINTMENT_INSTANCE_ID
          }""",
          ),
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

    @Test
    fun `create mapping - Duplicate db error`() = runTest {
      webTestClient
        .post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(nomisId = 101, appointmentId = APPOINTMENT_INSTANCE_ID),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      // Emulate calling service simultaneously twice by disabling the duplicate check
      // Note: the spy is automatically reset by ResetMocksTestExecutionListener
      whenever(repository.findById(APPOINTMENT_INSTANCE_ID)).thenReturn(null)

      val responseBody =
        webTestClient.post().uri("/mapping/appointments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              createMapping(nomisId = 102, appointmentId = APPOINTMENT_INSTANCE_ID),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<AppointmentMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Appointment mapping already exists, detected by org.springframework.dao.DuplicateKeyException")
        assertThat(errorCode).isEqualTo(1409)
      }
    }

    @Test
    fun `create mapping success - MIGRATED`() {
      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisEventId"          : $NOMIS_EVENT_ID,
            "appointmentInstanceId" : $APPOINTMENT_INSTANCE_ID,
            "label"                 : "2023-04-20",
            "mappingType"           : "MIGRATED"
          }""",
          ),
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
      assertThat(mapping2.label).isEqualTo("2023-04-20")
      assertThat(mapping2.mappingType).isEqualTo("MIGRATED")
    }
  }

  @DisplayName("GET /mapping/appointments/appointment-instance-id/{appointmentInstanceId}")
  @Nested
  inner class GetMappingTest {

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
  }

  @DisplayName("GET /mapping/appointments/migrated/latest")
  @Nested
  inner class GeMappingMigratedLatestTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/appointments/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/appointments/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/appointments/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {
      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              nomisId = 10,
              appointmentId = 10,
              label = "2022-01-01T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              nomisId = 20,
              appointmentId = 20,
              label = "2022-01-02T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              nomisId = 1,
              appointmentId = 1,
              label = "2022-01-02T10:00:00",
              mappingType = MIGRATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              nomisId = 99,
              appointmentId = 199,
              label = "whatever",
              mappingType = APPOINTMENT_CREATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/appointments/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AppointmentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisEventId).isEqualTo(1)
      assertThat(mapping.appointmentInstanceId).isEqualTo(1)
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo("MIGRATED")
      assertThat(mapping.whenCreated)
        .isCloseTo(LocalDateTime.now(), Assertions.byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              nomisId = 77,
              appointmentId = 77,
              label = "whatever",
              mappingType = APPOINTMENT_CREATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/appointments/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }

  @DisplayName("GET /mapping/appointments/nomis-event-id/{eventId}")
  @Nested
  inner class GetMappingByEventTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/appointments/nomis-event-id/$NOMIS_EVENT_ID")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/appointments/nomis-event-id/$NOMIS_EVENT_ID")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/appointments/nomis-event-id/$NOMIS_EVENT_ID")
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

      val mapping = webTestClient.get().uri("/mapping/appointments/nomis-event-id/$NOMIS_EVENT_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AppointmentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisEventId).isEqualTo(NOMIS_EVENT_ID)
      assertThat(mapping.appointmentInstanceId).isEqualTo(APPOINTMENT_INSTANCE_ID)
    }

    @Test
    fun `mapping not found`() {
      webTestClient.get().uri("/mapping/appointments/nomis-event-id/765")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).isEqualTo("Not Found: eventId=765")
        }
    }
  }

  @DisplayName("GET /mapping/appointments")
  @Nested
  inner class GetAllMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/appointments")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      postCreateMappingRequest(101, 201)
      postCreateMappingRequest(102, 202)

      val mapping = webTestClient.get().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody<List<AppointmentMappingDto>>()
        .returnResult().responseBody!!

      assertThat(mapping[0].nomisEventId).isEqualTo(101)
      assertThat(mapping[0].appointmentInstanceId).isEqualTo(201)
      assertThat(mapping[1].nomisEventId).isEqualTo(102)
      assertThat(mapping[1].appointmentInstanceId).isEqualTo(202)
      assertThat(mapping).hasSize(2)
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

  @OptIn(ExperimentalCoroutinesApi::class)
  @DisplayName("DELETE /mappings/appointments/migration-id/{migrationId}")
  @Nested
  inner class DeleteAllMappings {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/appointments/migration-id/2023-06-24T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/appointments/migration-id/2023-06-24T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/appointments/migration-id/2023-06-24T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete mapping success`() = runTest {
      postCreateMappingRequest(1, 11, "2023-06-23", mappingType = MIGRATED.name)
      postCreateMappingRequest(2, 22, "2023-06-24", mappingType = MIGRATED.name)
      postCreateMappingRequest(3, 33, mappingType = APPOINTMENT_CREATED.name)

      webTestClient.get().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].nomisEventId").isEqualTo(1)
        .jsonPath("$[0].appointmentInstanceId").isEqualTo(11)
        .jsonPath("$[0].label").isEqualTo("2023-06-23")
        .jsonPath("$[0].mappingType").isEqualTo("MIGRATED")
        .jsonPath("$[1].nomisEventId").isEqualTo(2)
        .jsonPath("$[1].appointmentInstanceId").isEqualTo(22)
        .jsonPath("$[1].label").isEqualTo("2023-06-24")
        .jsonPath("$[1].mappingType").isEqualTo("MIGRATED")
        .jsonPath("$[2].nomisEventId").isEqualTo(3)
        .jsonPath("$[2].appointmentInstanceId").isEqualTo(33)
        .jsonPath("$[2].mappingType").isEqualTo("APPOINTMENT_CREATED")

      webTestClient.delete().uri("/mapping/appointments/migration-id/2023-06-24")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/mapping/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].nomisEventId").isEqualTo(1)
        .jsonPath("$[0].appointmentInstanceId").isEqualTo(11)
        .jsonPath("$[0].label").isEqualTo("2023-06-23")
        .jsonPath("$[0].mappingType").isEqualTo("MIGRATED")
        .jsonPath("$[1].nomisEventId").isEqualTo(3)
        .jsonPath("$[1].appointmentInstanceId").isEqualTo(33)
        .jsonPath("$[1].mappingType").isEqualTo("APPOINTMENT_CREATED")
    }
  }

  @DisplayName("GET /mapping/appointments/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationIdTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/appointments/migration-id/2022-01-01T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/appointments/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get appointment mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/appointments/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get appointment mappings by migration id success`() {
      (1L..4L).forEach {
        postCreateMappingRequest(it, it, label = "2022-01-01", mappingType = "MIGRATED")
      }
      (5L..9L).forEach {
        postCreateMappingRequest(it, it, label = "2099-01-01", mappingType = "MIGRATED")
      }
      postCreateMappingRequest(12, 12, mappingType = APPOINTMENT_CREATED.name)

      webTestClient.get().uri("/mapping/appointments/migration-id/2022-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..nomisEventId").value(
          Matchers.contains(1, 2, 3, 4),
        )
        .jsonPath("$.content[0].whenCreated").isNotEmpty
    }

    @Test
    fun `get appointment mappings by migration id - no records exist`() {
      (1L..4L).forEach {
        postCreateMappingRequest(it, it, label = "2022-01-01", mappingType = "MIGRATED")
      }

      webTestClient.get().uri("/mapping/appointments/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
        .jsonPath("content").isEmpty
    }

    @Test
    fun `can request a different page size`() {
      (1L..6L).forEach {
        postCreateMappingRequest(it, it, label = "2022-01-01", mappingType = "MIGRATED")
      }
      webTestClient.get().uri {
        it.path("/mapping/appointments/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("sort", "nomisEventId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
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
        postCreateMappingRequest(it, it, label = "2022-01-01", mappingType = "MIGRATED")
      }
      webTestClient.get().uri {
        it.path("/mapping/appointments/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .queryParam("sort", "nomisEventId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
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
