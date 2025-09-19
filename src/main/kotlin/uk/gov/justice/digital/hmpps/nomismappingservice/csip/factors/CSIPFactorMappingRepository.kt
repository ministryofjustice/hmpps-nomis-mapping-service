package uk.gov.justice.digital.hmpps.nomismappingservice.csip.factors

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomismappingservice.csip.CSIPChildMappingType

@Repository
interface CSIPFactorMappingRepository : CoroutineCrudRepository<CSIPFactorMapping, String> {
  suspend fun findOneByNomisCSIPFactorId(nomisCSIPFactorId: Long): CSIPFactorMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: CSIPChildMappingType): CSIPFactorMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: CSIPChildMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CSIPChildMappingType, pageable: Pageable): Flow<CSIPFactorMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: CSIPChildMappingType)
  suspend fun deleteByDpsCSIPReportId(dpsCSIPReportId: String)
  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String): List<CSIPFactorMapping>
}
