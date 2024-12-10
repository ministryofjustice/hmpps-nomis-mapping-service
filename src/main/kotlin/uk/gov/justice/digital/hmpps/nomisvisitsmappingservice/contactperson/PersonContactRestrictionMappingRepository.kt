package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonContactRestrictionMappingRepository : CoroutineCrudRepository<PersonContactRestrictionMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): PersonContactRestrictionMapping?
  suspend fun findOneByDpsId(dpsId: String): PersonContactRestrictionMapping?
  suspend fun deleteByNomisId(nomisId: Long)
}
