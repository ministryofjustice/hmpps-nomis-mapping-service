package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AllocationMigrationMapping
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to Activities allocation mapping")
data class AllocationMigrationMappingDto(

  @Schema(description = "NOMIS allocation id", required = true)
  val nomisAllocationId: Long,

  @Schema(description = "Activity allocation id", required = true)
  val activityAllocationId: Long,

  @Schema(description = "Activity id", required = true)
  val activityId: Long,

  @Schema(description = "Label (a timestamp for migrated ids)", required = true)
  @field:Size(max = 20)
  val label: String,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: AllocationMigrationMapping) : this(
    nomisAllocationId = mapping.nomisAllocationId,
    activityAllocationId = mapping.activityAllocationId,
    activityId = mapping.activityId,
    label = mapping.label,
    whenCreated = mapping.whenCreated,
  )
}
