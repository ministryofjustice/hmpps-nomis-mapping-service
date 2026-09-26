package uk.gov.justice.digital.hmpps.nomismappingservice.finance

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType

@Repository
interface HoldTransactionMappingRepository : CoroutineCrudRepository<HoldTransactionMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): HoldTransactionMapping?
  suspend fun findOneByDpsId(dpsId: String): HoldTransactionMapping?
  suspend fun findAllBy(pageRequest: Pageable): Flow<HoldTransactionMapping>
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: StandardMappingType, pageRequest: Pageable): Flow<HoldTransactionMapping>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: StandardMappingType): Long
}
