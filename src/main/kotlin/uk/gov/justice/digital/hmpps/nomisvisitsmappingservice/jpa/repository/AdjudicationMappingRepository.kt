package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationMappingType

@Repository
interface AdjudicationMappingRepository : CoroutineCrudRepository<AdjudicationMapping, String> {
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: AdjudicationMappingType): AdjudicationMapping?

  suspend fun findByAdjudicationNumberAndChargeSequence(adjudicationNumber: Long, chargeSequence: Int): AdjudicationMapping?

  suspend fun countAllByLabelAndMappingType(label: String, mappingType: AdjudicationMappingType): Long

  fun findAllByLabelAndMappingTypeOrderByLabelDesc(
    label: String,
    mappingType: AdjudicationMappingType,
    pageable: Pageable,
  ): Flow<AdjudicationMapping>

  suspend fun deleteByLabel(label: String)
  suspend fun deleteAllByMappingType(adjudicationMappingType: AdjudicationMappingType)
}
