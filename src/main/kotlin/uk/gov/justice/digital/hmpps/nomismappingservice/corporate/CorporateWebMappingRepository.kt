package uk.gov.justice.digital.hmpps.nomismappingservice.corporate

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorporateWebMappingRepository : CoroutineCrudRepository<CorporateWebMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): CorporateWebMapping?
  suspend fun findOneByDpsId(dpsId: String): CorporateWebMapping?
  suspend fun deleteByNomisId(nomisId: Long)
  suspend fun deleteByDpsId(dpsId: String)
}
