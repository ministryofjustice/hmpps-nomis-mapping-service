package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.IncentiveMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.IncentiveMappingType.MIGRATED

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockUser
class IncentiveMappingRepositoryTest : TestBase() {
  @Qualifier("incentiveMappingRepository")
  @Autowired
  lateinit var repository: IncentiveMappingRepository

  @Test
  fun saveIncentiveMapping(): Unit = runBlocking {

    repository.save(
      IncentiveMapping(
        incentiveId = 123,
        nomisBookingId = 456,
        nomisIncentiveSequence = 3,
        label = "TIMESTAMP",
        mappingType = MIGRATED
      )
    )

    val persistedIncentiveMappingByIncentiveId = repository.findById(123L) ?: throw RuntimeException("123L not found")
    with(persistedIncentiveMappingByIncentiveId) {
      assertThat(incentiveId).isEqualTo(123L)
      assertThat(nomisBookingId).isEqualTo(456L)
      assertThat(nomisIncentiveSequence).isEqualTo(3L)
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(MIGRATED)
    }

    val persistedIncentiveMappingByNomisId = repository.findOneByNomisBookingIdAndNomisIncentiveSequence(456L, 3) ?: throw RuntimeException("123L not found")
    with(persistedIncentiveMappingByNomisId) {
      assertThat(incentiveId).isEqualTo(123L)
      assertThat(nomisBookingId).isEqualTo(456L)
      assertThat(nomisIncentiveSequence).isEqualTo(3L)
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(MIGRATED)
    }
  }
}
