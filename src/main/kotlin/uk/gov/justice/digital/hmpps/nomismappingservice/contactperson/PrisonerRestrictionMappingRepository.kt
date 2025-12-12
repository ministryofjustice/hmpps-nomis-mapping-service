package uk.gov.justice.digital.hmpps.nomismappingservice.contactperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PrisonerRestrictionMappingRepository : CoroutineCrudRepository<PrisonerRestrictionMapping, String> {
  suspend fun findAllByOffenderNo(offenderNo: String): List<PrisonerRestrictionMapping>
  suspend fun findOneByNomisId(nomisId: Long): PrisonerRestrictionMapping?
  suspend fun findOneByDpsId(dpsId: String): PrisonerRestrictionMapping?
  suspend fun deleteByNomisId(nomisId: Long)
  suspend fun deleteByDpsId(dpsId: String)
  suspend fun deleteAllByOffenderNo(offenderNo: String)
}
