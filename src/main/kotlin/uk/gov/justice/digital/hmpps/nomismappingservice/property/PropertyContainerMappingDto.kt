package uk.gov.justice.digital.hmpps.nomismappingservice.property

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "property container mapping")
data class PropertyContainerMappingDto(

  @Schema(description = "property container id in DPS", required = true)
  val dpsPropertyContainerId: String,

  @Schema(description = "Nomis property container id", required = true)
  val nomisPropertyContainerId: Long,

  @Schema(description = "Nomis booking id", required = true)
  val bookingId: Long,

  @Schema(description = "Prisoner number in Nomis", required = true)
  val offenderNo: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: PropertyContainerMappingType = PropertyContainerMappingType.DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: PropertyContainerMapping) : this(
    dpsPropertyContainerId = mapping.dpsPropertyContainerId.toString(),
    nomisPropertyContainerId = mapping.nomisPropertyContainerId,
    bookingId = mapping.bookingId,
    offenderNo = mapping.offenderNo,
    label = mapping.label,
    mappingType = mapping.mappingType,
    whenCreated = mapping.whenCreated,
  )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to DPS mapping IDs")
data class PropertyContainerMappingIdDto(
  @Schema(description = "DPS property container id")
  val dpsPropertyContainerId: String,

  @Schema(description = "Nomis property container id", required = true)
  val nomisPropertyContainerId: Long,
)
