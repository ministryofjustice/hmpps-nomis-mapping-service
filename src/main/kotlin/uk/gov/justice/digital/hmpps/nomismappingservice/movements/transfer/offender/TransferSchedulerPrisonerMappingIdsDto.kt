package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.offender

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner's entire transfer schedule history")
data class TransferSchedulerPrisonerMappingIdsDto(
  @Schema(description = "The NOMIS offender number", example = "A1234BC")
  val prisonerNumber: String,
  @Schema(description = "Mappings for schedule IDs")
  val schedules: List<TransferScheduleMappingIdsDto>,
  @Schema(description = "Mappings for movement IDs")
  val movements: List<TransferMovementMappingIdsDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single transfer schedule")
data class TransferScheduleMappingIdsDto(
  @Schema(description = "The NOMIS transfer schedule event id")
  val nomisEventId: Long,
  @Schema(description = "The DPS transfer schedule id")
  val dpsTransferScheduleId: UUID,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single transfer movement")
data class TransferMovementMappingIdsDto(
  @Schema(description = "The NOMIS booking id")
  val nomisBookingId: Long,
  @Schema(description = "The NOMIS movement sequence")
  val nomisMovementSeq: Int,
  @Schema(description = "The DPS transfer movement id")
  val dpsTransferMovementId: UUID,
)
