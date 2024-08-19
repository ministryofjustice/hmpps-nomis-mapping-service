package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMappingType.DPS_CREATED
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CSIP Plan mapping")
data class CSIPPlanMappingDto(

  @Schema(description = "NOMIS CSIP Plan id", required = true)
  val nomisCSIPPlanId: Long,

  @Schema(description = "DPS CSIP Plan id", required = true)
  val dpsCSIPPlanId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CSIPPlanMappingType = DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
