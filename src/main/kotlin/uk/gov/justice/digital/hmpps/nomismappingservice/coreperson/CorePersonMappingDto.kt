package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to CPR core person mapping IDs")
data class CorePersonMappingIdDto(
  @Schema(description = "CPR id")
  val cprId: String,
  @Schema(description = "NOMIS Prison number aka Offender number")
  val nomisPrisonNumber: String,
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

abstract class AbstractCorePersonMappingDto(
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CorePersonMappingType = CorePersonMappingType.CPR_CREATED,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
