package uk.gov.justice.digital.hmpps.nomismappingservice.staff

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType.NOMIS_CREATED
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@DataR2dbcTest
@WithMockAuthUser
class StaffMappingRepositoryTest : TestBase() {
  @Autowired
  private lateinit var repository: StaffMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    repository.deleteAll()
  }

  @Test
  fun saveMappingByNomisId(): Unit = runBlocking {
    repository.save(
      StaffMapping(
        dpsId = "123",
        nomisId = 456,
        label = "TIMESTAMP",
        mappingType = NOMIS_CREATED,
      ),
    )

    val persistedMappingByNomisId = repository.findOneByNomisId(456L) ?: throw RuntimeException("456L not found")
    with(persistedMappingByNomisId) {
      assertThat(dpsId).isEqualTo("123")
      assertThat(nomisId).isEqualTo(456L)
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(NOMIS_CREATED)
    }
  }
}
