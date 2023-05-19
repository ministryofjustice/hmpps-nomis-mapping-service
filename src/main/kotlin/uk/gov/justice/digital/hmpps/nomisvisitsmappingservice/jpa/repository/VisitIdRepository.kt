package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.MappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitId

@Repository
interface VisitIdRepository : CoroutineCrudRepository<VisitId, Long> {
  suspend fun findOneByVsipId(vsipId: String): VisitId?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: MappingType): VisitId?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: MappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: MappingType, pageable: Pageable): Flow<VisitId>
  suspend fun deleteByMappingTypeEquals(mappingType: MappingType): VisitId?
  suspend fun deleteByLabel(label: String)
}
