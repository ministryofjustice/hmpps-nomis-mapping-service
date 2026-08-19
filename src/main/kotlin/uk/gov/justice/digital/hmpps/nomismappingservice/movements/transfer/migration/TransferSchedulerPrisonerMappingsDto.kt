package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.migration

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner's entire transfer movement history")
class TransferSchedulerPrisonerMappingsDto(
  @Schema(description = "The NOMIS offender number", example = "A1234BC")
  val offenderNo: String,
  @Schema(description = "The mappings for each booking")
  val bookings: List<TransferSchedulerBookingMappingsDto>,
  @Schema(description = "The migration unique identifier", example = "2025-08-11T15:34:43")
  val migrationId: String,
  @Schema(description = "The created time of the mappings", example = "2025-08-11T15:34:43")
  val whenCreated: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner booking's entire transfer movement history")
data class TransferSchedulerBookingMappingsDto(
  @Schema(description = "The NOMIS ID of the booking", example = "12345")
  val bookingId: Long,
  @Schema(description = "Mappings for a single transfer schedule")
  val schedules: List<BookingTransferScheduleMappingsDto>,
  @Schema(description = "Mappings for unscheduled transfer movements")
  val unscheduledMovements: List<BookingTransferMovementMappingsDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a single transfer schedule")
data class BookingTransferScheduleMappingsDto(
  @Schema(description = "The NOMIS transfer schedule event ID")
  val nomisEventId: Long,
  @Schema(description = "The DPS transfer schedule ID")
  val dpsTransferScheduleId: UUID,
  @Schema(description = "Mapping for the transfer movement belonging to this transfer schedule, if it has occurred")
  val movement: BookingTransferMovementMappingsDto? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a single transfer movement")
data class BookingTransferMovementMappingsDto(
  @Schema(description = "The NOMIS movement sequence")
  val nomisMovementSeq: Int,
  @Schema(description = "The DPS transfer movement ID")
  val dpsTransferMovementId: UUID,
)
