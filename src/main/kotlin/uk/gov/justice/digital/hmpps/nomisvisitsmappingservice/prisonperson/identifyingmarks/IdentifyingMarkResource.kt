package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts.AlertMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.IdentifyingMarkMappingDto
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_PRISONPERSON')")
@RequestMapping("/mapping/prisonperson", produces = [MediaType.APPLICATION_JSON_VALUE])
class IdentifyingMarkResource(private val service: IdentifyingMarkService) {

  @GetMapping("/nomis-booking-id/{bookingId}/identifying-mark-sequence/{sequence}")
  @Operation(
    summary = "Gets a single identifying mark mapping by NOMIS id",
    description = "Gets a single identifying mark mapping by NOMIS id. Requires ROLE_NOMIS_PRISONPERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = IdentifyingMarkMappingDto::class))],
      ),
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
        responseCode = "404",
        description = "Not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getIdentifyingMarkMapping(
    @PathVariable bookingId: Long,
    @PathVariable sequence: Long,
  ) = service.getIdentifyingMarkMapping(bookingId, sequence)

  @GetMapping("/dps-identifying-mark-id/{dpsId}")
  @Operation(
    summary = "Gets all NOMIS identifying mark mappings by DPS id",
    description = "Gets all NOMIS identifying mark mappings by DPS id. Requires ROLE_NOMIS_PRISONPERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping returned",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = IdentifyingMarkMappingDto::class)))],
      ),
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
        responseCode = "404",
        description = "Not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getIdentifyingMarkMappings(
    @PathVariable dpsId: UUID,
  ) = service.getIdentifyingMarkMappings(dpsId)

  @PostMapping("/identifying-mark")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create an identifying mark mapping",
    description = "Create an identifying mark mapping. Requires ROLE_NOMIS_PRISONPERSON",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = AlertMappingDto::class))],
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
        description = "Indicates a duplicate mapping has been rejected. If Error code = 1409 the body will return a DuplicateErrorResponse",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateMappingErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun createIdentifyingMarkMapping(
    @RequestBody mapping: IdentifyingMarkMappingDto,
  ) =
    try {
      service.createIdentifyingMarkMapping(mapping)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Identifying mark mapping already exists",
        duplicate = mapping,
        existing = getExistingMappingSimilarTo(mapping),
        cause = e,
      )
    }

  private suspend fun getExistingMappingSimilarTo(mapping: IdentifyingMarkMappingDto) =
    service.getIdentifyingMarkMapping(mapping.nomisBookingId, mapping.nomisMarksSequence)
}
