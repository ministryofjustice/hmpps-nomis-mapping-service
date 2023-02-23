package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AppointmentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.AppointmentMappingService

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
class AppointmentMappingResource(private val mappingService: AppointmentMappingService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_APPOINTMENTS')")
  @PostMapping("/mapping/appointments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new appointment mapping",
    description = "Creates a mapping between nomis id and Appointment instance id. Requires NOMIS_APPOINTMENTS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = AppointmentMappingDto::class))]
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping entry created"),
      ApiResponse(
        responseCode = "400",
        description = "Nomis or appointment instance ids already exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun createMapping(@RequestBody @Valid createMappingRequest: AppointmentMappingDto) =
    mappingService.createMapping(createMappingRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_APPOINTMENTS')")
  @GetMapping("/mapping/appointments/appointment-instance-id/{appointmentInstanceId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by appointment instance id. Requires role NOMIS_APPOINTMENTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AppointmentMappingDto::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun getMappingGivenId(
    @Schema(description = "Appointment instance Id", example = "12345", required = true)
    @PathVariable appointmentInstanceId: Long
  ): AppointmentMappingDto = mappingService.getMappingById(appointmentInstanceId)

  @PreAuthorize("hasRole('ROLE_NOMIS_APPOINTMENTS')")
  @DeleteMapping("/mapping/appointments/appointment-instance-id/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a specific mapping by appointment instance id",
    description = "Deletes the mapping table row. Requires role NOMIS_APPOINTMENTS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Mapping deleted"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun deleteMapping(
    @Schema(description = "Appointment instance Id", example = "12345", required = true)
    @PathVariable id: Long
  ) = mappingService.deleteMapping(id)
}
