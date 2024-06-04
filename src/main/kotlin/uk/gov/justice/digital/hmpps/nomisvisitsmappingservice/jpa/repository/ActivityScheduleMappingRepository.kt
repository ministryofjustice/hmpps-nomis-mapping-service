package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityScheduleMapping

@Repository
interface ActivityScheduleMappingRepository : CoroutineCrudRepository<ActivityScheduleMapping, Long> {
  suspend fun findOneByNomisCourseScheduleId(nomisCourseScheduleId: Long): ActivityScheduleMapping?

  suspend fun findOneByActivityScheduleIdAndScheduledInstanceId(activityScheduleId: Long, scheduledInstanceId: Long): ActivityScheduleMapping?

  suspend fun findAllByActivityScheduleId(activityScheduleId: Long): List<ActivityScheduleMapping>

  suspend fun deleteAllByActivityScheduleId(activityScheduleId: Long)

  suspend fun deleteByNomisCourseScheduleIdGreaterThan(nomisCourseScheduleId: Long)
}
