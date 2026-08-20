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
@RequestMapping("/mapping/core-person/alias", produces = [MediaType.APPLICATION_JSON_VALUE])
class CorePersonAliasMappingResource(private val service: CorePersonService) {

  @GetMapping("/nomis-offender-id/{nomisOffenderId}")
  @Operation(
    summary = "Get offender alias mapping by nomis offender id",
    description = "Retrieves the offender alias mapping by NOMIS offender id. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender alias mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OffenderAliasMappingDto::class)),
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
  suspend fun getOffenderAliasMappingByNomisId(
    @Schema(description = "NOMIS offender id", example = "12345", required = true)
    @PathVariable
    nomisOffenderId: Long,
  ): OffenderAliasMappingDto = service.getOffenderAliasMappingByNomisId(nomisOffenderId = nomisOffenderId)

  @DeleteMapping("/nomis-offender-id/{nomisOffenderId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete offender alias mapping by nomis offender id",
    description = "Delete the offender alias mapping by NOMIS offender id. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Offender alias mapping deleted",
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
  suspend fun deleteOffenderAliasMappingByNomisId(
    @Schema(description = "NOMIS offender id", example = "12345", required = true)
    @PathVariable
    nomisOffenderId: Long,
  ) = service.deleteOffenderAliasMappingByNomisId(nomisOffenderId = nomisOffenderId)

  @GetMapping("/cpr-id/{cprId}")
  @Operation(
    summary = "Get offender alias mapping by cpr id",
    description = "Retrieves the offender alias mapping by CPR id. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender alias mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OffenderAliasMappingDto::class)),
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
  suspend fun getOffenderAliasMappingByCprId(
    @Schema(description = "CPR id", example = "12345", required = true)
    @PathVariable
    cprId: String,
  ): OffenderAliasMappingDto = service.getOffenderAliasMappingByCprId(cprId = cprId)

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates offender alias mappings for synchronisation",
    description = "Creates offender alias mappings for synchronisation between NOMIS ids and CPR ids. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = OffenderAliasMappingDto::class))],
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
  suspend fun createOffenderAliasMapping(
    @RequestBody @Valid
    mapping: OffenderAliasMappingDto,
  ) = try {
    service.createOffenderAliasMapping(mapping)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingOffenderAliasMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Offender alias mapping already exists",
      duplicate = mapping,
      existing = existingMapping ?: mapping,
      cause = e,
    )
  }

  private suspend fun getExistingOffenderAliasMappingSimilarTo(mapping: OffenderAliasMappingDto) = runCatching {
    service.getOffenderAliasMappingByNomisId(mapping.nomisOffenderId)
  }.getOrElse {
    service.getOffenderAliasMappingByCprIdOrNull(mapping.cprId)
  }
}
