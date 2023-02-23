package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AppointmentMapping

@Repository
interface AppointmentMappingRepository : CoroutineCrudRepository<AppointmentMapping, Long> {
  suspend fun findOneByNomisEventId(nomisEventId: Long): AppointmentMapping?
}
