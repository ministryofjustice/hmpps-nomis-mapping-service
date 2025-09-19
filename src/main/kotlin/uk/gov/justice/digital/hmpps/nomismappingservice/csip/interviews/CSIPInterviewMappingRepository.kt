package uk.gov.justice.digital.hmpps.nomismappingservice.csip.interviews

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomismappingservice.csip.CSIPChildMappingType

@Repository
interface CSIPInterviewMappingRepository : CoroutineCrudRepository<CSIPInterviewMapping, String> {
  suspend fun findOneByNomisCSIPInterviewId(nomisCSIPInterviewId: Long): CSIPInterviewMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: CSIPChildMappingType): CSIPInterviewMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: CSIPChildMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CSIPChildMappingType, pageable: Pageable): Flow<CSIPInterviewMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: CSIPChildMappingType)
  suspend fun deleteByDpsCSIPReportId(dpsCSIPReportId: String)
  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String): List<CSIPInterviewMapping>
}
