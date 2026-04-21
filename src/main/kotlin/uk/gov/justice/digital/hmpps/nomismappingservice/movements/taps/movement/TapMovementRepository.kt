package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.movement

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TapMovementRepository : CoroutineCrudRepository<TapMovementMapping, UUID> {
  suspend fun findByNomisBookingIdAndNomisMovementSeq(nomisBookingId: Long, nomisMovementSeq: Int): TapMovementMapping?
  suspend fun findByNomisBookingId(nomisBookingId: Long): List<TapMovementMapping>
  suspend fun findByOffenderNo(offenderNO: String): List<TapMovementMapping>
  suspend fun deleteByNomisBookingIdAndNomisMovementSeq(nomisBookingId: Long, nomisMovementSeq: Int)
  suspend fun deleteByOffenderNo(offenderNo: String)
}
