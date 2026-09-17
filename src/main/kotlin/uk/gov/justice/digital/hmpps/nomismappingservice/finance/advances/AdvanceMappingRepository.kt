package uk.gov.justice.digital.hmpps.nomismappingservice.finance

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType

@Repository
interface AdvanceMappingRepository : CoroutineCrudRepository<AdvanceMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): AdvanceMapping?
  suspend fun findOneByDpsId(dpsId: String): AdvanceMapping?
  suspend fun findAllBy(pageRequest: Pageable): Flow<AdvanceMapping>
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: StandardMappingType, pageRequest: Pageable): Flow<AdvanceMapping>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: StandardMappingType): Long
}
