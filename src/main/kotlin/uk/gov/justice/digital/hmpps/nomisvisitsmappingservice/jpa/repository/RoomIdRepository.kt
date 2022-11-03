package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.RoomId

@Repository
interface RoomIdRepository : CoroutineCrudRepository<RoomId, Long> {
  suspend fun findOneByPrisonIdAndNomisRoomDescription(prisonId: String, nomisRoomDescription: String): RoomId?

  suspend fun findByPrisonIdOrderByNomisRoomDescription(prisonId: String): List<RoomId>
}
