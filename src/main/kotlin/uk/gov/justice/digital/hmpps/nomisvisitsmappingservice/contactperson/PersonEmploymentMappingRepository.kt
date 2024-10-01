package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonEmploymentMappingRepository : CoroutineCrudRepository<PersonEmploymentMapping, String> {
  suspend fun findOneByNomisPersonIdAndNomisSequenceNumber(nomisPersonId: Long, nomisSequenceNumber: Long): PersonEmploymentMapping?
  suspend fun findOneByDpsId(dpsId: String): PersonEmploymentMapping?
}
