package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorePersonMappingRepository : CoroutineCrudRepository<CorePersonMapping, String> {
  suspend fun findOneByNomisPrisonNumber(nomisPrisonNumber: String): CorePersonMapping?
  suspend fun findOneByCprId(cprId: String): CorePersonMapping?
  suspend fun findAllBy(pageRequest: Pageable): Flow<CorePersonMapping>
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CorePersonMappingType, pageRequest: Pageable): Flow<CorePersonMapping>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: CorePersonMappingType): Long
  suspend fun deleteByNomisPrisonNumber(nomisPrisonNumber: String)
}
