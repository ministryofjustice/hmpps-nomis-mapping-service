package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.AbstractCorePersonMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingType
import java.time.LocalDateTime

class CorePersonAddressMappingDto(
  @Schema(description = "CPR id")
  val cprId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  @Schema(description = "NOMIS prison number aka Offender number")
  val nomisPrisonNumber: String,
  label: String?,
  mappingType: CorePersonMappingType,
  whenCreated: LocalDateTime?,
  @Schema(description = "Address usage mappings, only sent and not returned in the get calls")
  val addressUsageMappings: List<CorePersonAddressUsageMappingDto>? = null,
) : AbstractCorePersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

class CorePersonAddressUsageMappingDto(
  @Schema(description = "CPR id")
  val cprId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  @Schema(description = "NOMIS address usage code")
  val addressUsageCode: String,
  @Schema(description = "NOMIS prison number aka Offender number")
  val nomisPrisonNumber: String,
  label: String?,
  mappingType: CorePersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractCorePersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)
