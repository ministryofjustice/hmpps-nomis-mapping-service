package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonPhoneMappingRepository : CoroutineCrudRepository<PersonPhoneMapping, Long> {
  suspend fun findOneByNomisId(nomisId: Long): PersonPhoneMapping?
  suspend fun findOneByDpsIdAndDpsPhoneType(dpsId: String, dpsPhoneType: DpsPersonPhoneType): PersonPhoneMapping?
}
