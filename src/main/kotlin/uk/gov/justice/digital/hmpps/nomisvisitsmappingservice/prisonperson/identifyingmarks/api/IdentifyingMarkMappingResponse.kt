package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api

import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.IdentifyingMarkMapping
import java.time.LocalDateTime
import java.util.UUID

class IdentifyingMarkMappingResponse(
  val nomisBookingId: Long,
  val nomisMarksSequence: Long,
  val dpsId: UUID,
  val offenderNo: String,
  val label: String?,
  val mappingType: String,
  val whenCreated: LocalDateTime,
)

fun IdentifyingMarkMapping.toResponse(): IdentifyingMarkMappingResponse =
  IdentifyingMarkMappingResponse(
    nomisBookingId = this.nomisBookingId,
    nomisMarksSequence = this.nomisMarksSequence,
    dpsId = this.dpsId,
    offenderNo = this.offenderNo,
    label = this.label,
    mappingType = this.mappingType,
    whenCreated = this.whenCreated,
  )
