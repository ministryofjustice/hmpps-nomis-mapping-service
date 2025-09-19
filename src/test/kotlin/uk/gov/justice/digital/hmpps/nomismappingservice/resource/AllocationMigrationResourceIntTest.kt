@file:OptIn(ExperimentalCoroutinesApi::class)

package uk.gov.justice.digital.hmpps.nomismappingservice.resource

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.nomismappingservice.data.AllocationMigrationMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.builders.AllocationMigrationRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.AllocationMigrationMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository.AllocationMigrationMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.service.AllocationMigrationService

class AllocationMigrationResourceIntTest : IntegrationTestBase() {

  @Autowired
  @Qualifier("allocationMigrationMappingRepository")
  private lateinit var realAllocationMigrationMappingRepository: AllocationMigrationMappingRepository
  private lateinit var allocationMigrationMappingRepository: AllocationMigrationMappingRepository

  @Autowired
  private lateinit var allocationMigrationService: AllocationMigrationService

  @Autowired
  private lateinit var allocationMigrationRepository: AllocationMigrationRepository

  private companion object {
    private const val NOMIS_ALLOCATION_ID = 1234L
    private const val ACTIVITY_ALLOCATION_ID = 4444L
    private const val ACTIVITY_ID = 5555L
    private const val MIGRATION_ID = "migration-1"
  }

  @BeforeEach
  fun setup() {
    allocationMigrationMappingRepository = mock(defaultAnswer = AdditionalAnswers.delegatesTo(realAllocationMigrationMappingRepository))
    ReflectionTestUtils.setField(allocationMigrationService, "allocationMigrationMappingRepository", allocationMigrationMappingRepository)
  }

  @AfterEach
  fun deleteData() = runBlocking {
    allocationMigrationRepository.deleteAll()
  }

  private fun createMapping(
    nomisAllocationId: Long = NOMIS_ALLOCATION_ID,
    activityAllocationId: Long = ACTIVITY_ALLOCATION_ID,
    activityId: Long = ACTIVITY_ID,
    label: String = MIGRATION_ID,
  ): AllocationMigrationMappingDto = AllocationMigrationMappingDto(
    nomisAllocationId = nomisAllocationId,
    activityId = activityId,
    activityAllocationId = activityAllocationId,
    label = label,
  )

  private fun postCreateMappingRequest(
    nomisAllocationId: Long = NOMIS_ALLOCATION_ID,
    activityAllocationId: Long = ACTIVITY_ALLOCATION_ID,
    activityId: Long = ACTIVITY_ID,
    label: String = MIGRATION_ID,
  ) = webTestClient.post().uri("/mapping/allocations/migration")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
    .contentType(MediaType.APPLICATION_JSON)
    .body(
      fromValue(
        createMapping(
          nomisAllocationId = nomisAllocationId,
          activityAllocationId = activityAllocationId,
          activityId = activityId,
          label = label,
        ),
      ),
    )
    .exchange()

  private fun saveMapping(offset: Int, label: String = MIGRATION_ID) = saveMapping(NOMIS_ALLOCATION_ID + offset, ACTIVITY_ALLOCATION_ID + offset, ACTIVITY_ID + offset, label)

  private fun saveMapping(
    nomisAllocationId: Long = NOMIS_ALLOCATION_ID,
    activityAllocationId: Long = ACTIVITY_ALLOCATION_ID,
    activityId: Long = ACTIVITY_ID,
    label: String = MIGRATION_ID,
  ) = runTest {
    allocationMigrationRepository.save(
      AllocationMigrationMapping(
        nomisAllocationId = nomisAllocationId,
        activityAllocationId = activityAllocationId,
        activityId = activityId,
        label = label,
      ),
    )
  }

  @DisplayName("POST /mapping/allocation/migration")
  @Nested
  inner class CreateMapping {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/allocations/migration")
        .body(fromValue(createMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/allocations/migration")
        .headers(setAuthorisation(roles = listOf()))
        .body(fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/allocations/migration")
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
        assertThat(activityId).isEqualTo(ACTIVITY_ID)
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

      postCreateMappingRequest(nomisAllocationId = 1)
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Allocation migration mapping with allocation id=$ACTIVITY_ALLOCATION_ID already exists")
        }
    }

    @Test
    fun `should return conflict if attempting to create duplicate`() = runTest {
      postCreateMappingRequest(activityId = 101, activityAllocationId = 102)
        .expectStatus().isCreated

      // Emulate calling service simultaneously by disabling the duplicate check
      // Note: the spy is automatically reset by ResetMocksTestExecutionListener
      whenever(allocationMigrationMappingRepository.findById(NOMIS_ALLOCATION_ID)).thenReturn(null)

      postCreateMappingRequest(activityId = 103, activityAllocationId = 104)
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Conflict: Allocation migration mapping already exists, detected by org.springframework.dao.DuplicateKeyException")
        }
    }
  }

  @DisplayName("GET /mapping/allocations/migration/nomis-allocation-id/{nomisAllocationId}")
  @Nested
  inner class GetMapping {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/allocations/migration/nomis-allocation-id/$NOMIS_ALLOCATION_ID")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/allocations/migration/nomis-allocation-id/$NOMIS_ALLOCATION_ID")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/allocations/migration/nomis-allocation-id/$NOMIS_ALLOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return OK if mapping exists`() = runTest {
      saveMapping()

      webTestClient.get().uri("/mapping/allocations/migration/nomis-allocation-id/$NOMIS_ALLOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("nomisAllocationId").isEqualTo(NOMIS_ALLOCATION_ID)
        .jsonPath("activityAllocationId").isEqualTo(ACTIVITY_ALLOCATION_ID)
        .jsonPath("activityId").isEqualTo(ACTIVITY_ID)
        .jsonPath("label").isEqualTo(MIGRATION_ID)
    }

    @Test
    fun `should return not found `() = runTest {
      webTestClient.get().uri("/mapping/allocations/migration/nomis-allocation-id/$NOMIS_ALLOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("nomisAllocationId=$NOMIS_ALLOCATION_ID")
        }
    }
  }

  @DisplayName("GET /mapping/allocations/migrated/latest")
  @Nested
  inner class GetLatestMigratedMapping {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/allocations/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/allocations/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/allocations/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should get latest migrated mapping`() = runTest {
      // Note that this relies on the whenCreated value defaulted by the database
      saveMapping(offset = 1)
      saveMapping()

      webTestClient.get().uri("/mapping/allocations/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("nomisAllocationId").isEqualTo(NOMIS_ALLOCATION_ID)
        .jsonPath("activityAllocationId").isEqualTo(ACTIVITY_ALLOCATION_ID)
        .jsonPath("activityId").isEqualTo(ACTIVITY_ID)
        .jsonPath("label").isEqualTo(MIGRATION_ID)
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.get().uri("/mapping/allocations/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("No migrated mapping found")
        }
    }
  }

  @DisplayName("GET /mapping/allocations/migration/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationId {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/allocations/migration/migration-id/2022-01-01T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/allocations/migration/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/allocations/migration/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return only the migration requested`() {
      saveMapping()
      saveMapping(1)
      saveMapping(2, label = "wrong-migration")

      webTestClient.get().uri("/mapping/allocations/migration/migration-id/$MIGRATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(2)
        .jsonPath("content.size()").isEqualTo(2)
        .jsonPath("content[0].nomisAllocationId").isEqualTo(NOMIS_ALLOCATION_ID)
        .jsonPath("content[0].activityAllocationId").isEqualTo(ACTIVITY_ALLOCATION_ID)
        .jsonPath("content[0].activityId").isEqualTo(ACTIVITY_ID)
        .jsonPath("content[0].whenCreated").isNotEmpty
        .jsonPath("content[1].nomisAllocationId").isEqualTo(NOMIS_ALLOCATION_ID + 1)
        .jsonPath("content[1].activityAllocationId").isEqualTo(ACTIVITY_ALLOCATION_ID + 1)
        .jsonPath("content[1].activityId").isEqualTo(ACTIVITY_ID + 1)
        .jsonPath("content[1].whenCreated").isNotEmpty
    }

    @Test
    fun `should return an empty list`() {
      saveMapping(label = "wrong-migration")

      webTestClient.get().uri("/mapping/allocations/migration/migration-id/$MIGRATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
        .jsonPath("content.size()").isEqualTo(0)
    }

    @Test
    fun `should return mappings in pages`() {
      val pageSize = 3
      (1..(pageSize + 1)).forEach { saveMapping(it) }

      webTestClient.get().uri {
        it.path("/mapping/allocations/migration/migration-id/$MIGRATION_ID")
          .queryParam("size", "$pageSize")
          .queryParam("page", "0")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("numberOfElements").isEqualTo(pageSize)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(pageSize)
        .jsonPath("content.size()").isEqualTo(pageSize)
        .jsonPath("content[0].nomisAllocationId").isEqualTo(NOMIS_ALLOCATION_ID + 1)
        .jsonPath("content[1].nomisAllocationId").isEqualTo(NOMIS_ALLOCATION_ID + 2)
        .jsonPath("content[2].nomisAllocationId").isEqualTo(NOMIS_ALLOCATION_ID + 3)

      webTestClient.get().uri {
        it.path("/mapping/allocations/migration/migration-id/$MIGRATION_ID")
          .queryParam("size", "$pageSize")
          .queryParam("page", "1")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("numberOfElements").isEqualTo(1)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(pageSize)
        .jsonPath("content.size()").isEqualTo(1)
        .jsonPath("content[0].nomisAllocationId").isEqualTo(NOMIS_ALLOCATION_ID + 4)
    }
  }
}
