package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.corporate

import kotlinx.coroutines.flow.count
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestDuplicateErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.isDuplicateMapping
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class CorporateMappingResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var corporateMappingRepository: CorporateMappingRepository

  @Autowired
  private lateinit var corporateAddressMappingRepository: CorporateAddressMappingRepository

  @Autowired
  private lateinit var corporateAddressPhoneMappingRepository: CorporateAddressPhoneMappingRepository

  @Autowired
  private lateinit var corporatePhoneMappingRepository: CorporatePhoneMappingRepository

  @Autowired
  private lateinit var corporateEmailMappingRepository: CorporateEmailMappingRepository

  @Autowired
  private lateinit var corporateWebMappingRepository: CorporateWebMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    corporateWebMappingRepository.deleteAll()
    corporateEmailMappingRepository.deleteAll()
    corporatePhoneMappingRepository.deleteAll()
    corporateAddressMappingRepository.deleteAll()
    corporateAddressPhoneMappingRepository.deleteAll()
    corporateMappingRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST mapping/corporate/migrate")
  inner class CreateMappings {

    @Nested
    inner class Security {
      val mappings = CorporateMappingsDto(
        corporateMapping = CorporateMappingIdDto(
          dpsId = "12345",
          nomisId = 12345L,
        ),
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
        corporateEmailMapping = emptyList(),
        corporateWebMapping = emptyList(),
        corporatePhoneMapping = emptyList(),
        corporateAddressPhoneMapping = emptyList(),
        corporateAddressMapping = emptyList(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/corporate/migrate")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/corporate/migrate")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/corporate/migrate")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingCorporateMapping: CorporateMapping

      val mappings = CorporateMappingsDto(
        corporateMapping = CorporateMappingIdDto(
          dpsId = "54322",
          nomisId = 12345L,
        ),
        label = null,
        mappingType = CorporateMappingType.MIGRATED,
        whenCreated = LocalDateTime.now(),
        corporateWebMapping = emptyList(),
        corporateEmailMapping = emptyList(),
        corporatePhoneMapping = emptyList(),
        corporateAddressPhoneMapping = emptyList(),
        corporateAddressMapping = emptyList(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingCorporateMapping = corporateMappingRepository.save(
          CorporateMapping(
            dpsId = "54321",
            nomisId = 12345L,
            label = "2023-01-01T12:45:12",
            mappingType = CorporateMappingType.MIGRATED,
          ),
        )
        corporatePhoneMappingRepository.save(
          CorporatePhoneMapping(
            dpsId = "65432",
            nomisId = 23456,
            label = "2023-01-01T12:45:12",
            mappingType = CorporateMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same corporate to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/corporate/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will not allow a child of a corporate to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/corporate/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                // unique corporate who has never been migrated
                corporateMapping = CorporateMappingIdDto(
                  dpsId = "87654",
                  nomisId = 45678,
                ),
                // n phone from a different corporate - this would be coding error - a can't happen
                corporatePhoneMapping = listOf(
                  CorporateMappingIdDto(
                    dpsId = "9999",
                    nomisId = 23456,
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/corporate/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          // since this is an untyped map an int will be assumed for such small numbers
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisId", existingCorporateMapping.nomisId.toInt())
            .containsEntry("dpsId", existingCorporateMapping.dpsId)
            .containsEntry("mappingType", existingCorporateMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mappings.corporateMapping.nomisId.toInt())
            .containsEntry("dpsId", mappings.corporateMapping.dpsId)
            .containsEntry("mappingType", existingCorporateMapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mappings = CorporateMappingsDto(
        corporateMapping = CorporateMappingIdDto(
          dpsId = "54321",
          nomisId = 12345L,
        ),
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
        corporateEmailMapping = emptyList(),
        corporateWebMapping = emptyList(),
        corporatePhoneMapping = emptyList(),
        corporateAddressMapping = emptyList(),
        corporateAddressPhoneMapping = emptyList(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the corporate mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mappings.copy(label = "2023-01-01T12:45:12")))
          .exchange()
          .expectStatus().isCreated

        val corporateMapping =
          corporateMappingRepository.findOneByNomisId(mappings.corporateMapping.nomisId)!!

        assertThat(corporateMapping.dpsId).isEqualTo(mappings.corporateMapping.dpsId)
        assertThat(corporateMapping.nomisId).isEqualTo(mappings.corporateMapping.nomisId)
        assertThat(corporateMapping.label).isEqualTo("2023-01-01T12:45:12")
        assertThat(corporateMapping.mappingType).isEqualTo(mappings.mappingType)
        assertThat(corporateMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }

      @Test
      fun `will persist the corporate address mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                corporateAddressMapping = listOf(
                  CorporateMappingIdDto(dpsId = "11", nomisId = 1),
                  CorporateMappingIdDto(dpsId = "22", nomisId = 2),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(corporateAddressMappingRepository.findOneByNomisId(1)!!) {
          assertThat(dpsId).isEqualTo("11")
          assertThat(nomisId).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(corporateAddressMappingRepository.findOneByDpsId("22")!!) {
          assertThat(dpsId).isEqualTo("22")
          assertThat(nomisId).isEqualTo(2L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `will persist the corporate address phone mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                corporateAddressPhoneMapping = listOf(
                  CorporateMappingIdDto(dpsId = "11", nomisId = 1),
                  CorporateMappingIdDto(dpsId = "22", nomisId = 2),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(corporateAddressPhoneMappingRepository.findOneByNomisId(1)!!) {
          assertThat(dpsId).isEqualTo("11")
          assertThat(nomisId).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(corporateAddressPhoneMappingRepository.findOneByDpsId("22")!!) {
          assertThat(dpsId).isEqualTo("22")
          assertThat(nomisId).isEqualTo(2L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `will persist the corporate phone mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                corporatePhoneMapping = listOf(
                  CorporateMappingIdDto(dpsId = "11", nomisId = 1),
                  CorporateMappingIdDto(dpsId = "22", nomisId = 2),
                  CorporateMappingIdDto(dpsId = "33", nomisId = 3),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(corporatePhoneMappingRepository.findOneByNomisId(1)!!) {
          assertThat(dpsId).isEqualTo("11")
          assertThat(nomisId).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(corporatePhoneMappingRepository.findOneByDpsId("22")!!) {
          assertThat(dpsId).isEqualTo("22")
          assertThat(nomisId).isEqualTo(2L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(corporatePhoneMappingRepository.findOneByDpsId("33")!!) {
          assertThat(dpsId).isEqualTo("33")
          assertThat(nomisId).isEqualTo(3L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `will persist the corporate email mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                corporateEmailMapping = listOf(
                  CorporateMappingIdDto(dpsId = "11", nomisId = 1),
                  CorporateMappingIdDto(dpsId = "22", nomisId = 2),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(corporateEmailMappingRepository.findOneByNomisId(1)!!) {
          assertThat(dpsId).isEqualTo("11")
          assertThat(nomisId).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(corporateEmailMappingRepository.findOneByDpsId("22")!!) {
          assertThat(dpsId).isEqualTo("22")
          assertThat(nomisId).isEqualTo(2L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `will persist the corporate web mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              mappings.copy(
                corporateWebMapping = listOf(
                  CorporateMappingIdDto(dpsId = "11", nomisId = 1),
                  CorporateMappingIdDto(dpsId = "22", nomisId = 2),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isCreated

        with(corporateWebMappingRepository.findOneByNomisId(1)!!) {
          assertThat(dpsId).isEqualTo("11")
          assertThat(nomisId).isEqualTo(1L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
        with(corporateWebMappingRepository.findOneByDpsId("22")!!) {
          assertThat(dpsId).isEqualTo("22")
          assertThat(nomisId).isEqualTo(2L)
          assertThat(label).isEqualTo(mappings.label)
          assertThat(mappingType).isEqualTo(mappings.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }
    }
  }

  @DisplayName("GET /mapping/corporate/organisation/migration-id/{migrationId}")
  @Nested
  inner class GetCorporateMappingsByMigrationId {

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/corporate/organisation/migration-id/2022-01-01T00:00:00")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/corporate/organisation/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/corporate/organisation/migration-id/2022-01-01T00:00:00")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `can retrieve all mappings by migration Id`() = runTest {
        (1L..4L).forEach {
          corporateMappingRepository.save(
            CorporateMapping(
              dpsId = "$it",
              nomisId = it,
              label = "2023-01-01T12:45:12",
              mappingType = CorporateMappingType.MIGRATED,
            ),
          )
        }

        corporateMappingRepository.save(
          CorporateMapping(
            dpsId = "54321",
            nomisId = 54321L,
            label = "2022-01-01T12:43:12",
            mappingType = CorporateMappingType.MIGRATED,
          ),
        )

        webTestClient.get().uri("/mapping/corporate/organisation/migration-id/2023-01-01T12:45:12")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
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
      fun `200 response even when no mappings are found`() {
        webTestClient.get().uri("/mapping/corporate/organisation/migration-id/2044-01-01")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(0)
          .jsonPath("content").isEmpty
      }

      @Test
      fun `can request a different page size`() = runTest {
        (1L..6L).forEach {
          corporateMappingRepository.save(
            CorporateMapping(
              dpsId = "$it",
              nomisId = it,
              label = "2023-01-01T12:45:12",
              mappingType = CorporateMappingType.MIGRATED,
            ),
          )
        }
        webTestClient.get().uri {
          it.path("/mapping/corporate/organisation/migration-id/2023-01-01T12:45:12")
            .queryParam("size", "2")
            .queryParam("sort", "nomisId,asc")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(6)
          .jsonPath("numberOfElements").isEqualTo(2)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(3)
          .jsonPath("size").isEqualTo(2)
      }
    }
  }

  @DisplayName("GET /mapping/corporate/organisation/")
  @Nested
  inner class GetAllCorporateMappings {

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get().uri("/mapping/corporate/organisation")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/mapping/corporate/organisation")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/mapping/corporate/organisation")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `can retrieve all mappings`() = runTest {
        (1L..4L).forEach {
          corporateMappingRepository.save(
            CorporateMapping(
              dpsId = "$it",
              nomisId = it,
              label = "2023-01-01T12:45:12",
              mappingType = CorporateMappingType.MIGRATED,
            ),
          )
        }

        corporateMappingRepository.save(
          CorporateMapping(
            dpsId = "12345",
            nomisId = 54321L,
            label = "2022-01-01T12:43:12",
            mappingType = CorporateMappingType.MIGRATED,
          ),
        )

        webTestClient.get().uri("/mapping/corporate/organisation")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(5)
          .jsonPath("$.content..nomisId").value(
            Matchers.contains(
              1,
              2,
              3,
              4,
              54321,
            ),
          )
          .jsonPath("$.content[0].whenCreated").isNotEmpty
      }

      @Test
      fun `can request a different page size`() = runTest {
        (1L..6L).forEach {
          corporateMappingRepository.save(
            CorporateMapping(
              dpsId = "$it",
              nomisId = it,
              label = "2023-01-01T12:45:12",
              mappingType = CorporateMappingType.MIGRATED,
            ),
          )
        }
        webTestClient.get().uri {
          it.path("/mapping/corporate/organisation")
            .queryParam("size", "2")
            .queryParam("sort", "nomisId,asc")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(6)
          .jsonPath("numberOfElements").isEqualTo(2)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(3)
          .jsonPath("size").isEqualTo(2)
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/corporate")
  inner class DeleteAllMappings {
    @BeforeEach
    fun setUp() {
      val mappings = CorporateMappingsDto(
        corporateMapping = CorporateMappingIdDto(
          dpsId = "54321",
          nomisId = 12345L,
        ),
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
        corporateEmailMapping = listOf(
          CorporateMappingIdDto(
            dpsId = "54321",
            nomisId = 12345L,
          ),
        ),
        corporateWebMapping = listOf(
          CorporateMappingIdDto(
            dpsId = "54321",
            nomisId = 12345L,
          ),
        ),
        corporatePhoneMapping = listOf(
          CorporateMappingIdDto(
            dpsId = "54321",
            nomisId = 12345L,
          ),
        ),
        corporateAddressMapping = listOf(
          CorporateMappingIdDto(
            dpsId = "54321",
            nomisId = 12345L,
          ),
        ),
        corporateAddressPhoneMapping = listOf(
          CorporateMappingIdDto(
            dpsId = "54321",
            nomisId = 12345L,
          ),
        ),
      )
      webTestClient.post()
        .uri("/mapping/corporate/migrate")
        .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(mappings))
        .exchange()
        .expectStatus().isCreated
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/corporate")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/corporate")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/corporate")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns 204 when all mappings are deleted`() = runTest {
        assertThat(corporateWebMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(corporateEmailMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(corporatePhoneMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(corporateAddressPhoneMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(corporateAddressMappingRepository.findAll().count()).isEqualTo(1)
        assertThat(corporateMappingRepository.findAll().count()).isEqualTo(1)

        webTestClient.delete()
          .uri("/mapping/corporate")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(corporateWebMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(corporateEmailMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(corporatePhoneMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(corporateAddressPhoneMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(corporateAddressMappingRepository.findAll().count()).isEqualTo(0)
        assertThat(corporateMappingRepository.findAll().count()).isEqualTo(0)
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/corporate/organisation")
  inner class CreateCorporateMapping {

    @Nested
    inner class Security {
      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/corporate/organisation")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/corporate/organisation")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/corporate/organisation")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingCorporateMapping: CorporateMapping

      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingCorporateMapping = corporateMappingRepository.save(
          CorporateMapping(
            dpsId = "12345",
            nomisId = 12345L,
            label = "2023-01-01T12:45:12",
            mappingType = CorporateMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same organisation to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/corporate/organisation")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/corporate/organisation")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          // since this is an untyped map an int will be assumed for such small numbers
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisId", existingCorporateMapping.nomisId.toInt())
            .containsEntry("dpsId", existingCorporateMapping.dpsId)
            .containsEntry("mappingType", existingCorporateMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mapping.nomisId.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = OrganisationsMappingDto(
        dpsId = "12345",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/organisation")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the organisation mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/organisation")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        val corporateMapping =
          corporateMappingRepository.findOneByNomisId(mapping.nomisId)!!

        assertThat(corporateMapping.dpsId).isEqualTo(mapping.dpsId)
        assertThat(corporateMapping.nomisId).isEqualTo(mapping.nomisId)
        assertThat(corporateMapping.label).isNull()
        assertThat(corporateMapping.mappingType).isEqualTo(mapping.mappingType)
        assertThat(corporateMapping.whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/corporate/organisation/nomis-corporate-id/{corporateId}")
  inner class GetCorporateByNomisId {
    private val nomisCorporateId = 12345L
    private lateinit var corporateMapping: CorporateMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateMapping = corporateMappingRepository.save(
        CorporateMapping(
          dpsId = "12345",
          nomisId = nomisCorporateId,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/corporate/organisation/nomis-corporate-id/{corporateId}", nomisCorporateId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/corporate/organisation/nomis-corporate-id/{corporateId}", nomisCorporateId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/corporate/organisation/nomis-corporate-id/{corporateId}", nomisCorporateId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/corporate/organisation/nomis-corporate-id/{corporateId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/corporate/organisation/nomis-corporate-id/{corporateId}", nomisCorporateId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo("12345")
          .jsonPath("nomisId").isEqualTo(nomisCorporateId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/corporate/organisation/dps-organisation-id/{organisationId}")
  inner class GetCorporateByDpsId {
    private val dpsOrganisationId = "12345"
    private lateinit var corporateMapping: CorporateMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateMapping = corporateMappingRepository.save(
        CorporateMapping(
          dpsId = dpsOrganisationId,
          nomisId = 12345L,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/corporate/organisation/dps-organisation-id/{organisationId}", dpsOrganisationId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/corporate/organisation/dps-organisation-id/{organisationId}", dpsOrganisationId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/corporate/organisation/dps-organisation-id/{organisationId}", dpsOrganisationId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/corporate/organisation/dps-organisation-id/{organisationId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/corporate/organisation/dps-organisation-id/{organisationId}", dpsOrganisationId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsOrganisationId)
          .jsonPath("nomisId").isEqualTo(12345)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/corporate/organisation/nomis-corporate-id/{corporateId}")
  inner class DeleteCorporateByNomisId {
    private val nomisCorporateId = 12345L
    private lateinit var corporateMapping: CorporateMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateMapping = corporateMappingRepository.save(
        CorporateMapping(
          dpsId = "12345",
          nomisId = nomisCorporateId,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/corporate/organisation/nomis-corporate-id/{corporateId}", nomisCorporateId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/corporate/organisation/nomis-corporate-id/{corporateId}", nomisCorporateId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/corporate/organisation/nomis-corporate-id/{corporateId}", nomisCorporateId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping not found`() {
        webTestClient.delete()
          .uri("/mapping/corporate/organisation/nomis-corporate-id/{corporateId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping data`() = runTest {
        assertThat(corporateMappingRepository.findOneByNomisId(nomisId = nomisCorporateId)).isNotNull()

        webTestClient.delete()
          .uri("/mapping/corporate/organisation/nomis-corporate-id/{corporateId}", nomisCorporateId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(corporateMappingRepository.findOneByNomisId(nomisId = nomisCorporateId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/corporate/address")
  inner class CreateAddressMapping {

    @Nested
    inner class Security {
      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/corporate/address")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/corporate/address")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/corporate/address")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingMapping: CorporateAddressMapping

      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingMapping = corporateAddressMappingRepository.save(
          CorporateAddressMapping(
            dpsId = "98765",
            nomisId = 12345L,
            label = "2023-01-01T12:45:12",
            mappingType = CorporateMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same address to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/corporate/address")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/corporate/address")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          // since this is an untyped map an int will be assumed for such small numbers
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisId", existingMapping.nomisId.toInt())
            .containsEntry("dpsId", existingMapping.dpsId)
            .containsEntry("mappingType", existingMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mapping.nomisId.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/address")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the address mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/address")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        with(corporateAddressMappingRepository.findOneByNomisId(mapping.nomisId)!!) {
          assertThat(dpsId).isEqualTo(mapping.dpsId)
          assertThat(nomisId).isEqualTo(mapping.nomisId)
          assertThat(label).isNull()
          assertThat(mappingType).isEqualTo(mapping.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/corporate/address/nomis-address-id/{addressId}")
  inner class GetAddressByNomisId {
    private val nomisAddressId = 12345L
    private lateinit var corporateAddressMapping: CorporateAddressMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateAddressMapping = corporateAddressMappingRepository.save(
        CorporateAddressMapping(
          dpsId = "12345",
          nomisId = nomisAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/corporate/address/nomis-address-id/{addressId}", nomisAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/corporate/address/nomis-address-id/{addressId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/corporate/address/nomis-address-id/{addressId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/corporate/address/nomis-address-id/{addressId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/corporate/address/nomis-address-id/{addressId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo("12345")
          .jsonPath("nomisId").isEqualTo(nomisAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/corporate/address/dps-address-id/{addressId}")
  inner class GetAddressByDpsId {
    private val dpsAddressId = "12345"
    private lateinit var corporateAddressMapping: CorporateAddressMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateAddressMapping = corporateAddressMappingRepository.save(
        CorporateAddressMapping(
          dpsId = dpsAddressId,
          nomisId = 12345,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/corporate/address/dps-address-id/{addressId}", dpsAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/corporate/address/dps-address-id/{addressId}", dpsAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/corporate/address/dps-address-id/{addressId}", dpsAddressId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/corporate/address/dps-address-id/{addressId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/corporate/address/dps-address-id/{addressId}", dpsAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsAddressId)
          .jsonPath("nomisId").isEqualTo(12345)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/corporate/address/nomis-address-id/{addressId}")
  inner class DeleteAddressByNomisId {
    private val nomisAddressId = 12345L
    private lateinit var corporateAddressMapping: CorporateAddressMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateAddressMapping = corporateAddressMappingRepository.save(
        CorporateAddressMapping(
          dpsId = "12345",
          nomisId = nomisAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/corporate/address/nomis-address-id/{addressId}", nomisAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/corporate/address/nomis-address-id/{addressId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/corporate/address/nomis-address-id/{addressId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping not found`() {
        webTestClient.delete()
          .uri("/mapping/corporate/address/nomis-address-id/{addressId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping data`() = runTest {
        assertThat(corporateAddressMappingRepository.findOneByNomisId(nomisId = nomisAddressId)).isNotNull()

        webTestClient.delete()
          .uri("/mapping/corporate/address/nomis-address-id/{addressId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(corporateAddressMappingRepository.findOneByNomisId(nomisId = nomisAddressId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/corporate/address-phone")
  inner class CreateAddressAddressPhoneMapping {

    @Nested
    inner class Security {
      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/corporate/address-phone")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/corporate/address-phone")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/corporate/address-phone")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingMapping: CorporateAddressPhoneMapping

      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingMapping = corporateAddressPhoneMappingRepository.save(
          CorporateAddressPhoneMapping(
            dpsId = "98765",
            nomisId = 12345L,
            label = "2023-01-01T12:45:12",
            mappingType = CorporateMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same AddressPhone to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/corporate/address-phone")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/corporate/address-phone")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          // since this is an untyped map an int will be assumed for such small numbers
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisId", existingMapping.nomisId.toInt())
            .containsEntry("dpsId", existingMapping.dpsId)
            .containsEntry("mappingType", existingMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mapping.nomisId.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/address-phone")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the AddressPhone mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/address-phone")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        with(corporateAddressPhoneMappingRepository.findOneByNomisId(mapping.nomisId)!!) {
          assertThat(dpsId).isEqualTo(mapping.dpsId)
          assertThat(nomisId).isEqualTo(mapping.nomisId)
          assertThat(label).isNull()
          assertThat(mappingType).isEqualTo(mapping.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/corporate/address-phone/nomis-phone-id/{phoneId}")
  inner class GetAddressPhoneByNomisId {
    private val nomisAddressId = 12345L
    private lateinit var corporateAddressPhoneMapping: CorporateAddressPhoneMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateAddressPhoneMapping = corporateAddressPhoneMappingRepository.save(
        CorporateAddressPhoneMapping(
          dpsId = "12345",
          nomisId = nomisAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/corporate/address-phone/nomis-phone-id/{phoneId}", nomisAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/corporate/address-phone/nomis-phone-id/{phoneId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/corporate/address-phone/nomis-phone-id/{phoneId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/corporate/address-phone/nomis-phone-id/{phoneId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/corporate/address-phone/nomis-phone-id/{phoneId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo("12345")
          .jsonPath("nomisId").isEqualTo(nomisAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/corporate/address-phone/dps-address-phone-id/{addressPhoneId}")
  inner class GetAddressPhoneByDpsId {
    private val dpsAddressPhoneId = "12345"
    private lateinit var corporateAddressPhoneMapping: CorporateAddressPhoneMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateAddressPhoneMapping = corporateAddressPhoneMappingRepository.save(
        CorporateAddressPhoneMapping(
          dpsId = dpsAddressPhoneId,
          nomisId = 12345,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/corporate/address-phone/dps-address-phone-id/{addressPhoneId}", dpsAddressPhoneId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/corporate/address-phone/dps-address-phone-id/{addressPhoneId}", dpsAddressPhoneId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/corporate/address-phone/dps-address-phone-id/{addressPhoneId}", dpsAddressPhoneId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/corporate/address-phone/dps-address-phone-id/{addressPhoneId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/corporate/address-phone/dps-address-phone-id/{addressPhoneId}", dpsAddressPhoneId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsAddressPhoneId)
          .jsonPath("nomisId").isEqualTo(12345)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/corporate/address-phone/nomis-phone-id/{phoneId}")
  inner class DeleteAddressPhoneByNomisId {
    private val nomisAddressId = 12345L
    private lateinit var corporateAddressPhoneMapping: CorporateAddressPhoneMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateAddressPhoneMapping = corporateAddressPhoneMappingRepository.save(
        CorporateAddressPhoneMapping(
          dpsId = "12345",
          nomisId = nomisAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/corporate/address-phone/nomis-phone-id/{phoneId}", nomisAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/corporate/address-phone/nomis-phone-id/{phoneId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/corporate/address-phone/nomis-phone-id/{phoneId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping not found`() {
        webTestClient.delete()
          .uri("/mapping/corporate/address-phone/nomis-phone-id/{phoneId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the mapping data`() = runTest {
        assertThat(corporateAddressPhoneMappingRepository.findOneByNomisId(nomisAddressId)).isNotNull()
        webTestClient.delete()
          .uri("/mapping/corporate/address-phone/nomis-phone-id/{phoneId}", nomisAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
        assertThat(corporateAddressPhoneMappingRepository.findOneByNomisId(nomisAddressId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/corporate/phone")
  inner class CreatePhoneMapping {

    @Nested
    inner class Security {
      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/corporate/phone")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/corporate/phone")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/corporate/phone")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingMapping: CorporatePhoneMapping

      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingMapping = corporatePhoneMappingRepository.save(
          CorporatePhoneMapping(
            dpsId = "98765",
            nomisId = 12345L,
            label = "2023-01-01T12:45:12",
            mappingType = CorporateMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same phone to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/corporate/phone")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/corporate/phone")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          // since this is an untyped map an int will be assumed for such small numbers
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisId", existingMapping.nomisId.toInt())
            .containsEntry("dpsId", existingMapping.dpsId)
            .containsEntry("mappingType", existingMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mapping.nomisId.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/phone")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the phone mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/phone")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        with(corporatePhoneMappingRepository.findOneByNomisId(mapping.nomisId)!!) {
          assertThat(dpsId).isEqualTo(mapping.dpsId)
          assertThat(nomisId).isEqualTo(mapping.nomisId)
          assertThat(label).isNull()
          assertThat(mappingType).isEqualTo(mapping.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/corporate/phone/nomis-phone-id/{phoneId}")
  inner class GetPhoneByNomisId {
    private val nomisPhoneId = 12345L
    private lateinit var corporatePhoneMapping: CorporatePhoneMapping

    @BeforeEach
    fun setUp() = runTest {
      corporatePhoneMapping = corporatePhoneMappingRepository.save(
        CorporatePhoneMapping(
          dpsId = "12345",
          nomisId = nomisPhoneId,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/corporate/phone/nomis-phone-id/{phoneId}", nomisPhoneId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/corporate/phone/nomis-phone-id/{phoneId}", nomisPhoneId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/corporate/phone/nomis-phone-id/{phoneId}", nomisPhoneId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/corporate/phone/nomis-phone-id/{phoneId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/corporate/phone/nomis-phone-id/{phoneId}", nomisPhoneId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo("12345")
          .jsonPath("nomisId").isEqualTo(nomisPhoneId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/corporate/phone/dps-phone-id/{phoneId}")
  inner class GetPhoneByDpsId {
    private val dpsPhoneId = "12345"
    private lateinit var corporatePhoneMapping: CorporatePhoneMapping

    @BeforeEach
    fun setUp() = runTest {
      corporatePhoneMapping = corporatePhoneMappingRepository.save(
        CorporatePhoneMapping(
          dpsId = dpsPhoneId,
          nomisId = 12345,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/corporate/phone/dps-phone-id/{phoneId}", dpsPhoneId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/corporate/phone/dps-phone-id/{phoneId}", dpsPhoneId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/corporate/phone/dps-phone-id/{phoneId}", dpsPhoneId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/corporate/phone/dps-phone-id/{phoneId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/corporate/phone/dps-phone-id/{phoneId}", dpsPhoneId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsPhoneId)
          .jsonPath("nomisId").isEqualTo(12345)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/corporate/phone/nomis-phone-id/{phoneId}")
  inner class DeletePhoneByNomisId {
    private val nomisPhoneId = 12345L
    private lateinit var corporatePhoneMapping: CorporatePhoneMapping

    @BeforeEach
    fun setUp() = runTest {
      corporatePhoneMapping = corporatePhoneMappingRepository.save(
        CorporatePhoneMapping(
          dpsId = "12345",
          nomisId = nomisPhoneId,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/corporate/phone/nomis-phone-id/{phoneId}", nomisPhoneId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/corporate/phone/nomis-phone-id/{phoneId}", nomisPhoneId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/corporate/phone/nomis-phone-id/{phoneId}", nomisPhoneId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping not found`() {
        webTestClient.delete()
          .uri("/mapping/corporate/phone/nomis-phone-id/{phoneId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() = runTest {
        assertThat(corporatePhoneMappingRepository.findOneByNomisId(nomisPhoneId)).isNotNull
        webTestClient.delete()
          .uri("/mapping/corporate/phone/nomis-phone-id/{phoneId}", nomisPhoneId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
        assertThat(corporatePhoneMappingRepository.findOneByNomisId(nomisPhoneId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/corporate/email")
  inner class CreateEmailMapping {

    @Nested
    inner class Security {
      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/corporate/email")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/corporate/email")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/corporate/email")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingMapping: CorporateEmailMapping

      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingMapping = corporateEmailMappingRepository.save(
          CorporateEmailMapping(
            dpsId = "98765",
            nomisId = 12345L,
            label = "2023-01-01T12:45:12",
            mappingType = CorporateMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same email to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/corporate/email")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/corporate/email")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          // since this is an untyped map an int will be assumed for such small numbers
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisId", existingMapping.nomisId.toInt())
            .containsEntry("dpsId", existingMapping.dpsId)
            .containsEntry("mappingType", existingMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mapping.nomisId.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/email")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the email mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/email")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        with(corporateEmailMappingRepository.findOneByNomisId(mapping.nomisId)!!) {
          assertThat(dpsId).isEqualTo(mapping.dpsId)
          assertThat(nomisId).isEqualTo(mapping.nomisId)
          assertThat(label).isNull()
          assertThat(mappingType).isEqualTo(mapping.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/corporate/email/nomis-internet-address-id/{emailId}")
  inner class GetEmailByNomisId {
    private val nomisInternetAddressId = 12345L
    private lateinit var corporateEmailMapping: CorporateEmailMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateEmailMapping = corporateEmailMappingRepository.save(
        CorporateEmailMapping(
          dpsId = "12345",
          nomisId = nomisInternetAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/corporate/email/nomis-internet-address-id/{emailId}", nomisInternetAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/corporate/email/nomis-internet-address-id/{emailId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/corporate/email/nomis-internet-address-id/{emailId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/corporate/email/nomis-internet-address-id/{emailId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/corporate/email/nomis-internet-address-id/{emailId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo("12345")
          .jsonPath("nomisId").isEqualTo(nomisInternetAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/corporate/email/dps-email-id/{emailId}")
  inner class GetEmailByDpsId {
    private val dpsEmailId = "12345"
    private lateinit var corporateEmailMapping: CorporateEmailMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateEmailMapping = corporateEmailMappingRepository.save(
        CorporateEmailMapping(
          dpsId = dpsEmailId,
          nomisId = 12345,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/corporate/email/dps-email-id/{emailId}", dpsEmailId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/corporate/email/dps-email-id/{emailId}", dpsEmailId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/corporate/email/dps-email-id/{emailId}", dpsEmailId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/corporate/email/dps-email-id/{emailId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/corporate/email/dps-email-id/{emailId}", dpsEmailId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsEmailId)
          .jsonPath("nomisId").isEqualTo(12345)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/corporate/email/nomis-internet-address-id/{emailId}")
  inner class DeleteEmailByNomisId {
    private val nomisInternetAddressId = 12345L
    private lateinit var corporateEmailMapping: CorporateEmailMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateEmailMapping = corporateEmailMappingRepository.save(
        CorporateEmailMapping(
          dpsId = "12345",
          nomisId = nomisInternetAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/corporate/email/nomis-internet-address-id/{emailId}", nomisInternetAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/corporate/email/nomis-internet-address-id/{emailId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/corporate/email/nomis-internet-address-id/{emailId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping not found`() {
        webTestClient.delete()
          .uri("/mapping/corporate/email/nomis-internet-address-id/{emailId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() = runTest {
        assertThat(corporateEmailMappingRepository.findOneByNomisId(nomisInternetAddressId)).isNotNull
        webTestClient.delete()
          .uri("/mapping/corporate/email/nomis-internet-address-id/{emailId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
        assertThat(corporateEmailMappingRepository.findOneByNomisId(nomisInternetAddressId)).isNull()
      }
    }
  }

  @Nested
  @DisplayName("POST /mapping/corporate/web")
  inner class CreateWebMapping {

    @Nested
    inner class Security {
      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `access not authorised when no authority`() {
        webTestClient.post()
          .uri("/mapping/corporate/web")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post()
          .uri("/mapping/corporate/web")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post()
          .uri("/mapping/corporate/web")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      private lateinit var existingMapping: CorporateWebMapping

      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @BeforeEach
      fun setUp() = runTest {
        existingMapping = corporateWebMappingRepository.save(
          CorporateWebMapping(
            dpsId = "98765",
            nomisId = 12345L,
            label = "2023-01-01T12:45:12",
            mappingType = CorporateMappingType.MIGRATED,
          ),
        )
      }

      @Test
      fun `will not allow the same web to have duplicate mappings`() {
        webTestClient.post()
          .uri("/mapping/corporate/web")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
      }

      @Test
      fun `will return details of the existing and duplicate mappings`() {
        val duplicateResponse = webTestClient.post()
          .uri("/mapping/corporate/web")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isDuplicateMapping
          .expectBody(
            object :
              ParameterizedTypeReference<TestDuplicateErrorResponse>() {},
          )
          .returnResult().responseBody

        with(duplicateResponse!!) {
          // since this is an untyped map an int will be assumed for such small numbers
          assertThat(this.moreInfo.existing)
            .containsEntry("nomisId", existingMapping.nomisId.toInt())
            .containsEntry("dpsId", existingMapping.dpsId)
            .containsEntry("mappingType", existingMapping.mappingType.toString())
          assertThat(this.moreInfo.duplicate)
            .containsEntry("nomisId", mapping.nomisId.toInt())
            .containsEntry("dpsId", mapping.dpsId)
            .containsEntry("mappingType", mapping.mappingType.toString())
        }
      }
    }

    @Nested
    inner class HappyPath {
      val mapping = OrganisationsMappingDto(
        dpsId = "54321",
        nomisId = 12345L,
        label = null,
        mappingType = CorporateMappingType.DPS_CREATED,
        whenCreated = LocalDateTime.now(),
      )

      @Test
      fun `returns 201 when mappings created`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/web")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated
      }

      @Test
      fun `will persist the web mapping`() = runTest {
        webTestClient.post()
          .uri("/mapping/corporate/web")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(mapping))
          .exchange()
          .expectStatus().isCreated

        with(corporateWebMappingRepository.findOneByNomisId(mapping.nomisId)!!) {
          assertThat(dpsId).isEqualTo(mapping.dpsId)
          assertThat(nomisId).isEqualTo(mapping.nomisId)
          assertThat(label).isNull()
          assertThat(mappingType).isEqualTo(mapping.mappingType)
          assertThat(whenCreated).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
        }
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/corporate/web/nomis-internet-address-id/{webId}")
  inner class GetWebByNomisId {
    private val nomisInternetAddressId = 12345L
    private lateinit var corporateWebMapping: CorporateWebMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateWebMapping = corporateWebMappingRepository.save(
        CorporateWebMapping(
          dpsId = "12345",
          nomisId = nomisInternetAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/corporate/web/nomis-internet-address-id/{webId}", nomisInternetAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/corporate/web/nomis-internet-address-id/{webId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/corporate/web/nomis-internet-address-id/{webId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/corporate/web/nomis-internet-address-id/{webId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/corporate/web/nomis-internet-address-id/{webId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo("12345")
          .jsonPath("nomisId").isEqualTo(nomisInternetAddressId)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("GET /mapping/corporate/web/dps-web-address-id/{webId}")
  inner class GetWebByDpsId {
    private val dpsWebId = "12345"
    private lateinit var corporateWebMapping: CorporateWebMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateWebMapping = corporateWebMappingRepository.save(
        CorporateWebMapping(
          dpsId = dpsWebId,
          nomisId = 12345,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.get()
          .uri("/mapping/corporate/web/dps-web-address-id/{webId}", dpsWebId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get()
          .uri("/mapping/corporate/web/dps-web-address-id/{webId}", dpsWebId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get()
          .uri("/mapping/corporate/web/dps-web-address-id/{webId}", dpsWebId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 when mapping not found`() {
        webTestClient.get()
          .uri("/mapping/corporate/web/dps-web-address-id/{webId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() {
        webTestClient.get()
          .uri("/mapping/corporate/web/dps-web-address-id/{webId}", dpsWebId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("dpsId").isEqualTo(dpsWebId)
          .jsonPath("nomisId").isEqualTo(12345)
          .jsonPath("label").isEqualTo("2023-01-01T12:45:12")
          .jsonPath("mappingType").isEqualTo("MIGRATED")
          .jsonPath("whenCreated").isEqualTo("2023-01-01T12:45:12")
      }
    }
  }

  @Nested
  @DisplayName("DELETE /mapping/corporate/web/nomis-internet-address-id/{webId}")
  inner class DeleteWebByNomisId {
    private val nomisInternetAddressId = 12345L
    private lateinit var corporateWebMapping: CorporateWebMapping

    @BeforeEach
    fun setUp() = runTest {
      corporateWebMapping = corporateWebMappingRepository.save(
        CorporateWebMapping(
          dpsId = "12345",
          nomisId = nomisInternetAddressId,
          label = "2023-01-01T12:45:12",
          mappingType = CorporateMappingType.MIGRATED,
          whenCreated = LocalDateTime.parse("2023-01-01T12:45:12"),
        ),
      )
    }

    @Nested
    inner class Security {
      @Test
      fun `access not authorised when no authority`() {
        webTestClient.delete()
          .uri("/mapping/corporate/web/nomis-internet-address-id/{webId}", nomisInternetAddressId)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/mapping/corporate/web/nomis-internet-address-id/{webId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/mapping/corporate/web/nomis-internet-address-id/{webId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `204 when mapping not found`() {
        webTestClient.delete()
          .uri("/mapping/corporate/web/nomis-internet-address-id/{webId}", 99999)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the mapping data`() = runTest {
        assertThat(corporateWebMappingRepository.findOneByNomisId(nomisInternetAddressId)).isNotNull

        webTestClient.delete()
          .uri("/mapping/corporate/web/nomis-internet-address-id/{webId}", nomisInternetAddressId)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
        assertThat(corporateWebMappingRepository.findOneByNomisId(nomisInternetAddressId)).isNull()
      }
    }
  }
}
