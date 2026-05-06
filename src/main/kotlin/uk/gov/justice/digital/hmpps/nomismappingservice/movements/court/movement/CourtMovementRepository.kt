package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.movement

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CourtMovementRepository : CoroutineCrudRepository<CourtMovementMapping, UUID> {
  suspend fun findByNomisBookingIdAndNomisMovementSeq(nomisBookingId: Long, nomisMovementSeq: Int): CourtMovementMapping?
  suspend fun deleteByNomisBookingIdAndNomisMovementSeq(nomisBookingId: Long, nomisMovementSeq: Int)
}
