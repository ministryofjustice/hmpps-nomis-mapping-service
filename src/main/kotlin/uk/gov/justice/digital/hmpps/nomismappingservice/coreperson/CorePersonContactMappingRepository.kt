package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorePersonContactMappingRepository : CoroutineCrudRepository<CorePersonContactMapping, String> {
  suspend fun findOneByNomisIdAndNomisContactType(nomisId: Long, nomisContactType: NomisContactType): CorePersonContactMapping?
  suspend fun findOneByCprId(cprId: String): CorePersonContactMapping?
  suspend fun deleteByNomisIdAndNomisContactType(nomisId: Long, nomisContactType: NomisContactType)
  suspend fun deleteAllByNomisPrisonNumber(nomisPrisonNumber: String)
}
