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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityScheduleMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityScheduleMappingType

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockUser
class ActivityScheduleMappingRepositoryTest : TestBase() {
  @Autowired
  @Qualifier("activityScheduleMappingRepository")
  lateinit var repository: ActivityScheduleMappingRepository

  @Test
  fun saveMapping(): Unit = runBlocking {
    repository.save(
      ActivityScheduleMapping(
        scheduledInstanceId = 123,
        nomisCourseScheduleId = 456,
        mappingType = ActivityScheduleMappingType.ACTIVITY_CREATED,
        activityScheduleId = 1,
      ),
    )

    val persistedMappingById = repository.findById(123L) ?: throw RuntimeException("123L not found")
    with(persistedMappingById) {
      assertThat(activityScheduleId).isEqualTo(1L)
      assertThat(scheduledInstanceId).isEqualTo(123L)
      assertThat(nomisCourseScheduleId).isEqualTo(456L)
      assertThat(mappingType).isEqualTo(ActivityScheduleMappingType.ACTIVITY_CREATED)
    }

    val persistedMappingByNomisId = repository.findOneByNomisCourseScheduleId(456L) ?: throw RuntimeException("456L not found")
    with(persistedMappingByNomisId) {
      assertThat(activityScheduleId).isEqualTo(1L)
      assertThat(scheduledInstanceId).isEqualTo(123L)
      assertThat(nomisCourseScheduleId).isEqualTo(456L)
      assertThat(mappingType).isEqualTo(ActivityScheduleMappingType.ACTIVITY_CREATED)
    }
  }
}
