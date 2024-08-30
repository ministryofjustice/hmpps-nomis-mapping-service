package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.CSIPReviewMappingType.DPS_CREATED
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CSIP Review mapping")
data class CSIPReviewMappingDto(

  @Schema(description = "NOMIS CSIP Review id", required = true)
  val nomisCSIPReviewId: Long,

  @Schema(description = "DPS CSIP Review id", required = true)
  val dpsCSIPReviewId: String,

  @Schema(description = "DPS CSIP Report id", required = true)
  val dpsCSIPReportId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: CSIPReviewMappingType = DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
