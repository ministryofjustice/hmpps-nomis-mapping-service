package uk.gov.justice.digital.hmpps.nomismappingservice.casenotes

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

  @Schema(description = "Prisoner number in Nomis", required = true)
  val offenderNo: String,

  @Schema(description = "Nomis booking id", required = true)
  val nomisBookingId: Long,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CaseNoteMappingType = CaseNoteMappingType.DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: CaseNoteMapping) : this(
    dpsCaseNoteId = mapping.dpsCaseNoteId.toString(),
    nomisCaseNoteId = mapping.nomisCaseNoteId,
    offenderNo = mapping.offenderNo,
    nomisBookingId = mapping.nomisBookingId,
    label = mapping.label,
    mappingType = mapping.mappingType,
    whenCreated = mapping.whenCreated,
  )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner created during migration")
data class PrisonerCaseNoteMappingsDto(
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CaseNoteMappingType = CaseNoteMappingType.DPS_CREATED,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,

  @Schema(description = "Mapping IDs")
  val mappings: List<CaseNoteMappingIdDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to Case note mapping IDs")
data class CaseNoteMappingIdDto(
  @Schema(description = "DPS case note id")
  val dpsCaseNoteId: String,

  @Schema(description = "NOMIS case note id")
  val nomisCaseNoteId: Long,

  @Schema(description = "NOMIS booking id")
  val nomisBookingId: Long,
)

@Schema(description = "All mappings for a prisoner created either via migration or synchronisation")
data class AllPrisonerCaseNoteMappingsDto(
  @Schema(description = "Mappings")
  val mappings: List<CaseNoteMappingDto>,
)
