package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.NonAssociationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.NonAssociationMappingType

@Repository
interface NonAssociationMappingRepository : CoroutineCrudRepository<NonAssociationMapping, Long> {
  suspend fun findOneByFirstOffenderNoAndSecondOffenderNoAndNomisTypeSequence(firstOffenderNo: String, secondOffenderNo: String, nomisTypeSequence: Int): NonAssociationMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: NonAssociationMappingType): NonAssociationMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: NonAssociationMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: NonAssociationMappingType, pageable: Pageable): Flow<NonAssociationMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: NonAssociationMappingType): NonAssociationMapping?
}
