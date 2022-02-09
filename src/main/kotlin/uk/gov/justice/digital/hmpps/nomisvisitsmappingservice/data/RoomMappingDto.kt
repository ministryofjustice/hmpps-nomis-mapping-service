package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit creation request")
data class RoomMappingDto(

  @Schema(description = "vsip room id", required = true)
  val vsipId: String,

  @Schema(description = "nomis room description (unique within prison)", required = true)
  val nomisRoomDescription: String,

  @Schema(description = "prison id", required = true)
  val prisonId: String,

  @Schema(description = "open or closed indicator", required = true)
  val isOpen: Boolean,
)
