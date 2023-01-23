package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentenceAdjustmentMapping
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to Sentencing mapping")
data class SentenceAdjustmentMappingDto(

  @Schema(description = "NOMIS Adjustment id", required = true)
  val nomisAdjustmentId: Long,

  @Schema(description = "NOMIS Adjustment type", required = true, allowableValues = ["SENTENCE", "BOOKING"])
  val nomisAdjustmentType: String,

  @Schema(description = "Sentence Adjustment id", required = true)
  val sentenceAdjustmentId: Long,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "NOMIS_CREATED", "SENTENCING_CREATED"], required = true)
  @field:NotBlank
  @field:Size(max = 20, message = "mappingType has a maximum length of 20")
  val mappingType: String,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null
) {
  constructor(mapping: SentenceAdjustmentMapping) : this(
    sentenceAdjustmentId = mapping.sentenceAdjustmentId,
    nomisAdjustmentId = mapping.nomisAdjustmentId,
    nomisAdjustmentType = mapping.nomisAdjustmentType,
    label = mapping.label,
    mappingType = mapping.mappingType.name,
    whenCreated = mapping.whenCreated
  )
}
