package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit creation request")
data class MappingDto(

  @Schema(description = "nomis visit id", required = true)
  @NotNull
  val nomisId: Long,

  @Schema(description = "VSIP visit id", required = true)
  @NotBlank
  val vsipId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "ONLINE"], required = true)
  @NotEmpty
  val visitType: String,
)
