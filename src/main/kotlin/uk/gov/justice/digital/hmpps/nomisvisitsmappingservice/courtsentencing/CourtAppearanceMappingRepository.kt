package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CourtAppearanceMappingRepository : CoroutineCrudRepository<CourtAppearanceMapping, String> {
  suspend fun findByNomisCourtAppearanceId(nomisId: Long): CourtAppearanceMapping?
  suspend fun deleteByNomisCourtAppearanceId(nomisId: Long)
}
