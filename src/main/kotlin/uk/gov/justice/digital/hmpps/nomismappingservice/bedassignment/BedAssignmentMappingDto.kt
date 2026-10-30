package uk.gov.justice.digital.hmpps.nomismappingservice.bedassignment

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Bed assignment mapping")
data class BedAssignmentMappingDto(

  @Schema(description = "Bed assignment id in DPS", required = true)
  val dpsBedAssignmentId: String,

  @Schema(description = "Nomis booking id", required = true)
  val nomisBookingId: Long,

  @Schema(description = "Nomis sequence within booking", required = true)
  val nomisSequence: Int,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: BedAssignmentMappingType = BedAssignmentMappingType.DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: BedAssignmentMapping) : this(
    dpsBedAssignmentId = mapping.dpsBedAssignmentId.toString(),
    nomisBookingId = mapping.nomisBookingId,
    nomisSequence = mapping.nomisSequence,
    label = mapping.label,
    mappingType = mapping.mappingType,
    whenCreated = mapping.whenCreated,
  )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner created during migration")
data class PrisonerBedAssignmentMappingsDto(
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: BedAssignmentMappingType = BedAssignmentMappingType.DPS_CREATED,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,

  @Schema(description = "Mapping IDs")
  val mappings: List<BedAssignmentMappingIdDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to DPS mapping IDs")
data class BedAssignmentMappingIdDto(
  @Schema(description = "DPS bed assignment id")
  val dpsBedAssignmentId: String,

  @Schema(description = "NOMIS booking id")
  val nomisBookingId: Long,

  @Schema(description = "NOMIS sequence within booking")
  val nomisSequence: Int,
)
