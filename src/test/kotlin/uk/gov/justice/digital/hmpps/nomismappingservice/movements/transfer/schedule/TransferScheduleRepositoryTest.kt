package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.util.UUID

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class TransferScheduleRepositoryTest(
  @Autowired private val repository: TransferScheduleRepository,
) : TestBase() {

  private val dpsId = UUID.randomUUID()
  private val nomisId = 123456L
  private val offenderNo = "A1234BC"
  private val bookingId = 54321L

  @AfterEach
  fun tearDown() = runTest {
    repository.deleteAll()
  }

  @Test
  fun `should save and load mapping`() = runTest {
    repository.save(
      TransferScheduleMapping(
        dpsId,
        nomisId,
        offenderNo,
        bookingId,
        "some_label",
        TransferMappingType.MIGRATED,
      ),
    )

    with(repository.findById(dpsId)!!) {
      assertThat(dpsTransferScheduleId).isEqualTo(dpsId)
      assertThat(nomisEventId).isEqualTo(nomisId)
      assertThat(offenderNo).isEqualTo(offenderNo)
      assertThat(bookingId).isEqualTo(bookingId)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(TransferMappingType.MIGRATED)
    }

    with(repository.findByNomisEventId(nomisId)!!) {
      assertThat(dpsTransferScheduleId).isEqualTo(dpsId)
      assertThat(nomisEventId).isEqualTo(nomisId)
      assertThat(offenderNo).isEqualTo(offenderNo)
      assertThat(bookingId).isEqualTo(bookingId)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(TransferMappingType.MIGRATED)
    }
  }

  @Test
  fun `should update mapping`() = runTest {
    repository.save(
      TransferScheduleMapping(
        dpsId,
        nomisId,
        offenderNo,
        bookingId,
        "some_label",
        TransferMappingType.MIGRATED,
      ),
    )

    val newNomisId = 999999L
    val newBookingId = 11111L

    val saved = repository.findById(dpsId)!!
    repository.save(saved.copy(nomisEventId = newNomisId, bookingId = newBookingId))

    with(repository.findById(dpsId)!!) {
      assertThat(dpsTransferScheduleId).isEqualTo(dpsId)
      assertThat(nomisEventId).isEqualTo(newNomisId)
      assertThat(bookingId).isEqualTo(newBookingId)
      assertThat(offenderNo).isEqualTo(offenderNo)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(TransferMappingType.MIGRATED)
    }

    assertThat(repository.findByNomisEventId(nomisId)).isNull()
    assertThat(repository.findByNomisEventId(newNomisId)).isNotNull()
  }
}
