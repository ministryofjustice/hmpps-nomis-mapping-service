package uk.gov.justice.digital.hmpps.nomismappingservice.corporate

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorporatePhoneMappingRepository : CoroutineCrudRepository<CorporatePhoneMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): CorporatePhoneMapping?
  suspend fun findOneByDpsId(dpsId: String): CorporatePhoneMapping?
  suspend fun deleteByNomisId(nomisId: Long)
  suspend fun deleteByDpsId(dpsId: String)
}
