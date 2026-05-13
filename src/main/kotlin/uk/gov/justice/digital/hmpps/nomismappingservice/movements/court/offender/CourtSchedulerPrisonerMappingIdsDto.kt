package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.offender

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner's entire court schedule history")
data class CourtSchedulerPrisonerMappingIdsDto(
  @Schema(description = "The NOMIS offender number", example = "A1234BC")
  val prisonerNumber: String,
  @Schema(description = "Mappings for schedule IDs")
  val schedules: List<CourtScheduleMappingIdsDto>,
  @Schema(description = "Mappings for movement IDs")
  val movements: List<CourtMovementMappingIdsDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single court schedule")
data class CourtScheduleMappingIdsDto(
  @Schema(description = "The NOMIS court schedule id")
  val nomisEventId: Long,
  @Schema(description = "The DPS court appearance id")
  val dpsCourtAppearanceId: UUID,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single court movement")
data class CourtMovementMappingIdsDto(
  @Schema(description = "The NOMIS booking id")
  val nomisBookingId: Long,
  @Schema(description = "The NOMIS movement sequence")
  val nomisMovementSeq: Int,
  @Schema(description = "The DPS court movement id")
  val dpsCourtMovementId: UUID,
)
