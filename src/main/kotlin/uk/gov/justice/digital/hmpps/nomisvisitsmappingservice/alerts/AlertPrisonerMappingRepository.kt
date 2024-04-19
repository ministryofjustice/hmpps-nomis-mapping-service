package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AlertPrisonerMappingRepository : CoroutineCrudRepository<AlertPrisonerMapping, String> {
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: AlertMappingType, pageRequest: Pageable): Flow<AlertPrisonerMapping>

  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: AlertMappingType): Long
}
