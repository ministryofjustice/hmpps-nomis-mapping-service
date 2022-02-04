package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit creation request")
data class MappingDto(

  @Schema(description = "nomis visit id", required = true)
  @NotNull
  val nomisId: Long,

  @Schema(description = "VSIP visit id", required = true)
  @NotBlank
  @Size(max = 40)
  val vsipId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "ONLINE"], required = true)
  @NotBlank
  @Size(max = 20)
  val mappingType: String,
)
