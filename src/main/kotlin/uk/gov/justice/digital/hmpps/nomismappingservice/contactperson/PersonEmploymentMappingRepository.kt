package uk.gov.justice.digital.hmpps.nomismappingservice.contactperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonEmploymentMappingRepository : CoroutineCrudRepository<PersonEmploymentMapping, String> {
  suspend fun findOneByNomisPersonIdAndNomisSequenceNumber(nomisPersonId: Long, nomisSequenceNumber: Long): PersonEmploymentMapping?
  suspend fun findOneByDpsId(dpsId: String): PersonEmploymentMapping?
  suspend fun deleteByNomisPersonIdAndNomisSequenceNumber(nomisPersonId: Long, nomisSequenceNumber: Long)
}
