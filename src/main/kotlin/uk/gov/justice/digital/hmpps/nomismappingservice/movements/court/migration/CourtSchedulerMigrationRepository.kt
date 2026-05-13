package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.migration

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CourtSchedulerMigrationRepository : CoroutineCrudRepository<CourtSchedulerMigration, String> {
  suspend fun countAllByLabel(migrationId: String): Long
  suspend fun findAllByLabelOrderByLabelDesc(label: String, pageRequest: Pageable): Flow<CourtSchedulerMigration>
}
