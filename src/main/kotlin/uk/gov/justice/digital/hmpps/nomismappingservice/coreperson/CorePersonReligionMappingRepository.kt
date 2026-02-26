package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorePersonReligionMappingRepository : CoroutineCrudRepository<CorePersonReligionMapping, Long> {
  suspend fun findOneByNomisId(nomisId: Long): CorePersonReligionMapping?
  suspend fun findOneByCprId(cprId: String): CorePersonReligionMapping?
  suspend fun deleteByNomisId(nomisId: Long)
}
