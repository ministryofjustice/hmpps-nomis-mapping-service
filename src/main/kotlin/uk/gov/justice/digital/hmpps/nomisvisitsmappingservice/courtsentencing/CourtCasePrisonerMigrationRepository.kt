package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CourtCasePrisonerMigrationRepository : CoroutineCrudRepository<CourtCasePrisonerMigration, String> {
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CourtCaseMappingType, pageRequest: Pageable): Flow<CourtCasePrisonerMigration>

  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: CourtCaseMappingType): Long
}
