package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.movement

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PersonLocationMovementRepository : CoroutineCrudRepository<PersonLocationMovementMapping, UUID> {
  suspend fun findByNomisBookingIdAndNomisMovementSeq(nomisBookingId: Long, nomisMovementSeq: Int): PersonLocationMovementMapping?
  suspend fun deleteByNomisBookingIdIn(nomisBookingIds: Collection<Long>)
}
