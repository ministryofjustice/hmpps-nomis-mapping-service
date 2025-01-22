package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.corporate

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a Corporate and there associated child entities")
data class CorporateMappingsDto(
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,
  @Schema(description = "Mapping type")
  val mappingType: CorporateMappingType = CorporateMappingType.DPS_CREATED,
  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
  @Schema(description = "Corporate mapping")
  val corporateMapping: CorporateMappingIdDto,
  @Schema(description = "Corporate address mapping")
  val corporateAddressMapping: List<CorporateMappingIdDto>,
  @Schema(description = "Corporate address phone mapping")
  val corporateAddressPhoneMapping: List<CorporateMappingIdDto>,
  @Schema(description = "Corporate phone mapping")
  val corporatePhoneMapping: List<CorporateMappingIdDto>,
  @Schema(description = "Corporate email mapping")
  val corporateEmailMapping: List<CorporateMappingIdDto>,
  @Schema(description = "Corporate web mapping")
  val corporateWebMapping: List<CorporateMappingIdDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to DPS simple mapping IDs")
data class CorporateMappingIdDto(
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
)

class CorporateMappingDto(
  dpsId: String,
  nomisId: Long,
  label: String?,
  mappingType: CorporateMappingType,
  whenCreated: LocalDateTime?,
) : AbstractCorporateMappingDto(dpsId = dpsId, nomisId = nomisId, label = label, mappingType = mappingType, whenCreated = whenCreated)

class CorporateAddressMappingDto(
  dpsId: String,
  nomisId: Long,
  label: String?,
  mappingType: CorporateMappingType,
  whenCreated: LocalDateTime?,
) : AbstractCorporateMappingDto(dpsId = dpsId, nomisId = nomisId, label = label, mappingType = mappingType, whenCreated = whenCreated)

class CorporateAddressPhoneMappingDto(
  dpsId: String,
  nomisId: Long,
  label: String?,
  mappingType: CorporateMappingType,
  whenCreated: LocalDateTime?,
) : AbstractCorporateMappingDto(dpsId = dpsId, nomisId = nomisId, label = label, mappingType = mappingType, whenCreated = whenCreated)

class CorporateEmailMappingDto(
  dpsId: String,
  nomisId: Long,
  label: String?,
  mappingType: CorporateMappingType,
  whenCreated: LocalDateTime?,
) : AbstractCorporateMappingDto(dpsId = dpsId, nomisId = nomisId, label = label, mappingType = mappingType, whenCreated = whenCreated)

class CorporateWebMappingDto(
  dpsId: String,
  nomisId: Long,
  label: String?,
  mappingType: CorporateMappingType,
  whenCreated: LocalDateTime?,
) : AbstractCorporateMappingDto(dpsId = dpsId, nomisId = nomisId, label = label, mappingType = mappingType, whenCreated = whenCreated)

class CorporatePhoneMappingDto(
  dpsId: String,
  nomisId: Long,
  label: String?,
  mappingType: CorporateMappingType,
  whenCreated: LocalDateTime?,
) : AbstractCorporateMappingDto(dpsId = dpsId, nomisId = nomisId, label = label, mappingType = mappingType, whenCreated = whenCreated)

abstract class AbstractCorporateMappingDto(
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
