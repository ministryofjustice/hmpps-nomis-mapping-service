package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorePersonEmailAddressMappingRepository : CoroutineCrudRepository<CorePersonEmailAddressMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): CorePersonEmailAddressMapping?
  suspend fun findOneByCprId(cprId: String): CorePersonEmailAddressMapping?
  suspend fun deleteByNomisId(nomisId: Long)
}
