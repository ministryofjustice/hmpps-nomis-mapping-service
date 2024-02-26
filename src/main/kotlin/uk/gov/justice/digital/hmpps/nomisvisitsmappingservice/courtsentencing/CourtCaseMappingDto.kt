package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Court case mapping")
data class CourtCaseMappingDto(

  @Schema(description = "NOMIS court case id", example = "123456")
  val nomisCourtCaseId: Long,

  @Schema(description = "DPS court case id", example = "123456")
  val dpsCourtCaseId: String,

  @Schema(description = "Court Appearance mappings")
  val courtAppearances: List<CourtAppearanceMappingDto> = emptyList(),

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(
    description = "Mapping type",
    defaultValue = "DPS_CREATED",
  )
  val mappingType: CourtCaseMappingType? = null,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
