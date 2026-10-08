package uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.movement

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PrisonerLocationMovementRepository : CoroutineCrudRepository<PrisonerLocationMovementMapping, UUID> {
  suspend fun findByNomisBookingIdAndNomisMovementSeq(nomisBookingId: Long, nomisMovementSeq: Int): PrisonerLocationMovementMapping?
  suspend fun deleteByNomisBookingIdIn(nomisBookingIds: Collection<Long>)
}
