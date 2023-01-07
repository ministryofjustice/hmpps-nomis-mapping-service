package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.IncentiveMapping
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to Incentive mapping")
data class IncentiveMappingDto(

  @Schema(description = "NOMIS booking id", required = true)
  @NotNull
  val nomisBookingId: Long,

  @Schema(description = "NOMIS incentive sequence", required = true)
  @NotNull
  val nomisIncentiveSequence: Long,

  @Schema(description = "Incentive id", required = true)
  @NotNull
  val incentiveId: Long,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "NOMIS_CREATED", "INCENTIVE_CREATED"], required = true)
  @NotBlank
  @Size(max = 20)
  val mappingType: String,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null
) {
  constructor(mapping: IncentiveMapping) : this(
    incentiveId = mapping.incentiveId,
    nomisBookingId = mapping.nomisBookingId,
    nomisIncentiveSequence = mapping.nomisIncentiveSequence,
    label = mapping.label,
    mappingType = mapping.mappingType.name,
    whenCreated = mapping.whenCreated
  )
}
