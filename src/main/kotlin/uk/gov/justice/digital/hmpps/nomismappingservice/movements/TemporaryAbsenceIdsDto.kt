package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner's entire temporary absence history")
data class TemporaryAbsencesPrisonerMappingIdsDto(
  @Schema(description = "The NOMIS offender number", example = "A1234BC")
  val prisonerNumber: String,
  @Schema(description = "Mappings for application IDs")
  val applications: List<TemporaryAbsenceApplicationMappingIdsDto>,
  @Schema(description = "Mappings for schedule IDs")
  val schedules: List<ScheduledMovementMappingIdsDto>,
  @Schema(description = "Mappings for movement IDs")
  val movements: List<ExternalMovementMappingIdsDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a single temporary absence application and its children")
data class TemporaryAbsenceApplicationMappingIdsDto(
  @Schema(description = "The NOMIS temporary absence application id")
  val nomisMovementApplicationId: Long,
  @Schema(description = "The DPS temporary absence application id")
  val dpsMovementApplicationId: UUID,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single scheduled movement")
data class ScheduledMovementMappingIdsDto(
  @Schema(description = "The NOMIS scheduled movement id")
  val nomisEventId: Long,
  @Schema(description = "The DPS scheduled movement id")
  val dpsOccurrenceId: UUID,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single external movement")
data class ExternalMovementMappingIdsDto(
  @Schema(description = "The NOMIS booking id")
  val nomisBookingId: Long,
  @Schema(description = "The NOMIS movement sequence")
  val nomisMovementSeq: Int,
  @Schema(description = "The DPS movement id")
  val dpsMovementId: UUID,
)
