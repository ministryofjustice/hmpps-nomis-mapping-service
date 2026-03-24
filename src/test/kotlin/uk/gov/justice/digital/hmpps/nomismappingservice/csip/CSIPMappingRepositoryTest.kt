package uk.gov.justice.digital.hmpps.nomismappingservice.csip

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import uk.gov.justice.digital.hmpps.nomismappingservice.csip.CSIPMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@DataR2dbcTest
@WithMockAuthUser
class CSIPMappingRepositoryTest : TestBase() {
  @Qualifier("CSIPMappingRepository")
  @Autowired
  lateinit var repository: CSIPMappingRepository

  @Test
  fun saveCSIPMapping(): Unit = runBlocking {
    repository.save(
      CSIPMapping(
        dpsCSIPId = "123",
        nomisCSIPId = 456,
        label = "TIMESTAMP",
        mappingType = MIGRATED,
      ),
    )

    val persistedCSIPMappingByCSIPId = repository.findById("123") ?: throw RuntimeException("123L not found")
    with(persistedCSIPMappingByCSIPId) {
      assertThat(dpsCSIPId).isEqualTo("123")
      assertThat(nomisCSIPId).isEqualTo(456L)
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(MIGRATED)
    }

    val persistedCSIPMappingByNomisId = repository.findOneByNomisCSIPId(456L) ?: throw RuntimeException("456L not found")
    with(persistedCSIPMappingByNomisId) {
      assertThat(dpsCSIPId).isEqualTo("123")
      assertThat(nomisCSIPId).isEqualTo(456L)
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(MIGRATED)
    }
  }
}
