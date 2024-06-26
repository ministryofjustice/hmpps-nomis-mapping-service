package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts.AlertMappingType.DPS_CREATED
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

  @Schema(description = "Prisoner number")
  val offenderNo: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: AlertMappingType = DPS_CREATED,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Summary of mappings for a prisoner created during migration")
data class PrisonerAlertMappingsSummaryDto(
  @Schema(description = "The prisoner number for the set of mappings")
  val offenderNo: String,

  @Schema(description = "Count of the number mappings migrated (does not include subsequent alerts synchronised")
  val mappingsCount: Int,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner created during migration")
data class PrisonerAlertMappingsDto(
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: AlertMappingType = DPS_CREATED,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,

  @Schema(description = "Mapping IDs")
  val mappings: List<AlertMappingIdDto>,
)

data class MergedPrisonerAlertMappingsDto(
  @Schema(description = "Prisoner whose mappings need removing")
  val removedOffenderNo: String,
  @Schema(description = "Mappings for a prisoner after the merge")
  val prisonerMapping: PrisonerAlertMappingsDto,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to Alert mapping IDs")
data class AlertMappingIdDto(
  @Schema(description = "DPS alert id")
  val dpsAlertId: String,

  @Schema(description = "NOMIS booking id")
  val nomisBookingId: Long,

  @Schema(description = "NOMIS alert sequence")
  val nomisAlertSequence: Long,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Update mapping NOMIS Ids")
data class NomisMappingIdUpdate(
  @Schema(description = "NOMIS booking id")
  val bookingId: Long,
)

@Schema(description = "All mappings for a prisoner created either via migration or synchronisation")
data class AllPrisonerAlertMappingsDto(
  @Schema(description = "Mappings")
  val mappings: List<AlertMappingDto>,
)
