package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.movement

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TransferMovementRepository : CoroutineCrudRepository<TransferMovementMapping, UUID> {
  suspend fun findByNomisBookingIdAndNomisMovementSeq(nomisBookingId: Long, nomisMovementSeq: Int): TransferMovementMapping?
  suspend fun deleteByNomisBookingIdAndNomisMovementSeq(nomisBookingId: Long, nomisMovementSeq: Int)
}
