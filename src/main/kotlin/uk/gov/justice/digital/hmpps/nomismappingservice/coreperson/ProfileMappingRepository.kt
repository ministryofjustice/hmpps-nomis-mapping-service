package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ProfileMappingRepository : CoroutineCrudRepository<ProfileMapping, String> {
  suspend fun findOneByNomisBookingIdAndNomisProfileType(nomisBookingId: Long, nomisProfileType: String): ProfileMapping?
}
