package uk.gov.justice.digital.hmpps.nomismappingservice.csip

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CSIP Child mapping")
data class CSIPChildMappingDto(

  @Schema(description = "NOMIS CSIP child id", required = true)
  val nomisId: Long,

  @Schema(description = "DPS CSIP child id", required = true)
  val dpsId: String,

  @Schema(description = "DPS CSIP Report id", required = true)
  val dpsCSIPReportId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CSIPChildMappingType = CSIPChildMappingType.DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
