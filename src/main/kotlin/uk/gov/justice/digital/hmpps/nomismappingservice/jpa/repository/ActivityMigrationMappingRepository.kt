package uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.ActivityMigrationMapping

@Repository
interface ActivityMigrationMappingRepository : CoroutineCrudRepository<ActivityMigrationMapping, Long> {
  suspend fun findOneByActivityIdAndActivityId2(activityScheduleId: Long, activityScheduleId2: Long?): ActivityMigrationMapping?

  suspend fun findFirstByOrderByWhenCreatedDesc(): ActivityMigrationMapping?

  fun findAllByLabelOrderByNomisCourseActivityIdAsc(label: String, pageable: Pageable): Flow<ActivityMigrationMapping>

  @Query(
    value = """
      select count(*) from activity_migration_mapping am
      where am.label = :label
      and (:includeIgnored = true or am.activity_id is not null)
    """,
  )
  suspend fun countAllByLabel(label: String, includeIgnored: Boolean): Long
}
