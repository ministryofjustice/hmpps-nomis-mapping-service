package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.booking

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PersonLocationBookingRepository : CoroutineCrudRepository<PersonLocationBookingMapping, UUID> {
  suspend fun findByNomisBookingId(nomisBookingId: Long): PersonLocationBookingMapping?
  suspend fun deleteByNomisBookingIdIn(nomisBookingIds: Collection<Long>)
}
