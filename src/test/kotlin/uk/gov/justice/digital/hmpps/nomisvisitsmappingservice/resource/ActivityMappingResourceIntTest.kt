@file:OptIn(ExperimentalCoroutinesApi::class)

package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.ActivityMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.ActivityScheduleMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders.ScheduleRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityScheduleMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityScheduleMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.ActivityMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.ActivityMappingService
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class ActivityMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  @Qualifier("activityMappingRepository")
  private lateinit var realActivityMappingRepository: ActivityMappingRepository
  private lateinit var activityMappingRepository: ActivityMappingRepository

  @Autowired
  private lateinit var activityMappingService: ActivityMappingService

  @Autowired
  lateinit var activityRepository: ActivityRepository

  @Autowired
  lateinit var scheduleRepository: ScheduleRepository

  private val nomisCourseActivityId = 1234L
  private val activityScheduleId = 4444L
  private val activityId = 3333L
  private val nomisCourseScheduleId = 2345L
  private val activityScheduledInstanceId = 5555L

  @BeforeEach
  fun setup() {
    activityMappingRepository = mock(defaultAnswer = AdditionalAnswers.delegatesTo(realActivityMappingRepository))
    ReflectionTestUtils.setField(activityMappingService, "activityMappingRepository", activityMappingRepository)
  }

  @AfterEach
  internal fun deleteData() = runBlocking {
    activityRepository.deleteAll()
    scheduleRepository.deleteAll()
  }

  private fun createMapping(
    nomisId: Long = nomisCourseActivityId,
    activityScheduleId: Long = this.activityScheduleId,
    activityId: Long = this.activityId,
    scheduledInstanceMappings: List<Pair<Long, Long>> = listOf(Pair(activityScheduledInstanceId, nomisCourseScheduleId)),
    mappingType: String = ActivityMappingType.ACTIVITY_CREATED.name,
  ): ActivityMappingDto = ActivityMappingDto(
    nomisCourseActivityId = nomisId,
    activityScheduleId = activityScheduleId,
    activityId = activityId,
    scheduledInstanceMappings = scheduledInstanceMappings.map {
      ActivityScheduleMappingDto(it.first, it.second, mappingType)
    },
    mappingType = mappingType,
  )

  private fun postCreateMappingRequest(
    nomisId: Long = nomisCourseActivityId,
    activityScheduleId: Long = this.activityScheduleId,
    activityId: Long = this.activityId,
    scheduledInstanceMappings: List<Pair<Long, Long>> = listOf(),
    mappingType: String = ActivityMappingType.ACTIVITY_CREATED.name,
  ) {
    webTestClient.post().uri("/mapping/activities")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createMapping(
            nomisId = nomisId,
            activityScheduleId = activityScheduleId,
            activityId = activityId,
            scheduledInstanceMappings = scheduledInstanceMappings,
            mappingType = mappingType,
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  @DisplayName("POST /mapping/activities")
  @Nested
  inner class CreateMappingTest {

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
    fun `create forbidden with wrong role`() {
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
          .returnResult().responseBody?.userMessage,
      ).isEqualTo("Validation failure: Activity schedule mapping id = 4444 already exists")
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
            "activityId"            : $activityId,
            "mappingType"           : "ACTIVITY_CREATED"
          }""",
          ),
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
            "activityId"            : $activityId,
            "mappingType"           : "ACTIVITY_CREATED"
          }""",
          ),
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
          .returnResult().responseBody?.userMessage,
      ).isEqualTo("Validation failure: Activity with Nomis id=1234 already exists")
    }

    @Test
    fun `create mapping success with schedules`() = runTest {
      webTestClient.post().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisCourseActivityId" : $nomisCourseActivityId,
            "activityScheduleId"    : $activityScheduleId,
            "activityId"            : $activityId,
            "mappingType"           : "ACTIVITY_CREATED",
            "scheduledInstanceMappings" : [
              {
                "scheduledInstanceId": 1,
                "nomisCourseScheduleId": 11,
                "mappingType": "ACTIVITY_CREATED"
              },
              {
                "scheduledInstanceId": 2,
                "nomisCourseScheduleId": 22,
                "mappingType": "ACTIVITY_CREATED"
              } 
            ]
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val activityMapping = activityRepository.findOneByNomisCourseActivityId(nomisCourseActivityId)
        ?: throw NotFoundException("Activity mapping not saved $nomisCourseActivityId")

      assertThat(activityMapping.nomisCourseActivityId).isEqualTo(nomisCourseActivityId)
      assertThat(activityMapping.activityScheduleId).isEqualTo(activityScheduleId)
      assertThat(activityMapping.activityId).isEqualTo(activityId)
      assertThat(activityMapping.mappingType).isEqualTo(ActivityMappingType.ACTIVITY_CREATED)

      val scheduleMappings = scheduleRepository.findAllByActivityScheduleId(activityScheduleId).toList()
      assertThat(scheduleMappings).extracting(
        ActivityScheduleMapping::scheduledInstanceId,
        ActivityScheduleMapping::nomisCourseScheduleId,
      ).containsExactly(tuple(1L, 11L), tuple(2L, 22L))
    }

    @Test
    fun `create mapping with no schedules`() = runTest {
      webTestClient.post().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisCourseActivityId" : $nomisCourseActivityId,
            "activityScheduleId"    : $activityScheduleId,
            "activityId"            : $activityId,
            "mappingType"           : "ACTIVITY_CREATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val activityMapping = activityRepository.findOneByNomisCourseActivityId(nomisCourseActivityId)
        ?: throw NotFoundException("Activity mapping not saved $nomisCourseActivityId")

      assertThat(activityMapping.nomisCourseActivityId).isEqualTo(nomisCourseActivityId)
      assertThat(activityMapping.activityScheduleId).isEqualTo(activityScheduleId)
      assertThat(activityMapping.activityId).isEqualTo(activityId)
      assertThat(activityMapping.mappingType).isEqualTo(ActivityMappingType.ACTIVITY_CREATED)

      val scheduleMappings = scheduleRepository.findAllByActivityScheduleId(activityScheduleId).toList()
      assertThat(scheduleMappings).isEmpty()
    }

    @Test
    fun `create migration mapping`() = runTest {
      webTestClient.post().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisCourseActivityId" : $nomisCourseActivityId,
            "activityScheduleId"    : $activityScheduleId,
            "activityId"            : $activityId,
            "mappingType"           : "ACTIVITY_MIGRATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val activityMapping = activityRepository.findOneByNomisCourseActivityId(nomisCourseActivityId)
        ?: throw NotFoundException("Activity mapping not saved $nomisCourseActivityId")

      assertThat(activityMapping.nomisCourseActivityId).isEqualTo(nomisCourseActivityId)
      assertThat(activityMapping.activityScheduleId).isEqualTo(activityScheduleId)
      assertThat(activityMapping.activityId).isEqualTo(activityId)
      assertThat(activityMapping.mappingType).isEqualTo(ActivityMappingType.ACTIVITY_MIGRATED)
    }

    @Test
    fun `create mapping - Duplicate db error`() = runTest {
      webTestClient.post().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisCourseActivityId" : 101,
            "activityScheduleId"    : $activityScheduleId,
            "activityId"            : $activityId,
            "mappingType"           : "ACTIVITY_CREATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      // Emulate calling service simultaneously twice by disabling the duplicate check
      // Note: the spy is automatically reset by ResetMocksTestExecutionListener
      whenever(activityMappingRepository.findById(activityScheduleId)).thenReturn(null)

      val responseBody =
        webTestClient.post().uri("/mapping/activities")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "nomisCourseActivityId" : 102,
            "activityScheduleId"    : $activityScheduleId,
            "activityId"            : $activityId,
            "mappingType"           : "ACTIVITY_CREATED"
          }""",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<ActivityMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Activity mapping already exists, detected by org.springframework.dao.DuplicateKeyException")
        assertThat(errorCode).isEqualTo(1409)
      }
    }
  }

  @DisplayName("PUT /mapping/activities")
  @Nested
  inner class UpdateMappingTest {

    private fun createUpdateRequest(
      activityMapping: Pair<Long, Long> = activityScheduleId to nomisCourseActivityId,
      activity: Long = activityId,
      scheduleMappings: List<Pair<Long, Long>> = listOf(activityScheduledInstanceId to nomisCourseScheduleId),
    ): String {
      val mappingsJson = scheduleMappings.joinToString { mapping ->
        """
          {
            "scheduledInstanceId": ${mapping.first},
            "nomisCourseScheduleId":${mapping.second},
            "mappingType": "ACTIVITY_UPDATED"
          }
        """.trimIndent()
      }

      return """
          {
            "activityScheduleId"    : ${activityMapping.first},
            "activityId"            : $activity,
            "nomisCourseActivityId" : ${activityMapping.second},
            "mappingType"           : "ACTIVITY_UPDATED",
            "scheduledInstanceMappings" : [$mappingsJson]
          }
      """.trimIndent()
    }

    @BeforeEach
    fun setUp() = runTest {
      activityRepository.save(ActivityMapping(activityScheduleId, activityId, nomisCourseActivityId, ActivityMappingType.ACTIVITY_CREATED))
      scheduleRepository.save(ActivityScheduleMapping(activityScheduledInstanceId, nomisCourseScheduleId, ActivityScheduleMappingType.ACTIVITY_CREATED, activityScheduleId))
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/mapping/activities")
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createUpdateRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf()))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createUpdateRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create visit forbidden with wrong role`() {
      webTestClient.put().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createUpdateRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `not found if activity mapping does not exist`() {
      webTestClient.putScheduleMappings(createUpdateRequest(activityMapping = 999L to nomisCourseActivityId))
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Activity schedule id=999")
        }
    }

    @Test
    fun `OK if there is no change to the mappings`() = runTest {
      webTestClient.putScheduleMappings()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("activityScheduleId").isEqualTo(activityScheduleId)
        .jsonPath("activityId").isEqualTo(activityId)
        .jsonPath("nomisCourseActivityId").isEqualTo(nomisCourseActivityId)
        .jsonPath("mappingType").isEqualTo("ACTIVITY_CREATED")
        .jsonPath("scheduledInstanceMappings[0].scheduledInstanceId").isEqualTo(activityScheduledInstanceId)
        .jsonPath("scheduledInstanceMappings[0].nomisCourseScheduleId").isEqualTo(nomisCourseScheduleId)
        .jsonPath("scheduledInstanceMappings[0].mappingType").isEqualTo("ACTIVITY_CREATED")

      val saved = scheduleRepository.findAllByActivityScheduleId(activityScheduleId).toList()
      assertThat(saved)
        .extracting(ActivityScheduleMapping::scheduledInstanceId, ActivityScheduleMapping::nomisCourseScheduleId)
        .containsExactlyInAnyOrder(tuple(activityScheduledInstanceId, nomisCourseScheduleId))
    }

    @Test
    fun `OK when mappings added`() = runTest {
      val request = createUpdateRequest(scheduleMappings = listOf(activityScheduledInstanceId to nomisCourseScheduleId, 111L to 222L))
      webTestClient.putScheduleMappings(request)
        .expectStatus().isOk
        .expectBody()
        .jsonPath("scheduledInstanceMappings[0].scheduledInstanceId").isEqualTo(activityScheduledInstanceId)
        .jsonPath("scheduledInstanceMappings[0].nomisCourseScheduleId").isEqualTo(nomisCourseScheduleId)
        .jsonPath("scheduledInstanceMappings[0].mappingType").isEqualTo("ACTIVITY_CREATED")
        .jsonPath("scheduledInstanceMappings[1].scheduledInstanceId").isEqualTo("111")
        .jsonPath("scheduledInstanceMappings[1].nomisCourseScheduleId").isEqualTo("222")
        .jsonPath("scheduledInstanceMappings[1].mappingType").isEqualTo("ACTIVITY_UPDATED")

      val saved = scheduleRepository.findAllByActivityScheduleId(activityScheduleId).toList()
      assertThat(saved)
        .extracting(ActivityScheduleMapping::scheduledInstanceId, ActivityScheduleMapping::nomisCourseScheduleId)
        .containsExactlyInAnyOrder(tuple(activityScheduledInstanceId, nomisCourseScheduleId), tuple(111L, 222L))
    }

    @Test
    fun `OK when mappings deleted`() = runTest {
      webTestClient.putScheduleMappings(createUpdateRequest(scheduleMappings = listOf()))
        .expectStatus().isOk
        .expectBody()
        .jsonPath("scheduledInstanceMappings").isEmpty

      val saved = scheduleRepository.findAllByActivityScheduleId(activityScheduleId).toList()
      assertThat(saved).isEmpty()
    }

    @Test
    fun `OK when mappings updated`() = runTest {
      val request = createUpdateRequest(scheduleMappings = listOf(activityScheduledInstanceId to 222L))
      webTestClient.putScheduleMappings(request)
        .expectStatus().isOk
        .expectBody()
        .jsonPath("scheduledInstanceMappings[0].scheduledInstanceId").isEqualTo(activityScheduledInstanceId)
        .jsonPath("scheduledInstanceMappings[0].nomisCourseScheduleId").isEqualTo("222")

      val saved = scheduleRepository.findAllByActivityScheduleId(activityScheduleId).toList()
      assertThat(saved)
        .extracting(ActivityScheduleMapping::scheduledInstanceId, ActivityScheduleMapping::nomisCourseScheduleId)
        .containsExactlyInAnyOrder(tuple(activityScheduledInstanceId, 222L))
    }

    @Test
    fun `OK when mappings created, deleted and updated`() = runTest {
      scheduleRepository.save(ActivityScheduleMapping(111L, 222L, ActivityScheduleMappingType.ACTIVITY_CREATED, activityScheduleId))
      scheduleRepository.save(ActivityScheduleMapping(333L, 444L, ActivityScheduleMappingType.ACTIVITY_CREATED, activityScheduleId))

      val request = createUpdateRequest(
        scheduleMappings = listOf(
          // keep
          activityScheduledInstanceId to nomisCourseScheduleId,
          // update
          111L to 223L,
          // create
          555L to 666L,
        ),
      ) // missing 333L to 444L - delete

      webTestClient.putScheduleMappings(request)
        .expectStatus().isOk
        .expectBody()
        .jsonPath("scheduledInstanceMappings[0].scheduledInstanceId").isEqualTo(activityScheduledInstanceId)
        .jsonPath("scheduledInstanceMappings[0].nomisCourseScheduleId").isEqualTo(nomisCourseScheduleId)
        .jsonPath("scheduledInstanceMappings[1].scheduledInstanceId").isEqualTo("111")
        .jsonPath("scheduledInstanceMappings[1].nomisCourseScheduleId").isEqualTo("223")
        .jsonPath("scheduledInstanceMappings[2].scheduledInstanceId").isEqualTo("555")
        .jsonPath("scheduledInstanceMappings[2].nomisCourseScheduleId").isEqualTo("666")
        .jsonPath("scheduledInstanceMappings[3].scheduledInstanceId").doesNotExist()

      val saved = scheduleRepository.findAllByActivityScheduleId(activityScheduleId).toList()
      assertThat(saved)
        .extracting(ActivityScheduleMapping::scheduledInstanceId, ActivityScheduleMapping::nomisCourseScheduleId)
        .containsExactlyInAnyOrder(
          tuple(activityScheduledInstanceId, nomisCourseScheduleId),
          tuple(111L, 223L),
          tuple(555L, 666L),
        )
    }

    private fun WebTestClient.putScheduleMappings(request: String = createUpdateRequest()) =
      put().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(request))
        .exchange()
  }

  @DisplayName("GET /mapping/activities/activity-schedule/{activityScheduleId}")
  @Nested
  inner class GetMappingTest {

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
    fun `get mapping success no schedules`() = runTest {
      activityRepository.save(ActivityMapping(activityScheduleId, activityId, 2, ActivityMappingType.ACTIVITY_CREATED))

      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("activityScheduleId").isEqualTo(activityScheduleId)
        .jsonPath("activityId").isEqualTo(activityId)
        .jsonPath("nomisCourseActivityId").isEqualTo(2)
        .jsonPath("mappingType").isEqualTo("ACTIVITY_CREATED")
        .jsonPath("scheduledInstanceMappings").isEmpty
    }

    @Test
    fun `get mapping success with schedules`() = runTest {
      activityRepository.save(ActivityMapping(activityScheduleId, activityId, nomisCourseActivityId, ActivityMappingType.ACTIVITY_CREATED))
      scheduleRepository.save(ActivityScheduleMapping(11, 12, ActivityScheduleMappingType.ACTIVITY_CREATED, activityScheduleId))
      scheduleRepository.save(ActivityScheduleMapping(21, 22, ActivityScheduleMappingType.ACTIVITY_CREATED, activityScheduleId))

      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("activityScheduleId").isEqualTo(activityScheduleId)
        .jsonPath("activityId").isEqualTo(activityId)
        .jsonPath("nomisCourseActivityId").isEqualTo(nomisCourseActivityId)
        .jsonPath("mappingType").isEqualTo("ACTIVITY_CREATED")
        .jsonPath("scheduledInstanceMappings[0].scheduledInstanceId").isEqualTo(11)
        .jsonPath("scheduledInstanceMappings[0].nomisCourseScheduleId").isEqualTo(12)
        .jsonPath("scheduledInstanceMappings[0].mappingType").isEqualTo("ACTIVITY_CREATED")
        .jsonPath("scheduledInstanceMappings[1].scheduledInstanceId").isEqualTo(21)
        .jsonPath("scheduledInstanceMappings[1].nomisCourseScheduleId").isEqualTo(22)
        .jsonPath("scheduledInstanceMappings[1].mappingType").isEqualTo("ACTIVITY_CREATED")
    }

    @Test
    fun `get migration mapping`() = runTest {
      activityRepository.save(
        ActivityMapping(
          activityScheduleId = activityScheduleId,
          activityId = activityId,
          nomisCourseActivityId = nomisCourseActivityId,
          mappingType = ActivityMappingType.ACTIVITY_MIGRATED,
        ),
      )

      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("activityScheduleId").isEqualTo(activityScheduleId)
        .jsonPath("activityId").isEqualTo(activityId)
        .jsonPath("nomisCourseActivityId").isEqualTo(nomisCourseActivityId)
        .jsonPath("mappingType").isEqualTo("ACTIVITY_MIGRATED")
        .jsonPath("scheduledInstanceMappings").isEmpty
    }
  }

  @DisplayName("GET /mapping/activities/activity-schedule-id/{activityScheduleId}/scheduled-instance-id/{scheduledInstanceId}")
  @Nested
  inner class GetScheduleMappingTest {

    @BeforeEach
    fun setUp() = runTest {
      activityRepository.save(ActivityMapping(activityScheduleId, activityId, nomisCourseActivityId, ActivityMappingType.ACTIVITY_CREATED))
      scheduleRepository.save(ActivityScheduleMapping(activityScheduledInstanceId, nomisCourseScheduleId, ActivityScheduleMappingType.ACTIVITY_CREATED, activityScheduleId))
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId/scheduled-instance-id/$activityScheduledInstanceId")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId/scheduled-instance-id/$activityScheduledInstanceId")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId/scheduled-instance-id/$activityScheduledInstanceId")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `mapping not found for activity id`() {
      val error = webTestClient.get().uri("/mapping/activities/activity-schedule-id/765/scheduled-instance-id/$activityScheduledInstanceId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: Activity schedule id=765, Scheduled instance id=$activityScheduledInstanceId")
    }

    @Test
    fun `mapping not found for scheduled instance id`() {
      val error = webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId/scheduled-instance-id/432")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: Activity schedule id=$activityScheduleId, Scheduled instance id=432")
    }

    @Test
    fun `mapping found`() {
      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId/scheduled-instance-id/$activityScheduledInstanceId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("scheduledInstanceId").isEqualTo(activityScheduledInstanceId)
        .jsonPath("nomisCourseScheduleId").isEqualTo(nomisCourseScheduleId)
        .jsonPath("mappingType").isEqualTo("ACTIVITY_CREATED")
    }
  }

  @DisplayName("GET /mapping/activities/schedules/scheduled-instance-id/{scheduledInstanceId}")
  @Nested
  inner class GetScheduleByScheduleIdMappingTest {

    @BeforeEach
    fun setUp() = runTest {
      activityRepository.save(ActivityMapping(activityScheduleId, activityId, nomisCourseActivityId, ActivityMappingType.ACTIVITY_CREATED))
      scheduleRepository.save(ActivityScheduleMapping(activityScheduledInstanceId, nomisCourseScheduleId, ActivityScheduleMappingType.ACTIVITY_CREATED, activityScheduleId))
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/activities/schedules/scheduled-instance-id/$activityScheduledInstanceId")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/activities/schedules/scheduled-instance-id/$activityScheduledInstanceId")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/activities/schedules/scheduled-instance-id/$activityScheduledInstanceId")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `mapping not found for scheduled instance id`() {
      val error = webTestClient.get().uri("/mapping/activities/schedules/scheduled-instance-id/432")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: Scheduled instance id=432")
    }

    @Test
    fun `mapping found`() {
      webTestClient.get().uri("/mapping/activities/schedules/scheduled-instance-id/$activityScheduledInstanceId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("scheduledInstanceId").isEqualTo(activityScheduledInstanceId)
        .jsonPath("nomisCourseScheduleId").isEqualTo(nomisCourseScheduleId)
        .jsonPath("mappingType").isEqualTo("ACTIVITY_CREATED")
    }
  }

  @DisplayName("GET /mapping/activities")
  @Nested
  inner class GetAllMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/activities")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      postCreateMappingRequest(101, 201, 301)
      postCreateMappingRequest(102, 202, 302)

      val mapping = webTestClient.get().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody<List<ActivityMappingDto>>()
        .returnResult().responseBody!!

      assertThat(mapping[0].nomisCourseActivityId).isEqualTo(101)
      assertThat(mapping[0].activityScheduleId).isEqualTo(201)
      assertThat(mapping[0].activityId).isEqualTo(301)
      assertThat(mapping[1].nomisCourseActivityId).isEqualTo(102)
      assertThat(mapping[1].activityScheduleId).isEqualTo(202)
      assertThat(mapping[1].activityId).isEqualTo(302)
      assertThat(mapping).hasSize(2)
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
    fun `delete specific mapping success with schedules`() = runTest {
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

      // and present on the database
      val activityMapping = activityRepository.findOneByNomisCourseActivityId(nomisCourseActivityId)
      assertThat(activityMapping?.activityScheduleId).isEqualTo(activityScheduleId)
      assertThat(activityMapping?.activityId).isEqualTo(activityId)
      assertThat(activityMapping?.nomisCourseActivityId).isEqualTo(nomisCourseActivityId)
      val scheduleMappings = scheduleRepository.findAllByActivityScheduleId(activityScheduleId)
      assertThat(scheduleMappings).extracting(ActivityScheduleMapping::scheduledInstanceId, ActivityScheduleMapping::nomisCourseScheduleId)
        .containsExactlyInAnyOrder(tuple(activityScheduledInstanceId, nomisCourseScheduleId))

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

      // and not present on the database
      assertThat(activityRepository.findOneByNomisCourseActivityId(nomisCourseActivityId)).isNull()
      assertThat(scheduleRepository.findAllByActivityScheduleId(activityScheduleId)).isEmpty()
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

  @DisplayName("DELETE /mapping/schedules/max-nomis-schedule-id/{maxCourseScheduleId}")
  @Nested
  inner class DeleteMaxScheduleIdTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/schedules/max-nomis-schedule-id/999")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/schedules/max-nomis-schedule-id/999")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/schedules/max-nomis-schedule-id/999")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete specific mapping success with schedules`() = runTest {
      // create mapping
      webTestClient.post().uri("/mapping/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(createMapping(scheduledInstanceMappings = listOf(Pair(1, 1), Pair(2, 2)))),
        )
        .exchange()
        .expectStatus().isCreated

      // Schedules exist
      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId/scheduled-instance-id/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId/scheduled-instance-id/2")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk

      // and present on the database
      scheduleRepository.findAllByActivityScheduleId(activityScheduleId).also {
        assertThat(it).extracting(ActivityScheduleMapping::nomisCourseScheduleId)
          .containsExactlyInAnyOrder(tuple(1L), tuple(2L))
      }

      // delete everything after course schedule id 1
      webTestClient.delete().uri("/mapping/schedules/max-nomis-schedule-id/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNoContent

      // Only the first schedule exists
      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId/scheduled-instance-id/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
      webTestClient.get().uri("/mapping/activities/activity-schedule-id/$activityScheduleId/scheduled-instance-id/2")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNotFound

      // and present on the database
      scheduleRepository.findAllByActivityScheduleId(activityScheduleId).also {
        assertThat(it).extracting(ActivityScheduleMapping::nomisCourseScheduleId)
          .containsExactlyInAnyOrder(tuple(1L))
      }
    }
  }
}
