package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonerperson.identifyingmarks

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkImageMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkImageMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkMappingRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class IdentifyingMarksResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var identifyingMarkMappingRepository: IdentifyingMarkMappingRepository

  @Autowired
  private lateinit var identifyingMarkImageMappingRepository: IdentifyingMarkImageMappingRepository

  // TODO temporary tests to check JPA mappings - remove these when we test some actual functionality
  @Nested
  inner class TestJpa {
    @Nested
    inner class IdentifyingMarkMappingJpa {
      private suspend fun createMapping() =
        IdentifyingMarkMapping(
          nomisBookingId = Random.nextLong(),
          nomisMarksSequence = Random.nextLong(),
          dpsId = UUID.randomUUID(),
          offenderNo = "A134AA",
          label = "some_label",
          whenCreated = LocalDateTime.now(),
          mappingType = "some_mapping_type",
        ).let {
          identifyingMarkMappingRepository.save(it)
        }

      @Test
      fun `should create and load identifying marks mappings by NOMIS id`() = runTest {
        val (nomisBookingId, nomisMarksSequence) = createMapping()

        with(identifyingMarkMappingRepository.findByNomisBookingIdAndNomisMarksSequence(nomisBookingId, nomisMarksSequence)!!) {
          assertThat(this.nomisBookingId).isEqualTo(nomisBookingId)
          assertThat(this.nomisMarksSequence).isEqualTo(nomisMarksSequence)
          assertThat(dpsId).isNotNull
          assertThat(offenderNo).isEqualTo("A134AA")
          assertThat(label).isEqualTo("some_label")
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(mappingType).isEqualTo("some_mapping_type")
        }
      }

      @Test
      fun `should create and load identifying marks mappings by DPS id`() = runTest {
        val dpsId = createMapping().dpsId

        with(identifyingMarkMappingRepository.findByDpsId(dpsId)!!) {
          assertThat(nomisBookingId).isNotNull()
          assertThat(nomisMarksSequence).isNotNull()
          assertThat(this.dpsId).isEqualTo(dpsId)
          assertThat(offenderNo).isEqualTo("A134AA")
          assertThat(label).isEqualTo("some_label")
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(mappingType).isEqualTo("some_mapping_type")
        }
      }
    }

    @Nested
    inner class IdentifyingMarkImageMappingJpa {
      private suspend fun createMapping() =
        IdentifyingMarkImageMapping(
          nomisOffenderImageId = Random.nextLong(),
          dpsId = UUID.randomUUID(),
          nomisBookingId = Random.nextLong(),
          nomisMarksSequence = Random.nextLong(),
          offenderNo = "A134AA",
          label = "some_label",
          whenCreated = LocalDateTime.now(),
          mappingType = "some_mapping_type",
        ).let {
          identifyingMarkImageMappingRepository.save(it)
        }

      @Test
      fun `should create and load identifying marks mappings by NOMIS id`() = runTest {
        val nomisOffenderImageId = createMapping().nomisOffenderImageId

        with(identifyingMarkImageMappingRepository.findById(nomisOffenderImageId)!!) {
          assertThat(this.nomisOffenderImageId).isEqualTo(nomisOffenderImageId)
          assertThat(nomisBookingId).isNotNull()
          assertThat(nomisMarksSequence).isNotNull()
          assertThat(dpsId).isNotNull
          assertThat(offenderNo).isEqualTo("A134AA")
          assertThat(label).isEqualTo("some_label")
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(mappingType).isEqualTo("some_mapping_type")
        }
      }

      @Test
      fun `should create and load identifying marks mappings by DPS id`() = runTest {
        val dpsId = createMapping().dpsId

        with(identifyingMarkImageMappingRepository.findByDpsId(dpsId)!!) {
          assertThat(nomisOffenderImageId).isNotNull()
          assertThat(nomisBookingId).isNotNull()
          assertThat(nomisMarksSequence).isNotNull()
          assertThat(this.dpsId).isEqualTo(dpsId)
          assertThat(offenderNo).isEqualTo("A134AA")
          assertThat(label).isEqualTo("some_label")
          assertThat(whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(mappingType).isEqualTo("some_mapping_type")
        }
      }
    }
  }
}
