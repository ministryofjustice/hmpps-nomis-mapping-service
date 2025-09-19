package uk.gov.justice.digital.hmpps.nomismappingservice.csip

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Full CSIP mapping")
data class CSIPFullMappingDto(
  @Schema(description = "NOMIS CSIP Report id", required = true)
  val nomisCSIPReportId: Long,
  @Schema(description = "DPS CSIP Report id from DPS CSIP service", required = true)
  val dpsCSIPReportId: String,
  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,
  @Schema(description = "Mapping type")
  val mappingType: CSIPMappingType = CSIPMappingType.DPS_CREATED,
  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,

  @Schema(description = "CSIP Attendee Mappings")
  val attendeeMappings: List<CSIPChildMappingDto> = listOf(),
  @Schema(description = "CSIP Factor Mappings")
  val factorMappings: List<CSIPChildMappingDto> = listOf(),
  @Schema(description = "CSIP Interview Mappings")
  val interviewMappings: List<CSIPChildMappingDto> = listOf(),
  @Schema(description = "CSIP Plan Mappings")
  val planMappings: List<CSIPChildMappingDto> = listOf(),
  @Schema(description = "CSIP Review Mappings")
  val reviewMappings: List<CSIPChildMappingDto> = listOf(),

)
