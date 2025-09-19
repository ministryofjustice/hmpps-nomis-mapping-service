package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorePersonAddressMappingRepository : CoroutineCrudRepository<CorePersonAddressMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): CorePersonAddressMapping?
  suspend fun findOneByCprId(cprId: String): CorePersonAddressMapping?
  suspend fun deleteByNomisId(nomisId: Long)
}
