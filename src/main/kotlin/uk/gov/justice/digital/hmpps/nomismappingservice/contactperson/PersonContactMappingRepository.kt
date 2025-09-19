package uk.gov.justice.digital.hmpps.nomismappingservice.contactperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonContactMappingRepository : CoroutineCrudRepository<PersonContactMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): PersonContactMapping?
  suspend fun findOneByDpsId(dpsId: String): PersonContactMapping?
  suspend fun deleteByNomisId(nomisId: Long)
}
