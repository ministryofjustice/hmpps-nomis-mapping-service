package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class TemporaryAbsenceAddressMappingRepositoryTest(
  @Autowired private val repository: TemporaryAbsenceAddressRepository,
) : TestBase() {

  private val nomisAddressId = 12345L
  private val nomisAddressOwnerClass = "CORP"
  private val nomisOffenderNo = null
  private val dpsAddressText = "DPS address text"
  private val dpsUprn = null

  @AfterEach
  fun tearDown() = runTest {
    repository.deleteAll()
  }

  @Test
  fun `should prevent duplicate address mappings with null columns`() = runTest {
    repository.save(
      TemporaryAbsenceAddressMapping(
        nomisAddressId = nomisAddressId,
        nomisAddressOwnerClass = nomisAddressOwnerClass,
        nomisOffenderNo = nomisOffenderNo,
        dpsAddressText = dpsAddressText,
        dpsUprn = dpsUprn,
      ),
    )

    assertThrows<Exception> {
      repository.save(
        TemporaryAbsenceAddressMapping(
          nomisAddressId = nomisAddressId,
          nomisAddressOwnerClass = nomisAddressOwnerClass,
          nomisOffenderNo = nomisOffenderNo,
          dpsAddressText = dpsAddressText,
          dpsUprn = dpsUprn,
        ),
      )
    }
  }
}
