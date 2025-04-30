package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.corporate

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorporateMappingRepository : CoroutineCrudRepository<CorporateMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): CorporateMapping?
  suspend fun findOneByDpsId(dpsId: String): CorporateMapping?
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CorporateMappingType, pageRequest: Pageable): Flow<CorporateMapping>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: CorporateMappingType): Long
  suspend fun findAllBy(pageRequest: Pageable): Flow<CorporateMapping>
  suspend fun deleteByNomisId(nomisId: Long)
  suspend fun deleteByDpsId(dpsId: String)
}
