package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

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
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/core-person/identifier", produces = [MediaType.APPLICATION_JSON_VALUE])
class CorePersonIdentifierMappingResource(private val service: CorePersonService) {

  @GetMapping("/nomis-offender-id/{nomisOffenderId}/nomis-identifier-sequence/{nomisIdentifierSequence}")
  @Operation(
    summary = "Get offender identifier mapping by nomis offender id and sequence",
    description = "Retrieves the offender identifier mapping by NOMIS offender id and NOMIS identifier sequence. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender identifier mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OffenderIdentifierMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Access this endpoint is forbidden",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getOffenderIdentifierMappingByNomisId(
    @Schema(description = "NOMIS offender id", example = "12345", required = true)
    @PathVariable
    nomisOffenderId: Long,
    @Schema(description = "NOMIS identifier sequence", example = "4", required = true)
    @PathVariable
    nomisIdentifierSequence: Int,
  ): OffenderIdentifierMappingDto = service.getOffenderIdentifierMappingByNomisId(
    nomisOffenderId = nomisOffenderId,
    nomisIdentifierSequence = nomisIdentifierSequence,
  )

  @DeleteMapping("/nomis-offender-id/{nomisOffenderId}/nomis-identifier-sequence/{nomisIdentifierSequence}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete offender identifier mapping by nomis offender id and sequence",
    description = "Delete the offender identifier mapping by NOMIS offender id and NOMIS identifier sequence. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Offender identifier mapping deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Access this endpoint is forbidden",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteOffenderIdentifierMappingByNomisId(
    @Schema(description = "NOMIS offender id", example = "12345", required = true)
    @PathVariable
    nomisOffenderId: Long,
    @Schema(description = "NOMIS identifier sequence", example = "4", required = true)
    @PathVariable
    nomisIdentifierSequence: Int,
  ) = service.deleteOffenderIdentifierMappingByNomisId(
    nomisOffenderId = nomisOffenderId,
    nomisIdentifierSequence = nomisIdentifierSequence,
  )

  @GetMapping("/cpr-id/{cprOffenderIdentifierId}")
  @Operation(
    summary = "Get offender identifier mapping by cpr identifier id",
    description = "Retrieves the offender identifier mapping by CPR identifier id. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender identifier mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OffenderIdentifierMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Access this endpoint is forbidden",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getOffenderIdentifierMappingByCprId(
    @Schema(description = "CPR offender identifier id", example = "12345", required = true)
    @PathVariable
    cprOffenderIdentifierId: String,
  ): OffenderIdentifierMappingDto = service.getOffenderIdentifierMappingByCprId(cprId = cprOffenderIdentifierId)

  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates offender identifier mappings for synchronisation",
    description = "Creates offender identifier mappings for synchronisation between NOMIS ids and CPR ids. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = OffenderIdentifierMappingDto::class))],
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
  suspend fun createOffenderIdentifierMapping(
    @RequestBody @Valid
    mapping: OffenderIdentifierMappingDto,
  ) = try {
    service.createOffenderIdentifierMapping(mapping)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingOffenderIdentifierMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Offender identifier mapping already exists",
      duplicate = mapping,
      existing = existingMapping ?: mapping,
      cause = e,
    )
  }

  private suspend fun getExistingOffenderIdentifierMappingSimilarTo(mapping: OffenderIdentifierMappingDto) = runCatching {
    service.getOffenderIdentifierMappingByNomisId(
      nomisOffenderId = mapping.nomisOffenderId,
      nomisIdentifierSequence = mapping.nomisIdentifierSequence,
    )
  }.getOrElse {
    service.getOffenderIdentifierMappingByCprIdOrNull(
      cprId = mapping.cprId,
    )
  }
}
