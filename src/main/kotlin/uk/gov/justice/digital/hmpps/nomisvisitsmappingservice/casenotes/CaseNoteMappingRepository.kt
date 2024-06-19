package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.casenotes

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CaseNoteMappingRepository : CoroutineCrudRepository<CaseNoteMapping, String> {
  suspend fun findOneByNomisCaseNoteId(nomisCaseNoteId: Long): CaseNoteMapping?
  suspend fun findByNomisCaseNoteIdIn(nomisCaseNoteIds: List<Long>): List<CaseNoteMapping>
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: CaseNoteMappingType): CaseNoteMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: CaseNoteMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CaseNoteMappingType, pageable: Pageable): Flow<CaseNoteMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: CaseNoteMappingType): CaseNoteMapping?
  suspend fun deleteByNomisCaseNoteId(nomisCaseNoteId: Long)
}
