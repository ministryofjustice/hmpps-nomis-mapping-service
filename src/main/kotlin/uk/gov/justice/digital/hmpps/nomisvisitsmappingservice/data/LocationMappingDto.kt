package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.LocationMapping
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Location mapping (same value for NOMIS and DPS)")
data class LocationMappingDto(

  @Schema(description = "Location id in DPS", required = true)
  val dpsLocationId: Long,

  @Schema(description = "Location id in Nomis", required = true)
  val nomisLocationId: Long,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "NOMIS_CREATED", "NON_ASSOCIATION_CREATED"])
  @field:Size(max = 30)
  val mappingType: String,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: LocationMapping) : this(
    dpsLocationId = mapping.dpsLocationId,
    nomisLocationId = mapping.nomisLocationId,
    label = mapping.label,
    mappingType = mapping.mappingType.name,
    whenCreated = mapping.whenCreated,
  )
}
