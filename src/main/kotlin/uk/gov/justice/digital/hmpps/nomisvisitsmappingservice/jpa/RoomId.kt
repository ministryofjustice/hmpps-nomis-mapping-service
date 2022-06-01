package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class RoomId(
  @Id
  val id: Long,

  val nomisRoomDescription: String,

  val vsipId: String,

  val isOpen: Boolean,

  val prisonId: String,
)
