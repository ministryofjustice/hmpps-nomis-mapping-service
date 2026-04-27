package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.isDuplicateMapping
import java.util.UUID

class TapApplicationResourceIntTest(
  @Autowired private val applicationRepository: TapApplicationRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("POST /mapping/taps/application")
  inner class CreateApplicationMapping {

    @AfterEach
    fun tearDown() = runTest {
      applicationRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TapApplicationMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should create mapping`() = runTest {
        webTestClient.createApplicationSyncMapping(mapping)
          .expectStatus().isCreated

        with(applicationRepository.findByNomisApplicationId(23456L)!!) {
          assertThat(offenderNo).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(12345L)
          assertThat(dpsAuthorisationId).isEqualTo(mapping.dpsAuthorisationId)
          assertThat(mappingType).isEqualTo(MovementMappingType.NOMIS_CREATED)
        }
      }
    }

    @Nested
    inner class Validation {
      val mapping = TapApplicationMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
      )
      val duplicateMappingDps = TapApplicationMappingDto(
        "B2345CD",
        56789L,
        34567L,
        mapping.dpsAuthorisationId,
        MovementMappingType.MIGRATED,
      )
      val duplicateMappingNomis = TapApplicationMappingDto(
        "C3456DE",
        9101112L,
        mapping.nomisApplicationId,
        UUID.randomUUID(),
        MovementMappingType.MIGRATED,
      )

      @Test
      fun `should reject duplicate DPS ID mapping`() = runTest {
        webTestClient.createApplicationSyncMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createApplicationSyncMapping(duplicateMappingDps)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsAuthorisationId", mapping.dpsAuthorisationId.toString())
              .containsEntry("nomisApplicationId", mapping.nomisApplicationId.toInt())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingDps.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingDps.bookingId.toInt())
              .containsEntry("dpsAuthorisationId", duplicateMappingDps.dpsAuthorisationId.toString())
              .containsEntry("nomisApplicationId", duplicateMappingDps.nomisApplicationId.toInt())
              .containsEntry("mappingType", duplicateMappingDps.mappingType.toString())
          }
      }

      @Test
      fun `should reject duplicate NOMIS ID mapping`() = runTest {
        webTestClient.createApplicationSyncMapping(mapping)
          .expectStatus().isCreated

        webTestClient.createApplicationSyncMapping(duplicateMappingNomis)
          .expectStatus().isDuplicateMapping
          .expectBody(object : ParameterizedTypeReference<TestDuplicateErrorResponse>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(moreInfo.existing)
              .containsEntry("prisonerNumber", mapping.prisonerNumber)
              .containsEntry("bookingId", mapping.bookingId.toInt())
              .containsEntry("dpsAuthorisationId", mapping.dpsAuthorisationId.toString())
              .containsEntry("nomisApplicationId", mapping.nomisApplicationId.toInt())
              .containsEntry("mappingType", mapping.mappingType.toString())
            assertThat(moreInfo.duplicate)
              .containsEntry("prisonerNumber", duplicateMappingNomis.prisonerNumber)
              .containsEntry("bookingId", duplicateMappingNomis.bookingId.toInt())
              .containsEntry("dpsAuthorisationId", duplicateMappingNomis.dpsAuthorisationId.toString())
              .containsEntry("nomisApplicationId", duplicateMappingNomis.nomisApplicationId.toInt())
              .containsEntry("mappingType", duplicateMappingNomis.mappingType.toString())
          }
      }
    }

    @Nested
    inner class Security {
      val mapping = TapApplicationMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/taps/application")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/taps/application")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/taps/application")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.createApplicationSyncMapping(mapping: TapApplicationMappingDto) = post()
      .uri("/mapping/taps/application")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/taps/application/nomis-id/{nomisApplicationId}")
  inner class GetNomisApplicationMapping {

    @AfterEach
    fun tearDown() = runTest {
      applicationRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TapApplicationMapping(
        UUID.randomUUID(),
        23456L,
        "A1234BC",
        12345L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should get application mapping by NOMIS ID`() = runTest {
        applicationRepository.save(mapping)

        webTestClient.getApplicationSyncMapping(mapping.nomisApplicationId)
          .expectStatus().isOk
          .expectBody(object : ParameterizedTypeReference<TapApplicationMappingDto>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(nomisApplicationId).isEqualTo(mapping.nomisApplicationId)
            assertThat(dpsAuthorisationId).isEqualTo(mapping.dpsAuthorisationId)
            assertThat(prisonerNumber).isEqualTo(mapping.offenderNo)
            assertThat(bookingId).isEqualTo(mapping.bookingId)
            assertThat(mappingType).isEqualTo(mapping.mappingType)
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() = runTest {
        webTestClient.getApplicationSyncMapping(12345L)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      val mapping = TapApplicationMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/taps/application/nomis-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/taps/application/nomis-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/taps/application/nomis-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getApplicationSyncMapping(nomisApplicationId: Long) = get()
      .uri("/mapping/taps/application/nomis-id/$nomisApplicationId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/taps/application/dps-id/{dpsId}")
  inner class GetDpsAuthorisationMapping {

    @AfterEach
    fun tearDown() = runTest {
      applicationRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TapApplicationMapping(
        UUID.randomUUID(),
        23456L,
        "A1234BC",
        12345L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should get application mapping by DPS ID`() = runTest {
        applicationRepository.save(mapping)

        webTestClient.getAuthorisationSyncMapping(mapping.dpsAuthorisationId)
          .expectStatus().isOk
          .expectBody(object : ParameterizedTypeReference<TapApplicationMappingDto>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(nomisApplicationId).isEqualTo(mapping.nomisApplicationId)
            assertThat(dpsAuthorisationId).isEqualTo(mapping.dpsAuthorisationId)
            assertThat(prisonerNumber).isEqualTo(mapping.offenderNo)
            assertThat(bookingId).isEqualTo(mapping.bookingId)
            assertThat(mappingType).isEqualTo(mapping.mappingType)
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() = runTest {
        webTestClient.getAuthorisationSyncMapping(UUID.randomUUID())
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      val mapping = TapApplicationMappingDto(
        "A1234BC",
        12345L,
        23456L,
        UUID.randomUUID(),
        mappingType = MovementMappingType.NOMIS_CREATED,
      )
      val dpsId = UUID.randomUUID()

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/taps/application/dps-id/$dpsId")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/taps/application/dps-id/$dpsId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/taps/application/dps-id/$dpsId")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getAuthorisationSyncMapping(dpsAuthorisationId: UUID) = get()
      .uri("/mapping/taps/application/dps-id/$dpsAuthorisationId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("DELETE /mapping/taps/application/nomis-id/{nomisApplicationId}")
  inner class DeleteNomisApplicationMapping {

    @AfterEach
    fun tearDown() = runTest {
      applicationRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping1 = TapApplicationMapping(
        UUID.randomUUID(),
        23456L,
        "A1234BC",
        12345L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )
      val mapping2 = TapApplicationMapping(
        UUID.randomUUID(),
        65432L,
        "A1234BC",
        12345L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should delete application mapping by NOMIS ID`() = runTest {
        applicationRepository.save(mapping1)
        applicationRepository.save(mapping2)

        webTestClient.deleteApplicationSyncMapping(mapping1.nomisApplicationId)
          .expectStatus().isNoContent

        assertThat(applicationRepository.findByNomisApplicationId(mapping1.nomisApplicationId)).isNull()
        assertThat(applicationRepository.findByNomisApplicationId(mapping2.nomisApplicationId)).isNotNull
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `delete endpoint should be idempotent`() = runTest {
        webTestClient.deleteApplicationSyncMapping(12345L)
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/taps/application/nomis-id/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/taps/application/nomis-id/12345")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/taps/application/nomis-id/12345")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.deleteApplicationSyncMapping(nomisApplicationId: Long) = delete()
      .uri("/mapping/taps/application/nomis-id/$nomisApplicationId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }
}
