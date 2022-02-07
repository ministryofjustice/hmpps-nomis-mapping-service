package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitId

@Repository
interface VisitIdRepository : ReactiveCrudRepository<VisitId, Long> {
  fun findOneByVsipId(vsipId: String): Mono<VisitId>
}
