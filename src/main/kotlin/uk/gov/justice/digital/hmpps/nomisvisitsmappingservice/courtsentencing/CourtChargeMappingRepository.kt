package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CourtChargeMappingRepository : CoroutineCrudRepository<OffenderChargeMapping, String> {
  suspend fun findByNomisCourtChargeId(nomisId: Long): OffenderChargeMapping?
  suspend fun deleteByNomisCourtChargeId(nomisCourtChargeIdId: Long)
}
