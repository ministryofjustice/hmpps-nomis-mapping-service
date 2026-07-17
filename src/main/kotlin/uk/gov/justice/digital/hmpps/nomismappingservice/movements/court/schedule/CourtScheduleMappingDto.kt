package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single court schedule")
data class CourtScheduleMappingDto(
  @Schema(description = "The NOMIS offender number", example = "A1234BC")
  val prisonerNumber: String,
  @Schema(description = "The NOMIS ID of the booking", example = "12345")
  val bookingId: Long,
  @Schema(description = "The NOMIS event id")
  val nomisEventId: Long,
  @Schema(description = "The DPS court appearance id")
  val dpsCourtAppearanceId: UUID,
  @Schema(description = "The source of the mapping", example = "NOMIS_CREATED")
  val mappingType: CourtMappingType,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single court schedule")
data class CourtScheduleMappingUpsertByDpsIdResponse(
  @Schema(description = "The NOMIS event id that was replaced by the upserted event")
  val replacedNomisEventId: Long? = null,
) {
  companion object {
    val EVENT_ID_NOT_REPLACED = CourtScheduleMappingUpsertByDpsIdResponse()
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A request to update a mapping's prisoner")
data class UpdateScheduleMappingPrisonerRequest(
  @Schema(description = "The DPS court appearance id")
  val dpsCourtAppearanceId: UUID,
  @Schema(description = "The NOMIS offender number on the existing mapping", example = "A1234BC")
  val oldPrisonerNumber: String,
  @Schema(description = "The NOMIS ID of the booking on the existing mapping", example = "12345")
  val oldBookingId: Long,
  @Schema(description = "The new NOMIS offender number", example = "B1234BC")
  val newPrisonerNumber: String,
  @Schema(description = "The new NOMIS ID of the booking", example = "54321")
  val newBookingId: Long,
)
