package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMapping

@Repository
interface ActivityMappingRepository : CoroutineCrudRepository<ActivityMapping, Long> {
  suspend fun findOneByNomisCourseActivityId(nomisCourseActivityId: Long): ActivityMapping?
}
