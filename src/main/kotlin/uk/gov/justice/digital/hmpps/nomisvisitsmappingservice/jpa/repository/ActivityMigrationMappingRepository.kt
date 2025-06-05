package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMigrationMapping

@Repository
interface ActivityMigrationMappingRepository : CoroutineCrudRepository<ActivityMigrationMapping, Long> {
  suspend fun findOneByActivityIdAndActivityId2(activityScheduleId: Long?, activityScheduleId2: Long?): ActivityMigrationMapping?

  suspend fun findFirstByOrderByWhenCreatedDesc(): ActivityMigrationMapping?

  fun findAllByLabelOrderByNomisCourseActivityIdAsc(label: String, pageable: Pageable): Flow<ActivityMigrationMapping>

  suspend fun countAllByLabel(label: String): Long
}
