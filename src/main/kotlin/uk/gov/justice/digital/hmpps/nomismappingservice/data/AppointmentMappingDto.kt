package uk.gov.justice.digital.hmpps.nomismappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.AppointmentMapping
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to Appointment mapping")
data class AppointmentMappingDto(

  @Schema(description = "Appointment instance id", required = true)
  val appointmentInstanceId: Long,

  @Schema(description = "NOMIS course activity id", required = true)
  val nomisEventId: Long,

  @Schema(description = "Label (a timestamp for migrated ids)")
  @field:Size(max = 20)
  val label: String? = null,

  @Schema(description = "Mapping type", allowableValues = ["MIGRATED", "APPOINTMENT_CREATED"], defaultValue = "APPOINTMENT_CREATED")
  @field:Size(max = 20)
  val mappingType: String? = null,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null,
) {
  constructor(mapping: AppointmentMapping) : this(
    appointmentInstanceId = mapping.appointmentInstanceId,
    nomisEventId = mapping.nomisEventId,
    label = mapping.label,
    mappingType = mapping.mappingType.name,
    whenCreated = mapping.whenCreated,
  )
}
