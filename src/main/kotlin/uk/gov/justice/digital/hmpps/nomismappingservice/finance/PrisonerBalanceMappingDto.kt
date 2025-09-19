package uk.gov.justice.digital.hmpps.nomismappingservice.finance

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Offender Balance mapping")
data class PrisonerBalanceMappingDto(

  @Schema(description = "NOMIS Root Offender ID", example = "123456")
  val nomisRootOffenderId: Long,

  @Schema(description = "DPS representation of the offender")
  val dpsId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "NOMIS_CREATED", "DPS_CREATED"])
  val mappingType: PrisonerBalanceMappingType = PrisonerBalanceMappingType.DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
