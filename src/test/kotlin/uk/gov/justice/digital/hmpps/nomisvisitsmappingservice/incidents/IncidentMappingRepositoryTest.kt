package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.incidents

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.incidents.IncidentMappingType.MIGRATED
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class IncidentMappingRepositoryTest : TestBase() {
  @Qualifier("incidentMappingRepository")
  @Autowired
  lateinit var repository: IncidentMappingRepository

  @Test
  fun saveIncidentMapping(): Unit = runBlocking {
    repository.save(
      IncidentMapping(
        dpsIncidentId = "123",
        nomisIncidentId = 456,
        label = "TIMESTAMP",
        mappingType = MIGRATED,
      ),
    )

    val persistedIncidentMappingByIncidentId = repository.findById("123") ?: throw RuntimeException("123L not found")
    with(persistedIncidentMappingByIncidentId) {
      assertThat(dpsIncidentId).isEqualTo("123")
      assertThat(nomisIncidentId).isEqualTo(456L)
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(MIGRATED)
    }

    val persistedIncidentMappingByNomisId = repository.findOneByNomisIncidentId(456L) ?: throw RuntimeException("456L not found")
    with(persistedIncidentMappingByNomisId) {
      assertThat(dpsIncidentId).isEqualTo("123")
      assertThat(nomisIncidentId).isEqualTo(456L)
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(MIGRATED)
    }
  }
}
