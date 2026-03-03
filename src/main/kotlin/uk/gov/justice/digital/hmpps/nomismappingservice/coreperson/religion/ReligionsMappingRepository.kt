package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.religion

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ReligionsMappingRepository : CoroutineCrudRepository<CorePersonReligionsMapping, String> {
  suspend fun findOneByNomisPrisonNumber(nomisPrisonNumber: String): CorePersonReligionsMapping?
  suspend fun deleteByNomisPrisonNumber(nomisPrisonNumber: String)
  suspend fun findOneByCprId(cprId: String): CorePersonReligionsMapping?
  suspend fun countAllByLabel(migrationId: String): Long
  suspend fun findAllByLabelOrderByLabelDesc(label: String, pageRequest: Pageable): Flow<CorePersonReligionsMapping>
}
