package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Batch of Court Charge mappings to create and delete")
data class CourtChargeBatchUpdateMappingDto(
  @Schema(description = "Court Charge mappings to create")
  val courtChargesToCreate: List<OffenderChargeMappingDto> = emptyList(),

  @Schema(description = "Court Charge mappings to delete")
  val courtChargesToDelete: List<CourtChargeNomisIdDto> = emptyList(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "ID of mapping identified by the NOMIS id for a Court Charge mapping")
data class CourtChargeNomisIdDto(

  @Schema(description = "NOMIS court charge id", required = true, example = "123456")
  val nomisCourtChargeId: Long,
)
