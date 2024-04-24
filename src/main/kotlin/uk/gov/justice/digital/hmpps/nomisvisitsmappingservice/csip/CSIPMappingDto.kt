package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CSIP mapping")
data class CSIPMappingDto(

  @Schema(description = "NOMIS CSIP id", required = true)
  val nomisCSIPId: Long,

  @Schema(description = "CSIP id from DPS csip service", required = true)
  val dpsCSIPId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "NOMIS_CREATED", "CSIP_CREATED"])
  @field:Size(max = 20, message = "mappingType has a maximum length of 20")
  val mappingType: String,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: CSIPMapping) : this(
    dpsCSIPId = mapping.dpsCSIPId,
    nomisCSIPId = mapping.nomisCSIPId,

    label = mapping.label,
    mappingType = mapping.mappingType.name,
    whenCreated = mapping.whenCreated,
  )
}
