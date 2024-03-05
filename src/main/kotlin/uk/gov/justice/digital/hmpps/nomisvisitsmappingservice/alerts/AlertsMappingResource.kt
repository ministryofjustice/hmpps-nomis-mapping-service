package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

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
import org.springframework.web.bind.annotation.DeleteMapping
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

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_ALERTS')")
@RequestMapping("/mapping/alerts", produces = [MediaType.APPLICATION_JSON_VALUE])
class AlertsMappingResource(private val mappingService: AlertMappingService) {
  @GetMapping("/nomis-booking-id/{bookingId}/nomis-alert-sequence/{alertSequence}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by NOMIS id. Requires role NOMIS_ALERTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = AlertMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingByNomisId(
    @Schema(description = "NOMIS booking id", example = "12345", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "NOMIS booking id", example = "2", required = true)
    @PathVariable
    alertSequence: Long,
  ): AlertMappingDto = mappingService.getMappingByNomisId(bookingId = bookingId, alertSequence = alertSequence)

  @GetMapping("/dps-alert-id/{dpsAlertId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by DPS id. Requires role NOMIS_ALERTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = AlertMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingByDpsId(
    @Schema(description = "DPS alert id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsAlertId: String,
  ): AlertMappingDto = mappingService.getMappingByDpsId(alertId = dpsAlertId)

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new alert mapping",
    description = "Creates a mapping between nomis alert ids and dps alert id. Requires ROLE_NOMIS_ALERTS",
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
  suspend fun createMapping(
    @RequestBody @Valid
    mapping: AlertMappingDto,
  ) =
    try {
      mappingService.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Alert mapping already exists",
        duplicate = mapping,
        existing = getExistingMappingSimilarTo(mapping),
        cause = e,
      )
    }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes all alert mappings",
    description = "Deletes all alert mappings regardless of source. This is expected to only ever been used in a non-production environment. Requires ROLE_NOMIS_ALERTS",
    responses = [
      ApiResponse(responseCode = "204", description = "Mappings deleted"),
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
    ],
  )
  suspend fun deleteAllMappings() = mappingService.deleteAllMappings()

  private suspend fun getExistingMappingSimilarTo(mapping: AlertMappingDto) = runCatching {
    mappingService.getMappingByNomisId(
      bookingId = mapping.nomisBookingId,
      alertSequence = mapping.nomisAlertSequence,
    )
  }.getOrElse {
    mappingService.getMappingByDpsId(
      alertId = mapping.dpsAlertId,
    )
  }
}
