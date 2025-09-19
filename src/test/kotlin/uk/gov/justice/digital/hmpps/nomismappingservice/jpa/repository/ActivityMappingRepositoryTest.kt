package uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.ActivityMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.ActivityMappingType
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class ActivityMappingRepositoryTest : TestBase() {
  @Qualifier("activityMappingRepository")
  @Autowired
  lateinit var repository: ActivityMappingRepository

  @Test
  fun saveMapping(): Unit = runBlocking {
    repository.save(
      ActivityMapping(
        activityScheduleId = 123,
        nomisCourseActivityId = 456,
        mappingType = ActivityMappingType.ACTIVITY_CREATED,
      ),
    )

    val persistedMappingById = repository.findById(123L) ?: throw RuntimeException("123L not found")
    with(persistedMappingById) {
      assertThat(activityScheduleId).isEqualTo(123L)
      assertThat(nomisCourseActivityId).isEqualTo(456L)
      assertThat(mappingType).isEqualTo(ActivityMappingType.ACTIVITY_CREATED)
    }

    val persistedMappingByNomisId = repository.findOneByNomisCourseActivityId(456L) ?: throw RuntimeException("456L not found")
    with(persistedMappingByNomisId) {
      assertThat(activityScheduleId).isEqualTo(123L)
      assertThat(nomisCourseActivityId).isEqualTo(456L)
      assertThat(mappingType).isEqualTo(ActivityMappingType.ACTIVITY_CREATED)
    }
  }
}
