package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PrisonPersonMigrationMappingRepository : CoroutineCrudRepository<PrisonPersonMigrationMapping, String> {
  suspend fun findByNomisPrisonerNumberAndMigrationType(nomisPrisonerNumber: String, migrationType: PrisonPersonMigrationType): PrisonPersonMigrationMapping?
  fun findAllByLabelOrderByNomisPrisonerNumberAsc(label: String, pageable: Pageable): Flow<PrisonPersonMigrationMapping>
  suspend fun countAllByLabel(label: String): Long
}
