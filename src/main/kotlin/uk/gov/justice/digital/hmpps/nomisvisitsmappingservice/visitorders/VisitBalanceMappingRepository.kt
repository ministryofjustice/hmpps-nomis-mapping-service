package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.visitorders

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VisitBalanceMappingRepository : CoroutineCrudRepository<VisitBalanceMapping, String> {
  suspend fun findOneByNomisPrisonNumber(nomisPrisonNumber: String): VisitBalanceMapping?
  suspend fun findOneByDpsId(dpsId: String): VisitBalanceMapping?
  suspend fun findAllBy(pageRequest: Pageable): Flow<VisitBalanceMapping>
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: VisitBalanceMappingType, pageRequest: Pageable): Flow<VisitBalanceMapping>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: VisitBalanceMappingType): Long
}
