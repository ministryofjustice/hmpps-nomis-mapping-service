package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorePersonAddressUsageMappingRepository : CoroutineCrudRepository<CorePersonAddressUsageMapping, String> {
  suspend fun findOneByCprId(cprId: String): CorePersonAddressUsageMapping?
  suspend fun deleteByNomisIdAndAddressUsageCode(nomisOffenderId: Long, addressUsageCode: String)
  suspend fun deleteAllByNomisPrisonNumber(nomisPrisonNumber: String)
}
