package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentenceAdjustmentMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingMappingType

@Repository
interface SentenceAdjustmentMappingRepository : CoroutineCrudRepository<SentenceAdjustmentMapping, Long> {
  suspend fun findOneByNomisSentenceAdjustmentId(nomisSentenceAdjustmentId: Long): SentenceAdjustmentMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: SentencingMappingType): SentenceAdjustmentMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: SentencingMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: SentencingMappingType, pageable: Pageable): Flow<SentenceAdjustmentMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: SentencingMappingType): SentenceAdjustmentMapping?
}
