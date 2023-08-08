@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalCoroutinesApi::class)

package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.ActivityMigrationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders.ActivityMigrationRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
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
    activityId2: Long = ACTIVITY_ID_2,
    label: String = MIGRATION_ID,
  ) {
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
      .expectStatus().isCreated
  }

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
    fun `create mapping`() = runTest {
      postCreateMappingRequest()

      val saved = activityMigrationRepository.findById(NOMIS_ID)!!
      with(saved) {
        assertThat(activityScheduleId).isEqualTo(ACTIVITY_ID)
        assertThat(activityScheduleId2).isEqualTo(ACTIVITY_ID_2)
        assertThat(label).isEqualTo(MIGRATION_ID)
      }
    }
  }
}
