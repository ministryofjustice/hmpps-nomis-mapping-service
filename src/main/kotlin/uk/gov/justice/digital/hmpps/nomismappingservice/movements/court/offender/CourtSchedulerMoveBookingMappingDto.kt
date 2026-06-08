package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.offender

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner's entire court scheduler history")
data class CourtSchedulerMoveBookingMappingDto(
  @Schema(description = "A list mapping NOMIS event IDs to DPS court appearance IDs")
  val scheduleIds: List<CourtScheduleIdMapping>,
  @Schema(description = "A list mapping NOMIS movement sequence numbers to DPS movement IDs")
  val movementIds: List<CourtMovementIdMapping>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner booking's court schedules")
data class CourtScheduleIdMapping(
  @Schema(description = "The NOMIS event ID", example = "12345")
  val nomisEventId: Long,
  @Schema(description = "The DPS court appearance ID")
  val dpsCourtAppearanceId: UUID,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner booking's court movements")
data class CourtMovementIdMapping(
  @Schema(description = "The NOMIS movement's movement sequence")
  val nomisMovementSeq: Int,
  @Schema(description = "The DPS movement ID")
  val dpsCourtMovementId: UUID,
)
