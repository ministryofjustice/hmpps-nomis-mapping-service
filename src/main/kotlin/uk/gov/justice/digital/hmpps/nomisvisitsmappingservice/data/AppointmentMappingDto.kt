package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AppointmentMapping
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS to Appointment mapping")
data class AppointmentMappingDto(

  @Schema(description = "Appointment instance id", required = true)
  val appointmentInstanceId: Long,

  @Schema(description = "NOMIS course activity id", required = true)
  val nomisEventId: Long,

  @Schema(description = "Mapping type", allowableValues = ["NOMIS_CREATED", "APPOINTMENT_CREATED"], required = true)
  @field:NotBlank
  @field:Size(max = 20, message = "mappingType has a maximum length of 20")
  val mappingType: String,

  @Schema(description = "Date-time the mapping was created")
  val whenCreated: LocalDateTime? = null
) {
  constructor(mapping: AppointmentMapping) : this(
    appointmentInstanceId = mapping.appointmentInstanceId,
    nomisEventId = mapping.nomisEventId,
    mappingType = mapping.mappingType.name,
  )
}
