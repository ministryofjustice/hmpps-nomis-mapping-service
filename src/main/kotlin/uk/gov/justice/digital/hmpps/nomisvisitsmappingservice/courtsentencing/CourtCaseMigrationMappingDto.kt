package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Court cases mapping including child entity mapping")
data class CourtCaseMigrationMappingDto(
  @Schema(description = "Mappings")
  val mappings: List<CourtCaseAllMappingDto> = emptyList(),

  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CourtCaseMappingType = CourtCaseMappingType.MIGRATED,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
