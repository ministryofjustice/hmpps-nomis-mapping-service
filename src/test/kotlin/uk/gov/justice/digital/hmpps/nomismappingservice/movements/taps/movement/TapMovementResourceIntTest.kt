package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.movement

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomismappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application.MovementMappingType
import java.util.UUID

class TapMovementResourceIntTest(
  @Autowired private val movementRepository: TapMovementRepository,
) : IntegrationTestBase() {

  @Nested
  @DisplayName("PUT /mapping/taps/movement")
  inner class UpdateTapMovementMapping {

    @AfterEach
    fun tearDown() = runTest {
      movementRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TapMovementMappingDto(
        "A1234BC",
        12345L,
        12,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        "some description",
        "S1 1AA",
        77L,
      )

      @Test
      fun `should update mapping`() = runTest {
        webTestClient.createTapMovementMapping(mapping)
          .expectStatus().isCreated

        webTestClient.updateTapMovementMapping(
          mapping.copy(
            nomisAddressId = 77777L,
            nomisAddressOwnerClass = "OFF",
            dpsAddressText = "a different address",
          ),
        ).expectStatus().isOk

        with(movementRepository.findByNomisBookingIdAndNomisMovementSeq(12345L, 12)!!) {
          assertThat(offenderNo).isEqualTo("A1234BC")
          assertThat(nomisBookingId).isEqualTo(12345L)
          assertThat(nomisMovementSeq).isEqualTo(12)
          assertThat(dpsMovementId).isEqualTo(mapping.dpsMovementId)
          assertThat(mappingType).isEqualTo(MovementMappingType.NOMIS_CREATED)
          assertThat(nomisAddressId).isEqualTo(77777L)
          assertThat(nomisAddressOwnerClass).isEqualTo("OFF")
          assertThat(dpsAddressText).isEqualTo("a different address")
        }
      }
    }

    @Nested
    inner class Validation {
      val mapping = TapMovementMappingDto(
        "A1234BC",
        12345L,
        12,
        UUID.randomUUID(),
        MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        "some description",
        "S1 1AA",
        77L,
      )

      @Test
      fun `should return not found if mapping does not exist`() = runTest {
        webTestClient.updateTapMovementMapping(mapping)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      val mapping = TapMovementMappingDto(
        "A1234BC",
        12345L,
        12,
        UUID.randomUUID(),
        mappingType = MovementMappingType.NOMIS_CREATED,
        34567L,
        "CORP",
        "some address",
        "some description",
        "S1 1AA",
        77L,
      )

      @BeforeEach
      fun setUp() = runTest {
        webTestClient.createTapMovementMapping(mapping)
          .expectStatus().isCreated
      }

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.put()
          .uri("/mapping/taps/movement")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/mapping/taps/movement")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/mapping/taps/movement")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.createTapMovementMapping(mapping: TapMovementMappingDto) = post()
      .uri("/mapping/taps/movement")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()

    private fun WebTestClient.updateTapMovementMapping(mapping: TapMovementMappingDto) = put()
      .uri("/mapping/taps/movement")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(mapping))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/taps/nomis-id/{bookingId}/{movementSeq}")
  inner class GetTapMovementMappingByNomisId {

    @AfterEach
    fun tearDown() = runTest {
      movementRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TapMovementMapping(
        UUID.randomUUID(),
        12345L,
        12,
        "A1234BC",
        34567L,
        "CORP",
        "some address",
        dpsUprn = 77L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should get external movement mapping by NOMIS booking ID and movement sequence`() = runTest {
        movementRepository.save(mapping)

        webTestClient.getTapMovementMapping(mapping.nomisBookingId, mapping.nomisMovementSeq)
          .expectStatus().isOk
          .expectBody(object : ParameterizedTypeReference<TapMovementMappingDto>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(bookingId).isEqualTo(mapping.nomisBookingId)
            assertThat(nomisMovementSeq).isEqualTo(mapping.nomisMovementSeq)
            assertThat(dpsMovementId).isEqualTo(mapping.dpsMovementId)
            assertThat(prisonerNumber).isEqualTo(mapping.offenderNo)
            assertThat(nomisAddressId).isEqualTo(mapping.nomisAddressId)
            assertThat(nomisAddressOwnerClass).isEqualTo(mapping.nomisAddressOwnerClass)
            assertThat(dpsAddressText).isEqualTo(mapping.dpsAddressText)
            assertThat(mappingType).isEqualTo(mapping.mappingType)
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() = runTest {
        webTestClient.getTapMovementMapping(12345L, 23456)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/taps/movement/nomis-id/12345/23456")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/taps/movement/nomis-id/12345/23456")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/taps/movement/nomis-id/12345/23456")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getTapMovementMapping(bookingId: Long, movementSeq: Int) = get()
      .uri("/mapping/taps/movement/nomis-id/$bookingId/$movementSeq")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("GET /mapping/taps/movement/dps-id/{dpsId}")
  inner class GetTapMovementMappingByDpsId {

    @AfterEach
    fun tearDown() = runTest {
      movementRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping = TapMovementMapping(
        UUID.randomUUID(),
        12345L,
        12,
        "A1234BC",
        34567L,
        "CORP",
        "some address",
        dpsUprn = 77L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should get external movement mapping by DPS ID`() = runTest {
        movementRepository.save(mapping)

        webTestClient.getTapMovementMapping(mapping.dpsMovementId)
          .expectStatus().isOk
          .expectBody(object : ParameterizedTypeReference<TapMovementMappingDto>() {})
          .returnResult().responseBody!!
          .apply {
            assertThat(bookingId).isEqualTo(mapping.nomisBookingId)
            assertThat(nomisMovementSeq).isEqualTo(mapping.nomisMovementSeq)
            assertThat(dpsMovementId).isEqualTo(mapping.dpsMovementId)
            assertThat(prisonerNumber).isEqualTo(mapping.offenderNo)
            assertThat(nomisAddressId).isEqualTo(mapping.nomisAddressId)
            assertThat(nomisAddressOwnerClass).isEqualTo(mapping.nomisAddressOwnerClass)
            assertThat(dpsAddressText).isEqualTo(mapping.dpsAddressText)
            assertThat(mappingType).isEqualTo(mapping.mappingType)
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when mapping does not exist`() = runTest {
        webTestClient.getTapMovementMapping(UUID.randomUUID())
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/taps/movement/dps-id/${UUID.randomUUID()}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/taps/movement/dps-id/${UUID.randomUUID()}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/taps/movement/dps-id/${UUID.randomUUID()}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getTapMovementMapping(dpsId: UUID) = get()
      .uri("/mapping/taps/movement/dps-id/$dpsId")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("DELETE /mapping/taps/movement/nomis-id/{bookingId}/{movementSeq}")
  inner class DeleteTapMovementMapping {

    @AfterEach
    fun tearDown() = runTest {
      movementRepository.deleteAll()
    }

    @Nested
    inner class HappyPath {
      val mapping1 = TapMovementMapping(
        UUID.randomUUID(),
        12345L,
        12,
        "A1234BC",
        34567L,
        "CORP",
        "some address",
        dpsUprn = 77L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )
      val mapping2 = TapMovementMapping(
        UUID.randomUUID(),
        12345L,
        13,
        "A1234BC",
        34567L,
        "CORP",
        "some address",
        dpsUprn = 77L,
        mappingType = MovementMappingType.NOMIS_CREATED,
      )

      @Test
      fun `should delete external movement mapping by NOMIS booking ID and movement sequence`() = runTest {
        movementRepository.save(mapping1)
        movementRepository.save(mapping2)

        webTestClient.deleteTapMovementMapping(mapping1.nomisBookingId, mapping1.nomisMovementSeq)
          .expectStatus().isNoContent

        assertThat(movementRepository.findByNomisBookingIdAndNomisMovementSeq(mapping1.nomisBookingId, mapping1.nomisMovementSeq)).isNull()
        assertThat(movementRepository.findByNomisBookingIdAndNomisMovementSeq(mapping2.nomisBookingId, mapping2.nomisMovementSeq)).isNotNull
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `delete endpoint should be idempotent`() = runTest {
        webTestClient.deleteTapMovementMapping(12345L, 12)
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/taps/movement/nomis-id/12345/12")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/taps/movement/nomis-id/12345/12")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/taps/movement/nomis-id/12345/12")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.deleteTapMovementMapping(bookingId: Long, movementSeq: Int) = delete()
      .uri("/mapping/taps/movement/nomis-id/$bookingId/$movementSeq")
      .headers(setAuthorisation(roles = listOf("NOMIS_MAPPING_API__SYNCHRONISATION__RW")))
      .exchange()
  }
}
