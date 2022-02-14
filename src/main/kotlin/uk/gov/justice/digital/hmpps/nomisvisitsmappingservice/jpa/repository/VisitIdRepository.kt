package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitId

@Repository
interface VisitIdRepository : CoroutineCrudRepository<VisitId, Long> {
  suspend fun findOneByVsipId(vsipId: String): VisitId?
}
