package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationMapping
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Adjudication mapping (same value for NOMIS and DPS)")
data class AdjudicationMappingDto(

  @Schema(description = "Adjudication number - the adjudication id", required = true)
  val adjudicationNumber: Long,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(
    description = "Mapping type",
    allowableValues = ["MIGRATED", "ADJUDICATION_CREATED"],
    defaultValue = "ADJUDICATION_CREATED",
  )
  @field:Size(max = 20)
  val mappingType: String? = null,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: AdjudicationMapping) : this(
    adjudicationNumber = mapping.adjudicationNumber,
    label = mapping.label,
    mappingType = mapping.mappingType.name,
    whenCreated = mapping.whenCreated,
  )
}
