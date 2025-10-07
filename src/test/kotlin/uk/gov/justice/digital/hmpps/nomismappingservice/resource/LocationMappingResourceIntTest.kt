package uk.gov.justice.digital.hmpps.nomismappingservice.resource

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.byLessThan
import org.hamcrest.Matchers
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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.data.LocationMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.builders.LocationRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.LocationMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository.LocationMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.service.LocationMappingService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val DPS_LOCATION_ID = "abcd1234-cdef-5678-90ab-ef1234567890"
private const val NOMIS_LOCATION_ID = 5678L

class LocationMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  @Qualifier("locationMappingRepository")
  private lateinit var realLocationMappingRepository: LocationMappingRepository
  private lateinit var locationMappingRepository: LocationMappingRepository

  @Autowired
  private lateinit var locationRepository: LocationRepository

  @Autowired
  private lateinit var locationMappingService: LocationMappingService

  @BeforeEach
  fun setup() {
    locationMappingRepository =
      mock(defaultAnswer = AdditionalAnswers.delegatesTo(realLocationMappingRepository))
    ReflectionTestUtils.setField(
      locationMappingService,
      "locationMappingRepository",
      locationMappingRepository,
    )
  }

  private fun createLocationMapping(
    dpsLocationId: String = DPS_LOCATION_ID,
    nomisLocationId: Long = NOMIS_LOCATION_ID,
    label: String = "2022-01-01",
    mappingType: String = LocationMappingType.NOMIS_CREATED.name,
  ): LocationMappingDto = LocationMappingDto(
    dpsLocationId = dpsLocationId,
    nomisLocationId = nomisLocationId,
    label = label,
    mappingType = mappingType,
  )

  private fun postCreateLocationMappingRequest(
    dpsLocationId: String = DPS_LOCATION_ID,
    nomisLocationId: Long = NOMIS_LOCATION_ID,
    label: String = "2022-01-01",
    mappingType: String = LocationMappingType.NOMIS_CREATED.name,
  ) {
    webTestClient.post().uri("/mapping/locations")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createLocationMapping(
            dpsLocationId = dpsLocationId,
            nomisLocationId = nomisLocationId,
            label = label,
            mappingType = mappingType,
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  @DisplayName("POST /mapping/locations")
  @Nested
  inner class CreateLocationMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      locationRepository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/locations")
        .body(BodyInserters.fromValue(createLocationMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createLocationMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createLocationMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    internal fun `create mapping succeeds when the same mapping already exists for the same location`() {
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "dpsLocationId"     : "$DPS_LOCATION_ID",
            "nomisLocationId"   : $NOMIS_LOCATION_ID,
            "label"             : "2022-01-01",
            "mappingType"       : "NOMIS_CREATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "dpsLocationId"     : "$DPS_LOCATION_ID",
            "nomisLocationId"   : $NOMIS_LOCATION_ID,
            "label"             : "2022-01-01",
            "mappingType"       : "NOMIS_CREATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    fun `create when mapping for nomis ids already exists`() {
      postCreateLocationMappingRequest()

      val responseBody =
        webTestClient.post().uri("/mapping/locations")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createLocationMapping().copy(dpsLocationId = "other-dps-location-id")))
          .exchange()
          .expectStatus().isEqualTo(HttpStatus.CONFLICT.value())
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<LocationMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Location mapping already exists.\nExisting mapping: LocationMappingDto(dpsLocationId=$DPS_LOCATION_ID, nomisLocationId=$NOMIS_LOCATION_ID, label=2022-01-01, mappingType=NOMIS_CREATED")
        assertThat(userMessage).contains("Duplicate mapping: LocationMappingDto(dpsLocationId=other-dps-location-id, nomisLocationId=$NOMIS_LOCATION_ID, label=2022-01-01, mappingType=NOMIS_CREATED, whenCreated=null")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existingLocation = responseBody.moreInfo.existing
      with(existingLocation) {
        assertThat(dpsLocationId).isEqualTo(DPS_LOCATION_ID)
        assertThat(nomisLocationId).isEqualTo(NOMIS_LOCATION_ID)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }

      val duplicateLocation = responseBody.moreInfo.duplicate
      with(duplicateLocation) {
        assertThat(dpsLocationId).isEqualTo("other-dps-location-id")
        assertThat(nomisLocationId).isEqualTo(NOMIS_LOCATION_ID)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }
    }

    @Test
    fun `create when mapping for location id already exists for another location`() {
      postCreateLocationMappingRequest()

      val responseBody =
        webTestClient.post().uri("/mapping/locations")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createLocationMapping().copy(nomisLocationId = 9999)))
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<LocationMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Location mapping already exists.\nExisting mapping: LocationMappingDto(dpsLocationId=$DPS_LOCATION_ID, nomisLocationId=$NOMIS_LOCATION_ID, label=2022-01-01, mappingType=NOMIS_CREATED")
        assertThat(userMessage).contains("Duplicate mapping: LocationMappingDto(dpsLocationId=$DPS_LOCATION_ID, nomisLocationId=9999, label=2022-01-01, mappingType=NOMIS_CREATED, whenCreated=null")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existingLocation = responseBody.moreInfo.existing
      with(existingLocation) {
        assertThat(dpsLocationId).isEqualTo(DPS_LOCATION_ID)
        assertThat(nomisLocationId).isEqualTo(NOMIS_LOCATION_ID)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }

      val duplicateLocation = responseBody.moreInfo.duplicate
      with(duplicateLocation) {
        assertThat(dpsLocationId).isEqualTo(DPS_LOCATION_ID)
        assertThat(nomisLocationId).isEqualTo(9999)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }
    }

    @Test
    fun `create mapping success`() {
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                "dpsLocationId"   : "$DPS_LOCATION_ID",
                "nomisLocationId" : $NOMIS_LOCATION_ID,
                "label"           : "2024-01-01",
                "mappingType"     : "NOMIS_CREATED"
              }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.get()
        .uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody(LocationMappingDto::class.java)
        .returnResult().responseBody!!
        .apply {
          assertThat(dpsLocationId).isEqualTo(DPS_LOCATION_ID)
          assertThat(nomisLocationId).isEqualTo(NOMIS_LOCATION_ID)
          assertThat(label).isEqualTo("2024-01-01")
          assertThat(mappingType).isEqualTo(LocationMappingType.NOMIS_CREATED.name)
        }

      webTestClient.get().uri("/mapping/locations/dps/$DPS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody(LocationMappingDto::class.java)
        .returnResult().responseBody!!
        .apply {
          assertThat(dpsLocationId).isEqualTo(DPS_LOCATION_ID)
          assertThat(nomisLocationId).isEqualTo(NOMIS_LOCATION_ID)
          assertThat(label).isEqualTo("2024-01-01")
          assertThat(mappingType).isEqualTo("NOMIS_CREATED")
        }
    }

    @Test
    fun `create mapping - Duplicate db error`() = runTest {
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                "dpsLocationId"     : "$DPS_LOCATION_ID",
                "nomisLocationId"   : $NOMIS_LOCATION_ID,
                "label"             : "2022-01-01",
                "mappingType"       : "NOMIS_CREATED"
              }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      // Emulate calling service simultaneously twice by disabling the duplicate check
      // Note: the spy is automatically reset by ResetMocksTestExecutionListener
      whenever(locationMappingRepository.findById(DPS_LOCATION_ID)).thenReturn(null)

      val responseBody =
        webTestClient.post().uri("/mapping/locations")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
                "dpsLocationId"     : "$DPS_LOCATION_ID",
                "nomisLocationId"   : ${NOMIS_LOCATION_ID + 1},
                "label"             : "2022-01-01",
                "mappingType"       : "NOMIS_CREATED"
              }""",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(
            object :
              ParameterizedTypeReference<DuplicateMappingErrorResponse<LocationMappingDto>>() {},
          )
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage)
          .contains("Conflict: Location mapping already exists, detected by org.springframework.dao.DuplicateKeyException")
        assertThat(errorCode).isEqualTo(1409)
      }
    }
  }

  @DisplayName("GET /mapping/locations/nomis/{nomisLocationId}/second-offender-no/{secondOffenderNo}/type-sequence/{typeSequence}")
  @Nested
  inner class GetNomisMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      locationRepository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get()
        .uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get()
        .uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get()
        .uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createLocationMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping =
        webTestClient.get().uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody(LocationMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping.dpsLocationId).isEqualTo(DPS_LOCATION_ID)
      assertThat(mapping.nomisLocationId).isEqualTo(NOMIS_LOCATION_ID)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(LocationMappingType.NOMIS_CREATED.name)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get()
        .uri("/mapping/locations/nomis/999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: Location with nomisLocationId=999 not found")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createLocationMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /mapping/locations/dps/{dpsLocationId}")
  @Nested
  inner class GetLocationMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      locationRepository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/locations/dps/$DPS_LOCATION_ID")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/locations/dps/$DPS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/locations/dps/$DPS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createLocationMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/locations/dps/$DPS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody(LocationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.dpsLocationId).isEqualTo(DPS_LOCATION_ID)
      assertThat(mapping.nomisLocationId).isEqualTo(NOMIS_LOCATION_ID)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(LocationMappingType.NOMIS_CREATED.name)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/locations/dps/765")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: Location with dpsLocationId=765 not found")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createLocationMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/locations/dps/$DPS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /mapping/locations/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationIdTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      locationRepository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/migration-id/2022-01-01T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/locations/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get location mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/locations/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get location mappings by migration id success`() {
      (1L..4L).forEach {
        postCreateLocationMappingRequest(
          dpsLocationId = it.toString(),
          nomisLocationId = it,
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      (5L..9L).forEach {
        postCreateLocationMappingRequest(
          dpsLocationId = it.toString(),
          nomisLocationId = it,
          label = "2099-01-01",
          mappingType = "MIGRATED",
        )
      }
      postCreateLocationMappingRequest(
        dpsLocationId = "12",
        nomisLocationId = 12,
        mappingType = LocationMappingType.LOCATION_CREATED.name,
      )

      webTestClient.get().uri("/mapping/locations/migration-id/2022-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..dpsLocationId").value(Matchers.contains("1", "2", "3", "4"))
        .jsonPath("$.content..nomisLocationId").value(Matchers.contains(1, 2, 3, 4))
        .jsonPath("$.content[0].whenCreated").isNotEmpty
    }

    @Test
    fun `get location mappings by migration dpsLocationId - no records exist`() {
      (1L..4L).forEach {
        postCreateLocationMappingRequest(
          dpsLocationId = it.toString(),
          nomisLocationId = it,
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }

      webTestClient.get().uri("/mapping/locations/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
        .jsonPath("content").isEmpty
    }

    @Test
    fun `can request a different page size`() {
      (1L..6L).forEach {
        postCreateLocationMappingRequest(
          dpsLocationId = it.toString(),
          nomisLocationId = it,
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/locations/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("sort", "nomisLocationId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
        postCreateLocationMappingRequest(
          dpsLocationId = it.toString(),
          nomisLocationId = it,
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/locations/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .queryParam("sort", "nomisLocationId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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

  @DisplayName("GET /mapping/locations/migrated/latest")
  @Nested
  inner class GeMappingMigratedLatestTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      locationRepository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/locations/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/locations/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/locations/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createLocationMapping(
              dpsLocationId = "10",
              nomisLocationId = 2,
              label = "2022-01-01T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createLocationMapping(
              dpsLocationId = "20",
              nomisLocationId = 4,
              label = "2022-01-02T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createLocationMapping(
              dpsLocationId = "1",
              nomisLocationId = 1,
              label = "2022-01-02T10:00:00",
              mappingType = LocationMappingType.MIGRATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createLocationMapping(
              dpsLocationId = "99",
              nomisLocationId = 3,
              label = "whatever",
              mappingType = LocationMappingType.NOMIS_CREATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/locations/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody(LocationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.dpsLocationId).isEqualTo("1")
      assertThat(mapping.nomisLocationId).isEqualTo(1)
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo("MIGRATED")
      assertThat(mapping.whenCreated)
        .isCloseTo(LocalDateTime.now(), byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createLocationMapping(
              dpsLocationId = "77",
              nomisLocationId = 7,
              label = "whatever",
              mappingType = LocationMappingType.NOMIS_CREATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/locations/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }

  @DisplayName("DELETE /mapping/locations/dps/{dpsLocationId}")
  @Nested
  inner class DeleteMappingByDpsIdTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/locations/dps/999")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/locations/dps/999")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/locations/dps/999")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete specific mapping success`() {
      // create mapping
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createLocationMapping()))
        .exchange()
        .expectStatus().isCreated

      // it is present after creation by location id
      webTestClient.get().uri("/mapping/locations/dps/$DPS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
      // it is also present after creation by nomis id
      webTestClient.get()
        .uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk

      // delete mapping
      webTestClient.delete().uri("/mapping/locations/dps/$DPS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNoContent

      // no longer present by location id
      webTestClient.get().uri("/mapping/locations/dps/$DPS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
      // and also no longer present by nomis id
      webTestClient.get()
        .uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    internal fun `delete is idempotent`() {
      // create mapping
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createLocationMapping()))
        .exchange()
        .expectStatus().isCreated

      // delete mapping
      webTestClient.delete().uri("/mapping/locations/dps/$DPS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNoContent
      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/locations/dps/$DPS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @DisplayName("DELETE /mapping/locations/nomis/{nomisLocationId}")
  @Nested
  inner class DeleteMappingByNomisIdTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/locations/nomis/999")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/locations/nomis/999")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/locations/nomis/999")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete specific mapping success`() {
      // create mapping
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createLocationMapping()))
        .exchange()
        .expectStatus().isCreated

      // it is present after creation by location id
      webTestClient.get().uri("/mapping/locations/dps/$DPS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk

      // it is also present after creation by nomis id
      webTestClient.get()
        .uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk

      // delete mapping
      webTestClient.delete().uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNoContent

      // no longer present by location id
      webTestClient.get().uri("/mapping/locations/dps/$DPS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound

      // and also no longer present by nomis id
      webTestClient.get()
        .uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    internal fun `delete is idempotent`() {
      // create mapping
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createLocationMapping()))
        .exchange()
        .expectStatus().isCreated

      // delete mapping
      webTestClient.delete().uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNoContent

      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @DisplayName("DELETE /mapping/locations")
  @Nested
  inner class DeleteMappingsTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/locations")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete mapping success`() {
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createLocationMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get()
        .uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get()
        .uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `delete location mappings - migrated mappings only`() {
      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createLocationMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createLocationMapping(
              dpsLocationId = "333",
              nomisLocationId = 444,
              mappingType = LocationMappingType.MIGRATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/locations/dps/333")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/mapping/locations?onlyMigrated=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get()
        .uri("/mapping/locations/nomis/$NOMIS_LOCATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk

      webTestClient.get().uri("/mapping/locations/dps/333")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
    }
  }
}
