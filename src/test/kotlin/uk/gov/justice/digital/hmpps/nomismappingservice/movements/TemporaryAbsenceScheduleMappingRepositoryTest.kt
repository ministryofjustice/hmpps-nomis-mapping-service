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
import java.time.LocalDateTime
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
  private val addressId = 987654L
  private val addressOwnerClass = "CORP"
  private val dpsAddressText = "house, 1 street, town S1 1AA"
  private val eventTime = LocalDateTime.now()

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
        addressId,
        addressOwnerClass,
        dpsAddressText,
        eventTime,
        "some_label",
        MovementMappingType.MIGRATED,
      ),
    )

    with(repository.findById(dpsId)!!) {
      assertThat(dpsOccurrenceId).isEqualTo(dpsId)
      assertThat(nomisEventId).isEqualTo(nomisId)
      assertThat(offenderNo).isEqualTo(offenderNo)
      assertThat(bookingId).isEqualTo(bookingId)
      assertThat(nomisAddressId).isEqualTo(addressId)
      assertThat(nomisAddressOwnerClass).isEqualTo(addressOwnerClass)
      assertThat(dpsAddressText).isEqualTo(dpsAddressText)
      assertThat(eventTime).isEqualTo(eventTime)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
    }

    with(repository.findByNomisEventId(nomisId)!!) {
      assertThat(dpsOccurrenceId).isEqualTo(dpsId)
      assertThat(nomisEventId).isEqualTo(nomisId)
      assertThat(offenderNo).isEqualTo(offenderNo)
      assertThat(bookingId).isEqualTo(bookingId)
      assertThat(nomisAddressId).isEqualTo(addressId)
      assertThat(nomisAddressOwnerClass).isEqualTo(addressOwnerClass)
      assertThat(dpsAddressText).isEqualTo(dpsAddressText)
      assertThat(label).isEqualTo("some_label")
      assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
    }
  }
}
