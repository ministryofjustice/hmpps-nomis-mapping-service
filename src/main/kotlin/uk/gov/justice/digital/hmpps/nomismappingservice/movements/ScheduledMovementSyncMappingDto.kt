package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single scheduled movement")
data class ScheduledMovementSyncMappingDto(
  @Schema(description = "The NOMIS offender number", example = "A1234BC")
  val prisonerNumber: String,
  @Schema(description = "The NOMIS ID of the booking", example = "12345")
  val bookingId: Long,
  @Schema(description = "The NOMIS event id")
  val nomisEventId: Long,
  @Schema(description = "The DPS scheduled movement id")
  val dpsOccurrenceId: UUID,
  @Schema(description = "The source of the mapping", example = "NOMIS_CREATED")
  val mappingType: MovementMappingType,
  @Schema(description = "The NOMIS address id")
  val nomisAddressId: Long?,
  @Schema(description = "The NOMIS address owner class")
  val nomisAddressOwnerClass: String?,
  @Schema(description = "The DPS address")
  val dpsAddressText: String,
  @Schema(description = "The DPS address")
  val eventTime: LocalDateTime,
)
