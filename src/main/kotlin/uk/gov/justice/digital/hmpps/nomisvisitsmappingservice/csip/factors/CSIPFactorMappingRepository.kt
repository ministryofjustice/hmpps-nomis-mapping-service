package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CSIPFactorMappingRepository : CoroutineCrudRepository<CSIPFactorMapping, String> {
  suspend fun findOneByNomisCSIPFactorId(nomisCSIPFactorId: Long): CSIPFactorMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: CSIPFactorMappingType): CSIPFactorMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: CSIPFactorMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CSIPFactorMappingType, pageable: Pageable): Flow<CSIPFactorMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: CSIPFactorMappingType)
  suspend fun deleteByDpsCSIPReportId(dpsCSIPReportId: String)
  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String): List<CSIPFactorMapping>
}
