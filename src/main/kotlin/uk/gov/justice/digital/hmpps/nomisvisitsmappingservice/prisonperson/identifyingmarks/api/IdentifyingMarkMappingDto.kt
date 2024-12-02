package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkMapping
import java.time.LocalDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to DPS Identifying Mark Mapping")
class IdentifyingMarkMappingResponse(
  @Schema(description = "NOMIS OFFENDER_BOOK_ID")
  val nomisBookingId: Long,
  @Schema(description = "NOMIS ID_MARKS_SEQ")
  val nomisMarksSequence: Long,
  @Schema(description = "DPS unique id")
  val dpsId: UUID,
  @Schema(description = "NOMIS OFFENDER_ID_DISPLAY")
  val offenderNo: String,
  @Schema(description = "Migration ID")
  val label: String?,
  @Schema(description = "The source of the mapping")
  val mappingType: String,
  @Schema(description = "Created timestamp")
  val whenCreated: LocalDateTime,
)

fun IdentifyingMarkMapping.toDto(): IdentifyingMarkMappingResponse =
  IdentifyingMarkMappingResponse(
    nomisBookingId = this.nomisBookingId,
    nomisMarksSequence = this.nomisMarksSequence,
    dpsId = this.dpsId,
    offenderNo = this.offenderNo,
    label = this.label,
    mappingType = this.mappingType,
    whenCreated = this.whenCreated,
  )
