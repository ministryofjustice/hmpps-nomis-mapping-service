package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.MappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitId

interface VisitIdRepository : CrudRepository<VisitId, Long> {
  fun findOneByVsipId(vsipId: String): VisitId?
  fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: MappingType): VisitId?
  fun countAllByLabelAndMappingType(label: String, mappingType: MappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: MappingType, pageable: Pageable): List<VisitId>
  fun deleteByMappingType(mappingType: MappingType)
}
