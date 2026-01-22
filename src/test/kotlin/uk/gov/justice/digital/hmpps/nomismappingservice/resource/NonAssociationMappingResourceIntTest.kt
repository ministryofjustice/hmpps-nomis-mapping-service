package uk.gov.justice.digital.hmpps.nomismappingservice.resource

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.byLessThan
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
import uk.gov.justice.digital.hmpps.nomismappingservice.data.NonAssociationMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.builders.NonAssociationRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.NonAssociationMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.NonAssociationMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.NonAssociationMappingType.NOMIS_CREATED
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository.NonAssociationMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NonAssociationMappingService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val NON_ASSOCIATION_ID = 1234L
private const val FIRST_OFFENDER_NO = "A1234BC"
private const val SECOND_OFFENDER_NO = "D5678EF"
private const val TYPE_SEQUENCE = 1

class NonAssociationMappingResourceIntTest : IntegrationTestBase() {

  @Autowired
  @Qualifier("nonAssociationMappingRepository")
  private lateinit var realNonAssociationMappingRepository: NonAssociationMappingRepository
  private lateinit var nonAssociationMappingRepository: NonAssociationMappingRepository

  @Autowired
  private lateinit var nonAssociationRepository: NonAssociationRepository

  @Autowired
  private lateinit var nonAssociationMappingService: NonAssociationMappingService

  @BeforeEach
  fun setup() {
    nonAssociationMappingRepository =
      mock(defaultAnswer = AdditionalAnswers.delegatesTo(realNonAssociationMappingRepository))
    ReflectionTestUtils.setField(
      nonAssociationMappingService,
      "nonAssociationMappingRepository",
      nonAssociationMappingRepository,
    )
  }

  @AfterEach
  internal fun deleteData() = runBlocking {
    nonAssociationRepository.deleteAll()
  }

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
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createNonAssociationMapping().copy(nonAssociationId = 99)))
          .exchange()
          .expectStatus().isEqualTo(HttpStatus.CONFLICT.value())
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<NonAssociationMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Non-association mapping already exists.\nExisting mapping: NonAssociationMappingDto(nonAssociationId=1234, firstOffenderNo=A1234BC, secondOffenderNo=D5678EF, nomisTypeSequence=1, label=2022-01-01, mappingType=NOMIS_CREATED")
        assertThat(userMessage).contains("Duplicate mapping: NonAssociationMappingDto(nonAssociationId=99, firstOffenderNo=A1234BC, secondOffenderNo=D5678EF, nomisTypeSequence=1, label=2022-01-01, mappingType=NOMIS_CREATED, whenCreated=null")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existingNonAssociation = responseBody.moreInfo.existing
      with(existingNonAssociation) {
        assertThat(nonAssociationId).isEqualTo(1234)
        assertThat(firstOffenderNo).isEqualTo("A1234BC")
        assertThat(secondOffenderNo).isEqualTo("D5678EF")
        assertThat(nomisTypeSequence).isEqualTo(1)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }

      val duplicateNonAssociation = responseBody.moreInfo.duplicate
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createNonAssociationMapping().copy(nomisTypeSequence = 21)))
          .exchange()
          .expectStatus().isEqualTo(409)
          .expectBody(object : ParameterizedTypeReference<DuplicateMappingErrorResponse<NonAssociationMappingDto>>() {})
          .returnResult().responseBody

      with(responseBody!!) {
        assertThat(userMessage).contains("Conflict: Non-association mapping already exists.\nExisting mapping: NonAssociationMappingDto(nonAssociationId=1234, firstOffenderNo=A1234BC, secondOffenderNo=D5678EF, nomisTypeSequence=1, label=2022-01-01, mappingType=NOMIS_CREATED")
        assertThat(userMessage).contains("Duplicate mapping: NonAssociationMappingDto(nonAssociationId=1234, firstOffenderNo=A1234BC, secondOffenderNo=D5678EF, nomisTypeSequence=21, label=2022-01-01, mappingType=NOMIS_CREATED, whenCreated=null")
        assertThat(errorCode).isEqualTo(1409)
      }

      val existingNonAssociation = responseBody.moreInfo.existing
      with(existingNonAssociation) {
        assertThat(nonAssociationId).isEqualTo(1234)
        assertThat(firstOffenderNo).isEqualTo("A1234BC")
        assertThat(secondOffenderNo).isEqualTo("D5678EF")
        assertThat(nomisTypeSequence).isEqualTo(1)
        assertThat(mappingType).isEqualTo("NOMIS_CREATED")
      }

      val duplicateNonAssociation = responseBody.moreInfo.duplicate
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
        webTestClient.get()
          .uri("/mapping/non-associations/first-offender-no/$FIRST_OFFENDER_NO/second-offender-no/$SECOND_OFFENDER_NO/type-sequence/$TYPE_SEQUENCE")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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

      val mapping2 = webTestClient.get().uri("/mapping/non-associations/non-association-id/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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

  @DisplayName("GET /mapping/non-associations/first-offender-no/{firstOffenderNo}/second-offender-no/{secondOffenderNo}/type-sequence/{typeSequence}")
  @Nested
  inner class GetNomisMappingTest {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/$FIRST_OFFENDER_NO/second-offender-no/$SECOND_OFFENDER_NO/type-sequence/$TYPE_SEQUENCE")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/$FIRST_OFFENDER_NO/second-offender-no/$SECOND_OFFENDER_NO/type-sequence/$TYPE_SEQUENCE")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/$FIRST_OFFENDER_NO/second-offender-no/$SECOND_OFFENDER_NO/type-sequence/$TYPE_SEQUENCE")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createNonAssociationMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping =
        webTestClient.get()
          .uri("/mapping/non-associations/first-offender-no/$FIRST_OFFENDER_NO/second-offender-no/$SECOND_OFFENDER_NO/type-sequence/$TYPE_SEQUENCE")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
      val error = webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/$FIRST_OFFENDER_NO/second-offender-no/$SECOND_OFFENDER_NO/type-sequence/999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: Non-association with firstOffenderNo=A1234BC, secondOffenderNo=D5678EF, and nomisTypeSequence=999 not found")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createNonAssociationMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/$FIRST_OFFENDER_NO/second-offender-no/$SECOND_OFFENDER_NO/type-sequence/$TYPE_SEQUENCE")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /mapping/non-associations/non-association-id/{nonAssociationId}")
  @Nested
  inner class GetNonAssociationMappingTest {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/non-associations/non-association-id/$NON_ASSOCIATION_ID")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/non-associations/non-association-id/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/non-associations/non-association-id/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get mapping success`() {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createNonAssociationMapping()))
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/non-associations/non-association-id/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
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
      val error = webTestClient.get().uri("/mapping/non-associations/non-association-id/765")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: nonAssociationId=765")
    }

    @Test
    fun `get mapping success with update role`() {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createNonAssociationMapping()))
        .exchange()
        .expectStatus().isCreated

      webTestClient.get().uri("/mapping/non-associations/non-association-id/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /mapping/non-associations/migration-id/{migrationId}")
  @Nested
  inner class GetMappingByMigrationIdTest {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/migration-id/2022-01-01T00:00:00")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/non-associations/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get nonAssociation mappings by migration id forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/non-associations/migration-id/2022-01-01T00:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get nonAssociation mappings by migration id success`() {
      (1L..4L).forEach {
        postCreateNonAssociationMappingRequest(
          nonAssociationId = it,
          nomisTypeSequence = it.toInt(),
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      (5L..9L).forEach {
        postCreateNonAssociationMappingRequest(
          nonAssociationId = it,
          nomisTypeSequence = it.toInt(),
          label = "2099-01-01",
          mappingType = "MIGRATED",
        )
      }
      postCreateNonAssociationMappingRequest(
        nonAssociationId = 12,
        nomisTypeSequence = 12,
        mappingType = NonAssociationMappingType.NON_ASSOCIATION_CREATED.name,
      )

      webTestClient.get().uri("/mapping/non-associations/migration-id/2022-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("$.content..nonAssociationId").value<List<Int>> { assertThat(it).contains(1, 2, 3, 4) }
        .jsonPath("$.content..nomisTypeSequence").value<List<Int>> { assertThat(it).contains(1, 2, 3, 4) }
        .jsonPath("$.content[0].whenCreated").isNotEmpty
    }

    @Test
    fun `get nonAssociation mappings by migration id - no records exist`() {
      (1L..4L).forEach {
        postCreateNonAssociationMappingRequest(
          nonAssociationId = it,
          nomisTypeSequence = it.toInt(),
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }

      webTestClient.get().uri("/mapping/non-associations/migration-id/2044-01-01")
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
        postCreateNonAssociationMappingRequest(
          nonAssociationId = it,
          nomisTypeSequence = it.toInt(),
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/non-associations/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("sort", "firstOffenderNo,asc")
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
        postCreateNonAssociationMappingRequest(
          nonAssociationId = it,
          nomisTypeSequence = it.toInt(),
          label = "2022-01-01",
          mappingType = "MIGRATED",
        )
      }
      webTestClient.get().uri {
        it.path("/mapping/non-associations/migration-id/2022-01-01")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .queryParam("sort", "firstOffenderNo,asc")
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

  @DisplayName("GET /mapping/non-associations/migrated/latest")
  @Nested
  inner class GeMappingMigratedLatestTest {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/non-associations/migrated/latest")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/non-associations/migrated/latest")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/non-associations/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get retrieves latest migrated mapping`() {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createNonAssociationMapping(
              nonAssociationId = 10,
              nomisTypeSequence = 2,
              label = "2022-01-01T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createNonAssociationMapping(
              nonAssociationId = 20,
              nomisTypeSequence = 4,
              label = "2022-01-02T00:00:00",
              mappingType = "MIGRATED",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createNonAssociationMapping(
              nonAssociationId = 1,
              nomisTypeSequence = 1,
              label = "2022-01-02T10:00:00",
              mappingType = MIGRATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createNonAssociationMapping(
              nonAssociationId = 99,
              nomisTypeSequence = 3,
              label = "whatever",
              mappingType = NOMIS_CREATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val mapping = webTestClient.get().uri("/mapping/non-associations/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody(NonAssociationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nonAssociationId).isEqualTo(1)
      assertThat(mapping.firstOffenderNo).isEqualTo("A1234BC")
      assertThat(mapping.secondOffenderNo).isEqualTo("D5678EF")
      assertThat(mapping.nomisTypeSequence).isEqualTo(1)
      assertThat(mapping.label).isEqualTo("2022-01-02T10:00:00")
      assertThat(mapping.mappingType).isEqualTo("MIGRATED")
      assertThat(mapping.whenCreated).isCloseTo(LocalDateTime.now(), byLessThan(5, ChronoUnit.SECONDS))
    }

    @Test
    fun `404 when no migrated mapping found`() {
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            createNonAssociationMapping(
              nonAssociationId = 77,
              nomisTypeSequence = 7,
              label = "whatever",
              mappingType = NOMIS_CREATED.name,
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      val error = webTestClient.get().uri("/mapping/non-associations/migrated/latest")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!

      assertThat(error.userMessage).isEqualTo("Not Found: No migrated mapping found")
    }
  }

  @DisplayName("DELETE /mapping/non-associations/non-association-id/{nonAssociationId}")
  @Nested
  inner class DeleteMappingTest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/mapping/non-associations/non-association-id/999")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/mapping/non-associations/non-association-id/999")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/mapping/non-associations/non-association-id/999")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `delete specific mapping success`() {
      // create mapping
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createNonAssociationMapping()))
        .exchange()
        .expectStatus().isCreated

      // it is present after creation by nonAssociation id
      webTestClient.get().uri("/mapping/non-associations/non-association-id/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
      // it is also present after creation by nomis id
      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/$FIRST_OFFENDER_NO/second-offender-no/$SECOND_OFFENDER_NO/type-sequence/$TYPE_SEQUENCE")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk

      // delete mapping
      webTestClient.delete().uri("/mapping/non-associations/non-association-id/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNoContent

      // no longer present by nonAssociation id
      webTestClient.get().uri("/mapping/non-associations/non-association-id/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
      // and also no longer present by nomis id
      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/$FIRST_OFFENDER_NO/second-offender-no/$SECOND_OFFENDER_NO/type-sequence/$TYPE_SEQUENCE")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    internal fun `delete is idempotent`() {
      // create mapping
      webTestClient.post().uri("/mapping/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createNonAssociationMapping()))
        .exchange()
        .expectStatus().isCreated

      // delete mapping
      webTestClient.delete().uri("/mapping/non-associations/non-association-id/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNoContent
      // delete mapping second time still returns success
      webTestClient.delete().uri("/mapping/non-associations/non-association-id/$NON_ASSOCIATION_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @DisplayName("PUT /mapping/non-associations/merge/from/{firstOffenderNo}/to/{secondOffenderNo}")
  @Nested
  inner class MergeTest {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/mapping/non-associations/merge/from/$FIRST_OFFENDER_NO/to/$SECOND_OFFENDER_NO")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/mapping/non-associations/merge/from/$FIRST_OFFENDER_NO/to/$SECOND_OFFENDER_NO")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.put().uri("/mapping/non-associations/merge/from/$FIRST_OFFENDER_NO/to/$SECOND_OFFENDER_NO")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Merge success`() {
      postCreateNonAssociationMappingRequest(
        nonAssociationId = 1,
        firstOffenderNo = "A1234AA",
        secondOffenderNo = "A1234AB",
      )
      postCreateNonAssociationMappingRequest(
        nonAssociationId = 2,
        firstOffenderNo = "A1234AC",
        secondOffenderNo = "A1234AD",
      )

      webTestClient.put().uri("/mapping/non-associations/merge/from/A1234AA/to/B5678BB")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk

      // existing NA has gone
      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/A1234AA/second-offender-no/A1234AB/type-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound

      // NA now has new offender
      val mapping = webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/B5678BB/second-offender-no/A1234AB/type-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody(NonAssociationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nonAssociationId).isEqualTo(1)
      assertThat(mapping.firstOffenderNo).isEqualTo("B5678BB")
      assertThat(mapping.secondOffenderNo).isEqualTo("A1234AB")

      // other NA is unaffected
      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/A1234AC/second-offender-no/A1234AD/type-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Nothing happens if not found`() {
      postCreateNonAssociationMappingRequest(
        nonAssociationId = 1,
        firstOffenderNo = "A1234AA",
        secondOffenderNo = "A1234AB",
      )

      webTestClient.put().uri("/mapping/non-associations/merge/from/Z1234ZZ/to/B5678BB")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk

      val mapping = webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/A1234AA/second-offender-no/A1234AB/type-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody(NonAssociationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nonAssociationId).isEqualTo(1)
      assertThat(mapping.firstOffenderNo).isEqualTo("A1234AA")
      assertThat(mapping.secondOffenderNo).isEqualTo("A1234AB")
      assertThat(mapping.nomisTypeSequence).isEqualTo(TYPE_SEQUENCE)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED.name)
    }

    @Test
    fun `Error if it would result in both prisoners being the same`() {
      postCreateNonAssociationMappingRequest(
        nonAssociationId = 1,
        firstOffenderNo = "A1234AA",
        secondOffenderNo = "A1234AB",
      )

      webTestClient.put().uri("/mapping/non-associations/merge/from/A1234AA/to/A1234AB")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).matches(
            "Validation failure: Found NA clash in NonAssociationMapping\\(nonAssociationId=1, firstOffenderNo=A1234AA, secondOffenderNo=A1234AB, nomisTypeSequence=1, label=2022-01-01, mappingType=NOMIS_CREATED, new=false, whenCreated=.+\\) when updating offender id from A1234AA to A1234AB",
          )
        }

      // existing NA unaffected
      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/A1234AA/second-offender-no/A1234AB/type-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Merge success - multiple candidates`() {
      postCreateNonAssociationMappingRequest(
        nonAssociationId = 1,
        firstOffenderNo = "A1234AA",
        secondOffenderNo = "A1234AB",
      )
      postCreateNonAssociationMappingRequest(
        nonAssociationId = 2,
        firstOffenderNo = "A1234AC",
        secondOffenderNo = "A1234AA",
      )

      webTestClient.put().uri("/mapping/non-associations/merge/from/A1234AA/to/B5678BB")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk

      // existing NAs have gone
      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/A1234AA/second-offender-no/A1234AB/type-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/A1234AC/second-offender-no/A1234AA/type-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound

      // NAs now have new offenders
      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/B5678BB/second-offender-no/A1234AB/type-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody(NonAssociationMappingDto::class.java)
        .returnResult().responseBody!!
        .apply {
          assertThat(nonAssociationId).isEqualTo(1)
          assertThat(firstOffenderNo).isEqualTo("B5678BB")
          assertThat(secondOffenderNo).isEqualTo("A1234AB")
        }
      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/A1234AC/second-offender-no/B5678BB/type-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody(NonAssociationMappingDto::class.java)
        .returnResult().responseBody!!
        .apply {
          assertThat(nonAssociationId).isEqualTo(2)
          assertThat(firstOffenderNo).isEqualTo("A1234AC")
          assertThat(secondOffenderNo).isEqualTo("B5678BB")
        }
    }
  }

  @DisplayName("PUT /mapping/non-associations/update-list/from/{oldOffenderNo}/to/{newOffenderNo}")
  @Nested
  inner class BookingMovedTest {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/mapping/non-associations/update-list/from/$FIRST_OFFENDER_NO/to/$SECOND_OFFENDER_NO")
        .bodyValue(listOf("A1234AB", "A1234AC"))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/mapping/non-associations/update-list/from/$FIRST_OFFENDER_NO/to/$SECOND_OFFENDER_NO")
        .headers(setAuthorisation(roles = listOf()))
        .bodyValue(listOf("A1234AB", "A1234AC"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.put().uri("/mapping/non-associations/update-list/from/$FIRST_OFFENDER_NO/to/$SECOND_OFFENDER_NO")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .bodyValue(listOf("A1234AB", "A1234AC"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Move success`() {
      postCreateNonAssociationMappingRequest(
        nonAssociationId = 1,
        firstOffenderNo = "A1234AA",
        secondOffenderNo = "A1234AB",
      )
      postCreateNonAssociationMappingRequest(
        nonAssociationId = 2,
        firstOffenderNo = "A1234AC",
        secondOffenderNo = "A1234AA",
      )

      webTestClient.put().uri("/mapping/non-associations/update-list/from/A1234AA/to/B5678BB")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .bodyValue(
          listOf("A1234AB", "A1234AC"),
        )
        .exchange()
        .expectStatus().isOk

      // existing NAs have gone
      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/A1234AA/second-offender-no/A1234AB/type-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/A1234AC/second-offender-no/A1234AA/type-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound

      // NAs now have new offender
      val mapping = webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/B5678BB/second-offender-no/A1234AB/type-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody(NonAssociationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nonAssociationId).isEqualTo(1)
      assertThat(mapping.firstOffenderNo).isEqualTo("B5678BB")
      assertThat(mapping.secondOffenderNo).isEqualTo("A1234AB")

      webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/A1234AC/second-offender-no/B5678BB/type-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Nothing happens if not found`() {
      postCreateNonAssociationMappingRequest(
        nonAssociationId = 1,
        firstOffenderNo = "A1234AA",
        secondOffenderNo = "A1234AB",
      )

      webTestClient.put().uri("/mapping/non-associations/update-list/from/Z1234ZZ/to/B5678BB")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .bodyValue(
          listOf("A1234AB", "A1234AC"),
        )
        .exchange()
        .expectStatus().isOk

      val mapping = webTestClient.get()
        .uri("/mapping/non-associations/first-offender-no/A1234AA/second-offender-no/A1234AB/type-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody(NonAssociationMappingDto::class.java)
        .returnResult().responseBody!!

      assertThat(mapping.nonAssociationId).isEqualTo(1)
      assertThat(mapping.firstOffenderNo).isEqualTo("A1234AA")
      assertThat(mapping.secondOffenderNo).isEqualTo("A1234AB")
      assertThat(mapping.nomisTypeSequence).isEqualTo(TYPE_SEQUENCE)
      assertThat(mapping.label).isEqualTo("2022-01-01")
      assertThat(mapping.mappingType).isEqualTo(NOMIS_CREATED.name)
    }

    @Test
    fun `Error if it would result in both prisoners being the same - old offender`() {
      postCreateNonAssociationMappingRequest(
        nonAssociationId = 1,
        firstOffenderNo = "A1234AA",
        secondOffenderNo = "A1234AB",
      )

      webTestClient.put().uri("/mapping/non-associations/update-list/from/A1234AA/to/A1234AB")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .bodyValue(listOf("A1234AA"))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).matches(
            "Validation failure: Old offenderNo is in the list, when updating offender id from A1234AA to A1234AB",
          )
        }
    }

    @Test
    fun `Error if it would result in both prisoners being the same - new offender`() {
      postCreateNonAssociationMappingRequest(
        nonAssociationId = 1,
        firstOffenderNo = "A1234AA",
        secondOffenderNo = "A1234AB",
      )

      webTestClient.put().uri("/mapping/non-associations/update-list/from/A1234AA/to/A1234AB")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .bodyValue(
          listOf("A1234AB"),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).matches(
            "Validation failure: New offenderNo is in the list, when updating offender id from A1234AA to A1234AB",
          )
        }
    }
  }

  @DisplayName("GET /find/common-between/{oldOffenderNo}/and/{newOffenderNo")
  @Nested
  inner class FindCommonTest {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/mapping/non-associations/find/common-between/$FIRST_OFFENDER_NO/and/$SECOND_OFFENDER_NO")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/mapping/non-associations/find/common-between/$FIRST_OFFENDER_NO/and/$SECOND_OFFENDER_NO")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/mapping/non-associations/find/common-between/$FIRST_OFFENDER_NO/and/$SECOND_OFFENDER_NO")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `one common 3rd party - 1`() {
      postCreateNonAssociationMappingRequest(1, "A1234AA", "COMMON")
      postCreateNonAssociationMappingRequest(2, "COMMON", "A1234AB")

      assertResultList(
        listOf(
          dto(1, "A1234AA", "COMMON", 1),
          dto(2, "COMMON", "A1234AB", 1),
        ),
      )
    }

    @Test
    fun `one common 3rd party - 2`() {
      postCreateNonAssociationMappingRequest(1, "A1234AA", "COMMON")
      postCreateNonAssociationMappingRequest(2, "A1234AB", "COMMON")

      assertResultList(
        listOf(
          dto(1, "A1234AA", "COMMON", 1),
          dto(2, "A1234AB", "COMMON", 1),
        ),
      )
    }

    @Test
    fun `one common 3rd party - 3`() {
      postCreateNonAssociationMappingRequest(1, "COMMON", "A1234AA")
      postCreateNonAssociationMappingRequest(2, "COMMON", "A1234AB")

      assertResultList(
        listOf(
          dto(1, "COMMON", "A1234AA", 1),
          dto(2, "COMMON", "A1234AB", 1),
        ),
      )
    }

    @Test
    fun `one common 3rd party - 4`() {
      postCreateNonAssociationMappingRequest(1, "COMMON", "A1234AA", 3)
      postCreateNonAssociationMappingRequest(2, "A1234AB", "COMMON", 3)

      assertResultList(
        listOf(
          dto(1, "COMMON", "A1234AA", 3),
          dto(2, "A1234AB", "COMMON", 3),
        ),
      )
    }

    @Test
    fun `a common 3rd party but with different sequences`() {
      postCreateNonAssociationMappingRequest(1, "COMMON", "A1234AA", 1)
      postCreateNonAssociationMappingRequest(2, "A1234AA", "COMMON", 2)
      postCreateNonAssociationMappingRequest(3, "A1234AB", "COMMON", 3)
      postCreateNonAssociationMappingRequest(4, "COMMON", "A1234AB", 4)

      assertResultList(emptyList())
    }

    @Test
    fun `multiple common 3rd parties`() {
      postCreateNonAssociationMappingRequest(1, "COMMON1", "A1234AA")
      postCreateNonAssociationMappingRequest(2, "A1234AB", "COMMON1")
      postCreateNonAssociationMappingRequest(3, "COMMON2", "A1234AA")
      postCreateNonAssociationMappingRequest(4, "A1234AB", "COMMON2")
      postCreateNonAssociationMappingRequest(5, "A1234AA", "OTHER-A")
      postCreateNonAssociationMappingRequest(6, "A1234AB", "OTHER-B")

      assertResultList(
        listOf(
          dto(1, "COMMON1", "A1234AA", 1),
          dto(2, "A1234AB", "COMMON1", 1),
          dto(3, "COMMON2", "A1234AA", 1),
          dto(4, "A1234AB", "COMMON2", 1),
        ),
      )
    }

    private fun assertResultList(expected: List<NonAssociationMappingDto>) {
      val actual = webTestClient.get()
        .uri("/mapping/non-associations/find/common-between/A1234AA/and/A1234AB")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody(object : ParameterizedTypeReference<List<NonAssociationMappingDto>>() {})
        .returnResult()
        .responseBody!!

      assertThat(actual).hasSize(expected.size)
      assertThat(actual).matches(
        { list ->
          !list.mapIndexed { index, dto ->
            dto.nonAssociationId == expected[index].nonAssociationId &&
              dto.firstOffenderNo == expected[index].firstOffenderNo &&
              dto.secondOffenderNo == expected[index].secondOffenderNo &&
              dto.nomisTypeSequence == expected[index].nomisTypeSequence
          }
            .contains(false)
        },
        "expected:\n$expected",
      )
    }

    @Test
    fun `different sequence values`() {
      postCreateNonAssociationMappingRequest(1, "COMMON", "A1234AA", 1)
      postCreateNonAssociationMappingRequest(2, "A1234AA", "COMMON", 2)
      postCreateNonAssociationMappingRequest(3, "A1234AB", "COMMON", 3)
      postCreateNonAssociationMappingRequest(4, "COMMON", "A1234AB", 4)

      assertResultList(emptyList())
    }
  }

  @DisplayName("PUT /non-association-id/{nonAssociationId}/sequence/{newSequence}")
  @Nested
  inner class ResetSequenceTest {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/mapping/non-associations/non-association-id/123456/sequence/2")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/mapping/non-associations/non-association-id/123456/sequence/2")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.put().uri("/mapping/non-associations/non-association-id/123456/sequence/2")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `sets sequence value correctly`() {
      postCreateNonAssociationMappingRequest(1, "A1234AA", "A1234AB", 1)
      postCreateNonAssociationMappingRequest(2, "A1234AA", "A1234AB", 2)

      webTestClient.put().uri("/mapping/non-associations/non-association-id/1/sequence/3")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk

      //  NA sequence has changed
      webTestClient.get()
        .uri("/mapping/non-associations/non-association-id/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("nomisTypeSequence").isEqualTo(3)

      // The other NA has not changed
      webTestClient.get()
        .uri("/mapping/non-associations/non-association-id/2")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("nomisTypeSequence").isEqualTo(2)
    }
  }
}

private fun dto(
  nonAssociationId: Long,
  firstOffenderNo: String,
  secondOffenderNo: String,
  nomisTypeSequence: Int,
) = NonAssociationMappingDto(
  nonAssociationId,
  firstOffenderNo,
  secondOffenderNo,
  nomisTypeSequence,
  null,
  "",
)
