package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CourtScheduleRepository : CoroutineCrudRepository<CourtScheduleMapping, UUID> {
  suspend fun findByBookingId(bookingId: Long): List<CourtScheduleMapping>
  suspend fun findByDpsCourtAppearanceId(dpsId: UUID): CourtScheduleMapping?
  suspend fun findByNomisEventId(nomisEventId: Long): CourtScheduleMapping?
  suspend fun findByOffenderNo(offenderNo: String): List<CourtScheduleMapping>
  suspend fun deleteByDpsCourtAppearanceId(dpsId: UUID)
  suspend fun deleteByNomisEventId(nomisEventId: Long)
  suspend fun deleteByOffenderNo(offenderNo: String)
}
