package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CourtCaseMappingRepository : CoroutineCrudRepository<CourtCaseMapping, String> {
  suspend fun findByNomisCourtCaseId(nomisId: Long): CourtCaseMapping?
  suspend fun deleteByNomisCourtCaseId(nomisId: Long)
}
