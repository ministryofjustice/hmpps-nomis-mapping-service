package uk.gov.justice.digital.hmpps.nomismappingservice.courtsentencing

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CourtAppearanceRecallMappingRepository : CoroutineCrudRepository<CourtAppearanceRecallMapping, Long> {
  suspend fun findAllByDpsRecallId(dpsRecallId: String): List<CourtAppearanceRecallMapping>
  suspend fun deleteByNomisCourtAppearanceId(nomisId: Long)
  suspend fun deleteAllByDpsRecallId(dpsRecallId: String)
}
