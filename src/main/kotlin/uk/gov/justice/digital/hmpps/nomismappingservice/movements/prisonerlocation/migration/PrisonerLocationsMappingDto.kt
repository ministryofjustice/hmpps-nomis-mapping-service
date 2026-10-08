package uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.migration

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner's entire location history")
data class PrisonerLocationsMappingDto(
  @Schema(description = "The NOMIS offender number", example = "A1234BC")
  val offenderNo: String,
  @Schema(description = "The mappings for each booking")
  val bookings: List<PrisonerLocationBookingMappingDto>,
  @Schema(description = "The migration unique identifier", example = "2025-08-11T15:34:43")
  val migrationId: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner booking (DPS custodial series) and its external movements")
data class PrisonerLocationBookingMappingDto(
  @Schema(description = "The NOMIS ID of the booking", example = "12345")
  val bookingId: Long,
  @Schema(description = "The DPS custodial series ID")
  val dpsCustodialSeriesId: UUID,
  @Schema(description = "Mappings for the booking's external movements")
  val movements: List<PrisonerLocationMovementMappingDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a single external movement")
data class PrisonerLocationMovementMappingDto(
  @Schema(description = "The NOMIS movement sequence", example = "1")
  val nomisMovementSeq: Int,
  @Schema(description = "The DPS external movement ID")
  val dpsExternalMovementId: UUID,
)
