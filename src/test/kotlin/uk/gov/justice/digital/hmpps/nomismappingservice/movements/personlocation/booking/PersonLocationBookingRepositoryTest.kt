package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.booking

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.PersonLocationMappingType
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.util.UUID

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class PersonLocationBookingRepositoryTest(
  @Autowired private val repository: PersonLocationBookingRepository,
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
      PersonLocationBookingMapping(
        dpsId,
        bookingId,
        "some_label",
        PersonLocationMappingType.MIGRATED,
      ),
    )

    with(repository.findById(dpsId)!!) {
      assertThat(dpsCustodialSeriesId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(bookingId)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(PersonLocationMappingType.MIGRATED)
      assertThat(whenCreated).isNotNull
    }

    with(repository.findByNomisBookingId(bookingId)!!) {
      assertThat(dpsCustodialSeriesId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(bookingId)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(PersonLocationMappingType.MIGRATED)
    }
  }

  @Test
  fun `should update mapping`() = runTest {
    repository.save(
      PersonLocationBookingMapping(
        dpsId,
        bookingId,
        "some_label",
        PersonLocationMappingType.MIGRATED,
      ),
    )

    val saved = repository.findById(dpsId)!!
    repository.save(saved.copy(label = "new_label", mappingType = PersonLocationMappingType.NOMIS_CREATED))

    with(repository.findById(dpsId)!!) {
      assertThat(dpsCustodialSeriesId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(bookingId)
      assertThat(label).isEqualTo("new_label")
      assertThat(mappingType).isEqualTo(PersonLocationMappingType.NOMIS_CREATED)
    }
  }

  @Test
  fun `should delete mappings by NOMIS booking ids`() = runTest {
    val otherDpsId = UUID.randomUUID()
    val untouchedDpsId = UUID.randomUUID()
    repository.save(PersonLocationBookingMapping(dpsId, bookingId, "some_label", PersonLocationMappingType.MIGRATED))
    repository.save(PersonLocationBookingMapping(otherDpsId, 54322L, "some_label", PersonLocationMappingType.MIGRATED))
    repository.save(PersonLocationBookingMapping(untouchedDpsId, 54323L, "some_label", PersonLocationMappingType.MIGRATED))

    repository.deleteByNomisBookingIdIn(listOf(bookingId, 54322L))

    assertThat(repository.findById(dpsId)).isNull()
    assertThat(repository.findById(otherDpsId)).isNull()
    assertThat(repository.findById(untouchedDpsId)).isNotNull
  }
}
