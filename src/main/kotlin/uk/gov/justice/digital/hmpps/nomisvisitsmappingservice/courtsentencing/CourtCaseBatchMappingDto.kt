package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Court cases mapping including child entity mapping")
data class CourtCaseBatchMappingDto(
  @Schema(description = "Mappings")
  val courtCases: List<CourtCaseMappingDto> = emptyList(),

  @Schema(description = "Court Appearance mappings")
  val courtAppearances: List<CourtAppearanceMappingDto> = emptyList(),

  @Schema(description = "Court Charge mappings")
  val courtCharges: List<CourtChargeMappingDto> = emptyList(),

  @Schema(description = "Sentence mappings")
  val sentences: List<SentenceMappingDto> = emptyList(),

  @Schema(description = "Sentence term mappings")
  val sentenceTerms: List<SentenceTermMappingDto> = emptyList(),

  @Schema(description = "Label (a timestamp for migrated ids)")
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CourtCaseMappingType = CourtCaseMappingType.MIGRATED,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
