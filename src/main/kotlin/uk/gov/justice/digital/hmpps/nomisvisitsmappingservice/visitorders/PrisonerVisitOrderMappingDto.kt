package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.visitorders

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prisoner Visit Order mapping")
data class PrisonerVisitOrderMappingDto(

  @Schema(description = "NOMIS Prison Number", required = true)
  val nomisPrisonNumber: String,

  @Schema(description = "DPS representation of the offender", required = true)
  val dpsId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "NOMIS_CREATED", "DPS_CREATED"])
  val mappingType: PrisonerVisitOrderMappingType = PrisonerVisitOrderMappingType.DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
