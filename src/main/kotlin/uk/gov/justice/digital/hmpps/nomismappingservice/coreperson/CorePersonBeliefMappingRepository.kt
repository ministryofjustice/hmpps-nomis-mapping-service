package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorePersonBeliefMappingRepository : CoroutineCrudRepository<CorePersonBeliefMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): CorePersonBeliefMapping?
  suspend fun findOneByCprId(cprId: String): CorePersonBeliefMapping?
  suspend fun deleteByNomisId(nomisId: Long)
  suspend fun countAllByLabel(migrationId: String): Long
  suspend fun findAllByLabelOrderByLabelDesc(label: String, pageRequest: Pageable): Flow<CorePersonBeliefMapping>
}
