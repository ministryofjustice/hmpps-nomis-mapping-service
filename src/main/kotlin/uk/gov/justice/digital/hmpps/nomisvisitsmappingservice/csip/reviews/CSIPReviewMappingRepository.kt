package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CSIPReviewMappingRepository : CoroutineCrudRepository<CSIPReviewMapping, String> {
  suspend fun findOneByNomisCSIPReviewId(nomisCSIPReviewId: Long): CSIPReviewMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: CSIPReviewMappingType): CSIPReviewMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: CSIPReviewMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CSIPReviewMappingType, pageable: Pageable): Flow<CSIPReviewMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: CSIPReviewMappingType)
  suspend fun deleteByDpsCSIPReportId(dpsCSIPReportId: String)
}
