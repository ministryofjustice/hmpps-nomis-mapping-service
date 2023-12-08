package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

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
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.CreateRoomMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.RoomMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.VisitMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.VisitIdRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.VisitMappingService
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val nomisId = 1234L
private const val vsipId = "12345678"

private fun createMapping(
  nomisIdOverride: Long = nomisId,
  vsipIdOverride: String = vsipId,
  label: String = "2022-01-01",
  mappingType: String = "ONLINE",
): VisitMappingDto = VisitMappingDto(
  nomisId = nomisIdOverride,
  vsipId = vsipIdOverride,
  label = label,
  mappingType = mappingType,
)

private fun createRoomMapping(
  nomisRoomDescriptionOverride: String = "HEI-VISITS-SOC_VIS_TEST",
  vsipIdOverride: String = "Visits Main Room",
  isOpenOverride: Boolean = true,
): CreateRoomMappingDto = CreateRoomMappingDto(
  nomisRoomDescription = nomisRoomDescriptionOverride,
  vsipId = vsipIdOverride,
  isOpen = isOpenOverride,
)

class VisitMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  @Qualifier("visitIdRepository")
  private lateinit var realVisitIdRepository: VisitIdRepository
  private lateinit var visitIdRepository: VisitIdRepository

  @Autowired
  private lateinit var visitMappingService: VisitMappingService

  @Autowired
  lateinit var repository: Repository

  @BeforeEach
  fun setup() {
    visitIdRepository = mock(defaultAnswer = AdditionalAnswers.delegatesTo(realVisitIdRepository))
    visitMappingService.visitIdRepository = visitIdRepository
  }

  private fun postCreateMappingRequest(
    nomisIdOverride: Long = nomisId,
    vsipIdOverride: String = vsipId,
    label: String = "2022-01-01",
    mappingType: String = "ONLINE",
  ) {
    webTestClient.post().uri("/mapping/visits")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createMapping(
            vsipIdOverride = vsipIdOverride,
            nomisIdOverride = nomisIdOverride,
            label = label,
            mappingType = mappingType,
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  @DisplayName("POST /mapping/visits")
  @Nested
  inner class CreateMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/visits")
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create visit forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create mapping returns duplicate error details when visit id already exists for another mapping`() {
      postCreateMappingRequest()

      val responseBody = webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping().copy(vsipId = "other")))
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<VisitMappingDto>>() {})
        .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Visit mapping already exists. \nExisting mapping: VisitMappingDto(nomisId=1234, vsipId=12345678, label=2022-01-01, mappingType=ONLINE")
        assertThat(userMessage).contains("Duplicate mapping: VisitMappingDto(nomisId=1234, vsipId=other, label=2022-01-01, mappingType=ONLINE, whenCreated=null)")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existingVisit = responseBody.moreInfo?.existing!!
      with(existingVisit) {
        assertThat(vsipId).isEqualTo("12345678")
        assertThat(nomisId).isEqualTo(1234)
        assertThat(mappingType).isEqualTo("ONLINE")
      }

      val duplicateVisit = responseBody.moreInfo?.duplicate!!
      with(duplicateVisit) {
        assertThat(vsipId).isEqualTo("other")
        assertThat(nomisId).isEqualTo(1234)
        assertThat(mappingType).isEqualTo("ONLINE")
      }
    }

    @Test
    fun `create will succeed when mapping already exists for same visit ids`() {
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisId"     : $nomisId,
            "vsipId"      : "$vsipId",
            "label"       : "2022-01-01",
            "mappingType" : "ONLINE"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisId"     : $nomisId,
            "vsipId"      : "$vsipId",
            "label"       : "2022-01-01",
            "mappingType" : "ONLINE"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    fun `create mapping success`() {
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisId"     : $nomisId,
            "vsipId"      : "$vsipId",
            "label"       : "2022-01-01",
            "mappingType" : "ONLINE"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping1 = webTestClient.get().uri("/mapping/visits/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(VisitMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping1.nomisId).isEqualTo(nomisId)
      assertThat(mapping1.vsipId).isEqualTo(vsipId)
      assertThat(mapping1.label).isEqualTo("2022-01-01")
      assertThat(mapping1.mappingType).isEqualTo("ONLINE")

      val mapping2 = webTestClient.get().uri("/mapping/visits/vsipId/$vsipId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(VisitMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.nomisId).isEqualTo(nomisId)
      assertThat(mapping2.vsipId).isEqualTo(vsipId)
      assertThat(mapping2.label).isEqualTo("2022-01-01")
      assertThat(mapping2.mappingType).isEqualTo("ONLINE")
    }

    @Test
    fun `create mapping - Duplicate db error`() = runTest {
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisId"     : 101,
            "vsipId"      : "$vsipId",
            "label"       : "2022-01-01",
            "mappingType" : "ONLINE"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      // Emulate calling service simultaneously twice by disabling the duplicate check
      // Note: the spy is automatically reset by ResetMocksTestExecutionListener
      whenever(visitIdRepository.findOneByVsipId(vsipId)).thenReturn(null)

      val responseBody =
        webTestClient.post().uri("/mapping/visits")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "nomisId"     : 102,
            "vsipId"      : "$vsipId",
            "label"       : "2022-01-01",
            "mappingType" : "ONLINE"
          }""",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<VisitMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Visit mapping already exists, detected by org.springframework.dao.DuplicateKeyException")
        assertThat(errorCode).isEqualTo(1409)
      }
    }
  }

  @DisplayName("GET /mapping/visits/nomisId/{nomisId}")
  @Nested
  inner class GetNomisMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/visits/nomisId/$nomisId")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/visits/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/visits/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/visits/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(VisitMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisId).isEqualTo(nomisId)
      assertThat(mapping.vsipId).isEqualTo(vsipId)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo("ONLINE")
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/visits/nomisId/99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: NOMIS visit id=99999")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/visits/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /mapping/visits/migrated/latest")
  @Nested
  inner class GeMappingMigratedLatestTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/visits/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/visits/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/visits/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              vsipIdOverride = "10",
              nomisIdOverride = 10,
              label = "2022-01-01T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              vsipIdOverride = "20",
              nomisIdOverride = 20,
              label = "2022-01-02T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              vsipIdOverride = "1",
              nomisIdOverride = 1,
              label = "2022-01-02T10:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              vsipIdOverride = "99",
              nomisIdOverride = 199,
              label = "whatever",
              mappingType = "ONLINE",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/visits/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(VisitMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisId).isEqualTo(1)
      assertThat(mapping.vsipId).isEqualTo("1")
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo("MIGRATED")
      assertThat(mapping.whenCreated).isCloseTo(LocalDateTime.now(), byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              vsipIdOverride = "99",
              nomisIdOverride = 199,
              label = "whatever",
              mappingType = "ONLINE",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/visits/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }

  @DisplayName("GET /mapping/visits/vsipId/{vsipId}")
  @Nested
  inner class GetVsipMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/visits/vsipId/$vsipId")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/visits/vsipId/$vsipId")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/visits/vsipId/$vsipId")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/visits/vsipId/$vsipId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(VisitMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisId).isEqualTo(nomisId)
      assertThat(mapping.vsipId).isEqualTo(vsipId)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo("ONLINE")
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/visits/vsipId/NOT_THERE")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: VSIP visit id=NOT_THERE")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/visits/vsipId/$vsipId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /prison/{prisonId}/room/nomis-room-id/{roomId}")
  @Nested
  inner class GetRoomMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/prison/HEI/room/nomis-room-id/HEI-VISITS-SOC_VIS")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/prison/HEI/room/nomis-room-id/HEI-VISITS-SOC_VIS")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/prison/HEI/room/nomis-room-id/HEI-VISITS-SOC_VIS")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get room mapping success`() {
      val mapping1 = webTestClient.get().uri("/prison/HEI/room/nomis-room-id/HEI-VISITS-SOC_VIS")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(RoomMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping1.nomisRoomDescription).isEqualTo("HEI-VISITS-SOC_VIS")
      assertThat(mapping1.vsipId).isEqualTo("Visits Main Room")
      assertThat(mapping1.isOpen).isEqualTo(true)
      assertThat(mapping1.prisonId).isEqualTo("HEI")
    }

    @Test
    fun `room mapping not found`() {
      val error = webTestClient.get().uri("/prison/HEI/room/nomis-room-id/HEI-NOT_THERE")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: prison id=HEI, nomis room id=HEI-NOT_THERE")
    }
  }

  @DisplayName("GET /prison/{prisonId}/room-mappings")
  @Nested
  inner class GetRoomMappingsTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/prison/HEI/room-mappings")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/prison/HEI/room-mappings")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/prison/HEI/room-mappings")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get room mapping success`() {
      webTestClient.get().uri("/prison/HEI/room-mappings")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(17)
        .jsonPath("$[0].prisonId").isEqualTo("HEI")
        .jsonPath("$[0].nomisRoomDescription").isEqualTo("HEI-FAMILYTIME")
        .jsonPath("$[0].vsipId").isEqualTo("Visits Main Room")
        .jsonPath("$[0].isOpen").isEqualTo(true)
        .returnResult().responseBody!!
    }

    @Test
    fun `room mappings not found`() {
      webTestClient.get().uri("/prison/JJJ/room-mappings")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(0)
        .returnResult().responseBody!!
    }
  }

  @DisplayName("POST /prison/{prisonId}/room-mappings")
  @Nested
  inner class CreateRoomMappingTest {

    @AfterEach
    internal fun deleteData() {
      webTestClient.delete().uri("/prison/HEI/room-mappings/nomis-room-id/HEI-VISITS-SOC_VIS_TEST")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
      webTestClient.delete().uri("/prison/FGF/room-mappings/nomis-room-id/nomisroom")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/prison/HEI/room-mappings")
        .body(BodyInserters.fromValue(createRoomMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/prison/HEI/room-mappings")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createRoomMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.post().uri("/prison/HEI/room-mappings")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createRoomMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create room mapping success`() {
      webTestClient.post().uri("/prison/HEI/room-mappings")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .body(BodyInserters.fromValue(createRoomMapping()))
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    fun `create room mapping allows isOpen flag to be omitted `() {
      webTestClient.post().uri("/prison/FGF/room-mappings")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nomisRoomDescription"     : "nomisroom",
            "vsipId"      : "vsiproom"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/prison/FGF/room-mappings")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(1)
        .jsonPath("$[0].isOpen").isEqualTo(false)
    }

    @Test
    fun `create room mapping rejects duplicate`() {
      webTestClient.post().uri("/prison/HEI/room-mappings")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .body(BodyInserters.fromValue(createRoomMapping(nomisRoomDescriptionOverride = "HEI-VISITS")))
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @DisplayName("DELETE /prison/{prisonId}/room-mappings/nomis-room-id/{nomisRoomDescription}")
  @Nested
  inner class DeleteRoomMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/prison/HEI/room-mappings/nomis-room-id/HEI-VISITS-SOC_VIS")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/prison/HEI/room-mappings/nomis-room-id/HEI-VISITS-SOC_VIS")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/prison/HEI/room-mappings/nomis-room-id/HEI-VISITS-SOC_VIS")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete room mapping success`() {
      webTestClient.post().uri("/prison/HEI/room-mappings")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .body(BodyInserters.fromValue(createRoomMapping(nomisRoomDescriptionOverride = "ROOM_TO_BE_DELETED")))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/prison/HEI/room/nomis-room-id/ROOM_TO_BE_DELETED")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/prison/HEI/room-mappings/nomis-room-id/ROOM_TO_BE_DELETED")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/prison/HEI/room/nomis-room-id/ROOM_TO_BE_DELETED")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `delete nonexistent room - no error`() {
      webTestClient.delete().uri("/prison/HEI/room-mappings/nomis-room-id/NONEXISTENT_ROOM")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @DisplayName("DELETE /mapping/visits")
  @Nested
  inner class DeleteMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/visits")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete visit mapping success`() {
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/visits/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/mapping/visits/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `delete visit mappings - migrated mappings only`() {
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createMapping(
              nomisIdOverride = 222,
              vsipIdOverride = "333",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.delete().uri("/mapping/visits?onlyMigrated=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/mapping/visits/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk

      webTestClient.get().uri("/mapping/visits/nomisId/222")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @DisplayName("DELETE /mapping/visits/migration-id/{migrationId}")
  @Nested
  inner class DeleteMappingByMigrationIdTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/visits/migration-id/2022-01-01T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/visits/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/visits/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete visit mapping by migration id success`() {
      // add two visit mappings to a migration
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping(label = "2022-01-01T00:00:00")))
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping(nomisIdOverride = 9999, vsipIdOverride = "jj-12-23", label = "2022-01-01T00:00:00")))
        .exchange()
        .expectStatus().isCreated

      // add 1 visit to a different migration
      webTestClient.post().uri("/mapping/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createMapping(nomisIdOverride = 8888, vsipIdOverride = "gh-12-23", label = "2022-05-05T00:00:00")))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/visits/nomisId/8888")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk

      webTestClient.get().uri("/mapping/visits/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk

      webTestClient.get().uri("/mapping/visits/nomisId/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/mapping/visits/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/mapping/visits/nomisId/$nomisId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isNotFound

      webTestClient.get().uri("/mapping/visits/nomisId/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isNotFound

      // should not have deleted visit mapping from a different migration
      webTestClient.get().uri("/mapping/visits/nomisId/8888")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /mapping/visits/migration-id/{migrationId}")
  @Nested
  inner class GetVisitMappingByMigrationIdTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/visits/migration-id/2022-01-01T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/visits/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get visit mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/visits/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get visit mappings by migration id success`() {
      (1L..4L).forEach {
        postCreateMappingRequest(
          vsipIdOverride = it.toString(),
          nomisIdOverride = it,
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      (5L..9L).forEach {
        postCreateMappingRequest(
          vsipIdOverride = it.toString(),
          nomisIdOverride = it,
          label = "2099-01-01",
          mappingType = "MIGRATED",
        )
      }
      postCreateMappingRequest(nomisIdOverride = 12, vsipIdOverride = "12", mappingType = "ONLINE")

      webTestClient.get().uri("/mapping/visits/migration-id/2022-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..nomisId").value(
          Matchers.contains(
            1,
            2,
            3,
            4,
          ),
        )
        .jsonPath("$.content[0].whenCreated").isNotEmpty
    }

    @Test
    fun `get visit mappings by migration id - no records exist`() {
      (1L..4L).forEach {
        postCreateMappingRequest(
          vsipIdOverride = it.toString(),
          nomisIdOverride = it,
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }

      webTestClient.get().uri("/mapping/visits/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
        .jsonPath("content").isEmpty
    }

    @Test
    fun `can request a different page size`() {
      (1L..6L).forEach {
        postCreateMappingRequest(
          vsipIdOverride = it.toString(),
          nomisIdOverride = it,
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/visits/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("sort", "nomisId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
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
        postCreateMappingRequest(
          vsipIdOverride = it.toString(),
          nomisIdOverride = it,
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/visits/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .queryParam("sort", "nomisId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISITS")))
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
