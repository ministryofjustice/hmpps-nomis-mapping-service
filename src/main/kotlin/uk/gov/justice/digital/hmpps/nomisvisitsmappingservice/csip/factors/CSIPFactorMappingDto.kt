package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.CSIPFactorMappingType.DPS_CREATED
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CSIP Factor mapping")
data class CSIPFactorMappingDto(

  @Schema(description = "NOMIS CSIP Factor id", required = true)
  val nomisCSIPFactorId: Long,

  @Schema(description = "DPS CSIP Factor id", required = true)
  val dpsCSIPFactorId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CSIPFactorMappingType = DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
