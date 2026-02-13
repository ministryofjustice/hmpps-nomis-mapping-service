package uk.gov.justice.digital.hmpps.nomismappingservice.csra

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CSRA mapping")
data class CsraMappingDto(

  @Schema(description = "CSRA id in DPS", required = true)
  val dpsCsraId: String,

  @Schema(description = "Nomis booking id", required = true)
  val nomisBookingId: Long,

  @Schema(description = "Nomis sequence within booking", required = true)
  val nomisSequence: Int,

  @Schema(description = "Prisoner number in Nomis", required = true)
  val offenderNo: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CsraMappingType = CsraMappingType.DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: CsraMapping) : this(
    dpsCsraId = mapping.dpsCsraId.toString(),
    nomisBookingId = mapping.nomisBookingId,
    nomisSequence = mapping.nomisSequence,
    offenderNo = mapping.offenderNo,
    label = mapping.label,
    mappingType = mapping.mappingType,
    whenCreated = mapping.whenCreated,
  )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner created during migration")
data class PrisonerCsraMappingsDto(
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CsraMappingType = CsraMappingType.DPS_CREATED,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,

  @Schema(description = "Mapping IDs")
  val mappings: List<CsraMappingIdDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to Case note mapping IDs")
data class CsraMappingIdDto(
  @Schema(description = "DPS case note id")
  val dpsCsraId: String,

  @Schema(description = "NOMIS booking id")
  val nomisBookingId: Long,

  @Schema(description = "NOMIS sequence within booking")
  val nomisSequence: Int,
)

@Schema(description = "All mappings for a prisoner created either via migration or synchronisation")
data class AllPrisonerCsraMappingsDto(
  @Schema(description = "Mappings")
  val mappings: List<CsraMappingDto>,
)
