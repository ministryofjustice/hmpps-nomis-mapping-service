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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMappingType

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockUser
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
        label = "some_label",
      ),
    )

    val persistedMappingById = repository.findById(123L) ?: throw RuntimeException("123L not found")
    with(persistedMappingById) {
      assertThat(activityScheduleId).isEqualTo(123L)
      assertThat(nomisCourseActivityId).isEqualTo(456L)
      assertThat(mappingType).isEqualTo(ActivityMappingType.ACTIVITY_CREATED)
      assertThat(label).isEqualTo("some_label")
    }

    val persistedMappingByNomisId = repository.findOneByNomisCourseActivityId(456L) ?: throw RuntimeException("456L not found")
    with(persistedMappingByNomisId) {
      assertThat(activityScheduleId).isEqualTo(123L)
      assertThat(nomisCourseActivityId).isEqualTo(456L)
      assertThat(mappingType).isEqualTo(ActivityMappingType.ACTIVITY_CREATED)
      assertThat(label).isEqualTo("some_label")
    }
  }
}
