package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

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
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.IncidentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.IncidentMappingType.NOMIS_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.IncidentMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.IncidentMappingService

class IncidentMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  @Qualifier("incidentMappingRepository")
  private lateinit var realRepository: IncidentMappingRepository
  private lateinit var repository: IncidentMappingRepository

  @Autowired
  private lateinit var incidentMappingService: IncidentMappingService

  private val nomisIncidentNo = 1234L
  private val incidentNo = "4444"

  @BeforeEach
  fun setup() {
    repository = mock(defaultAnswer = AdditionalAnswers.delegatesTo(realRepository))
    ReflectionTestUtils.setField(incidentMappingService, "incidentMappingRepository", repository)
  }

  private fun createIncidentMapping(
    nomisIncidentId: Long = nomisIncidentNo,
    incidentId: String = incidentNo,
    label: String = "2022-01-01",
    mappingType: String = NOMIS_CREATED.name,
  ): IncidentMappingDto = IncidentMappingDto(
    nomisIncidentId = nomisIncidentId,
    incidentId = incidentId,
    label = label,
    mappingType = mappingType,
  )

  private fun postcreateIncidentMappingRequest(
    nomisIncidentId: Long = 1234L,
    incidentId: String = "4444",
    label: String = "2022-01-01",
    mappingType: String = NOMIS_CREATED.name,
  ) {
    webTestClient.post().uri("/mapping/incidents")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createIncidentMapping(
            nomisIncidentId = nomisIncidentId,
            incidentId = incidentId,
            label = label,
            mappingType = mappingType,
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  @DisplayName("POST /mapping/incidents")
  @Nested
  inner class CreateIncidentMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/incidents")
        .body(BodyInserters.fromValue(createIncidentMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createIncidentMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createIncidentMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create when mapping for incident id already exists for another mapping`() {
      postcreateIncidentMappingRequest()

      val responseBody = webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncidentMapping().copy(nomisIncidentId = 21)))
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<IncidentMappingDto>>() {})
        .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Incident mapping already exists.\nExisting mapping: IncidentMappingDto(nomisIncidentId=1234, incidentId=4444, label=2022-01-01, mappingType=NOMIS_CREATED")
        assertThat(userMessage).contains("Duplicate mapping: IncidentMappingDto(nomisIncidentId=21, incidentId=4444, label=2022-01-01, mappingType=NOMIS_CREATED, whenCreated=null)")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existingIncident = responseBody.moreInfo?.existing!!
      with(existingIncident) {
        assertThat(incidentId).isEqualTo("4444")
        assertThat(nomisIncidentId).isEqualTo(1234)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }

      val duplicateIncident = responseBody.moreInfo?.duplicate!!
      with(duplicateIncident) {
        assertThat(incidentId).isEqualTo("4444")
        assertThat(nomisIncidentId).isEqualTo(21)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }
    }

    @Test
    internal fun `create mapping does not error when the same mapping already exists for the same incident`() {
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
              "nomisIncidentId" : $nomisIncidentNo,
              "incidentId"      : "$incidentNo",
              "label"           : "2022-01-01",
              "mappingType"     : "INCIDENT_CREATED"
            }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
              "nomisIncidentId" : $nomisIncidentNo,
              "incidentId"      : "$incidentNo",
              "label"           : "2022-01-01",
              "mappingType"     : "INCIDENT_CREATED"
            }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    fun `create when mapping for nomis ids already exists`() {
      postcreateIncidentMappingRequest()

      val responseBody = webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncidentMapping().copy(incidentId = "99")))
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<IncidentMappingDto>>() {})
        .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Incident mapping already exists.\nExisting mapping: IncidentMappingDto(nomisIncidentId=1234, incidentId=4444, label=2022-01-01, mappingType=NOMIS_CREATED")
        assertThat(userMessage).contains("Duplicate mapping: IncidentMappingDto(nomisIncidentId=1234, incidentId=99, label=2022-01-01, mappingType=NOMIS_CREATED, whenCreated=null)")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existingIncident = responseBody.moreInfo?.existing!!
      with(existingIncident) {
        assertThat(incidentId).isEqualTo("4444")
        assertThat(nomisIncidentId).isEqualTo(1234)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }

      val duplicateIncident = responseBody.moreInfo?.duplicate!!
      with(duplicateIncident) {
        assertThat(incidentId).isEqualTo("99")
        assertThat(nomisIncidentId).isEqualTo(1234)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }
    }

    @Test
    fun `create mapping success`() {
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
              "nomisIncidentId" : $nomisIncidentNo,
              "incidentId"      : "$incidentNo",
              "label"           : "2022-01-01",
              "mappingType"     : "INCIDENT_CREATED"
            }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping1 =
        webTestClient.get().uri("/mapping/incidents/nomis-incident-id/$nomisIncidentNo")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(IncidentMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping1.nomisIncidentId).isEqualTo(nomisIncidentNo)
      assertThat(mapping1.incidentId).isEqualTo(incidentNo)
      assertThat(mapping1.label).isEqualTo("2022-01-01")
      assertThat(mapping1.mappingType).isEqualTo("INCIDENT_CREATED")

      val mapping2 = webTestClient.get().uri("/mapping/incidents/incident-id/$incidentNo")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(IncidentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.nomisIncidentId).isEqualTo(nomisIncidentNo)
      assertThat(mapping2.incidentId).isEqualTo(incidentNo)
      assertThat(mapping2.label).isEqualTo("2022-01-01")
      assertThat(mapping2.mappingType).isEqualTo("INCIDENT_CREATED")
    }

    @Test
    fun `create mapping - Duplicate db error`() = runTest {
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
              "nomisIncidentId" : 101,
              "incidentId"      : "$incidentNo",
              "label"           : "2022-01-01",
              "mappingType"     : "INCIDENT_CREATED"
            }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      // Emulate calling service simultaneously twice by disabling the duplicate check
      // Note: the spy is automatically reset by ResetMocksTestExecutionListener
      whenever(repository.findById(incidentNo)).thenReturn(null)

      val responseBody =
        webTestClient.post().uri("/mapping/incidents")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
                "nomisIncidentId" : 102,
                "incidentId"      : "$incidentNo",
                "label"           : "2022-01-01",
                "mappingType"     : "INCIDENT_CREATED"
              }""",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<IncidentMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Incident mapping already exists, detected by org.springframework.dao.DuplicateKeyException")
        assertThat(errorCode).isEqualTo(1409)
      }
    }

    @Test
    fun `create rejects bad filter data - missing mapping type`() {
      assertThat(
        webTestClient.post().uri("/mapping/incidents")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
                "nomisIncidentId" : $nomisIncidentNo,
                "label"           : "2022-01-01",
                "incidentId"      : "$incidentNo"
              }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage,
      ).contains("missing (therefore NULL) value for creator parameter mappingType which is a non-nullable type")
    }

    @Test
    fun `create rejects bad filter data - mapping max 20`() {
      assertThat(
        webTestClient.post().uri("/mapping/incidents")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
                "nomisIncidentId" : $nomisIncidentNo,
                "label"           : "2022-01-01",
                "incidentId"      : "$incidentNo",
                "mappingType"     : "MASSIVELY_LONG_PROPERTY_INCIDENT_CREATED"
              }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage,
      ).contains("mappingType has a maximum length of 20")
    }

    @Test
    fun `create rejects bad filter data - incident property must be present (Long)`() {
      assertThat(
        webTestClient.post().uri("/mapping/incidents")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "nomisIncidentId" : $nomisIncidentNo,
            "label"           : "2022-01-01",
            "mappingType"     : "INCIDENT_CREATED"
          }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody?.userMessage,
      )
        .contains("Validation failure: JSON decoding error")
        .contains("incidentId")
    }
  }

  @DisplayName("GET /mapping/incidents/nomis-incident-id/{nomisIncidentId}")
  @Nested
  inner class GetNomisMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/incidents/nomis-incident-id/$nomisIncidentNo")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/incidents/nomis-incident-id/$nomisIncidentNo")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/incidents/nomis-incident-id/$nomisIncidentNo")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncidentMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping =
        webTestClient.get().uri("/mapping/incidents/nomis-incident-id/$nomisIncidentNo")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(IncidentMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping.nomisIncidentId).isEqualTo(nomisIncidentNo)
      assertThat(mapping.incidentId).isEqualTo(incidentNo)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED.name)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/incidents/incident-id/99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: incidentId=99999")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncidentMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/incidents/nomis-incident-id/$nomisIncidentNo")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /mapping/incidents/incident-id/{incidentId}")
  @Nested
  inner class GetIncidentMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/incidents/incident-id/$incidentNo")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/incidents/incident-id/$incidentNo")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/incidents/incident-id/$incidentNo")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncidentMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/incidents/incident-id/$incidentNo")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(IncidentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisIncidentId).isEqualTo(nomisIncidentNo)
      assertThat(mapping.incidentId).isEqualTo(incidentNo)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED.name)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/incidents/incident-id/765")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: incidentId=765")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncidentMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/incidents/incident-id/$incidentNo")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
    }
  }
}
