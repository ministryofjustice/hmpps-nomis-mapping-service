package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.schedule

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application.MovementMappingType
import java.time.LocalDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single scheduled movement")
data class TapScheduleMappingDto(
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
  @Schema(description = "The DPS address unique ID")
  val dpsUprn: Long?,
  @Schema(description = "The DPS description")
  val dpsDescription: String?,
  @Schema(description = "The DPS postcode")
  val dpsPostcode: String?,
  @Schema(description = "The DPS address")
  val eventTime: LocalDateTime,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Tap schedule mappings affected by a change of address")
data class FindTapScheduleMappingsForAddressResponse(
  @Schema(description = "All tap schedule mappings related to the passed NOMIS address ID and date. Note historical sync mappings are not included by default.")
  val scheduleMappings: List<TapScheduleMappingDto>,
)
