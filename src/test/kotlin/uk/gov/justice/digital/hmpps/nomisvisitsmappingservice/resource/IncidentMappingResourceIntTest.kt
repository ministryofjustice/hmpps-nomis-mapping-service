package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.IncidentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.IncidentMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.IncidentMappingType.INCIDENT_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.IncidentMappingType.NOMIS_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.IncidentMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.IncidentMappingService
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val INCIDENT_ID = "4321"
private const val NOMIS_INCIDENT_ID = 1234L

class IncidentMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  @Qualifier("incidentMappingRepository")
  private lateinit var realRepository: IncidentMappingRepository
  private lateinit var repository: IncidentMappingRepository

  @Autowired
  private lateinit var incidentMappingService: IncidentMappingService

  @BeforeEach
  fun setup() {
    repository = mock(defaultAnswer = AdditionalAnswers.delegatesTo(realRepository))
    ReflectionTestUtils.setField(incidentMappingService, "incidentMappingRepository", repository)
  }

  private fun createIncidentMapping(
    nomisIncidentId: Long = NOMIS_INCIDENT_ID,
    incidentId: String = INCIDENT_ID,
    label: String = "2022-01-01",
    mappingType: String = NOMIS_CREATED.name,
  ): IncidentMappingDto = IncidentMappingDto(
    nomisIncidentId = nomisIncidentId,
    incidentId = incidentId,
    label = label,
    mappingType = mappingType,
  )

  private fun postCreateIncidentMappingRequest(
    nomisIncidentId: Long = NOMIS_INCIDENT_ID,
    incidentId: String = INCIDENT_ID,
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
      postCreateIncidentMappingRequest()

      val responseBody = webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncidentMapping().copy(nomisIncidentId = 21)))
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<IncidentMappingDto>>() {})
        .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Incident mapping already exists.\nExisting mapping: IncidentMappingDto(nomisIncidentId=$NOMIS_INCIDENT_ID, incidentId=$INCIDENT_ID, label=2022-01-01, mappingType=NOMIS_CREATED")
        assertThat(userMessage).contains("Duplicate mapping: IncidentMappingDto(nomisIncidentId=21, incidentId=$INCIDENT_ID, label=2022-01-01, mappingType=NOMIS_CREATED, whenCreated=null)")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existingIncident = responseBody.moreInfo?.existing!!
      with(existingIncident) {
        assertThat(incidentId).isEqualTo(INCIDENT_ID)
        assertThat(nomisIncidentId).isEqualTo(NOMIS_INCIDENT_ID)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }

      val duplicateIncident = responseBody.moreInfo?.duplicate!!
      with(duplicateIncident) {
        assertThat(incidentId).isEqualTo(INCIDENT_ID)
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
              "nomisIncidentId" : $NOMIS_INCIDENT_ID,
              "incidentId"      : "$INCIDENT_ID",
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
              "nomisIncidentId" : $NOMIS_INCIDENT_ID,
              "incidentId"      : "$INCIDENT_ID",
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
      postCreateIncidentMappingRequest()

      val responseBody = webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncidentMapping().copy(incidentId = "99")))
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<IncidentMappingDto>>() {})
        .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Incident mapping already exists.\nExisting mapping: IncidentMappingDto(nomisIncidentId=$NOMIS_INCIDENT_ID, incidentId=$INCIDENT_ID, label=2022-01-01, mappingType=NOMIS_CREATED")
        assertThat(userMessage).contains("Duplicate mapping: IncidentMappingDto(nomisIncidentId=$NOMIS_INCIDENT_ID, incidentId=99, label=2022-01-01, mappingType=NOMIS_CREATED, whenCreated=null)")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existingIncident = responseBody.moreInfo?.existing!!
      with(existingIncident) {
        assertThat(incidentId).isEqualTo(INCIDENT_ID)
        assertThat(nomisIncidentId).isEqualTo(NOMIS_INCIDENT_ID)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }

      val duplicateIncident = responseBody.moreInfo?.duplicate!!
      with(duplicateIncident) {
        assertThat(incidentId).isEqualTo("99")
        assertThat(nomisIncidentId).isEqualTo(NOMIS_INCIDENT_ID)
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
              "nomisIncidentId" : $NOMIS_INCIDENT_ID,
              "incidentId"      : "$INCIDENT_ID",
              "label"           : "2022-01-01",
              "mappingType"     : "INCIDENT_CREATED"
            }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping1 =
        webTestClient.get().uri("/mapping/incidents/nomis-incident-id/$NOMIS_INCIDENT_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(IncidentMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping1.nomisIncidentId).isEqualTo(NOMIS_INCIDENT_ID)
      assertThat(mapping1.incidentId).isEqualTo(INCIDENT_ID)
      assertThat(mapping1.label).isEqualTo("2022-01-01")
      assertThat(mapping1.mappingType).isEqualTo("INCIDENT_CREATED")

      val mapping2 = webTestClient.get().uri("/mapping/incidents/incident-id/$INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(IncidentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.nomisIncidentId).isEqualTo(NOMIS_INCIDENT_ID)
      assertThat(mapping2.incidentId).isEqualTo(INCIDENT_ID)
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
              "incidentId"      : "$INCIDENT_ID",
              "label"           : "2022-01-01",
              "mappingType"     : "INCIDENT_CREATED"
            }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      // Emulate calling service simultaneously twice by disabling the duplicate check
      // Note: the spy is automatically reset by ResetMocksTestExecutionListener
      whenever(repository.findById(INCIDENT_ID)).thenReturn(null)

      val responseBody =
        webTestClient.post().uri("/mapping/incidents")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
                "nomisIncidentId" : 102,
                "incidentId"      : "$INCIDENT_ID",
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
                "nomisIncidentId" : $NOMIS_INCIDENT_ID,
                "label"           : "2022-01-01",
                "incidentId"      : "$INCIDENT_ID"
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
                "nomisIncidentId" : $NOMIS_INCIDENT_ID,
                "label"           : "2022-01-01",
                "incidentId"      : "$INCIDENT_ID",
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
            "nomisIncidentId" : $NOMIS_INCIDENT_ID,
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
      webTestClient.get().uri("/mapping/incidents/nomis-incident-id/$NOMIS_INCIDENT_ID")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/incidents/nomis-incident-id/$NOMIS_INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/incidents/nomis-incident-id/$NOMIS_INCIDENT_ID")
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
        webTestClient.get().uri("/mapping/incidents/nomis-incident-id/$NOMIS_INCIDENT_ID")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(IncidentMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping.nomisIncidentId).isEqualTo(NOMIS_INCIDENT_ID)
      assertThat(mapping.incidentId).isEqualTo(INCIDENT_ID)
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

      webTestClient.get().uri("/mapping/incidents/nomis-incident-id/$NOMIS_INCIDENT_ID")
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
      webTestClient.get().uri("/mapping/incidents/incident-id/$INCIDENT_ID")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/incidents/incident-id/$INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/incidents/incident-id/$INCIDENT_ID")
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

      val mapping = webTestClient.get().uri("/mapping/incidents/incident-id/$INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(IncidentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisIncidentId).isEqualTo(NOMIS_INCIDENT_ID)
      assertThat(mapping.incidentId).isEqualTo(INCIDENT_ID)
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

      webTestClient.get().uri("/mapping/incidents/incident-id/$INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("DELETE /mapping/incidents/incident-id/{incidentId}")
  @Nested
  inner class DeleteMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/incidents/incident-id/999")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/incidents/incident-id/999")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/incidents/incident-id/999")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete specific mapping success`() {
      // create mapping
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncidentMapping()))
        .exchange()
        .expectStatus().isCreated

      // it is present after creation by incident id
      webTestClient.get().uri("/mapping/incidents/incident-id/$INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
      // it is also present after creation by nomis id
      webTestClient.get().uri("/mapping/incidents/nomis-incident-id/$NOMIS_INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk

      // delete mapping
      webTestClient.delete().uri("/mapping/incidents/incident-id/$INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNoContent

      // no longer present by incident id
      webTestClient.get().uri("/mapping/incidents/incident-id/$INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNotFound
      // and also no longer present by nomis id
      webTestClient.get().uri("/mapping/incidents/nomis-incident-id/$NOMIS_INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    internal fun `delete is idempotent`() {
      // create mapping
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncidentMapping()))
        .exchange()
        .expectStatus().isCreated

      // delete mapping
      webTestClient.delete().uri("/mapping/incidents/incident-id/$INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNoContent
      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/incidents/incident-id/$INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @DisplayName("DELETE /mapping/incidents")
  @Nested
  inner class DeleteMappingsTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/incidents")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete mapping success`() {
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncidentMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get()
        .uri("/mapping/incidents/nomis-incident-id/$NOMIS_INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get()
        .uri("/mapping/incidents/nomis-incident-id/$NOMIS_INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `delete incident mappings - migrated mappings only`() {
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createIncidentMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createIncidentMapping(
              nomisIncidentId = 2345,
              incidentId = "5432",
              mappingType = IncidentMappingType.MIGRATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/incidents/incident-id/5432")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/mapping/incidents?onlyMigrated=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get()
        .uri("/mapping/incidents/nomis-incident-id/$NOMIS_INCIDENT_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk

      webTestClient.get().uri("/mapping/incidents/incident-id/5432")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @DisplayName("GET /mapping/incidents/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationIdTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/migration-id/2022-01-01T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/incidents/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get incident mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/incidents/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get incident mappings by migration id success`() {
      (1L..4L).forEach {
        postCreateIncidentMappingRequest(
          nomisIncidentId = it,
          incidentId = "$it",
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      (5L..9L).forEach {
        postCreateIncidentMappingRequest(
          nomisIncidentId = it,
          incidentId = "$it",
          label = "2099-01-01",
          mappingType = "MIGRATED",
        )
      }
      postCreateIncidentMappingRequest(
        nomisIncidentId = 12,
        incidentId = "12",
        mappingType = INCIDENT_CREATED.name,
      )

      webTestClient.get().uri("/mapping/incidents/migration-id/2022-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..incidentId").value(Matchers.contains("1", "2", "3", "4"))
        .jsonPath("$.content..nomisIncidentId").value(Matchers.contains(1, 2, 3, 4))
        .jsonPath("$.content[0].whenCreated").isNotEmpty
    }

    @Test
    fun `get incident mappings by migration id - no records exist`() {
      (1L..4L).forEach {
        postCreateIncidentMappingRequest(
          nomisIncidentId = it,
          incidentId = "$it",
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }

      webTestClient.get().uri("/mapping/incidents/migration-id/2044-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
        .jsonPath("content").isEmpty
    }

    @Test
    fun `can request a different page size`() {
      (1L..6L).forEach {
        postCreateIncidentMappingRequest(
          nomisIncidentId = it,
          incidentId = "$it",
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/incidents/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("sort", "nomisIncidentId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
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
        postCreateIncidentMappingRequest(
          nomisIncidentId = it,
          incidentId = "$it",
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/incidents/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .queryParam("sort", "nomisIncidentId,asc")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
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

  @DisplayName("GET /mapping/incidents/migrated/latest")
  @Nested
  inner class GeMappingMigratedLatestTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      repository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/incidents/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/incidents/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/incidents/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createIncidentMapping(
              nomisIncidentId = 10,
              incidentId = "10",
              label = "2022-01-01T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createIncidentMapping(
              nomisIncidentId = 20,
              incidentId = "4",
              label = "2022-01-02T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createIncidentMapping(
              nomisIncidentId = 1,
              incidentId = "1",
              label = "2022-01-02T10:00:00",
              mappingType = IncidentMappingType.MIGRATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createIncidentMapping(
              nomisIncidentId = 99,
              incidentId = "3",
              label = "whatever",
              mappingType = NOMIS_CREATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/incidents/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(IncidentMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nomisIncidentId).isEqualTo(1)
      assertThat(mapping.incidentId).isEqualTo("1")
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo("MIGRATED")
      assertThat(mapping.whenCreated).isCloseTo(LocalDateTime.now(), Assertions.byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/incidents")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createIncidentMapping(
              nomisIncidentId = 77,
              incidentId = "7",
              label = "whatever",
              mappingType = NOMIS_CREATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/incidents/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }
}
