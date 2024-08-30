package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.CSIPInterviewMappingType.DPS_CREATED
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CSIP Interview mapping")
data class CSIPInterviewMappingDto(

  @Schema(description = "NOMIS CSIP Interview id", required = true)
  val nomisCSIPInterviewId: Long,

  @Schema(description = "DPS CSIP Interview id", required = true)
  val dpsCSIPInterviewId: String,

  @Schema(description = "DPS CSIP Report id", required = true)
  val dpsCSIPReportId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CSIPInterviewMappingType = DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
