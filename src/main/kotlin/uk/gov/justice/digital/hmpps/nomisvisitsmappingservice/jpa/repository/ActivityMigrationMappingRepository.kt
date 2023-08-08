package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMigrationMapping

@Repository
interface ActivityMigrationMappingRepository : CoroutineCrudRepository<ActivityMigrationMapping, Long> {
  suspend fun findOneByActivityScheduleIdAndActivityScheduleId2(activityScheduleId: Long, activityScheduleId2: Long?): ActivityMigrationMapping?
}
