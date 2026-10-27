package uk.gov.justice.digital.hmpps.nomismappingservice.finance.scheduledpayments

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType

@Repository
interface ScheduledPaymentMappingRepository : CoroutineCrudRepository<ScheduledPaymentMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): ScheduledPaymentMapping?
  suspend fun findOneByDpsId(dpsId: String): ScheduledPaymentMapping?
  suspend fun findAllBy(pageRequest: Pageable): Flow<ScheduledPaymentMapping>
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: StandardMappingType, pageRequest: Pageable): Flow<ScheduledPaymentMapping>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: StandardMappingType): Long
}
