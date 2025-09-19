package uk.gov.justice.digital.hmpps.nomismappingservice.courtsentencing

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CourtChargeMappingRepository : CoroutineCrudRepository<CourtChargeMapping, String> {
  suspend fun findByNomisCourtChargeId(nomisId: Long): CourtChargeMapping?
  suspend fun deleteByNomisCourtChargeId(nomisCourtChargeIdId: Long)
}
