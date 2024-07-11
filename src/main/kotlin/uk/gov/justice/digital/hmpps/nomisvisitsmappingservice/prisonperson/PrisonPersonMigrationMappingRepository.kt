package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PrisonPersonMigrationMappingRepository : CoroutineCrudRepository<PrisonPersonMigrationMapping, String> {
  suspend fun findByNomisPrisonerNumberAndMigrationType(nomisPrisonerNumber: String, migrationType: PrisonPersonMigrationType): PrisonPersonMigrationMapping?
}
