package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.IncidentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.IncidentMappingService

@RestController
@Validated
@RequestMapping("/mapping/incidents", produces = [MediaType.APPLICATION_JSON_VALUE])
class IncidentMappingResource(private val mappingService: IncidentMappingService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_INCIDENTS')")
  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new Incident Report mapping",
    description = "Creates a mapping between a Nomis incident report id and DPS Incident report id. Requires role NOMIS_INCIDENTS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = IncidentMappingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping entry created"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate incident has been rejected. If Error code = 409 the body will return a DuplicateErrorResponse",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateMappingErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun createMapping(
    @RequestBody @Valid
    createMappingRequest: IncidentMappingDto,
  ) =
    try {
      mappingService.createIncidentMapping(createMappingRequest)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Incident mapping already exists, detected by $e",
        duplicate = createMappingRequest,
        cause = e,
      )
    }

  @PreAuthorize("hasRole('ROLE_NOMIS_INCIDENTS')")
  @GetMapping("/nomis-incident-id/{nomisIncidentId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by nomisIncidentId. Requires role NOMIS_INCIDENTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = IncidentMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Nomis incident id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getIncidentMappingGivenNomisId(
    @Schema(description = "Nomis Incident Id", required = true)
    @PathVariable
    nomisIncidentId: Long,
  ): IncidentMappingDto = mappingService.getIncidentMappingByNomisId(nomisIncidentId)

  @PreAuthorize("hasRole('ROLE_NOMIS_INCIDENTS')")
  @GetMapping("/incident-id/{incidentId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by DPS Incident Id. Requires role NOMIS_INCIDENTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = IncidentMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Incident id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getIncidentMappingGivenIncidentId(
    @Schema(description = "Incident id", example = "12345", required = true)
    @PathVariable
    incidentId: String,
  ): IncidentMappingDto = mappingService.getIncidentMappingByIncidentId(incidentId)
}