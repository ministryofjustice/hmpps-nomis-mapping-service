package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.migration

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

// Throwaway test to exercise saving/loading the new entity - to be superseded once full migration logic lands
@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class TransferSchedulerMigrationRepositoryTest(
  @Autowired private val repository: TransferSchedulerMigrationRepository,
) : TestBase() {

  private val offenderNo = "A1234BC"

  @AfterEach
  fun tearDown() = runTest {
    repository.deleteAll()
  }

  @Test
  fun `should save and load mapping`() = runTest {
    repository.save(
      TransferSchedulerMigration(
        offenderNo = offenderNo,
        label = "some_label",
      ),
    )

    with(repository.findById(offenderNo)!!) {
      assertThat(offenderNo).isEqualTo(this@TransferSchedulerMigrationRepositoryTest.offenderNo)
      assertThat(label).isEqualTo("some_label")
    }
  }
}
