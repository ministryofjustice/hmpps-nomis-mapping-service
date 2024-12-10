package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonMappingRepository : CoroutineCrudRepository<PersonMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): PersonMapping?
  suspend fun findOneByDpsId(dpsId: String): PersonMapping?
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: ContactPersonMappingType, pageRequest: Pageable): Flow<PersonMapping>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: ContactPersonMappingType): Long
  suspend fun findAllBy(pageRequest: Pageable): Flow<PersonMapping>
  suspend fun deleteByNomisId(nomisId: Long)
}
