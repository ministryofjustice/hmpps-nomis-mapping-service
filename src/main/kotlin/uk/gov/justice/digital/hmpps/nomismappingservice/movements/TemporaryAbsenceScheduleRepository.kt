package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TemporaryAbsenceScheduleRepository : CoroutineCrudRepository<TemporaryAbsenceScheduleMapping, UUID> {
  suspend fun findByNomisEventId(nomisScheduleId: Long): TemporaryAbsenceScheduleMapping?
  suspend fun deleteByNomisEventId(nomisScheduleId: Long)
  suspend fun deleteByOffenderNo(offenderNo: String)
}
