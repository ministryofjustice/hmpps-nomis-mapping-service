package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors

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
@RequestMapping("/mapping/csip/factors", produces = [MediaType.APPLICATION_JSON_VALUE])
class CSIPFactorMappingResource(
  private val factorMappingService: CSIPFactorMappingService,
) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new CSIP Factor mapping",
    description = "Creates a mapping between a Nomis CSIP factor id and DPS CSIP factor id. Requires role NOMIS_CSIP",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CSIPFactorMappingDto::class),
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
  suspend fun createFactorMapping(
    @RequestBody
    @Valid
    csipFactorMapping: CSIPFactorMappingDto,
  ) =
    try {
      factorMappingService.createCSIPFactorMapping(csipFactorMapping)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "CSIP Factor mapping already exists, detected by $e",
        duplicate = csipFactorMapping,
        existing = getExistingMappingSimilarTo(csipFactorMapping),
        cause = e,
      )
    }

  @GetMapping("/nomis-csip-factor-id/{nomisCSIPFactorId}")
  @Operation(
    summary = "get CSIP Factor mapping",
    description = "Retrieves a CSIP Factor mapping by NOMIS id. Requires role NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CSIPFactorMappingDto::class)),
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
    @Schema(description = "NOMIS CSIP Factor id", example = "12345", required = true)
    @PathVariable
    nomisCSIPFactorId: Long,
  ): CSIPFactorMappingDto = factorMappingService.getMappingByNomisId(nomisCSIPFactorId = nomisCSIPFactorId)

  @GetMapping("/dps-csip-factor-id/{dpsCSIPFactorId}")
  @Operation(
    summary = "get CSIP Factor mapping",
    description = "Retrieves a csip factor mapping by DPS id. Requires role NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CSIPFactorMappingDto::class)),
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
  suspend fun getCSIPFactorMappingByDpsId(
    @Schema(description = "DPS CSIP factor id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsCSIPFactorId: String,
  ): CSIPFactorMappingDto = factorMappingService.getMappingByDpsId(dpsCSIPFactorId = dpsCSIPFactorId)

  @DeleteMapping("/dps-csip-factor-id/{dpsCSIPFactorId}")
  @Operation(
    summary = "Deletes CSIP Factor mapping",
    description = "Deletes a CSIP Factor mapping by DPS id. Requires role NOMIS_CSIP",
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
  suspend fun deleteCSIPFactorMappingByDpsId(
    @Schema(description = "DPS CSIP Factor id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsCSIPFactorId: String,
  ) = factorMappingService.deleteMappingByDpsId(dpsCSIPFactorId = dpsCSIPFactorId)

  private suspend fun getExistingMappingSimilarTo(mapping: CSIPFactorMappingDto) = runCatching {
    factorMappingService.getMappingByNomisId(nomisCSIPFactorId = mapping.nomisCSIPFactorId)
  }.getOrElse {
    factorMappingService.getMappingByDpsId(dpsCSIPFactorId = mapping.dpsCSIPFactorId)
  }
}
