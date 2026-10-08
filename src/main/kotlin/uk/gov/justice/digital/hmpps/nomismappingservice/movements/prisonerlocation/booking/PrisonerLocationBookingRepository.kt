package uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.booking

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PrisonerLocationBookingRepository : CoroutineCrudRepository<PrisonerLocationBookingMapping, UUID> {
  suspend fun findByNomisBookingId(nomisBookingId: Long): PrisonerLocationBookingMapping?
  suspend fun deleteByNomisBookingIdIn(nomisBookingIds: Collection<Long>)
}
