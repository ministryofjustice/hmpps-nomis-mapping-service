package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.movement

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule.TransferMappingType
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single transfer movement")
data class TransferMovementMappingDto(
  @Schema(description = "The NOMIS offender number", example = "A1234BC")
  val prisonerNumber: String,
  @Schema(description = "The NOMIS ID of the booking", example = "12345")
  val nomisBookingId: Long,
  @Schema(description = "The NOMIS movement sequence number", example = "3")
  val nomisMovementSeq: Int,
  @Schema(description = "The DPS transfer movement id")
  val dpsTransferMovementId: UUID,
  @Schema(description = "The source of the mapping", example = "NOMIS_CREATED")
  val mappingType: TransferMappingType,
)
