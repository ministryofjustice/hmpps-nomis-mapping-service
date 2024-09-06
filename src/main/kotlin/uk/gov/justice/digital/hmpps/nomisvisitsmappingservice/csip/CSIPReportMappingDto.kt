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
@Schema(description = "CSIP mapping")
data class CSIPReportMappingDto(

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
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner created during migration")
data class PrisonerCSIPMappingsDto(
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CSIPMappingType = CSIPMappingType.DPS_CREATED,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,

  @Schema(description = "Mapping IDs")
  val mappings: List<CSIPMappingIdDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to CSIP mapping IDs")
data class CSIPMappingIdDto(
  @Schema(description = "DPS CSIP id")
  val dpsCSIPId: String,

  @Schema(description = "NOMIS CSIP id")
  val nomisCSIPId: Long,
)

@Schema(description = "All csip mappings for a prisoner created either via migration or synchronisation")
data class AllPrisonerCSIPMappingsDto(
  @Schema(description = "Mappings")
  val mappings: List<CSIPReportMappingDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Summary of mappings for a prisoner created during migration")
data class PrisonerCSIPMappingsSummaryDto(
  @Schema(description = "The prisoner number for the set of mappings")
  val offenderNo: String,

  @Schema(description = "Count of the number mappings migrated (does not include subsequent csips synchronised")
  val mappingsCount: Int,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime?,
)

data class CSIPFullMappingDto(

  @Schema(description = "NOMIS CSIP Report Id")
  val nomisCSIPReportId: Long,

  @Schema(description = "CSIP Report id from DPS CSIP service")
  val dpsCSIPReportId: String,

  @Schema(description = "CSIP Attendee Mappings")
  val attendeeMappings: List<CSIPAttendeeMappingDto>,
  @Schema(description = "CSIP Factor Mappings")
  val factorMappings: List<CSIPFactorMappingDto>,
  @Schema(description = "CSIP Interview Mappings")
  val interviewMappings: List<CSIPInterviewMappingDto>,
  @Schema(description = "CSIP Plan Mappings")
  val planMappings: List<CSIPPlanMappingDto>,
  @Schema(description = "CSIP Review Mappings")
  val reviewMappings: List<CSIPReviewMappingDto>,
)
