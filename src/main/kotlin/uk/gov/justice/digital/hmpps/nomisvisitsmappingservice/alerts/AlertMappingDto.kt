package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to Alert mapping")
data class AlertMappingDto(
  @Schema(description = "DPS alert id")
  val dpsAlertId: String,

  @Schema(description = "NOMIS booking id")
  val nomisBookingId: Long,

  @Schema(description = "NOMIS alert sequence")
  val nomisAlertSequence: Long,

  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "NOMIS_CREATED", "DPS_CREATED"])
  val mappingType: AlertMappingType,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
