package uk.gov.justice.digital.hmpps.nomismappingservice.corporate

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorporateAddressMappingRepository : CoroutineCrudRepository<CorporateAddressMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): CorporateAddressMapping?
  suspend fun findOneByDpsId(dpsId: String): CorporateAddressMapping?
  suspend fun deleteByNomisId(nomisId: Long)
  suspend fun deleteByDpsId(dpsId: String)
}
