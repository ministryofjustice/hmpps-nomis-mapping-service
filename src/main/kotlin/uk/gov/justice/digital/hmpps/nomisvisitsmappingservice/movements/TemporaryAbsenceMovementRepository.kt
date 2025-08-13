package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.movements

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TemporaryAbsenceMovementRepository : CoroutineCrudRepository<TemporaryAbsenceMovementMapping, UUID> {
  suspend fun findByNomisBookingIdAndNomisMovementSeq(nomisBookingId: Long, nomisMovementSeq: Int): TemporaryAbsenceMovementMapping?
  suspend fun deleteByOffenderNo(offenderNo: String)
}
