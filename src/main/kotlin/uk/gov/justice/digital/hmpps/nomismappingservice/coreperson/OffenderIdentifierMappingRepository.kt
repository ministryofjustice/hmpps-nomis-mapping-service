package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface OffenderIdentifierMappingRepository : CoroutineCrudRepository<OffenderIdentifierMapping, String> {
  suspend fun findOneByNomisOffenderIdAndNomisIdentifierSequence(nomisOffenderId: Long, nomisIdentifierSequence: Int): OffenderIdentifierMapping?
  suspend fun findOneByCprId(cprId: String): OffenderIdentifierMapping?
  suspend fun deleteByNomisOffenderIdAndNomisIdentifierSequence(nomisOffenderId: Long, nomisIdentifierSequence: Int)
  suspend fun deleteAllByNomisPrisonNumber(nomisPrisonNumber: String)
}
