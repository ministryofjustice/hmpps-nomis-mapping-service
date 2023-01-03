package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.ActivityMapping
import java.time.LocalDateTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to Activity schedule mapping")
data class ActivityMappingDto(

  @Schema(description = "Activity schedule id", required = true)
  @NotNull
  val activityScheduleId: Long,

  @Schema(description = "NOMIS course activity id", required = true)
  @NotNull
  val nomisCourseActivityId: Long,

  @Schema(description = "Mapping type", allowableValues = ["NOMIS_CREATED", "ACTIVITY_CREATED"], required = true)
  @NotBlank
  @Size(max = 20)
  val mappingType: String,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null
) {
  constructor(mapping: ActivityMapping) : this(
    activityScheduleId = mapping.activityScheduleId,
    nomisCourseActivityId = mapping.nomisCourseActivityId,
    mappingType = mapping.mappingType.name,
  )
}
