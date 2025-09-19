package uk.gov.justice.digital.hmpps.nomismappingservice.contactperson

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PrisonerRestrictionMappingRepository : CoroutineCrudRepository<PrisonerRestrictionMapping, String> {
  suspend fun findAllByOffenderNo(offenderNo: String): List<PrisonerRestrictionMapping>
  suspend fun findOneByNomisId(nomisId: Long): PrisonerRestrictionMapping?
  suspend fun findOneByDpsId(dpsId: String): PrisonerRestrictionMapping?
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: ContactPersonMappingType, pageRequest: Pageable): Flow<PrisonerRestrictionMapping>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: ContactPersonMappingType): Long
  suspend fun deleteByNomisId(nomisId: Long)
  suspend fun deleteByDpsId(dpsId: String)
  suspend fun deleteAllByOffenderNo(offenderNo: String)
}
