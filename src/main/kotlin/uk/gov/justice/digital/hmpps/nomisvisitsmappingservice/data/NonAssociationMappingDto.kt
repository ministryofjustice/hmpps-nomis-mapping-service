package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.NonAssociationMapping
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Non-association mapping")
data class NonAssociationMappingDto(

  @Schema(description = "Non-Association id", required = true)
  val nonAssociationId: Long,

  @Schema(description = "First NOMIS Offender No", required = true, example = "A1234BC")
  val firstOffenderNo: String,

  @Schema(description = "Second NOMIS Offender No", required = true, example = "D5678EF")
  val secondOffenderNo: String,

  @Schema(description = "NOMIS type sequence", required = true)
  @NotNull
  val nomisTypeSequence: Int,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "NOMIS_CREATED", "NON_ASSOCIATION_CREATED"])
  @field:Size(max = 30)
  val mappingType: String,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: NonAssociationMapping) : this(
    nonAssociationId = mapping.nonAssociationId,
    firstOffenderNo = mapping.firstOffenderNo,
    secondOffenderNo = mapping.secondOffenderNo,
    nomisTypeSequence = mapping.nomisTypeSequence,
    label = mapping.label,
    mappingType = mapping.mappingType.name,
    whenCreated = mapping.whenCreated,
  )
}
