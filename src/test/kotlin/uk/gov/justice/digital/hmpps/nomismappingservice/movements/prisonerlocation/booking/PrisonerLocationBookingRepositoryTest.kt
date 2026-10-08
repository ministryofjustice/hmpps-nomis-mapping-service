package uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.booking

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.PrisonerLocationMappingType
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.util.UUID

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class PrisonerLocationBookingRepositoryTest(
  @Autowired private val repository: PrisonerLocationBookingRepository,
) : TestBase() {

  private val dpsId = UUID.randomUUID()
  private val bookingId = 54321L

  @AfterEach
  fun tearDown() = runTest {
    repository.deleteAll()
  }

  @Test
  fun `should save and load mapping`() = runTest {
    repository.save(
      PrisonerLocationBookingMapping(
        dpsId,
        bookingId,
        "some_label",
        PrisonerLocationMappingType.MIGRATED,
      ),
    )

    with(repository.findById(dpsId)!!) {
      assertThat(dpsCustodialSeriesId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(bookingId)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(PrisonerLocationMappingType.MIGRATED)
      assertThat(whenCreated).isNotNull
    }

    with(repository.findByNomisBookingId(bookingId)!!) {
      assertThat(dpsCustodialSeriesId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(bookingId)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(PrisonerLocationMappingType.MIGRATED)
    }
  }

  @Test
  fun `should update mapping`() = runTest {
    repository.save(
      PrisonerLocationBookingMapping(
        dpsId,
        bookingId,
        "some_label",
        PrisonerLocationMappingType.MIGRATED,
      ),
    )

    val saved = repository.findById(dpsId)!!
    repository.save(saved.copy(label = "new_label", mappingType = PrisonerLocationMappingType.NOMIS_CREATED))

    with(repository.findById(dpsId)!!) {
      assertThat(dpsCustodialSeriesId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(bookingId)
      assertThat(label).isEqualTo("new_label")
      assertThat(mappingType).isEqualTo(PrisonerLocationMappingType.NOMIS_CREATED)
    }
  }

  @Test
  fun `should delete mappings by NOMIS booking ids`() = runTest {
    val otherDpsId = UUID.randomUUID()
    val untouchedDpsId = UUID.randomUUID()
    repository.save(PrisonerLocationBookingMapping(dpsId, bookingId, "some_label", PrisonerLocationMappingType.MIGRATED))
    repository.save(PrisonerLocationBookingMapping(otherDpsId, 54322L, "some_label", PrisonerLocationMappingType.MIGRATED))
    repository.save(PrisonerLocationBookingMapping(untouchedDpsId, 54323L, "some_label", PrisonerLocationMappingType.MIGRATED))

    repository.deleteByNomisBookingIdIn(listOf(bookingId, 54322L))

    assertThat(repository.findById(dpsId)).isNull()
    assertThat(repository.findById(otherDpsId)).isNull()
    assertThat(repository.findById(untouchedDpsId)).isNotNull
  }
}
