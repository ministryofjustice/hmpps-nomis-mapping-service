package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CourtAppearanceRecallMappingRepository : CoroutineCrudRepository<CourtAppearanceRecallMapping, Long> {
  suspend fun findByDpsRecallId(dpsRecallId: String): CourtAppearanceRecallMapping?
  suspend fun deleteByNomisCourtAppearanceId(nomisId: Long)
}
