package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonAddressMappingRepository : CoroutineCrudRepository<PersonAddressMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): PersonAddressMapping?
  suspend fun findOneByDpsId(dpsId: String): PersonAddressMapping?
}
