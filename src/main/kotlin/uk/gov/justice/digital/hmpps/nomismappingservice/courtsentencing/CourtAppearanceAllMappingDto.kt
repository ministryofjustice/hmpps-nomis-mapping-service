package uk.gov.justice.digital.hmpps.nomismappingservice.courtsentencing

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Court appearance mapping")
data class CourtAppearanceAllMappingDto(

  @Schema(description = "NOMIS court appearance id", example = "123456")
  val nomisCourtAppearanceId: Long,

  @Schema(description = "DPS court appearance id", example = "123456")
  val dpsCourtAppearanceId: String,

  @Schema(description = "Court Charge mappings")
  val courtCharges: List<CourtChargeMappingDto> = emptyList(),

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(
    description = "Mapping type",
    defaultValue = "DPS_CREATED",
  )
  val mappingType: CourtAppearanceMappingType? = null,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
