package uk.gov.justice.digital.hmpps.nomismappingservice.corporate

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorporateAddressPhoneMappingRepository : CoroutineCrudRepository<CorporateAddressPhoneMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): CorporateAddressPhoneMapping?
  suspend fun findOneByDpsId(dpsId: String): CorporateAddressPhoneMapping?
  suspend fun deleteByNomisId(nomisId: Long)
  suspend fun deleteByDpsId(dpsId: String)
}
