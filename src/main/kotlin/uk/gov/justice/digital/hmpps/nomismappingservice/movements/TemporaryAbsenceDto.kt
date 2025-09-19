package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner's entire temporary absence history")
data class TemporaryAbsencesPrisonerMappingDto(
  @Schema(description = "The NOMIS offender number", example = "A1234BC")
  val prisonerNumber: String,
  @Schema(description = "The mappings for each booking")
  val bookings: List<TemporaryAbsenceBookingMappingDto>,
  @Schema(description = "The migration unique identifier", example = "2025-08-11T15:34:43")
  val migrationId: String,
  @Schema(description = "The created time of the mappings", example = "2025-08-11T15:34:43")
  val whenCreated: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner booking's entire temporary absence history")
data class TemporaryAbsenceBookingMappingDto(
  @Schema(description = "The NOMIS ID of the booking", example = "12345")
  val bookingId: Long,
  @Schema(description = "Mappings for a single temporary absence application")
  val applications: List<TemporaryAbsenceApplicationMappingDto>,
  @Schema(description = "Mappings for unscheduled external movements, e.g. no application")
  val unscheduledMovements: List<ExternalMovementMappingDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a single temporary absence application and its children")
data class TemporaryAbsenceApplicationMappingDto(
  @Schema(description = "The NOMIS temporary absence application id")
  val nomisMovementApplicationId: Long,
  @Schema(description = "The DPS temporary absence application id")
  val dpsMovementApplicationId: UUID,
  @Schema(description = "Mappings for each outside movement recorded against the application")
  val outsideMovements: List<TemporaryAbsencesOutsideMovementMappingDto>,
  @Schema(description = "All scheduled movement mappings")
  val schedules: List<ScheduledMovementMappingDto>,
  @Schema(description = "All actual external movement mappings")
  val movements: List<ExternalMovementMappingDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for an outside movement on a temporary absence application")
data class TemporaryAbsencesOutsideMovementMappingDto(
  @Schema(description = "The NOMIS outside movement id")
  val nomisMovementApplicationMultiId: Long,
  @Schema(description = "The DPS outside movement id")
  val dpsOutsideMovementId: UUID,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single scheduled movement")
data class ScheduledMovementMappingDto(
  @Schema(description = "The NOMIS scheduled movement id")
  val nomisEventId: Long,
  @Schema(description = "The DPS scheduled movement id")
  val dpsScheduledMovementId: UUID,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single external movement")
data class ExternalMovementMappingDto(
  @Schema(description = "The NOMIS external movement id")
  val nomisMovementSeq: Int,
  @Schema(description = "The DPS external movement id")
  val dpsExternalMovementId: UUID,
)
