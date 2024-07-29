package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CSIP mapping")
data class CSIPMappingDto(

  @Schema(description = "Prisoner number")
  val offenderNo: String,

  @Schema(description = "NOMIS CSIP id", required = true)
  val nomisCSIPId: Long,

  @Schema(description = "CSIP id from DPS csip service", required = true)
  val dpsCSIPId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CSIPMappingType = CSIPMappingType.DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a prisoner created during migration")
data class PrisonerCSIPMappingsDto(
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CSIPMappingType = CSIPMappingType.DPS_CREATED,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,

  @Schema(description = "Mapping IDs")
  val mappings: List<CSIPMappingIdDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to CSIP mapping IDs")
data class CSIPMappingIdDto(
  @Schema(description = "DPS CSIP id")
  val dpsCSIPId: String,

  @Schema(description = "NOMIS CSIP id")
  val nomisCSIPId: Long,
)

@Schema(description = "All csip mappings for a prisoner created either via migration or synchronisation")
data class AllPrisonerCSIPMappingsDto(
  @Schema(description = "Mappings")
  val mappings: List<CSIPMappingDto>,
)
