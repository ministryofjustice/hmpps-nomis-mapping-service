package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.religion

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ReligionMappingRepository : CoroutineCrudRepository<CorePersonReligionMapping, Long> {
  suspend fun findOneByCprId(cprId: String): CorePersonReligionMapping?
  suspend fun findByCprIdIn(cprIds: List<String>): List<CorePersonReligionMapping>

  @Query("UPDATE CORE_PERSON_RELIGION_MAPPING SET nomis_prison_number = :toPrisonNumber WHERE cpr_id IN (:cprIds) RETURNING *")
  suspend fun updatePrisonNumberByCprIds(cprIds: List<String>, toPrisonNumber: String): List<CorePersonReligionMapping>
  suspend fun findOneByNomisId(nomisId: Long): CorePersonReligionMapping?
  suspend fun findByNomisPrisonNumber(prisonNumber: String): List<CorePersonReligionMapping>
  suspend fun existsByNomisPrisonNumber(prisonNumber: String): Boolean
  suspend fun deleteByNomisId(nomisId: Long)
  suspend fun deleteAllByNomisPrisonNumber(nomisPrisonNumber: String)
}
