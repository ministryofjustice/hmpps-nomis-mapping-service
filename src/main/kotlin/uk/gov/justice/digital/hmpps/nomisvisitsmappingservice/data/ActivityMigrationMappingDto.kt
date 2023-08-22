package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMigrationMapping
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to Activity schedule mapping")
data class ActivityMigrationMappingDto(

  @Schema(description = "NOMIS course activity id", required = true)
  val nomisCourseActivityId: Long,

  @Schema(description = "Activity id", required = true)
  val activityId: Long,

  @Schema(description = "2nd Activity id", required = false)
  val activityId2: Long?,

  @Schema(description = "Label (a timestamp for migrated ids)", required = true)
  @field:Size(max = 20)
  val label: String,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: ActivityMigrationMapping) : this(
    nomisCourseActivityId = mapping.nomisCourseActivityId,
    activityId = mapping.activityId,
    activityId2 = mapping.activityId2,
    label = mapping.label,
    whenCreated = mapping.whenCreated,
  )
}
