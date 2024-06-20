package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.casenotes

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Case note mapping")
data class CaseNoteMappingDto(

  @Schema(description = "Case note id in DPS", required = true)
  val dpsCaseNoteId: String,

  @Schema(description = "Case note id in Nomis", required = true)
  val nomisCaseNoteId: Long,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "NOMIS_CREATED", "DPS_CREATED"])
  val mappingType: CaseNoteMappingType = CaseNoteMappingType.DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: CaseNoteMapping) : this(
    dpsCaseNoteId = mapping.dpsCaseNoteId,
    nomisCaseNoteId = mapping.nomisCaseNoteId,
    label = mapping.label,
    mappingType = mapping.mappingType,
    whenCreated = mapping.whenCreated,
  )
}
