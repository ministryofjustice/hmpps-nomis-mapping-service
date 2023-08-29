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
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.NonAssociationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders.NonAssociationRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.NonAssociationMappingType.NOMIS_CREATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.NonAssociationMappingRepository

@OptIn(ExperimentalCoroutinesApi::class)
class NonAssociationMappingResourceIntTest : IntegrationTestBase() {

  @SpyBean(name = "nonAssociationMappingRepository")
  lateinit var nonAssociationMappingRepository: NonAssociationMappingRepository

  @SpyBean
  lateinit var nonAssociationRepository: NonAssociationRepository

  private val NON_ASSOCIATION_ID = 1234L
  private val FIRST_OFFENDER_NO = "A1234BC"
  private val SECOND_OFFENDER_NO = "D5678EF"
  private val TYPE_SEQUENCE = 1

  private fun createNonAssociationMapping(
    nonAssociationId: Long = NON_ASSOCIATION_ID,
    firstOffenderNo: String = FIRST_OFFENDER_NO,
    secondOffenderNo: String = SECOND_OFFENDER_NO,
    nomisTypeSequence: Int = TYPE_SEQUENCE,
    label: String = "2022-01-01",
    mappingType: String = NOMIS_CREATED.name,
  ): NonAssociationMappingDto = NonAssociationMappingDto(
    nonAssociationId = nonAssociationId,
    firstOffenderNo = firstOffenderNo,
    secondOffenderNo = secondOffenderNo,
    nomisTypeSequence = nomisTypeSequence,
    label = label,
    mappingType = mappingType,
  )

  private fun postCreateNonAssociationMappingRequest(
    nonAssociationId: Long = NON_ASSOCIATION_ID,
    firstOffenderNo: String = FIRST_OFFENDER_NO,
    secondOffenderNo: String = SECOND_OFFENDER_NO,
    nomisTypeSequence: Int = TYPE_SEQUENCE,
    label: String = "2022-01-01",
    mappingType: String = NOMIS_CREATED.name,
  ) {
    webTestClient.post().uri("/mapping/non-associations")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          createNonAssociationMapping(
            nonAssociationId = nonAssociationId,
            firstOffenderNo = firstOffenderNo,
            secondOffenderNo = secondOffenderNo,
            nomisTypeSequence = nomisTypeSequence,
            label = label,
            mappingType = mappingType,
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  @DisplayName("POST /mapping/non-associations")
  @Nested
  inner class CreateNonAssociationMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      nonAssociationRepository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/mapping/non-associations")
        .body(BodyInserters.fromValue(createNonAssociationMapping()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createNonAssociationMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create forbidden with wrong role`() {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createNonAssociationMapping()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    internal fun `create mapping succeeds when the same mapping already exists for the same non-association`() {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nonAssociationId"  : $NON_ASSOCIATION_ID,
            "firstOffenderNo"   : "$FIRST_OFFENDER_NO",
            "secondOffenderNo"  : "$SECOND_OFFENDER_NO",
            "nomisTypeSequence" : $TYPE_SEQUENCE,
            "label"             : "2022-01-01",
            "mappingType"       : "NOMIS_CREATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "nonAssociationId"  : $NON_ASSOCIATION_ID,
            "firstOffenderNo"   : "$FIRST_OFFENDER_NO",
            "secondOffenderNo"  : "$SECOND_OFFENDER_NO",
            "nomisTypeSequence" : $TYPE_SEQUENCE,
            "label"       : "2022-01-01",
            "mappingType" : "NOMIS_CREATED"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    fun `create when mapping for nomis ids already exists`() {
      postCreateNonAssociationMappingRequest()

      val responseBody =
        webTestClient.post().uri("/mapping/non-associations")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createNonAssociationMapping().copy(nonAssociationId = 99)))
          .exchange()
          .expectStatus().isEqualTo(HttpStatus.CONFLICT.value())
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<NonAssociationMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Non-association mapping already exists. \nExisting mapping: NonAssociationMappingDto(nonAssociationId=1234, firstOffenderNo=A1234BC, secondOffenderNo=D5678EF, nomisTypeSequence=1, label=2022-01-01, mappingType=NOMIS_CREATED")
        assertThat(userMessage).contains("Duplicate mapping: NonAssociationMappingDto(nonAssociationId=99, firstOffenderNo=A1234BC, secondOffenderNo=D5678EF, nomisTypeSequence=1, label=2022-01-01, mappingType=NOMIS_CREATED, whenCreated=null")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existingNonAssociation = responseBody.moreInfo?.existing!!
      with(existingNonAssociation) {
        assertThat(nonAssociationId).isEqualTo(1234)
        assertThat(firstOffenderNo).isEqualTo("A1234BC")
        assertThat(secondOffenderNo).isEqualTo("D5678EF")
        assertThat(nomisTypeSequence).isEqualTo(1)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }

      val duplicateNonAssociation = responseBody.moreInfo?.duplicate!!
      with(duplicateNonAssociation) {
        assertThat(nonAssociationId).isEqualTo(99)
        assertThat(firstOffenderNo).isEqualTo("A1234BC")
        assertThat(secondOffenderNo).isEqualTo("D5678EF")
        assertThat(nomisTypeSequence).isEqualTo(1)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }
    }

    @Test
    fun `create when mapping for nonAssociation id already exists for another non-association`() {
      postCreateNonAssociationMappingRequest()

      val responseBody =
        webTestClient.post().uri("/mapping/non-associations")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createNonAssociationMapping().copy(nomisTypeSequence = 21)))
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<NonAssociationMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Non-association mapping already exists. \nExisting mapping: NonAssociationMappingDto(nonAssociationId=1234, firstOffenderNo=A1234BC, secondOffenderNo=D5678EF, nomisTypeSequence=1, label=2022-01-01, mappingType=NOMIS_CREATED")
        assertThat(userMessage).contains("Duplicate mapping: NonAssociationMappingDto(nonAssociationId=1234, firstOffenderNo=A1234BC, secondOffenderNo=D5678EF, nomisTypeSequence=21, label=2022-01-01, mappingType=NOMIS_CREATED, whenCreated=null")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existingNonAssociation = responseBody.moreInfo?.existing!!
      with(existingNonAssociation) {
        assertThat(nonAssociationId).isEqualTo(1234)
        assertThat(firstOffenderNo).isEqualTo("A1234BC")
        assertThat(secondOffenderNo).isEqualTo("D5678EF")
        assertThat(nomisTypeSequence).isEqualTo(1)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }

      val duplicateNonAssociation = responseBody.moreInfo?.duplicate!!
      with(duplicateNonAssociation) {
        assertThat(nonAssociationId).isEqualTo(1234)
        assertThat(firstOffenderNo).isEqualTo("A1234BC")
        assertThat(secondOffenderNo).isEqualTo("D5678EF")
        assertThat(nomisTypeSequence).isEqualTo(21)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }
    }

    @Test
    fun `create mapping success`() {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                "nonAssociationId"  : $NON_ASSOCIATION_ID,
                "firstOffenderNo"   : "$FIRST_OFFENDER_NO",
                "secondOffenderNo"  : "$SECOND_OFFENDER_NO",
                "nomisTypeSequence" : $TYPE_SEQUENCE,
                "label"             : "2022-01-01",
                "mappingType"       : "NOMIS_CREATED"
              }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping1 =
        webTestClient.get().uri("/mapping/non-associations/firstOffenderNo/$FIRST_OFFENDER_NO/secondOffenderNo/$SECOND_OFFENDER_NO/typeSequence/$TYPE_SEQUENCE")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(NonAssociationMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping1.nonAssociationId).isEqualTo(NON_ASSOCIATION_ID)
      assertThat(mapping1.firstOffenderNo).isEqualTo(FIRST_OFFENDER_NO)
      assertThat(mapping1.secondOffenderNo).isEqualTo(SECOND_OFFENDER_NO)
      assertThat(mapping1.nomisTypeSequence).isEqualTo(TYPE_SEQUENCE)
      assertThat(mapping1.label).isEqualTo("2022-01-01")
      assertThat(mapping1.mappingType).isEqualTo(NOMIS_CREATED.name)

      val mapping2 = webTestClient.get().uri("/mapping/non-associations/nonAssociationId/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(NonAssociationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping2.nonAssociationId).isEqualTo(NON_ASSOCIATION_ID)
      assertThat(mapping2.firstOffenderNo).isEqualTo(FIRST_OFFENDER_NO)
      assertThat(mapping2.secondOffenderNo).isEqualTo(SECOND_OFFENDER_NO)
      assertThat(mapping2.nomisTypeSequence).isEqualTo(TYPE_SEQUENCE)
      assertThat(mapping2.label).isEqualTo("2022-01-01")
      assertThat(mapping2.mappingType).isEqualTo("NOMIS_CREATED")
    }

    @Test
    fun `create mapping - Duplicate db error`() = runTest {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                "nonAssociationId"  : $NON_ASSOCIATION_ID,
                "firstOffenderNo"   : "$FIRST_OFFENDER_NO",
                "secondOffenderNo"  : "$SECOND_OFFENDER_NO",
                "nomisTypeSequence" : $TYPE_SEQUENCE,
                "label"             : "2022-01-01",
                "mappingType"       : "NOMIS_CREATED"
              }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      // Emulate calling service simultaneously twice by disabling the duplicate check
      // Note: the spy is automatically reset by ResetMocksTestExecutionListener
      whenever(nonAssociationMappingRepository.findById(NON_ASSOCIATION_ID)).thenReturn(null)

      val responseBody =
        webTestClient.post().uri("/mapping/non-associations")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
                "nonAssociationId"  : $NON_ASSOCIATION_ID,
                "firstOffenderNo"   : "$FIRST_OFFENDER_NO",
                "secondOffenderNo"  : "$SECOND_OFFENDER_NO",
                "nomisTypeSequence" : 2,
                "label"             : "2022-01-01",
                "mappingType"       : "NOMIS_CREATED"
              }""",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<NonAssociationMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Non-association mapping already exists, detected by org.springframework.dao.DuplicateKeyException")
        assertThat(errorCode).isEqualTo(1409)
      }
    }
  }

  @DisplayName("GET /mapping/non-associations/firstOffenderNo/{firstOffenderNo}/secondOffenderNo/{secondOffenderNo}/typeSequence/{typeSequence}")
  @Nested
  inner class GetNomisMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      nonAssociationRepository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/non-associations/firstOffenderNo/$FIRST_OFFENDER_NO/secondOffenderNo/$SECOND_OFFENDER_NO/typeSequence/$TYPE_SEQUENCE")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/non-associations/firstOffenderNo/$FIRST_OFFENDER_NO/secondOffenderNo/$SECOND_OFFENDER_NO/typeSequence/$TYPE_SEQUENCE")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/non-associations/firstOffenderNo/$FIRST_OFFENDER_NO/secondOffenderNo/$SECOND_OFFENDER_NO/typeSequence/$TYPE_SEQUENCE")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createNonAssociationMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping =
        webTestClient.get().uri("/mapping/non-associations/firstOffenderNo/$FIRST_OFFENDER_NO/secondOffenderNo/$SECOND_OFFENDER_NO/typeSequence/$TYPE_SEQUENCE")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody(NonAssociationMappingDto::class.java)
          .returnResult().responseBody!!

      assertThat(mapping.nonAssociationId).isEqualTo(NON_ASSOCIATION_ID)
      assertThat(mapping.firstOffenderNo).isEqualTo(FIRST_OFFENDER_NO)
      assertThat(mapping.secondOffenderNo).isEqualTo(SECOND_OFFENDER_NO)
      assertThat(mapping.nomisTypeSequence).isEqualTo(TYPE_SEQUENCE)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED.name)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/non-associations/firstOffenderNo/$FIRST_OFFENDER_NO/secondOffenderNo/$SECOND_OFFENDER_NO/typeSequence/999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: Non-association with firstOffenderNo=A1234BC, secondOffenderNo=D5678EF, and nomisTypeSequence=999 not found")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createNonAssociationMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/non-associations/firstOffenderNo/$FIRST_OFFENDER_NO/secondOffenderNo/$SECOND_OFFENDER_NO/typeSequence/$TYPE_SEQUENCE")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /mapping/non-associations/nonAssociation/{nonAssociationId}")
  @Nested
  inner class GetNonAssociationMappingTest {

    @AfterEach
    internal fun deleteData() = runBlocking {
      nonAssociationRepository.deleteAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/non-associations/nonAssociationId/$NON_ASSOCIATION_ID")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/non-associations/nonAssociationId/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/non-associations/nonAssociationId/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createNonAssociationMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/non-associations/nonAssociationId/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(NonAssociationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nonAssociationId).isEqualTo(NON_ASSOCIATION_ID)
      assertThat(mapping.firstOffenderNo).isEqualTo(FIRST_OFFENDER_NO)
      assertThat(mapping.secondOffenderNo).isEqualTo(SECOND_OFFENDER_NO)
      assertThat(mapping.nomisTypeSequence).isEqualTo(TYPE_SEQUENCE)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED.name)
    }

    @Test
    fun `mapping not found`() {
      val error = webTestClient.get().uri("/mapping/non-associations/nonAssociationId/765")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: nonAssociationId=765")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createNonAssociationMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/non-associations/nonAssociationId/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
    }
  }
}
