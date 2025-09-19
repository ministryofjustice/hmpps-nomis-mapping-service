package uk.gov.justice.digital.hmpps.nomismappingservice.contactperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonRestrictionMappingRepository : CoroutineCrudRepository<PersonRestrictionMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): PersonRestrictionMapping?
  suspend fun findOneByDpsId(dpsId: String): PersonRestrictionMapping?
  suspend fun deleteByNomisId(nomisId: Long)
}
