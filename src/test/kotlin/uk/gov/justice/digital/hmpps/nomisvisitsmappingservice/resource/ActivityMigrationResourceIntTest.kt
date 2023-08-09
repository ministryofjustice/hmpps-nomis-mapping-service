@file:OptIn(ExperimentalCoroutinesApi::class)

package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.ActivityMigrationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders.ActivityMigrationRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMigrationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.ActivityMigrationMappingRepository

class ActivityMigrationResourceIntTest : IntegrationTestBase() {

  @SpyBean(name = "activityMigrationMappingRepository")
  private lateinit var activityMigrationMappingRepository: ActivityMigrationMappingRepository

  @Autowired
  private lateinit var activityMigrationRepository: ActivityMigrationRepository

  private val NOMIS_ID = 1234L
  private val ACTIVITY_ID = 4444L
  private val ACTIVITY_ID_2 = 5555L
  private val MIGRATION_ID = "migration-1"

  @AfterEach
  fun deleteData() = runBlocking {
    activityMigrationRepository.deleteAll()
  }

  private fun createMapping(
    nomisId: Long = NOMIS_ID,
    activityId: Long = ACTIVITY_ID,
    activityId2: Long? = ACTIVITY_ID_2,
    label: String = MIGRATION_ID,
  ): ActivityMigrationMappingDto = ActivityMigrationMappingDto(
    nomisCourseActivityId = nomisId,
    activityScheduleId = activityId,
    activityScheduleId2 = activityId2,
    label = label,
  )

  private fun postCreateMappingRequest(
    nomisId: Long = NOMIS_ID,
    activityId: Long = ACTIVITY_ID,
    activityId2: Long? = ACTIVITY_ID_2,
    label: String = MIGRATION_ID,
  ) =
    webTestClient.post().uri("/mapping/activities/migration")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        fromValue(
          createMapping(
            nomisId = nomisId,
            activityId = activityId,
            activityId2 = activityId2,
            label = label,
          ),
        ),
      )
      .exchange()

  @DisplayName("POST /mapping/activities/migration")
  @Nested
  inner class CreateMapping {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/activities/migration")
        .body(fromValue(createMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/activities/migration")
        .headers(setAuthorisation(roles = listOf()))
        .body(fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/activities/migration")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should create mapping`() = runTest {
      postCreateMappingRequest()
        .expectStatus().isCreated

      val saved = activityMigrationRepository.findById(NOMIS_ID)!!
      with(saved) {
        assertThat(activityScheduleId).isEqualTo(ACTIVITY_ID)
        assertThat(activityScheduleId2).isEqualTo(ACTIVITY_ID_2)
        assertThat(label).isEqualTo(MIGRATION_ID)
      }
    }

    @Test
    fun `should create mapping if 2nd activity id is null`() = runTest {
      postCreateMappingRequest(activityId2 = null)
        .expectStatus().isCreated

      val saved = activityMigrationRepository.findById(NOMIS_ID)!!
      with(saved) {
        assertThat(activityScheduleId).isEqualTo(ACTIVITY_ID)
        assertThat(activityScheduleId2).isNull()
        assertThat(label).isEqualTo(MIGRATION_ID)
      }
    }

    @Test
    fun `should return bad request if mapping already exists for different Activity id`() = runTest {
      postCreateMappingRequest()
        .expectStatus().isCreated

      postCreateMappingRequest(activityId = 1)
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Nomis mapping id = $NOMIS_ID already exists")
        }
    }

    @Test
    fun `should return created if mapping already exists for same Nomis and Activity id`() = runTest {
      postCreateMappingRequest()
        .expectStatus().isCreated

      postCreateMappingRequest()
        .expectStatus().isCreated
    }

    @Test
    fun `should return bad request if activity already exists for different Nomis id`() = runTest {
      postCreateMappingRequest()
        .expectStatus().isCreated

      postCreateMappingRequest(nomisId = 1)
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Activity migration mapping with Activity id=$ACTIVITY_ID and 2nd Activity Id=$ACTIVITY_ID_2 already exists")
        }
    }

    @Test
    fun `should return conflict if attempting to create duplicate`() = runTest {
      postCreateMappingRequest(activityId = 101, activityId2 = 102)
        .expectStatus().isCreated

      // Emulate calling service simultaneously by disabling the duplicate check
      // Note: the spy is automatically reset by ResetMocksTestExecutionListener
      whenever(activityMigrationMappingRepository.findById(NOMIS_ID)).thenReturn(null)

      postCreateMappingRequest(activityId = 103, activityId2 = 104)
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Conflict: Activity migration mapping already exists, detected by org.springframework.dao.DuplicateKeyException")
        }
    }
  }

  @DisplayName("GET /mapping/activities/migration/nomis-course-activity-id/{courseActivityId}")
  @Nested
  inner class GetMapping {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/activities/migration/nomis-course-activity-id/$NOMIS_ID")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/activities/migration/nomis-course-activity-id/$NOMIS_ID")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/activities/migration/nomis-course-activity-id/$NOMIS_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return OK if mapping exists`() = runTest {
      activityMigrationRepository.save(
        ActivityMigrationMapping(
          nomisCourseActivityId = NOMIS_ID,
          activityScheduleId = ACTIVITY_ID,
          activityScheduleId2 = ACTIVITY_ID_2,
          label = MIGRATION_ID,
        ),
      )

      webTestClient.get().uri("/mapping/activities/migration/nomis-course-activity-id/$NOMIS_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("nomisCourseActivityId").isEqualTo(NOMIS_ID)
        .jsonPath("activityScheduleId").isEqualTo(ACTIVITY_ID)
        .jsonPath("activityScheduleId2").isEqualTo(ACTIVITY_ID_2)
        .jsonPath("label").isEqualTo(MIGRATION_ID)
    }

    @Test
    fun `should handle null 2nd activity id`() = runTest {
      activityMigrationRepository.save(
        ActivityMigrationMapping(
          nomisCourseActivityId = NOMIS_ID,
          activityScheduleId = ACTIVITY_ID,
          activityScheduleId2 = null,
          label = MIGRATION_ID,
        ),
      )

      webTestClient.get().uri("/mapping/activities/migration/nomis-course-activity-id/$NOMIS_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("nomisCourseActivityId").isEqualTo(NOMIS_ID)
        .jsonPath("activityScheduleId").isEqualTo(ACTIVITY_ID)
        .jsonPath("activityScheduleId2").doesNotExist()
        .jsonPath("label").isEqualTo(MIGRATION_ID)
    }

    @Test
    fun `should return not found `() = runTest {
      webTestClient.get().uri("/mapping/activities/migration/nomis-course-activity-id/$NOMIS_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("nomisCourseActivityId=$NOMIS_ID")
        }
    }
  }

  @DisplayName("GET /mapping/activities/migrated/latest")
  @Nested
  inner class GetLatestMigratedMapping {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/activities/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/activities/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/activities/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should get latest migrated mapping`() = runTest {
      // Note that this relies on the whenCreated value defaulted by the database
      activityMigrationRepository.save(
        ActivityMigrationMapping(
          nomisCourseActivityId = NOMIS_ID + 1,
          activityScheduleId = ACTIVITY_ID + 1,
          activityScheduleId2 = ACTIVITY_ID_2 + 1,
          label = MIGRATION_ID,
        ),
      )
      activityMigrationRepository.save(
        ActivityMigrationMapping(
          nomisCourseActivityId = NOMIS_ID,
          activityScheduleId = ACTIVITY_ID,
          activityScheduleId2 = ACTIVITY_ID_2,
          label = MIGRATION_ID,
        ),
      )

      webTestClient.get().uri("/mapping/activities/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("nomisCourseActivityId").isEqualTo(NOMIS_ID)
        .jsonPath("activityScheduleId").isEqualTo(ACTIVITY_ID)
        .jsonPath("activityScheduleId2").isEqualTo(ACTIVITY_ID_2)
        .jsonPath("label").isEqualTo(MIGRATION_ID)
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.get().uri("/mapping/activities/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("No migrated mapping found")
        }
    }
  }
}
