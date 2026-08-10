package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.movement

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule.TransferMappingType
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.util.UUID

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class TransferMovementRepositoryTest(
  @Autowired private val repository: TransferMovementRepository,
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
      TransferMovementMapping(
        dpsId,
        bookingId,
        nomisSeq,
        offenderNo,
        "some_label",
        TransferMappingType.MIGRATED,
      ),
    )

    with(repository.findById(dpsId)!!) {
      assertThat(dpsTransferMovementId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(bookingId)
      assertThat(nomisMovementSeq).isEqualTo(nomisSeq)
assertThat(this.offenderNo).isEqualTo(this@TransferMovementRepositoryTest.offenderNo)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(TransferMappingType.MIGRATED)
    }

    with(repository.findByNomisBookingIdAndNomisMovementSeq(bookingId, nomisSeq)!!) {
      assertThat(dpsTransferMovementId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(bookingId)
      assertThat(nomisMovementSeq).isEqualTo(nomisSeq)
      assertThat(offenderNo).isEqualTo(offenderNo)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(TransferMappingType.MIGRATED)
    }
  }

  @Test
  fun `should update mapping`() = runTest {
    repository.save(
      TransferMovementMapping(
        dpsId,
        bookingId,
        nomisSeq,
        offenderNo,
        "some_label",
        TransferMappingType.MIGRATED,
      ),
    )

    val newOffenderNo = "B2345CD"

    val saved = repository.findById(dpsId)!!
    repository.save(saved.copy(offenderNo = newOffenderNo))

    with(repository.findById(dpsId)!!) {
      assertThat(dpsTransferMovementId).isEqualTo(dpsId)
      assertThat(nomisBookingId).isEqualTo(bookingId)
      assertThat(nomisMovementSeq).isEqualTo(nomisSeq)
      assertThat(offenderNo).isEqualTo(newOffenderNo)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(TransferMappingType.MIGRATED)
    }
  }

  @Test
  fun `should delete mapping by NOMIS booking id and movement sequence`() = runTest {
    repository.save(
      TransferMovementMapping(
        dpsId,
        bookingId,
        nomisSeq,
        offenderNo,
        "some_label",
        TransferMappingType.MIGRATED,
      ),
    )

    repository.deleteByNomisBookingIdAndNomisMovementSeq(bookingId, nomisSeq)

    assertThat(repository.findById(dpsId)).isNull()
  }
}
