package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CourtScheduleRepository : CoroutineCrudRepository<CourtScheduleMapping, UUID> {
  suspend fun findByNomisEventId(nomisEventId: Long): CourtScheduleMapping?
}
