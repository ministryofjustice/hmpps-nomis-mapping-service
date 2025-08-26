package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.movements

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a single temporary absence application")
data class TemporaryAbsenceApplicationSyncMappingDto(
  @Schema(description = "The NOMIS offender number", example = "A1234BC")
  val prisonerNumber: String,
  @Schema(description = "The NOMIS ID of the booking", example = "12345")
  val bookingId: Long,
  @Schema(description = "The NOMIS temporary absence application id")
  val nomisMovementApplicationId: Long,
  @Schema(description = "The DPS temporary absence application id")
  val dpsMovementApplicationId: UUID,
  @Schema(description = "The source of the mapping", example = "NOMIS_CREATED")
  val mappingType: MovementMappingType,
)
