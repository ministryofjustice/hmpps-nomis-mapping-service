package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationPunishmentMapping
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Adjudication punishment (aka award) mapping")
data class AdjudicationPunishmentMappingDto(

  @Schema(description = "NOMIS booking id", required = true, example = "123456")
  val nomisBookingId: Long,

  @Schema(description = "NOMIS sanction sequence", required = true, example = "4")
  val nomisSanctionSequence: Int,

  @Schema(description = "DPS punishment id", required = true, example = "123456")
  val dpsPunishmentId: String,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(
    description = "Mapping type",
    allowableValues = ["MIGRATED", "ADJUDICATION_CREATED"],
    defaultValue = "ADJUDICATION_CREATED",
  )
  @field:Size(max = 20)
  val mappingType: String? = null,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: AdjudicationPunishmentMapping) : this(
    dpsPunishmentId = mapping.dpsPunishmentId,
    nomisBookingId = mapping.nomisBookingId,
    nomisSanctionSequence = mapping.nomisSanctionSequence,
    label = mapping.label,
    mappingType = mapping.mappingType.name,
    whenCreated = mapping.whenCreated,
  )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "ID of mapping identified by the NOMIS id for an adjudication punishment (aka award) mapping")
data class AdjudicationPunishmentNomisIdDto(

  @Schema(description = "NOMIS booking id", required = true, example = "123456")
  val nomisBookingId: Long,

  @Schema(description = "NOMIS sanction sequence", required = true, example = "4")
  val nomisSanctionSequence: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Batch of Adjudication punishment (aka award) mappings")
data class AdjudicationPunishmentBatchMappingDto(
  @Schema(description = "Adjudication punishment mapping", required = true)
  val punishments: List<AdjudicationPunishmentMappingDto> = emptyList(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Batch of Adjudication punishment (aka award) mappings to create and delete")
data class AdjudicationPunishmentBatchUpdateMappingDto(
  @Schema(description = "Adjudication punishment mapping", required = true)
  val punishmentsToCreate: List<AdjudicationPunishmentMappingDto> = emptyList(),
  val punishmentsToDelete: List<AdjudicationPunishmentNomisIdDto> = emptyList(),
)
