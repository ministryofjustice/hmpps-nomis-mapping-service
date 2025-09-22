package uk.gov.justice.digital.hmpps.nomismappingservice.finance

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PrisonBalanceMappingRepository : CoroutineCrudRepository<PrisonBalanceMapping, String> {
  suspend fun findOneByNomisId(nomisId: String): PrisonBalanceMapping?
  suspend fun findOneByDpsId(dpsId: String): PrisonBalanceMapping?
  suspend fun findAllBy(pageRequest: Pageable): Flow<PrisonBalanceMapping>
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: PrisonBalanceMappingType, pageRequest: Pageable): Flow<PrisonBalanceMapping>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: PrisonBalanceMappingType): Long
}
