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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.ActivityMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMappingType

class ActivityMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var repository: ActivityRepository

  private val nomisCourseActivityId = 1234L
  private val activityScheduleId = 4444L

  private fun createMapping(
    nomisId: Long = nomisCourseActivityId,
    activityId: Long = activityScheduleId,
    mappingType: String = ActivityMappingType.NOMIS_CREATED.name
  ): ActivityMappingDto = ActivityMappingDto(
    nomisCourseActivityId = nomisId,
    activityScheduleId = activityId,
    mappingType = mappingType
  )

  private fun postCreateMappingRequest(
    nomisId: Long = nomisCourseActivityId,
    activityId: Long = activityScheduleId,
    mappingType: String = ActivityMappingType.NOMIS_CREATED.name
  ) {
    webTestClient.post().uri("/mapping/activities")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createMapping(
            nomisId = nomisId,
            activityId = activityId,
            mappingType = mappingType
          )
        )
      )
      .exchange()
      .expectStatus().isCreated
  }

  @DisplayName("POST /mapping/activities")
  @Nested
  inner class CreateMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/activities")
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create visit forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create when mapping for activity id already exists for another activity schedule`() {
      postCreateMappingRequest()

      assertThat(
        webTestClient.post().uri("/mapping/activities")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createMapping().copy(nomisCourseActivityId = 21)))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Validation failure: Activity mapping id = 4444 already exists")
    }

    @Test
    internal fun `create mapping succeeds when the same mapping already exists for the same activity schedule`() {
      webTestClient.post().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisCourseActivityId" : $nomisCourseActivityId,
            "activityScheduleId"    : $activityScheduleId,
            "mappingType"           : "ACTIVITY_CREATED"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisCourseActivityId" : $nomisCourseActivityId,
            "activityScheduleId"    : $activityScheduleId,
            "mappingType"           : "ACTIVITY_CREATED"
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
        webTestClient.post().uri("/mapping/activities")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createMapping().copy(activityScheduleId = 99)))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage
      ).isEqualTo("Validation failure: Activity with Nomis id=1234 already exists")
    }

    @Test
    fun `create mapping success`() {
      webTestClient.post().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisCourseActivityId" : $nomisCourseActivityId,
            "activityScheduleId"    : $activityScheduleId,
            "mappingType"           : "ACTIVITY_CREATED"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated

//      val mapping1 =
//        webTestClient.get().uri("/mapping/activities/activity-schedule-id/$nomisCourseActivityId")
//          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
//          .exchange()
//          .expectStatus().isOk
//          .expectBody(ActivityMappingDto::class.java)
//          .returnResult().responseBody!!
//
//      assertThat(mapping1.nomisBookingId).isEqualTo(nomisCourseActivityId)
//      assertThat(mapping1.incentiveId).isEqualTo(activityScheduleId)
//      assertThat(mapping1.label).isEqualTo("2022-01-01")
//      assertThat(mapping1.mappingType).isEqualTo("ACTIVITY_CREATED")

      val mapping2 = webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody(ActivityMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.nomisCourseActivityId).isEqualTo(nomisCourseActivityId)
      assertThat(mapping2.activityScheduleId).isEqualTo(activityScheduleId)
      assertThat(mapping2.mappingType).isEqualTo("ACTIVITY_CREATED")
    }
  }

  @DisplayName("GET /mapping/activities/activity-schedule/{activityScheduleId}")
  @Nested
  inner class GetMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {

      webTestClient.post().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody(ActivityMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisCourseActivityId).isEqualTo(nomisCourseActivityId)
      assertThat(mapping.activityScheduleId).isEqualTo(activityScheduleId)
      assertThat(mapping.mappingType).isEqualTo(ActivityMappingType.NOMIS_CREATED.name)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/activities/activity-schedule-id/765")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: Activity schedule id=765")
    }

    @Test
    fun `get mapping success with update role`() {

      webTestClient.post().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("DELETE /mapping/activities/activity-schedule-id/{activityScheduleId}")
  @Nested
  inner class DeleteMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/activities/activity-schedule-id/999")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/activities/activity-schedule-id/999")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/activities/activity-schedule-id/999")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete specific mapping success`() {
      // create mapping
      webTestClient.post().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      // it is present after creation by activity id
      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk

      // delete mapping
      webTestClient.delete().uri("/mapping/activities/activity-schedule-id/$activityScheduleId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNoContent

      // no longer present by activity id
      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    internal fun `delete is idempotent`() {
      // create mapping
      webTestClient.post().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      // delete mapping
      webTestClient.delete().uri("/mapping/activities/activity-schedule-id/$activityScheduleId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNoContent

      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/activities/activity-schedule-id/$activityScheduleId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNoContent
    }
  }
}
