package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.movement

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application.MovementMappingType
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.util.UUID

@DataR2dbcTest
@WithMockAuthUser
class TapMovementMappingRepositoryTest(
  @Autowired private val repository: TapMovementRepository,
) : TestBase() {

  private val dpsId = UUID.randomUUID()
  private val nomisBookingId = 123456L
  private val nomisMovementSeq = 1
  private val offenderNo = "A1234BC"
  private val addressId = 987654L
  private val addressOwnerClass = "CORP"
  private val dpsAddressText = "house, 1 street, town S1 1AA"
  private val dpsUprn = 77L

  @AfterEach
  fun tearDown() = runTest {
    repository.deleteAll()
  }

  @Test
  fun `should save and load mapping`() = runTest {
    repository.save(
      TapMovementMapping(
        dpsId,
        nomisBookingId,
        nomisMovementSeq,
        offenderNo,
        addressId,
        addressOwnerClass,
        dpsAddressText,
        "some_label",
        dpsUprn,
        null,
        null,
        MovementMappingType.MIGRATED,
      ),
    )

    with(repository.findById(dpsId)!!) {
      Assertions.assertThat(dpsMovementId).isEqualTo(dpsId)
      Assertions.assertThat(nomisBookingId).isEqualTo(nomisBookingId)
      Assertions.assertThat(nomisMovementSeq).isEqualTo(nomisMovementSeq)
      Assertions.assertThat(offenderNo).isEqualTo(offenderNo)
      Assertions.assertThat(nomisAddressId).isEqualTo(addressId)
      Assertions.assertThat(nomisAddressOwnerClass).isEqualTo(addressOwnerClass)
      Assertions.assertThat(dpsAddressText).isEqualTo("house, 1 street, town S1 1AA")
      Assertions.assertThat(dpsUprn).isEqualTo(77L)
      Assertions.assertThat(label).isEqualTo("some_label")
      Assertions.assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
    }

    with(repository.findByNomisBookingIdAndNomisMovementSeq(nomisBookingId, nomisMovementSeq)!!) {
      Assertions.assertThat(dpsMovementId).isEqualTo(dpsId)
      Assertions.assertThat(nomisBookingId).isEqualTo(nomisBookingId)
      Assertions.assertThat(nomisMovementSeq).isEqualTo(nomisMovementSeq)
      Assertions.assertThat(offenderNo).isEqualTo(offenderNo)
      Assertions.assertThat(nomisAddressId).isEqualTo(addressId)
      Assertions.assertThat(nomisAddressOwnerClass).isEqualTo(addressOwnerClass)
      Assertions.assertThat(dpsAddressText).isEqualTo("house, 1 street, town S1 1AA")
      Assertions.assertThat(dpsUprn).isEqualTo(77L)
      Assertions.assertThat(label).isEqualTo("some_label")
      Assertions.assertThat(mappingType).isEqualTo(MovementMappingType.MIGRATED)
    }
  }
}
