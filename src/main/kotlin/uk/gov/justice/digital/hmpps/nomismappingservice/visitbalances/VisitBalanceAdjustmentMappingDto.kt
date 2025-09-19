package uk.gov.justice.digital.hmpps.nomismappingservice.visitbalances

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prisoner Visit Order Balance mapping")
data class VisitBalanceAdjustmentMappingDto(

  @Schema(description = "NOMIS Visit Balance adjustment ID", required = true)
  val nomisVisitBalanceAdjustmentId: Long,

  @Schema(description = "DPS representation of the offender", required = true)
  val dpsId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["NOMIS_CREATED", "DPS_CREATED"])
  val mappingType: VisitBalanceAdjustmentMappingType = VisitBalanceAdjustmentMappingType.DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
