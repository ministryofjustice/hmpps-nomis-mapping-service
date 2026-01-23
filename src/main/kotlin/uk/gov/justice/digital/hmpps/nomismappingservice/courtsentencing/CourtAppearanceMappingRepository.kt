package uk.gov.justice.digital.hmpps.nomismappingservice.courtsentencing

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface CourtAppearanceMappingRepository : CoroutineCrudRepository<CourtAppearanceMapping, String> {
  suspend fun findByNomisCourtAppearanceId(nomisId: Long): CourtAppearanceMapping?
  suspend fun deleteByNomisCourtAppearanceId(nomisId: Long)
  suspend fun deleteByWhenCreatedAfter(dateTime: LocalDateTime)
}
