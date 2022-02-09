package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.RoomId

@Repository
interface RoomIdRepository : ReactiveCrudRepository<RoomId, Long> {
  fun findOneByPrisonIdAndNomisRoomDescription(prisonId: String, nomisRoomDescription: String): Mono<RoomId>
}
