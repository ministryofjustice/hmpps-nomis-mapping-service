package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CSIPMappingRepository : CoroutineCrudRepository<CSIPMapping, String> {
  suspend fun findOneByNomisCSIPId(nomisCSIPId: Long): CSIPMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: CSIPMappingType): CSIPMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: CSIPMappingType): Long
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CSIPMappingType, pageable: Pageable): Flow<CSIPMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: CSIPMappingType): CSIPMapping?

  suspend fun deleteAllByOffenderNo(offenderNo: String)
  suspend fun findAllByOffenderNoOrderByNomisCSIPIdAsc(offenderNo: String): List<CSIPMapping>
}
