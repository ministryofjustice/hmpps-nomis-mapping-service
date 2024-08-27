package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CSIPPlanMappingRepository : CoroutineCrudRepository<CSIPPlanMapping, String> {
  suspend fun findOneByNomisCSIPPlanId(nomisCSIPPlanId: Long): CSIPPlanMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: CSIPPlanMappingType): CSIPPlanMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: CSIPPlanMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CSIPPlanMappingType, pageable: Pageable): Flow<CSIPPlanMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: CSIPPlanMappingType)
  suspend fun deleteByDpsCSIPReportId(dpsCSIPReportId: String)
}
