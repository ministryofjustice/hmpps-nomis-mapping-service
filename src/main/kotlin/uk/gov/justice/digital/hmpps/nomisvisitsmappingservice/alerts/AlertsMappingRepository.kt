package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Suppress("SpringDataRepositoryMethodReturnTypeInspection")
@Repository
interface AlertsMappingRepository : CoroutineCrudRepository<AlertMapping, String> {
  suspend fun findOneByNomisBookingIdAndNomisAlertSequence(bookingId: Long, alertSequence: Long): AlertMapping?
  suspend fun findOneByDpsAlertId(dpsAlertId: String): AlertMapping?
}
