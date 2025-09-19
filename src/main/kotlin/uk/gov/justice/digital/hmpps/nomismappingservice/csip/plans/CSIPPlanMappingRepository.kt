package uk.gov.justice.digital.hmpps.nomismappingservice.csip.plans

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomismappingservice.csip.CSIPChildMappingType

@Repository
interface CSIPPlanMappingRepository : CoroutineCrudRepository<CSIPPlanMapping, String> {
  suspend fun findOneByNomisCSIPPlanId(nomisCSIPPlanId: Long): CSIPPlanMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: CSIPChildMappingType): CSIPPlanMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: CSIPChildMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CSIPChildMappingType, pageable: Pageable): Flow<CSIPPlanMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: CSIPChildMappingType)
  suspend fun deleteByDpsCSIPReportId(dpsCSIPReportId: String)
  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String): List<CSIPPlanMapping>
}
