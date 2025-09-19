package uk.gov.justice.digital.hmpps.nomismappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.ActivityMapping
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to Activity schedule mapping")
data class ActivityMappingDto(

  @Schema(description = "Activity schedule id", required = true)
  val activityScheduleId: Long,

  @Schema(description = "Activity id")
  val activityId: Long? = null,

  @Schema(description = "NOMIS course activity id", required = true)
  val nomisCourseActivityId: Long,

  @Schema(description = "Mapping type", allowableValues = ["NOMIS_CREATED", "ACTIVITY_CREATED"], required = true)
  @field:NotBlank
  @field:Size(max = 20, message = "mappingType has a maximum length of 20")
  val mappingType: String,

  @Schema(description = "Scheduled instance to course schedule mappings", required = true)
  val scheduledInstanceMappings: List<ActivityScheduleMappingDto> = listOf(),

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,

) {
  constructor(mapping: ActivityMapping, scheduleMappings: List<ActivityScheduleMappingDto>) : this(
    activityScheduleId = mapping.activityScheduleId,
    activityId = mapping.activityId,
    nomisCourseActivityId = mapping.nomisCourseActivityId,
    mappingType = mapping.mappingType.name,
    scheduledInstanceMappings = scheduleMappings,
  )
}
