package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingIdDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingType
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a Core Person and its associated child entities")
data class CorePersonAddressContactMappingsDto(
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
data class CorePersonAddressContactMergeMappingsDto(
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
data class CorePersonAddressContactMappingsResult(
  @Schema(description = "List of address mappings requested")
  val addresses: List<CorePersonAddressMappingDto>,
  @Schema(description = "List of contact mappings requested")
  val contacts: List<CorePersonContactMappingDto>,
  @Schema(description = "List of address usage mappings requested")
  val addressUsages: List<CorePersonAddressUsageMappingDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request for mappings on nomis ids")
data class CorePersonAddressContactMappingsByNomisIdsRequest(
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
