package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.visitorders

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VisitOrderBalanceMappingRepository : CoroutineCrudRepository<VisitOrderBalanceMapping, String> {
  suspend fun findOneByNomisPrisonNumber(nomisPrisonNumber: String): VisitOrderBalanceMapping?
  suspend fun findOneByDpsId(dpsId: String): VisitOrderBalanceMapping?
  suspend fun findAllBy(pageRequest: Pageable): Flow<VisitOrderBalanceMapping>
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: VisitOrderBalanceMappingType, pageRequest: Pageable): Flow<VisitOrderBalanceMapping>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: VisitOrderBalanceMappingType): Long
}
