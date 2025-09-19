package uk.gov.justice.digital.hmpps.nomismappingservice.finance

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PrisonerBalanceMappingRepository : CoroutineCrudRepository<PrisonerBalanceMapping, String> {
  suspend fun findOneByNomisRootOffenderId(nomisRootOffenderId: Long): PrisonerBalanceMapping?
  suspend fun findOneByDpsId(dpsId: String): PrisonerBalanceMapping?
  suspend fun findAllBy(pageRequest: Pageable): Flow<PrisonerBalanceMapping>
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: PrisonerBalanceMappingType, pageRequest: Pageable): Flow<PrisonerBalanceMapping>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: PrisonerBalanceMappingType): Long
}
