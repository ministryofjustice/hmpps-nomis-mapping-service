package uk.gov.justice.digital.hmpps.nomismappingservice.courtsentencing

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface CourtChargeMappingRepository : CoroutineCrudRepository<CourtChargeMapping, String> {
  suspend fun findByNomisCourtChargeId(nomisId: Long): CourtChargeMapping?
  suspend fun deleteByNomisCourtChargeId(nomisCourtChargeIdId: Long)
  suspend fun deleteByWhenCreatedAfter(dateTime: LocalDateTime)
}
