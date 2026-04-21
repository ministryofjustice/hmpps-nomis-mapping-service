package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.migration

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TapMigrationRepository : CoroutineCrudRepository<TapMigration, String> {
  suspend fun countAllByLabel(migrationId: String): Long
  suspend fun findAllByLabelOrderByLabelDesc(label: String, pageRequest: Pageable): Flow<TapMigration>
}
