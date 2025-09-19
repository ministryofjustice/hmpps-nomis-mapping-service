package uk.gov.justice.digital.hmpps.nomismappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Adjudication mapping for all entities in an adjudication")
data class AdjudicationAllMappingDto(
  @Schema(description = "Adjudication ID mapping", required = true)
  val adjudicationId: AdjudicationMappingDto,
  @Schema(description = "Adjudication hearing mapping", required = true)
  val hearings: List<AdjudicationHearingMappingDto> = emptyList(),
  @Schema(description = "Adjudication punishment mapping", required = true)
  val punishments: List<AdjudicationPunishmentMappingDto> = emptyList(),
  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String,
  @Schema(
    description = "Mapping type",
    allowableValues = ["MIGRATED", "ADJUDICATION_CREATED"],
    defaultValue = "MIGRATED",
  )
  @field:Size(max = 20)
  val mappingType: String? = "MIGRATED",
)
