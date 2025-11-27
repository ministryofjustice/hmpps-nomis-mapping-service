package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a single external movement")
data class ExternalMovementSyncMappingDto(
  @Schema(description = "The NOMIS offender number", example = "A1234BC")
  val prisonerNumber: String,
  @Schema(description = "The NOMIS ID of the booking", example = "12345")
  val bookingId: Long,
  @Schema(description = "The NOMIS movement sequence number")
  val nomisMovementSeq: Int,
  @Schema(description = "The DPS external movement id")
  val dpsMovementId: UUID,
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
)
