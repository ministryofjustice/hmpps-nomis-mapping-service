package uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.IncentiveMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.IncentiveMappingType

@Repository
interface IncentiveMappingRepository : CoroutineCrudRepository<IncentiveMapping, Long> {
  suspend fun findOneByNomisBookingIdAndNomisIncentiveSequence(bookingId: Long, incentiveSequence: Long): IncentiveMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: IncentiveMappingType): IncentiveMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: IncentiveMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: IncentiveMappingType, pageable: Pageable): Flow<IncentiveMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: IncentiveMappingType): IncentiveMapping?
}
