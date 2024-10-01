package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CSIPInterviewMappingRepository : CoroutineCrudRepository<CSIPInterviewMapping, String> {
  suspend fun findOneByNomisCSIPInterviewId(nomisCSIPInterviewId: Long): CSIPInterviewMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: CSIPInterviewMappingType): CSIPInterviewMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: CSIPInterviewMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CSIPInterviewMappingType, pageable: Pageable): Flow<CSIPInterviewMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: CSIPInterviewMappingType)
  suspend fun deleteByDpsCSIPReportId(dpsCSIPReportId: String)
  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String): List<CSIPInterviewMapping>
}
