package uk.gov.justice.digital.hmpps.nomismappingservice.finance

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Transaction mapping")
data class TransactionMappingDto(

  @Schema(description = "Transaction id in DPS", required = true)
  val dpsTransactionId: String,

  @Schema(description = "Transaction id in Nomis", required = true)
  val nomisTransactionId: Long,

  @Schema(description = "Prisoner number in Nomis", required = true)
  val offenderNo: String,

  @Schema(description = "Nomis booking id", required = true)
  val nomisBookingId: Long? = null,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type")
  val mappingType: TransactionMappingType = TransactionMappingType.DPS_CREATED,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: TransactionMapping) : this(
    dpsTransactionId = mapping.dpsTransactionId.toString(),
    nomisTransactionId = mapping.nomisTransactionId,
    offenderNo = mapping.offenderNo,
    nomisBookingId = mapping.nomisBookingId,
    label = mapping.label,
    mappingType = mapping.mappingType,
    whenCreated = mapping.whenCreated,
  )
}

@Schema(description = "All mappings for a prisoner created either via migration or synchronisation")
data class AllPrisonerTransactionMappingsDto(
  @Schema(description = "Mappings")
  val mappings: List<TransactionMappingDto>,
)
