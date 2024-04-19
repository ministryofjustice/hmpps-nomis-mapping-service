package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.incidents

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Incident mapping")
data class IncidentMappingDto(

  @Schema(description = "NOMIS Incident id", required = true)
  val nomisIncidentId: Long,

  @Schema(description = "Incident id from incidents service", required = true)
  val incidentId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "NOMIS_CREATED", "INCIDENT_CREATED"])
  @field:Size(max = 20, message = "mappingType has a maximum length of 20")
  val mappingType: String,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: IncidentMapping) : this(
    incidentId = mapping.incidentId,
    nomisIncidentId = mapping.nomisIncidentId,

    label = mapping.label,
    mappingType = mapping.mappingType.name,
    whenCreated = mapping.whenCreated,
  )
}
