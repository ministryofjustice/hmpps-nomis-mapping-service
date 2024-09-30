package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonMappingRepository : CoroutineCrudRepository<PersonMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): PersonMapping?
  suspend fun findOneByDpsId(dpsId: String): PersonMapping?
}
