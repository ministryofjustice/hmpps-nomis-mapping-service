package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.AddressUsageByNomisIdRequest
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.ContactByNomisIdRequest
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressUsageMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonContactMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier.IdentifierByNomisIdRequest
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier.OffenderAliasMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier.OffenderIdentifierMappingDto
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

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request for mappings on nomis ids")
data class CorePersonMappingsByNomisIdsRequest(
  @Schema(description = "Alias mappings with requested nomis ids")
  val aliases: List<Long>,
  @Schema(description = "Identifiers with requested nomis ids")
  val identifiers: List<IdentifierByNomisIdRequest>,
  @Schema(description = "Address mappings with requested nomis ids")
  val addresses: List<Long>,
  @Schema(description = "Contacts with requested nomis ids")
  val contacts: List<ContactByNomisIdRequest>,
  @Schema(description = "Address usages with requested nomis ids")
  val addressUsages: List<AddressUsageByNomisIdRequest>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Alias, identifier, address and contact mappings matching the requested nomis ids")
data class CorePersonMappingsResult(
  @Schema(description = "List of alias mappings requested")
  val aliases: List<OffenderAliasMappingDto>,
  @Schema(description = "List of identifier mappings requested")
  val identifiers: List<OffenderIdentifierMappingDto>,
  @Schema(description = "List of address mappings requested")
  val addresses: List<CorePersonAddressMappingDto>,
  @Schema(description = "List of contact mappings requested")
  val contacts: List<CorePersonContactMappingDto>,
  @Schema(description = "List of address usage mappings requested")
  val addressUsages: List<CorePersonAddressUsageMappingDto>,
)
