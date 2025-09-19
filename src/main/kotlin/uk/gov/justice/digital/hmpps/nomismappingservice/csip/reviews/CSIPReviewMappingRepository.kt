package uk.gov.justice.digital.hmpps.nomismappingservice.csip.reviews

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomismappingservice.csip.CSIPChildMappingType

@Repository
interface CSIPReviewMappingRepository : CoroutineCrudRepository<CSIPReviewMapping, String> {
  suspend fun findOneByNomisCSIPReviewId(nomisCSIPReviewId: Long): CSIPReviewMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: CSIPChildMappingType): CSIPReviewMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: CSIPChildMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CSIPChildMappingType, pageable: Pageable): Flow<CSIPReviewMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: CSIPChildMappingType)
  suspend fun deleteByDpsCSIPReportId(dpsCSIPReportId: String)
  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String): List<CSIPReviewMapping>
}
