package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CSIPPrisonerMappingRepository : CoroutineCrudRepository<CSIPPrisonerMapping, String> {
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CSIPMappingType, pageRequest: Pageable): Flow<CSIPPrisonerMapping>

  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: CSIPMappingType): Long
}
