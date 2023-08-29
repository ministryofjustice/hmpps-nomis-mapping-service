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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.NonAssociationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NonAssociationMappingService

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
class NonAssociationsMappingResource(private val mappingService: NonAssociationMappingService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
  @PostMapping("/mapping/non-associations")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new Non-association mapping",
    description = "Creates a mapping between a Nomis non-association and non-association instance id. Requires role NOMIS_NON_ASSOCIATIONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = NonAssociationMappingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping entry created"),
      ApiResponse(
        responseCode = "409",
        description = "Non-association mapping already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate non-association has been rejected. If Error code = 409 the body will return a DuplicateErrorResponse",
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
    createMappingRequest: NonAssociationMappingDto,
  ) =
    try {
      mappingService.createNonAssociationMapping(createMappingRequest)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Non-association mapping already exists, detected by $e",
        duplicate = createMappingRequest,
        cause = e,
      )
    }

  @PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
  @GetMapping("/mapping/non-associations/firstOffenderNo/{firstOffenderNo}/secondOffenderNo/{secondOffenderNo}/typeSequence/{typeSequence}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by firstOffenderNo, secondOffenderNo and Nomis type sequence. Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = NonAssociationMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Non association id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getNonAssociationMappingGivenNomisId(
    @Schema(description = "First offender number", example = "A1234BC", required = true)
    @PathVariable
    firstOffenderNo: String,
    @Schema(description = "Second offender number", example = "D5678EF", required = true)
    @PathVariable
    secondOffenderNo: String,
    @Schema(description = "Nomis type sequence", example = "2", required = true)
    @PathVariable
    typeSequence: Int,
  ): NonAssociationMappingDto = mappingService.getNonAssociationMappingByNomisId(firstOffenderNo, secondOffenderNo, typeSequence)

  @PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
  @GetMapping("/mapping/non-associations/nonAssociationId/{nonAssociationId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by Non-Association Id. Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = NonAssociationMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Non association id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getNonAssociationMappingGivenNonAssociationId(
    @Schema(description = "Non-association id", example = "2", required = true)
    @PathVariable
    nonAssociationId: Long,
  ): NonAssociationMappingDto = mappingService.getNonAssociationMappingByNonAssociationId(nonAssociationId)
}
