package uk.gov.justice.digital.hmpps.nomismappingservice.csip.attendees

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
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomismappingservice.csip.CSIPChildMappingDto
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/csip/attendees", produces = [MediaType.APPLICATION_JSON_VALUE])
class CSIPAttendeeMappingResource(
  private val attendeeMappingService: CSIPAttendeeMappingService,
) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new CSIP Attendee mapping",
    description = "Creates a mapping between a Nomis CSIP attendee id and DPS CSIP attendee id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CSIPChildMappingDto::class),
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
        description = "Indicates a duplicate csip has been rejected. If Error code = 409 the body will return a DuplicateErrorResponse",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateMappingErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun createAttendeeMapping(
    @RequestBody
    @Valid
    csipAttendeeMapping: CSIPChildMappingDto,
  ) = try {
    attendeeMappingService.createMapping(csipAttendeeMapping)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "CSIP Attendee mapping already exists, detected by $e",
      duplicate = csipAttendeeMapping,
      existing = getExistingMappingSimilarTo(csipAttendeeMapping),
      cause = e,
    )
  }

  @GetMapping("/nomis-csip-attendee-id/{nomisCSIPAttendeeId}")
  @Operation(
    summary = "get CSIP Attendee mapping",
    description = "Retrieves a CSIP Attendee mapping by NOMIS id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CSIPChildMappingDto::class)),
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
    @Schema(description = "NOMIS CSIP Attendee id", example = "12345", required = true)
    @PathVariable
    nomisCSIPAttendeeId: Long,
  ): CSIPChildMappingDto = attendeeMappingService.getMappingByNomisId(nomisCSIPAttendeeId = nomisCSIPAttendeeId)

  @GetMapping("/dps-csip-attendee-id/{dpsCSIPAttendeeId}")
  @Operation(
    summary = "get CSIP Attendee mapping",
    description = "Retrieves a csip attendee mapping by DPS id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CSIPChildMappingDto::class)),
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
  suspend fun getCSIPAttendeeMappingByDpsId(
    @Schema(description = "DPS CSIP attendee id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsCSIPAttendeeId: String,
  ): CSIPChildMappingDto = attendeeMappingService.getMappingByDpsId(dpsCSIPAttendeeId = dpsCSIPAttendeeId)

  @DeleteMapping("/dps-csip-attendee-id/{dpsCSIPAttendeeId}")
  @Operation(
    summary = "Deletes CSIP Attendee mapping",
    description = "Deletes a CSIP Attendee mapping by DPS id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Mapping Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend fun deleteCSIPAttendeeMappingByDpsId(
    @Schema(description = "DPS CSIP Attendee id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsCSIPAttendeeId: String,
  ) = attendeeMappingService.deleteMappingByDpsId(dpsCSIPAttendeeId = dpsCSIPAttendeeId)

  private suspend fun getExistingMappingSimilarTo(mapping: CSIPChildMappingDto) = runCatching {
    attendeeMappingService.getMappingByNomisId(nomisCSIPAttendeeId = mapping.nomisId)
  }.getOrElse {
    attendeeMappingService.getMappingByDpsId(dpsCSIPAttendeeId = mapping.dpsId)
  }
}
