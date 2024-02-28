package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

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

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_COURT_SENTENCING')")
@RequestMapping("/mapping/court-sentencing", produces = [MediaType.APPLICATION_JSON_VALUE])
class CourtSentencingMappingResource(private val mappingService: CourtSentencingMappingService) {
  @GetMapping("/court-cases/dps-court-case-id/{courtCaseId}")
  @Operation(
    summary = "get court case mapping",
    description = "Retrieves a mapping by DPS id. Requires role NOMIS_COURT_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CourtCaseMappingDto::class)),
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
    @Schema(description = "DPS court case id", example = "D123", required = true)
    @PathVariable
    courtCaseId: String,
  ): CourtCaseMappingDto = mappingService.getCourtCaseMappingByDpsId(
    courtCaseId = courtCaseId,
  )

  @PostMapping("/court-cases")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new court case hierarchical mapping",
    description = "Creates a mapping between nomis Court Case ID and DPS Court Case ID. Also maps child entities: Court appearances and charges. Requires ROLE_NOMIS_COURT_SENTENCING",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CourtCaseAllMappingDto::class),
        ),
      ],
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
    mapping: CourtCaseAllMappingDto,
  ) =
    try {
      mappingService.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Court Case mapping already exists",
        duplicate = mapping,
        existing = getExistingMappingSimilarTo(mapping),
        cause = e,
      )
    }

  @GetMapping("/court-appearances/dps-court-appearance-id/{courtAppearanceId}")
  @Operation(
    summary = "get court appearance mapping",
    description = "Retrieves a mapping by DPS id. Requires role NOMIS_COURT_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CourtAppearanceMappingDto::class)),
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
  suspend fun getCourtAppearanceMappingByNomisId(
    @Schema(description = "DPS court appearance id", example = "D123", required = true)
    @PathVariable
    courtAppearanceId: String,
  ): CourtAppearanceMappingDto = mappingService.getCourtAppearanceMappingByDpsId(
    courtAppearanceId = courtAppearanceId,
  )

  @PostMapping("/court-appearances")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new court appearance mapping",
    description = "Creates a mapping between nomis Court appearance ID and DPS Court appearance ID. Requires ROLE_NOMIS_COURT_SENTENCING",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CourtAppearanceMappingDto::class),
        ),
      ],
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
  suspend fun createCourtAppearanceMapping(
    @RequestBody @Valid
    mapping: CourtAppearanceMappingDto,
  ) =
    try {
      mappingService.createCourtAppearanceMapping(mapping)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Court Case mapping already exists",
        duplicate = mapping,
        existing = getExistingCourtAppearanceMappingSimilarTo(mapping),
        cause = e,
      )
    }

  private suspend fun getExistingMappingSimilarTo(mapping: CourtCaseAllMappingDto) = runCatching {
    mappingService.getCourtCaseMappingByNomisId(
      courtCaseId = mapping.nomisCourtCaseId,
    )
  }.getOrElse {
    mappingService.getCourtCaseMappingByDpsId(
      courtCaseId = mapping.dpsCourtCaseId,
    )
  }

  private suspend fun getExistingCourtAppearanceMappingSimilarTo(mapping: CourtAppearanceMappingDto) = runCatching {
    mappingService.getCourtAppearanceMappingByNomisId(
      courtAppearanceId = mapping.nomisCourtAppearanceId,
    )
  }.getOrElse {
    mappingService.getCourtAppearanceMappingByDpsId(
      courtAppearanceId = mapping.dpsCourtAppearanceId,
    )
  }
}
