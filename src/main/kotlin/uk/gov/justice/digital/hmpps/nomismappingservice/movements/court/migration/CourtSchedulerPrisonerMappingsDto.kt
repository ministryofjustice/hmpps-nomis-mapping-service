package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.migration

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner's entire court movement history")
class CourtSchedulerPrisonerMappingsDto(
  @Schema(description = "The NOMIS offender number", example = "A1234BC")
  val prisonerNumber: String,
  @Schema(description = "The mappings for each booking")
  val bookings: List<CourtSchedulerBookingMappingsDto>,
  @Schema(description = "The migration unique identifier", example = "2025-08-11T15:34:43")
  val migrationId: String,
  @Schema(description = "The created time of the mappings", example = "2025-08-11T15:34:43")
  val whenCreated: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner booking's entire court movement history")
data class CourtSchedulerBookingMappingsDto(
  @Schema(description = "The NOMIS ID of the booking", example = "12345")
  val bookingId: Long,
  @Schema(description = "Mappings for a single court schedule")
  val courtSchedules: List<BookingCourtScheduleMappingsDto>,
  @Schema(description = "Mappings for unscheduled court movements")
  val unscheduledMovements: List<BookingCourtMovementMappingsDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a single court schedule")
data class BookingCourtScheduleMappingsDto(
  @Schema(description = "The NOMIS court schedule ID")
  val nomisEventId: Long,
  @Schema(description = "The DPS court appearance ID")
  val dpsCourtAppearanceId: UUID,
  @Schema(description = "Mappings for court movements belonging to this court schedule")
  val movements: List<BookingCourtMovementMappingsDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a single court movement")
data class BookingCourtMovementMappingsDto(
  @Schema(description = "The NOMIS movement sequence")
  val nomisMovementSeq: Int,
  @Schema(description = "The DPS court movement ID")
  val dpsCourtMovementId: UUID,
)
