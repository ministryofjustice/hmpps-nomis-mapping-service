package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface TemporaryAbsenceScheduleRepository : CoroutineCrudRepository<TemporaryAbsenceScheduleMapping, UUID> {
  suspend fun findByBookingId(bookingId: Long): List<TemporaryAbsenceScheduleMapping>
  suspend fun findByNomisAddressIdAndEventTimeIsGreaterThanEqual(nomisAddressId: Long, eventTime: LocalDateTime): List<TemporaryAbsenceScheduleMapping>
  suspend fun findByNomisEventId(nomisScheduleId: Long): TemporaryAbsenceScheduleMapping?
  suspend fun findByOffenderNo(offenderNo: String): List<TemporaryAbsenceScheduleMapping>
  suspend fun deleteByNomisEventId(nomisScheduleId: Long)
  suspend fun deleteByOffenderNo(offenderNo: String)
}
