package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface OffenderAliasMappingRepository : CoroutineCrudRepository<OffenderAliasMapping, String> {
  suspend fun findOneByNomisOffenderId(nomisOffenderId: Long): OffenderAliasMapping?
  suspend fun findOneByCprId(cprId: String): OffenderAliasMapping?
  suspend fun deleteByNomisOffenderId(nomisOffenderId: Long)
  suspend fun deleteAllByNomisPrisonNumber(nomisPrisonNumber: String)
}
