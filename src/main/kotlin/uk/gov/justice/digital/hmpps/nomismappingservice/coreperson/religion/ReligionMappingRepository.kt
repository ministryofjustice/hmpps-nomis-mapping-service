package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.religion

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ReligionMappingRepository : CoroutineCrudRepository<CorePersonReligionMapping, String> {
  suspend fun findOneByCprId(cprId: String): CorePersonReligionMapping?
  suspend fun findOneByNomisId(nomisId: Long): CorePersonReligionMapping?
  suspend fun deleteByNomisId(nomisId: Long)
  suspend fun deleteAllByNomisPrisonNumber(nomisPrisonNumber: String)
}
