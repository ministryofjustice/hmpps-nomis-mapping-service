package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import org.springframework.data.annotation.Id

data class RoomId(

  @Id
  val id: Long,

  val nomisRoomDescription: String,

  val vsipId: String,

  val isOpen: Boolean,

  val prisonId: String,
)
