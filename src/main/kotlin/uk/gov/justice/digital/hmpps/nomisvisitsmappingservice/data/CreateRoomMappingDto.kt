package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Create room mapping request")
data class CreateRoomMappingDto(

  @Schema(description = "VSIP room id", required = true)
  val vsipId: String,

  @Schema(description = "nomis room description (unique within prison)", required = true)
  val nomisRoomDescription: String,

  @Schema(description = "open or closed indicator, default is false")
  val isOpen: Boolean = false,
)
