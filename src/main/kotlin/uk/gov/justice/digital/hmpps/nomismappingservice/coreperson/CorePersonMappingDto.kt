package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a Core Person and its associated child entities")
data class CorePersonMappingsDto(
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,
  @Schema(description = "Mapping type")
  val mappingType: CorePersonMappingType = CorePersonMappingType.MIGRATED,
  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
  @Schema(description = "Core Person mapping")
  val personMapping: CorePersonMappingIdDto,
  @Schema(description = "Core Person address mappings")
  val addresses: List<CorePersonAddressMappingDto>,
  @Schema(description = "Core Person contact mappings")
  val contacts: List<CorePersonContactMappingDto>,
  @Schema(description = "Core Person address usage mappings")
  val addressUsages: List<CorePersonAddressUsageMappingDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a Core Person and its associated child entities after it has been merged to.")
data class CorePersonMergeMappingsDto(
  @Schema(description = "NOMIS Prison number aka Offender number of the person being merged to")
  val toNomisPrisonNumber: String,
  @Schema(description = "NOMIS Prison number aka Offender number of the person being merged from")
  val fromNomisPrisonNumber: String,
  @Schema(description = "Core Person address mappings of the person being merged to")
  val addresses: List<CorePersonAddressMappingDto>,
  @Schema(description = "Core Person contact mappings of the person being merged to")
  val contacts: List<CorePersonContactMappingDto>,
  @Schema(description = "Core Person address usage mappings of the person being merged to")
  val addressUsages: List<CorePersonAddressUsageMappingDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Lists of mappings requested directly by ids")
data class CorePersonMappingsResult(
  @Schema(description = "List of address mappings requested")
  val addresses: List<CorePersonAddressMappingDto>,
  @Schema(description = "List of contact mappings requested")
  val contacts: List<CorePersonContactMappingDto>,
  @Schema(description = "List of address usage mappings requested")
  val addressUsages: List<CorePersonAddressUsageMappingDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request for mappings on nomis ids")
data class CorePersonMappingsByNomisIdsRequest(
  @Schema(description = "Address mappings with requested nomis ids")
  val addresses: List<Long>,
  @Schema(description = "Contacts with requested nomis ids")
  val contacts: List<ContactByNomisIdRequest>,
  @Schema(description = "Address usages with requested nomis ids")
  val addressUsages: List<AddressUsageByNomisIdRequest>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request for a contact mapping on nomis id")
data class ContactByNomisIdRequest(
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  @Schema(description = "NOMIS contact type")
  val nomisContactType: NomisContactType,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request for an address usage mapping on nomis id")
data class AddressUsageByNomisIdRequest(
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  @Schema(description = "NOMIS address usage code")
  val addressUsageCode: String,
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

class CorePersonContactMappingDto(
  @Schema(description = "CPR id")
  val cprId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  @Schema(description = "NOMIS contact type")
  val nomisContactType: NomisContactType,
  @Schema(description = "NOMIS prison number aka Offender number")
  val nomisPrisonNumber: String,
  label: String?,
  mappingType: CorePersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractCorePersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

class ProfileMappingIdDto(
  @Schema(description = "CPR profile id")
  val cprId: String,
  @Schema(description = "NOMIS booking id")
  val nomisBookingId: Long,
  @Schema(description = "NOMIS profile type")
  val nomisProfileType: String,
  @Schema(description = "NOMIS Prison number aka Offender number")
  val nomisPrisonNumber: String,
  @Schema(description = "Mapping type - currently defaulted to be NOMIS_CREATED")
  val mappingType: CorePersonMappingType = CorePersonMappingType.NOMIS_CREATED,
)

class ProfileMappingDto(
  @Schema(description = "CPR profile id")
  val cprId: String,
  @Schema(description = "NOMIS booking id")
  val nomisBookingId: Long,
  @Schema(description = "NOMIS profile type")
  val nomisProfileType: String,
  @Schema(description = "NOMIS Prison number aka Offender number")
  val nomisPrisonNumber: String,
  label: String?,
  mappingType: CorePersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractCorePersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

class OffenderIdentifierMappingDto(
  @Schema(description = "CPR id")
  val cprId: String,
  @Schema(description = "NOMIS offender id")
  val nomisOffenderId: Long,
  @Schema(description = "NOMIS identifier sequence")
  val nomisIdentifierSequence: Int,
  @Schema(description = "NOMIS prison number aka Offender number")
  val nomisPrisonNumber: String,
  label: String?,
  mappingType: CorePersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractCorePersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

class OffenderAliasMappingDto(
  @Schema(description = "CPR id")
  val cprId: String,
  @Schema(description = "NOMIS offender id")
  val nomisOffenderId: Long,
  @Schema(description = "NOMIS prison number aka Offender number")
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
