package uk.gov.justice.digital.hmpps.nomismappingservice.courtsentencing

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

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Court cases mapping including child entity mapping for both updating and creating")
data class CourtCaseBatchUpdateAndCreateMappingDto(
  @Schema(description = "Mappings that need creating")
  val mappingsToCreate: CourtCaseBatchMappingDto,
  @Schema(description = "Mappings that need creating")
  val mappingsToUpdate: CourtCaseBatchUpdateMappingDto,

)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Court cases mapping including child entity mapping for both updating the NOMIS ID")
data class CourtCaseBatchUpdateMappingDto(
  @Schema(description = "Mappings")
  val courtCases: List<SimpleCourtSentencingIdPair> = emptyList(),

  @Schema(description = "Court Appearance mappings")
  val courtAppearances: List<SimpleCourtSentencingIdPair> = emptyList(),

  @Schema(description = "Court Charge mappings")
  val courtCharges: List<SimpleCourtSentencingIdPair> = emptyList(),

  @Schema(description = "Sentence mappings")
  val sentences: List<CourtSentenceIdPair> = emptyList(),

  @Schema(description = "Sentence term mappings")
  val sentenceTerms: List<CourtSentenceTermIdPair> = emptyList(),

)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DPS Ids for Court cases mapping deletion")
data class DpsCourtCaseBatchMappingDto(
  @Schema(description = "Court case Ids")
  val courtCases: List<String> = emptyList(),

  @Schema(description = "Court Appearance Ids")
  val courtAppearances: List<String> = emptyList(),

  @Schema(description = "Court Charge Ids")
  val courtCharges: List<String> = emptyList(),

  @Schema(description = "Sentence Ids")
  val sentences: List<String> = emptyList(),

  @Schema(description = "Sentence term Ids")
  val sentenceTerms: List<String> = emptyList(),
)

data class SimpleCourtSentencingIdPair(val fromNomisId: Long, val toNomisId: Long)
data class SentenceId(val nomisBookingId: Long, val nomisSequence: Int)
data class CourtSentenceIdPair(val fromNomisId: SentenceId, val toNomisId: SentenceId)
data class SentenceTermId(val nomisSentenceId: SentenceId, val nomisSequence: Int)
data class CourtSentenceTermIdPair(val fromNomisId: SentenceTermId, val toNomisId: SentenceTermId)
