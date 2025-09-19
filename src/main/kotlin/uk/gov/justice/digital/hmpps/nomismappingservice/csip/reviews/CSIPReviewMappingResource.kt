package uk.gov.justice.digital.hmpps.nomismappingservice.csip.reviews

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
@PreAuthorize("hasRole('ROLE_NOMIS_CSIP')")
@RequestMapping("/mapping/csip/reviews", produces = [MediaType.APPLICATION_JSON_VALUE])
class CSIPReviewMappingResource(
  private val reviewMappingService: CSIPReviewMappingService,
) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new CSIP Review mapping",
    description = "Creates a mapping between a Nomis CSIP review id and DPS CSIP review id. Requires role NOMIS_CSIP",
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
  suspend fun createReviewMapping(
    @RequestBody
    @Valid
    csipReviewMapping: CSIPChildMappingDto,
  ) = try {
    reviewMappingService.createMapping(csipReviewMapping)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "CSIP Review mapping already exists, detected by $e",
      duplicate = csipReviewMapping,
      existing = getExistingMappingSimilarTo(csipReviewMapping),
      cause = e,
    )
  }

  @GetMapping("/nomis-csip-review-id/{nomisCSIPReviewId}")
  @Operation(
    summary = "get CSIP Review mapping",
    description = "Retrieves a CSIP Review mapping by NOMIS id. Requires role NOMIS_CSIP",
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
    @Schema(description = "NOMIS CSIP Review id", example = "12345", required = true)
    @PathVariable
    nomisCSIPReviewId: Long,
  ): CSIPChildMappingDto = reviewMappingService.getMappingByNomisId(nomisCSIPReviewId = nomisCSIPReviewId)

  @GetMapping("/dps-csip-review-id/{dpsCSIPReviewId}")
  @Operation(
    summary = "get CSIP Review mapping",
    description = "Retrieves a csip review mapping by DPS id. Requires role NOMIS_CSIP",
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
  suspend fun getCSIPReviewMappingByDpsId(
    @Schema(description = "DPS CSIP review id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsCSIPReviewId: String,
  ): CSIPChildMappingDto = reviewMappingService.getMappingByDpsId(dpsCSIPReviewId = dpsCSIPReviewId)

  @DeleteMapping("/dps-csip-review-id/{dpsCSIPReviewId}")
  @Operation(
    summary = "Deletes CSIP Review mapping",
    description = "Deletes a CSIP Review mapping by DPS id. Requires role NOMIS_CSIP",
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
  suspend fun deleteCSIPReviewMappingByDpsId(
    @Schema(description = "DPS CSIP Review id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsCSIPReviewId: String,
  ) = reviewMappingService.deleteMappingByDpsId(dpsCSIPReviewId = dpsCSIPReviewId)

  private suspend fun getExistingMappingSimilarTo(mapping: CSIPChildMappingDto) = runCatching {
    reviewMappingService.getMappingByNomisId(nomisCSIPReviewId = mapping.nomisId)
  }.getOrElse {
    reviewMappingService.getMappingByDpsId(dpsCSIPReviewId = mapping.dpsId)
  }
}
