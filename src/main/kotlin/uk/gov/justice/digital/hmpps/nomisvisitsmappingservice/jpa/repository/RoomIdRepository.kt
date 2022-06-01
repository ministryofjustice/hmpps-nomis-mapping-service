package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.RoomId

interface RoomIdRepository : CrudRepository<RoomId, Long> {
  fun findOneByPrisonIdAndNomisRoomDescription(prisonId: String, nomisRoomDescription: String): RoomId?
}
