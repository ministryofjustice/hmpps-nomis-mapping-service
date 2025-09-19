package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.util.*

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class TemporaryAbsenceMovementMappingRepositoryTest(
  @Autowired private val repository: TemporaryAbsenceMovementRepository,
) : TestBase() {

  private val dpsId = UUID.randomUUID()
  private val nomisBookingId = 123456L
  private val nomisMovementSeq = 1
  private val offenderNo = "A1234BC"

  @AfterEach
  fun tearDown() = runTest {
    repository.deleteAll()
  }

  @Test
  fun `should save and load mapping`() = runTest {
    repository.save(
      TemporaryAbsenceMovementMapping(
        dpsId,
        nomisBookingId,
        nomisMovementSeq,
        offenderNo,
        "some_label",
        MovementMappingType.MIGRATED,
      ),
    )

    with(repository.findById(dpsId)!!) {
      assertThat(dpsMovementId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(nomisBookingId)
      assertThat(nomisMovementSeq).isEqualTo(nomisMovementSeq)
      assertThat(offenderNo).isEqualTo(offenderNo)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
    }

    with(repository.findByNomisBookingIdAndNomisMovementSeq(nomisBookingId, nomisMovementSeq)!!) {
      assertThat(dpsMovementId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(nomisBookingId)
      assertThat(nomisMovementSeq).isEqualTo(nomisMovementSeq)
      assertThat(offenderNo).isEqualTo(offenderNo)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
    }
  }
}
