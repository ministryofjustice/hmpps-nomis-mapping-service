package uk.gov.justice.digital.hmpps.nomismappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "All DPS ids related to mappings that need deleting")
data class AdjudicationDeleteMappingDto(
  @Schema(description = "DPS Charge number", required = true, example = "123456/1")
  val dpsChargeNumber: String,
  @Schema(description = "List of DPS hearing Ids", required = true)
  val dpsHearingIds: List<String> = emptyList(),
  @Schema(description = "List of DPS punishment Ids", required = true)
  val dpsPunishmentIds: List<String> = emptyList(),
)
