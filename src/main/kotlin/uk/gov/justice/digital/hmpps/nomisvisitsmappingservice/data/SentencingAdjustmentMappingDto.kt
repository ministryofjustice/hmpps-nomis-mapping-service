package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingAdjustmentMapping
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to Sentencing mapping")
data class SentencingAdjustmentMappingDto(

  @Schema(description = "NOMIS Adjustment id", required = true)
  val nomisAdjustmentId: Long,

  @Schema(description = "NOMIS Adjustment category", required = true, allowableValues = ["SENTENCE", "KEY-DATE"])
  val nomisAdjustmentCategory: String,

  @Schema(description = "Adjustment id from sentencing service", required = true)
  val adjustmentId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "NOMIS_CREATED", "SENTENCING_CREATED"], required = true)
  @field:NotBlank
  @field:Size(max = 20, message = "mappingType has a maximum length of 20")
  val mappingType: String,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: SentencingAdjustmentMapping) : this(
    adjustmentId = mapping.adjustmentId,
    nomisAdjustmentId = mapping.nomisAdjustmentId,
    nomisAdjustmentCategory = mapping.nomisAdjustmentCategory,
    label = mapping.label,
    mappingType = mapping.mappingType.name,
    whenCreated = mapping.whenCreated,
  )
}
