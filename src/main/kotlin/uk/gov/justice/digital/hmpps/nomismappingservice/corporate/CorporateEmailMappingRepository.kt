package uk.gov.justice.digital.hmpps.nomismappingservice.corporate

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorporateEmailMappingRepository : CoroutineCrudRepository<CorporateEmailMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): CorporateEmailMapping?
  suspend fun findOneByDpsId(dpsId: String): CorporateEmailMapping?
  suspend fun deleteByNomisId(nomisId: Long)
  suspend fun deleteByDpsId(dpsId: String)
}
