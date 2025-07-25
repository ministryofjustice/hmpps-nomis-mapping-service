package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a Person and there associated child entities")
data class ContactPersonMappingsDto(
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,
  @Schema(description = "Mapping type")
  val mappingType: ContactPersonMappingType = ContactPersonMappingType.DPS_CREATED,
  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
  @Schema(description = "Person mapping")
  val personMapping: ContactPersonSimpleMappingIdDto,
  @Schema(description = "Person address mapping")
  val personAddressMapping: List<ContactPersonSimpleMappingIdDto>,
  @Schema(description = "Person phone mapping")
  val personPhoneMapping: List<ContactPersonPhoneMappingIdDto>,
  @Schema(description = "Person email mapping")
  val personEmailMapping: List<ContactPersonSimpleMappingIdDto>,
  @Schema(description = "Person employment mapping")
  val personEmploymentMapping: List<ContactPersonSequenceMappingIdDto>,
  @Schema(description = "Person identifier mapping")
  val personIdentifierMapping: List<ContactPersonSequenceMappingIdDto>,
  @Schema(description = "Person restriction mapping")
  val personRestrictionMapping: List<ContactPersonSimpleMappingIdDto>,
  @Schema(description = "Person contact mapping")
  val personContactMapping: List<ContactPersonSimpleMappingIdDto>,
  @Schema(description = "Person contact restriction mapping")
  val personContactRestrictionMapping: List<ContactPersonSimpleMappingIdDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "New and old mappings related to a prisoner")
data class ContactPersonPrisonerMappingsDto(
  @Schema(description = "Mapping type")
  val mappingType: ContactPersonMappingType = ContactPersonMappingType.NOMIS_CREATED,
  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
  @Schema(description = "Person contact mapping")
  val personContactMapping: List<ContactPersonSimpleMappingIdDto>,
  @Schema(description = "Person contact restriction mapping")
  val personContactRestrictionMapping: List<ContactPersonSimpleMappingIdDto>,
  @Schema(description = "Person contact mappings to remove")
  val personContactMappingsToRemoveByDpsId: List<String>,
  @Schema(description = "Person contact restriction mappings to remove")
  val personContactRestrictionMappingsToRemoveByDpsId: List<String>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to DPS simple mapping IDs")
data class ContactPersonSimpleMappingIdDto(
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to DPS simple mapping IDs")
data class ContactPersonPhoneMappingIdDto(
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "DPS phone type")
  val dpsPhoneType: DpsPersonPhoneType,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to DPS simple mapping IDs")
data class ContactPersonSequenceMappingIdDto(
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "NOMIS id")
  val nomisPersonId: Long,
  @Schema(description = "NOMIS sequence")
  val nomisSequenceNumber: Long,
)

class PersonMappingDto(
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  label: String?,
  mappingType: ContactPersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractContactPersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

class PersonContactMappingDto(
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  label: String?,
  mappingType: ContactPersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractContactPersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

class PersonAddressMappingDto(
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  label: String?,
  mappingType: ContactPersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractContactPersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

class PersonEmailMappingDto(
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  label: String?,
  mappingType: ContactPersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractContactPersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

class PersonPhoneMappingDto(
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  @Schema(description = "DPS phone type")
  val dpsPhoneType: DpsPersonPhoneType,
  label: String?,
  mappingType: ContactPersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractContactPersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

class PersonIdentifierMappingDto(
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "NOMIS id")
  val nomisPersonId: Long,
  @Schema(description = "NOMIS sequence")
  val nomisSequenceNumber: Long,
  label: String?,
  mappingType: ContactPersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractContactPersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

class PersonEmploymentMappingDto(
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "NOMIS id")
  val nomisPersonId: Long,
  @Schema(description = "NOMIS sequence")
  val nomisSequenceNumber: Long,
  label: String?,
  mappingType: ContactPersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractContactPersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

class PersonContactRestrictionMappingDto(
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  label: String?,
  mappingType: ContactPersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractContactPersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

class PersonRestrictionMappingDto(
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  label: String?,
  mappingType: ContactPersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractContactPersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

class PrisonerRestrictionMappingDto(
  @Schema(description = "DPS id")
  val dpsId: String,
  @Schema(description = "NOMIS id")
  val nomisId: Long,
  @Schema(description = "Prisoner number")
  val offenderNo: String,
  label: String?,
  mappingType: ContactPersonMappingType,
  whenCreated: LocalDateTime?,
) : AbstractContactPersonMappingDto(label = label, mappingType = mappingType, whenCreated = whenCreated)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "New and old mappings related to a prisoner's restrictions")
data class PrisonerRestrictionMappingsDto(
  @Schema(description = "Mapping type")
  val mappingType: ContactPersonMappingType = ContactPersonMappingType.NOMIS_CREATED,
  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
  @Schema(description = "Restrictions mappings")
  val mappings: List<ContactPersonSimpleMappingIdDto>,
)

abstract class AbstractContactPersonMappingDto(
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: ContactPersonMappingType = ContactPersonMappingType.DPS_CREATED,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
