package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.coreperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorePersonEmailAddressMappingRepository : CoroutineCrudRepository<CorePersonEmailAddressMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): CorePersonEmailAddressMapping?
  suspend fun findOneByCprId(cprId: String): CorePersonEmailAddressMapping?
  suspend fun deleteByNomisId(nomisId: Long)
}
