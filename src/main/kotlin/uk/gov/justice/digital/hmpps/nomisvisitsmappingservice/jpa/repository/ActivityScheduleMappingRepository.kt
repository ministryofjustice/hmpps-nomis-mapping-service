package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityScheduleMapping

@Repository
interface ActivityScheduleMappingRepository : CoroutineCrudRepository<ActivityScheduleMapping, Long> {
  suspend fun findOneByNomisCourseScheduleId(nomisCourseScheduleId: Long): ActivityScheduleMapping?

  fun findAllByActivityScheduleId(activityScheduleId: Long): Flow<ActivityScheduleMapping>

  suspend fun deleteAllByActivityScheduleId(activityScheduleId: Long)
}
