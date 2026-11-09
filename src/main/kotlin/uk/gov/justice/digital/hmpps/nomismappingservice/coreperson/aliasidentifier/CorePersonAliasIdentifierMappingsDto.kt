package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingIdDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingType
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a Core Person and its associated alias and identifier entities")
data class CorePersonAliasIdentifierMappingsDto(
  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,
  @Schema(description = "Mapping type")
  val mappingType: CorePersonMappingType = CorePersonMappingType.MIGRATED,
  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
  @Schema(description = "Core Person mapping")
  val personMapping: CorePersonMappingIdDto,
  @Schema(description = "Offender alias mappings")
  val aliases: List<OffenderAliasMappingDto>,
  @Schema(description = "Offender identifier mappings")
  val identifiers: List<OffenderIdentifierMappingDto>,
)
