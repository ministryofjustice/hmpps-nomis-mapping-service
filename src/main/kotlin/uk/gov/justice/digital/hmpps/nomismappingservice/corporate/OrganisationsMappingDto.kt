package uk.gov.justice.digital.hmpps.nomismappingservice.corporate

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

class OrganisationsMappingDto(
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,
  @Schema(description = "Mapping type")
  val mappingType: CorporateMappingType = CorporateMappingType.DPS_CREATED,
  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
