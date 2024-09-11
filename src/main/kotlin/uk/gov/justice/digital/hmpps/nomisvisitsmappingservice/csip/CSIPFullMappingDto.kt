package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.CSIPAttendeeMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.CSIPFactorMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.CSIPInterviewMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.CSIPReviewMappingDto
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Full CSIP mapping")
data class CSIPFullMappingDto(
  @Schema(description = "NOMIS CSIP Report id", required = true)
  val nomisCSIPReportId: Long,
  @Schema(description = "CSIP Report id from DPS CSIP service", required = true)
  val dpsCSIPReportId: String,
  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,
  @Schema(description = "Mapping type")
  val mappingType: CSIPMappingType = CSIPMappingType.DPS_CREATED,
  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,

  @Schema(description = "CSIP Attendee Mappings")
  val attendeeMappings: List<CSIPAttendeeMappingDto> = listOf(),
  @Schema(description = "CSIP Factor Mappings")
  val factorMappings: List<CSIPFactorMappingDto> = listOf(),
  @Schema(description = "CSIP Interview Mappings")
  val interviewMappings: List<CSIPInterviewMappingDto> = listOf(),
  @Schema(description = "CSIP Plan Mappings")
  val planMappings: List<CSIPPlanMappingDto> = listOf(),
  @Schema(description = "CSIP Review Mappings")
  val reviewMappings: List<CSIPReviewMappingDto> = listOf(),

)
