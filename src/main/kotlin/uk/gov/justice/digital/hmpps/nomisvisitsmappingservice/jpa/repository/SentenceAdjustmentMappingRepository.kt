package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingAdjustmentMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingMappingType

@Repository
interface SentenceAdjustmentMappingRepository : CoroutineCrudRepository<SentencingAdjustmentMapping, String> {
  suspend fun findOneByNomisAdjustmentIdAndNomisAdjustmentCategory(
    nomisAdjustmentId: Long,
    nomisAdjustmentCategory: String,
  ): SentencingAdjustmentMapping?

  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: SentencingMappingType): SentencingAdjustmentMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: SentencingMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(
    label: String,
    mappingType: SentencingMappingType,
    pageable: Pageable,
  ): Flow<SentencingAdjustmentMapping>

  fun findAllBy(
    pageable: Pageable,
  ): Flow<SentencingAdjustmentMapping>

  suspend fun deleteByMappingTypeEquals(mappingType: SentencingMappingType): SentencingAdjustmentMapping?
}
