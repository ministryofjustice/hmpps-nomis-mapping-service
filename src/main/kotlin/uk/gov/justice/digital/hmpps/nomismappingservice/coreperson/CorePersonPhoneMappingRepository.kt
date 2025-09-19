package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorePersonPhoneMappingRepository : CoroutineCrudRepository<CorePersonPhoneMapping, Long> {
  suspend fun findOneByNomisId(nomisId: Long): CorePersonPhoneMapping?
  suspend fun findOneByCprIdAndCprPhoneType(cprId: String, cprPhoneType: CprPhoneType): CorePersonPhoneMapping?
  suspend fun deleteByNomisId(nomisId: Long)
}
