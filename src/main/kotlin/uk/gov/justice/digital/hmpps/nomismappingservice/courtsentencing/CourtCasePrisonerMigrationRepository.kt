package uk.gov.justice.digital.hmpps.nomismappingservice.courtsentencing

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CourtCasePrisonerMigrationRepository : CoroutineCrudRepository<CourtCasePrisonerMigration, String> {
  suspend fun findAllByLabel(label: String, pageRequest: Pageable): Flow<CourtCasePrisonerMigration>

  suspend fun countAllByLabel(label: String): Long
}
