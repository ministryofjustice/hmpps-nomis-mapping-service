package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Court appearance recall mapping")
data class CourtAppearanceRecallMappingDto(

  @Schema(description = "NOMIS court appearance id", example = "123456")
  val nomisCourtAppearanceId: Long,

  @Schema(description = "DPS recall id", example = "89442cf5-a22d-4113-b9d8-63daa757c962")
  val dpsRecallId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(
    description = "Mapping type",
    defaultValue = "DPS_CREATED",
  )
  val mappingType: CourtAppearanceRecallMappingType? = null,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
