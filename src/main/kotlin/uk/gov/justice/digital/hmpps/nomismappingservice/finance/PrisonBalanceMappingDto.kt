package uk.gov.justice.digital.hmpps.nomismappingservice.finance

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prison Balance mapping")
data class PrisonBalanceMappingDto(

  @Schema(description = "NOMIS Prison ID", example = "MDI")
  val nomisId: String,

  @Schema(description = "DPS representation of the Prison", example = "MDI")
  val dpsId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "NOMIS_CREATED", "DPS_CREATED"])
  val mappingType: PrisonBalanceMappingType = PrisonBalanceMappingType.DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
