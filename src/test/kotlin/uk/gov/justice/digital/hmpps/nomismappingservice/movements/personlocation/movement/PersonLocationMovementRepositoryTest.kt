package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.movement

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
class PersonLocationMovementRepositoryTest(
  @Autowired private val repository: PersonLocationMovementRepository,
) : TestBase() {

  private val dpsId = UUID.randomUUID()
  private val bookingId = 54321L
  private val nomisSeq = 3

  @AfterEach
  fun tearDown() = runTest {
    repository.deleteAll()
  }

  @Test
  fun `should save and load mapping`() = runTest {
    repository.save(
      PersonLocationMovementMapping(
        dpsId,
        bookingId,
        nomisSeq,
        "some_label",
        PersonLocationMappingType.MIGRATED,
      ),
    )

    with(repository.findById(dpsId)!!) {
      assertThat(dpsExternalMovementId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(bookingId)
      assertThat(nomisMovementSeq).isEqualTo(nomisSeq)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(PersonLocationMappingType.MIGRATED)
      assertThat(whenCreated).isNotNull
    }

    with(repository.findByNomisBookingIdAndNomisMovementSeq(bookingId, nomisSeq)!!) {
      assertThat(dpsExternalMovementId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(bookingId)
      assertThat(nomisMovementSeq).isEqualTo(nomisSeq)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(PersonLocationMappingType.MIGRATED)
    }
  }

  @Test
  fun `should update mapping`() = runTest {
    repository.save(
      PersonLocationMovementMapping(
        dpsId,
        bookingId,
        nomisSeq,
        "some_label",
        PersonLocationMappingType.MIGRATED,
      ),
    )

    val saved = repository.findById(dpsId)!!
    repository.save(saved.copy(label = "new_label", mappingType = PersonLocationMappingType.NOMIS_CREATED))

    with(repository.findById(dpsId)!!) {
      assertThat(dpsExternalMovementId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(bookingId)
      assertThat(nomisMovementSeq).isEqualTo(nomisSeq)
      assertThat(label).isEqualTo("new_label")
      assertThat(mappingType).isEqualTo(PersonLocationMappingType.NOMIS_CREATED)
    }
  }

  @Test
  fun `should delete mappings by NOMIS booking ids`() = runTest {
    val sameBookingDpsId = UUID.randomUUID()
    val otherBookingDpsId = UUID.randomUUID()
    val untouchedDpsId = UUID.randomUUID()
    repository.save(PersonLocationMovementMapping(dpsId, bookingId, 1, "some_label", PersonLocationMappingType.MIGRATED))
    repository.save(PersonLocationMovementMapping(sameBookingDpsId, bookingId, 2, "some_label", PersonLocationMappingType.MIGRATED))
    repository.save(PersonLocationMovementMapping(otherBookingDpsId, 54322L, 1, "some_label", PersonLocationMappingType.MIGRATED))
    repository.save(PersonLocationMovementMapping(untouchedDpsId, 54323L, 1, "some_label", PersonLocationMappingType.MIGRATED))

    repository.deleteByNomisBookingIdIn(listOf(bookingId, 54322L))

    assertThat(repository.findById(dpsId)).isNull()
    assertThat(repository.findById(sameBookingDpsId)).isNull()
    assertThat(repository.findById(otherBookingDpsId)).isNull()
    assertThat(repository.findById(untouchedDpsId)).isNotNull
  }
}
