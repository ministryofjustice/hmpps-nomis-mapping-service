package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CourtCaseMappingRepository : CoroutineCrudRepository<CourtCaseMapping, String> {
  suspend fun findByNomisCourtCaseId(nomisId: Long): CourtCaseMapping?
  suspend fun findAllByNomisCourtCaseIdIn(nomisCourtCaseIds: List<Long>): Flow<CourtCaseMapping>
  suspend fun findByDpsCourtCaseId(dpsId: String): CourtCaseMapping?
  suspend fun deleteByNomisCourtCaseId(nomisId: Long)
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CourtCaseMappingType, pageRequest: Pageable): Flow<CourtCaseMapping>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: CourtCaseMappingType): Long
}
