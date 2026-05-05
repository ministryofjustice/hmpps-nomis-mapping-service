package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.movement

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule.CourtMappingType
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single court movement")
data class CourtMovementMappingDto(
  @Schema(description = "The NOMIS offender number", example = "A1234BC")
  val prisonerNumber: String,
  @Schema(description = "The NOMIS ID of the booking", example = "12345")
  val nomisBookingId: Long,
  @Schema(description = "The NOMIS movement sequence number", example = "3")
  val nomisMovementSeq: Int,
  @Schema(description = "The DPS court movement id")
  val dpsCourtMovementId: UUID,
  @Schema(description = "The source of the mapping", example = "NOMIS_CREATED")
  val mappingType: CourtMappingType,
)
