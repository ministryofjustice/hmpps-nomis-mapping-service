package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.movement

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule.CourtMappingType
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.util.UUID

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class CourtMovementRepositoryTest(
  @Autowired private val repository: CourtMovementRepository,
) : TestBase() {

  private val dpsId = UUID.randomUUID()
  private val bookingId = 54321L
  private val nomisSeq = 3
  private val offenderNo = "A1234BC"

  @AfterEach
  fun tearDown() = runTest {
    repository.deleteAll()
  }

  @Test
  fun `should save and load mapping`() = runTest {
    repository.save(
      CourtMovementMapping(
        dpsId,
        bookingId,
        nomisSeq,
        offenderNo,
        "some_label",
        CourtMappingType.MIGRATED,
      ),
    )

    with(repository.findById(dpsId)!!) {
      assertThat(dpsCourtMovementId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(bookingId)
      assertThat(nomisMovementSeq).isEqualTo(nomisSeq)
      assertThat(offenderNo).isEqualTo(offenderNo)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(CourtMappingType.MIGRATED)
    }

    with(repository.findByNomisBookingIdAndNomisMovementSeq(bookingId, nomisSeq)!!) {
      assertThat(dpsCourtMovementId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(bookingId)
      assertThat(nomisMovementSeq).isEqualTo(nomisSeq)
      assertThat(offenderNo).isEqualTo(offenderNo)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(CourtMappingType.MIGRATED)
    }
  }
}
