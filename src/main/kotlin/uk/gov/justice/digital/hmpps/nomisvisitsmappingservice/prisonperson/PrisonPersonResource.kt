package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson

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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_PRISONPERSON')")
@RequestMapping("/mapping/prisonperson", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonPersonResource(private val service: PrisonPersonMigrationService) {

  @PostMapping("/migration")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new prison person migration mapping",
    description = "Creates a mapping between nomis alert ids and dps alert id. Requires ROLE_NOMIS_PRISONPERSON",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonPersonMigrationMappingRequest::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping created"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Access forbidden for this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate mapping has been rejected",
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
    @RequestBody @Valid mappingRequest: PrisonPersonMigrationMappingRequest,
  ) =
    try {
      service.create(mappingRequest)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Prison Person mapping already exists",
        duplicate = mappingRequest,
        existing = service.find(mappingRequest.nomisPrisonerNumber, mappingRequest.migrationType),
        cause = e,
      )
    }
}
