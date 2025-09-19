package uk.gov.justice.digital.hmpps.nomismappingservice.jpa

import org.springframework.data.annotation.Id

data class RoomId(

  @Id
  val id: Long = 0,

  val nomisRoomDescription: String,

  val vsipId: String,

  val isOpen: Boolean,

  val prisonId: String,
)
