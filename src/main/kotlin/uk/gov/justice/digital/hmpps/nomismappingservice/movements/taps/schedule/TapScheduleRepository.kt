package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.schedule

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface TapScheduleRepository : CoroutineCrudRepository<TapScheduleMapping, UUID> {
  suspend fun findByBookingId(bookingId: Long): List<TapScheduleMapping>
  suspend fun findByNomisAddressIdAndEventTimeIsGreaterThanEqual(nomisAddressId: Long, eventTime: LocalDateTime): List<TapScheduleMapping>
  suspend fun findByNomisEventId(nomisScheduleId: Long): TapScheduleMapping?
  suspend fun findByOffenderNo(offenderNo: String): List<TapScheduleMapping>
  suspend fun deleteByNomisEventId(nomisScheduleId: Long)
  suspend fun deleteByOffenderNo(offenderNo: String)
}
