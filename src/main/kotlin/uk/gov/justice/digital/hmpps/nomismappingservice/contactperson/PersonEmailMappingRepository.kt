package uk.gov.justice.digital.hmpps.nomismappingservice.contactperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonEmailMappingRepository : CoroutineCrudRepository<PersonEmailMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): PersonEmailMapping?
  suspend fun findOneByDpsId(dpsId: String): PersonEmailMapping?
  suspend fun deleteByNomisId(nomisId: Long)
}
