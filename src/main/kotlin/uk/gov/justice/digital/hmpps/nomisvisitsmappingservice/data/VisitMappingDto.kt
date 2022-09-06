package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitId
import java.time.LocalDateTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to VSIP Visit Id mapping")
data class VisitMappingDto(

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

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null
) {
  constructor(visitId: VisitId) : this(
    nomisId = visitId.nomisId,
    vsipId = visitId.vsipId,
    label = visitId.label,
    mappingType = visitId.mappingType.name,
    whenCreated = visitId.whenCreated
  )
}
