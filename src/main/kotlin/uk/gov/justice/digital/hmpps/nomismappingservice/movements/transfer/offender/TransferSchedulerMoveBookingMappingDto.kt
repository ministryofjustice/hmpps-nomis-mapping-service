package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.offender

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner booking's entire transfer scheduler history")
data class TransferSchedulerMoveBookingMappingDto(
  @Schema(description = "A list mapping NOMIS event IDs to DPS transfer schedule IDs")
  val scheduleIds: List<TransferScheduleIdMapping>,
  @Schema(description = "A list mapping NOMIS movement sequence numbers to DPS movement IDs")
  val movementIds: List<TransferMovementIdMapping>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner booking's transfer schedules")
data class TransferScheduleIdMapping(
  @Schema(description = "The NOMIS event ID", example = "12345")
  val nomisEventId: Long,
  @Schema(description = "The DPS transfer schedule ID")
  val dpsTransferScheduleId: UUID,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner booking's transfer movements")
data class TransferMovementIdMapping(
  @Schema(description = "The NOMIS movement's movement sequence")
  val nomisMovementSeq: Int,
  @Schema(description = "The DPS movement ID")
  val dpsTransferMovementId: UUID,
)
