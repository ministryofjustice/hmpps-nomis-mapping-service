package uk.gov.justice.digital.hmpps.nomismappingservice.finance.scheduledpayments

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Scheduled payment mapping")
data class ScheduledPaymentMappingDto(

  @Schema(description = "NOMIS scheduled payment ID", example = "123456")
  val nomisScheduledPaymentId: Long,

  @Schema(description = "DPS representation of the scheduled payment")
  val dpsId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "NOMIS_CREATED", "DPS_CREATED"])
  val mappingType: StandardMappingType = StandardMappingType.DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
