package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AlertsMappingRepository : CoroutineCrudRepository<AlertMapping, String> {
  suspend fun findOneByNomisBookingIdAndNomisAlertSequence(bookingId: Long, alertSequence: Long): AlertMapping?
  suspend fun findOneByDpsAlertId(dpsAlertId: String): AlertMapping?
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: AlertMappingType, pageRequest: Pageable): Flow<AlertMapping>

  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: AlertMappingType): Long
  suspend fun deleteAllByOffenderNo(offenderNo: String)
  suspend fun findAllByOffenderNoOrderByNomisBookingIdAscNomisAlertSequenceAsc(offenderNo: String): List<AlertMapping>
}
