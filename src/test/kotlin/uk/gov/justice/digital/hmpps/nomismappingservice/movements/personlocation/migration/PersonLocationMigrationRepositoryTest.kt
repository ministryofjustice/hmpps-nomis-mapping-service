package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.migration

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class PersonLocationMigrationRepositoryTest(
  @Autowired private val repository: PersonLocationMigrationRepository,
) : TestBase() {

  private val offenderNo = "A1234BC"

  @AfterEach
  fun tearDown() = runTest {
    repository.deleteAll()
  }

  @Test
  fun `should save and load migration`() = runTest {
    repository.save(PersonLocationMigration(offenderNo, "some_label"))

    with(repository.findById(offenderNo)!!) {
      assertThat(this.offenderNo).isEqualTo(this@PersonLocationMigrationRepositoryTest.offenderNo)
      assertThat(label).isEqualTo("some_label")
      assertThat(whenCreated).isNotNull
    }
  }

  @Test
  fun `should delete migration`() = runTest {
    repository.save(PersonLocationMigration(offenderNo, "some_label"))

    repository.deleteById(offenderNo)

    assertThat(repository.findById(offenderNo)).isNull()
  }
}
