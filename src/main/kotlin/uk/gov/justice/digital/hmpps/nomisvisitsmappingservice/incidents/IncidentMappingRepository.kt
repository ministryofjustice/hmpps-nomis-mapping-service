package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.incidents

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface IncidentMappingRepository : CoroutineCrudRepository<IncidentMapping, String> {
  suspend fun findOneByNomisIncidentId(nomisIncidentId: Long): IncidentMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: IncidentMappingType): IncidentMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: IncidentMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: IncidentMappingType, pageable: Pageable): Flow<IncidentMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: IncidentMappingType): IncidentMapping?
}
