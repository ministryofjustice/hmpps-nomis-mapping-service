package uk.gov.justice.digital.hmpps.nomismappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.AdjudicationMapping
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Adjudication mapping (same value for NOMIS and DPS)")
data class AdjudicationMappingDto(

  @Schema(description = "NOMIS Adjudication number", required = true, example = "123456")
  val adjudicationNumber: Long,

  @Schema(description = "NOMIS Charges sequence", required = true, example = "1")
  val chargeSequence: Int,

  @Schema(description = "DPS Charges number", required = true, example = "123456/1")
  val chargeNumber: String,

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
    chargeSequence = mapping.chargeSequence,
    chargeNumber = mapping.chargeNumber,
    label = mapping.label,
    mappingType = mapping.mappingType.name,
    whenCreated = mapping.whenCreated,
  )
}
