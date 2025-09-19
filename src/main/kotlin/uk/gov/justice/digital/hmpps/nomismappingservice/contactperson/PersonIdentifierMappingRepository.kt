package uk.gov.justice.digital.hmpps.nomismappingservice.contactperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonIdentifierMappingRepository : CoroutineCrudRepository<PersonIdentifierMapping, String> {
  suspend fun findOneByNomisPersonIdAndNomisSequenceNumber(nomisPersonId: Long, nomisSequenceNumber: Long): PersonIdentifierMapping?
  suspend fun findOneByDpsId(dpsId: String): PersonIdentifierMapping?
  suspend fun deleteByNomisPersonIdAndNomisSequenceNumber(nomisPersonId: Long, nomisSequenceNumber: Long)
}
