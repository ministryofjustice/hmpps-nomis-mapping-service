package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner's entire temporary absence history")
data class TemporaryAbsenceMoveBookingMappingDto(
  @Schema(description = "A list mapping NOMIS application IDs to DPS authorisation IDs")
  val applicationIds: List<TemporaryAbsenceApplicationIdMapping>,
  @Schema(description = "A list mapping NOMIS movement sequence numbers to DPS movement IDs")
  val movementIds: List<TemporaryAbsenceMovementIdMapping>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner booking's applications")
data class TemporaryAbsenceApplicationIdMapping(
  @Schema(description = "The NOMIS application ID", example = "12345")
  val applicationId: Long,
  @Schema(description = "The DPS authorisation ID")
  val authorisationId: UUID,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner booking's movements")
data class TemporaryAbsenceMovementIdMapping(
  @Schema(description = "The movement's movement sequence")
  val movementSeq: Int,
  @Schema(description = "The DPS movement ID")
  val dpsMovementId: UUID,
)
