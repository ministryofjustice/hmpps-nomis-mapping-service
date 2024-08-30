package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.CSIPAttendeeMappingType.DPS_CREATED
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CSIP Attendee mapping")
data class CSIPAttendeeMappingDto(

  @Schema(description = "NOMIS CSIP Attendee id", required = true)
  val nomisCSIPAttendeeId: Long,

  @Schema(description = "DPS CSIP Attendee id", required = true)
  val dpsCSIPAttendeeId: String,

  @Schema(description = "DPS CSIP Report id", required = true)
  val dpsCSIPReportId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CSIPAttendeeMappingType = DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
