package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews

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
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('ROLE_NOMIS_CSIP')")
@RequestMapping("/mapping/csip/interviews", produces = [MediaType.APPLICATION_JSON_VALUE])
class CSIPInterviewMappingResource(
  private val interviewMappingService: CSIPInterviewMappingService,
) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new CSIP Interview mapping",
    description = "Creates a mapping between a Nomis CSIP interview id and DPS CSIP interview id. Requires role NOMIS_CSIP",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CSIPInterviewMappingDto::class),
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
  suspend fun createInterviewMapping(
    @RequestBody
    @Valid
    csipInterviewMapping: CSIPInterviewMappingDto,
  ) =
    try {
      interviewMappingService.createMapping(csipInterviewMapping)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "CSIP Interview mapping already exists, detected by $e",
        duplicate = csipInterviewMapping,
        existing = getExistingMappingSimilarTo(csipInterviewMapping),
        cause = e,
      )
    }

  @GetMapping("/nomis-csip-interview-id/{nomisCSIPInterviewId}")
  @Operation(
    summary = "get CSIP Interview mapping",
    description = "Retrieves a CSIP Interview mapping by NOMIS id. Requires role NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CSIPInterviewMappingDto::class)),
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
    @Schema(description = "NOMIS CSIP Interview id", example = "12345", required = true)
    @PathVariable
    nomisCSIPInterviewId: Long,
  ): CSIPInterviewMappingDto = interviewMappingService.getMappingByNomisId(nomisCSIPInterviewId = nomisCSIPInterviewId)

  @GetMapping("/dps-csip-interview-id/{dpsCSIPInterviewId}")
  @Operation(
    summary = "get CSIP Interview mapping",
    description = "Retrieves a csip interview mapping by DPS id. Requires role NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CSIPInterviewMappingDto::class)),
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
  suspend fun getCSIPInterviewMappingByDpsId(
    @Schema(description = "DPS CSIP interview id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsCSIPInterviewId: String,
  ): CSIPInterviewMappingDto = interviewMappingService.getMappingByDpsId(dpsCSIPInterviewId = dpsCSIPInterviewId)

  @DeleteMapping("/dps-csip-interview-id/{dpsCSIPInterviewId}")
  @Operation(
    summary = "Deletes CSIP Interview mapping",
    description = "Deletes a CSIP Interview mapping by DPS id. Requires role NOMIS_CSIP",
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
  suspend fun deleteCSIPInterviewMappingByDpsId(
    @Schema(description = "DPS CSIP Interview id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsCSIPInterviewId: String,
  ) = interviewMappingService.deleteMappingByDpsId(dpsCSIPInterviewId = dpsCSIPInterviewId)

  private suspend fun getExistingMappingSimilarTo(mapping: CSIPInterviewMappingDto) = runCatching {
    interviewMappingService.getMappingByNomisId(nomisCSIPInterviewId = mapping.nomisCSIPInterviewId)
  }.getOrElse {
    interviewMappingService.getMappingByDpsId(dpsCSIPInterviewId = mapping.dpsCSIPInterviewId)
  }
}
