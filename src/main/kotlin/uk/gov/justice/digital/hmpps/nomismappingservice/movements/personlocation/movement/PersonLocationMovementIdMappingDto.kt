package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.movement

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.PersonLocationMappingType
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single person location external movement")
data class PersonLocationMovementIdMappingDto(
  @Schema(description = "The NOMIS ID of the booking", example = "12345")
  val nomisBookingId: Long,
  @Schema(description = "The NOMIS movement sequence number", example = "3")
  val nomisMovementSeq: Int,
  @Schema(description = "The DPS external movement ID")
  val dpsExternalMovementId: UUID,
  @Schema(description = "The source of the mapping", example = "NOMIS_CREATED")
  val mappingType: PersonLocationMappingType,
)
