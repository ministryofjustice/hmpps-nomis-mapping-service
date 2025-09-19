package uk.gov.justice.digital.hmpps.nomismappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.ActivityScheduleMapping
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to Activity schedule mapping")
data class ActivityScheduleMappingDto(

  @Schema(description = "Activity scheduled instance id", required = true)
  val scheduledInstanceId: Long,

  @Schema(description = "NOMIS course schedule id", required = true)
  val nomisCourseScheduleId: Long,

  @Schema(description = "Mapping type", allowableValues = ["ACTIVITY_CREATED", "ACTIVITY_UPDATED"], required = true)
  @field:NotBlank
  @field:Size(max = 20, message = "mappingType has a maximum length of 20")
  val mappingType: String,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: ActivityScheduleMapping) : this(
    scheduledInstanceId = mapping.scheduledInstanceId,
    nomisCourseScheduleId = mapping.nomisCourseScheduleId,
    mappingType = mapping.mappingType.name,
  )
}
