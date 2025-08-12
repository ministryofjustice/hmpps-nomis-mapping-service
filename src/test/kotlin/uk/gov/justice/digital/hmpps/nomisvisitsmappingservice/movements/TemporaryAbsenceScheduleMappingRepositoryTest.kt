package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.movements

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestBase
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.util.*

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class TemporaryAbsenceScheduleMappingRepositoryTest(
  @Autowired private val repository: TemporaryAbsenceScheduleRepository,
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
      TemporaryAbsenceScheduleMapping(
        dpsId,
        nomisId,
        offenderNo,
        bookingId,
        "some_label",
        MovementMappingType.MIGRATED,
      ),
    )

    with(repository.findById(dpsId)!!) {
      assertThat(dpsScheduleId).isEqualTo(dpsId)
      assertThat(nomisScheduleId).isEqualTo(nomisId)
      assertThat(offenderNo).isEqualTo(offenderNo)
      assertThat(bookingId).isEqualTo(bookingId)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
    }

    with(repository.findByNomisScheduleId(nomisId)!!) {
      assertThat(dpsScheduleId).isEqualTo(dpsId)
      assertThat(nomisScheduleId).isEqualTo(nomisId)
      assertThat(offenderNo).isEqualTo(offenderNo)
      assertThat(bookingId).isEqualTo(bookingId)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
    }
  }
}
