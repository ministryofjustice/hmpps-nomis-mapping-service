package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Sentence mapping including child entity mapping")
data class SentenceAllMappingDto(

  @Schema(description = "NOMIS booking id", required = true, example = "123456")
  val nomisBookingId: Long,

  @Schema(description = "NOMIS sentence sequence", required = true, example = "4")
  val nomisSentenceSequence: Int,

  @Schema(description = "DPS sentence id", example = "123456")
  val dpsSentenceId: String,

  @Schema(description = "Offender Charges related to a sentence")
  val sentenceCharges: List<CourtChargeMappingDto> = emptyList(),

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(
    description = "Mapping type",
    defaultValue = "DPS_CREATED",
  )
  val mappingType: SentenceMappingType? = null,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
