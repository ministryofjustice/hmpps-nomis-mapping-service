package uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.AdjudicationHearingMapping

@Repository
interface AdjudicationHearingMappingRepository : CoroutineCrudRepository<AdjudicationHearingMapping, String> {
  suspend fun findByNomisHearingId(hearingId: Long): AdjudicationHearingMapping?
}
