package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonEmailMappingRepository : CoroutineCrudRepository<PersonEmailMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): PersonEmailMapping?
  suspend fun findOneByDpsId(dpsId: String): PersonEmailMapping?
}
