package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.movements

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TemporaryAbsenceScheduleRepository : CoroutineCrudRepository<TemporaryAbsenceScheduleMapping, UUID> {
  suspend fun findByNomisScheduleId(nomisScheduleId: Long): TemporaryAbsenceScheduleMapping?
  suspend fun deleteByNomisScheduleId(nomisScheduleId: Long)
  suspend fun deleteByOffenderNo(offenderNo: String)
}
