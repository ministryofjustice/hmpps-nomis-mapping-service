package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.coreperson

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a Core Person and its associated child entities")
data class CorePersonMappingsDto(
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,
  @Schema(description = "Mapping type")
  val mappingType: CorePersonMappingType = CorePersonMappingType.CPR_CREATED,
  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
  @Schema(description = "Core Person mapping")
  val personMapping: CorePersonMappingIdDto,
  // @Schema(description = "Core Person address mappings")
  // val addressMappings: List<CorePersonSimpleMappingIdDto>,
  // TODO add more child mappings here
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to CPR core person mapping IDs")
data class CorePersonMappingIdDto(
  @Schema(description = "CPR id")
  val cprId: String,
  @Schema(description = "NOMIS Prison number aka Offender number")
  val nomisPrisonNumber: String,
)

@Schema(description = "NOMIS to CPR simple mapping IDs")
data class CorePersonSimpleMappingIdDto(
  @Schema(description = "CPR id")
  val cprId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
)
class CorePersonMappingDto(
  @Schema(description = "CPR id")
  val cprId: String,
  @Schema(description = "NOMIS Prison number aka Offender number")
  val nomisPrisonNumber: String,
  label: String?,
  mappingType: CorePersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractCorePersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

/*
class CorePersonAddressMappingDto(
  @Schema(description = "CPR id")
  val cprId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  label: String?,
  mappingType: CorePersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractCorePersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)
 */

abstract class AbstractCorePersonMappingDto(
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CorePersonMappingType = CorePersonMappingType.CPR_CREATED,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
