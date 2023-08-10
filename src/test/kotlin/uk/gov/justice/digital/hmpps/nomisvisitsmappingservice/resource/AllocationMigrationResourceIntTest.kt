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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AllocationMigrationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders.AllocationMigrationRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AllocationMigrationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.AllocationMigrationMappingRepository

class AllocationMigrationResourceIntTest : IntegrationTestBase() {

  @SpyBean(name = "allocationMigrationMappingRepository")
  private lateinit var allocationMigrationMappingRepository: AllocationMigrationMappingRepository

  @Autowired
  private lateinit var allocationMigrationRepository: AllocationMigrationRepository

  private val NOMIS_ALLOCATION_ID = 1234L
  private val ACTIVITY_ALLOCATION_ID = 4444L
  private val ACTIVITY_ID = 5555L
  private val MIGRATION_ID = "migration-1"

  @AfterEach
  fun deleteData() = runBlocking {
    allocationMigrationRepository.deleteAll()
  }

  private fun createMapping(
    nomisAllocationId: Long = NOMIS_ALLOCATION_ID,
    activityAllocationId: Long = ACTIVITY_ALLOCATION_ID,
    activityScheduleId: Long = ACTIVITY_ID,
    label: String = MIGRATION_ID,
  ): AllocationMigrationMappingDto = AllocationMigrationMappingDto(
    nomisAllocationId = nomisAllocationId,
    activityScheduleId = activityScheduleId,
    activityAllocationId = activityAllocationId,
    label = label,
  )

  private fun postCreateMappingRequest(
    nomisAlloationId: Long = NOMIS_ALLOCATION_ID,
    activityAllocationId: Long = ACTIVITY_ALLOCATION_ID,
    activityScheduleId: Long = ACTIVITY_ID,
    label: String = MIGRATION_ID,
  ) =
    webTestClient.post().uri("/mapping/allocation/migration")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        fromValue(
          createMapping(
            nomisAllocationId = nomisAlloationId,
            activityAllocationId = activityAllocationId,
            activityScheduleId = activityScheduleId,
            label = label,
          ),
        ),
      )
      .exchange()

  private fun saveMapping(offset: Int, label: String = MIGRATION_ID) =
    saveMapping(NOMIS_ALLOCATION_ID + offset, ACTIVITY_ALLOCATION_ID + offset, ACTIVITY_ID + offset, label)

  private fun saveMapping(
    nomisAllocationId: Long = NOMIS_ALLOCATION_ID,
    activityAllocationId: Long = ACTIVITY_ALLOCATION_ID,
    activityScheduleId: Long = ACTIVITY_ID,
    label: String = MIGRATION_ID,
  ) = runTest {
    allocationMigrationRepository.save(
      AllocationMigrationMapping(
        nomisAllocationId = nomisAllocationId,
        activityAllocationId = activityAllocationId,
        activityScheduleId = activityScheduleId,
        label = label,
      ),
    )
  }

  @DisplayName("POST /mapping/allocation/migration")
  @Nested
  inner class CreateMapping {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/allocation/migration")
        .body(fromValue(createMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/allocation/migration")
        .headers(setAuthorisation(roles = listOf()))
        .body(fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/allocation/migration")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should create mapping`() = runTest {
      postCreateMappingRequest()
        .expectStatus().isCreated

      val saved = allocationMigrationRepository.findById(NOMIS_ALLOCATION_ID)!!
      with(saved) {
        assertThat(activityAllocationId).isEqualTo(ACTIVITY_ALLOCATION_ID)
        assertThat(activityScheduleId).isEqualTo(ACTIVITY_ID)
        assertThat(label).isEqualTo(MIGRATION_ID)
      }
    }

    @Test
    fun `should return bad request if mapping already exists for different Allocation id`() = runTest {
      postCreateMappingRequest()
        .expectStatus().isCreated

      postCreateMappingRequest(activityAllocationId = 1)
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Nomis mapping id = $NOMIS_ALLOCATION_ID already exists")
        }
    }

    @Test
    fun `should return created if mapping already exists for same Nomis and Allocation id`() = runTest {
      postCreateMappingRequest()
        .expectStatus().isCreated

      postCreateMappingRequest()
        .expectStatus().isCreated
    }

    @Test
    fun `should return bad request if mapping already exists for different Nomis id`() = runTest {
      postCreateMappingRequest()
        .expectStatus().isCreated

      postCreateMappingRequest(nomisAlloationId = 1)
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Allocation migration mapping with allocation id=$ACTIVITY_ALLOCATION_ID already exists")
        }
    }

    @Test
    fun `should return conflict if attempting to create duplicate`() = runTest {
      postCreateMappingRequest(activityScheduleId = 101, activityAllocationId = 102)
        .expectStatus().isCreated

      // Emulate calling service simultaneously by disabling the duplicate check
      // Note: the spy is automatically reset by ResetMocksTestExecutionListener
      whenever(allocationMigrationMappingRepository.findById(NOMIS_ALLOCATION_ID)).thenReturn(null)

      postCreateMappingRequest(activityScheduleId = 103, activityAllocationId = 104)
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Conflict: Allocation migration mapping already exists, detected by org.springframework.dao.DuplicateKeyException")
        }
    }
  }
}
