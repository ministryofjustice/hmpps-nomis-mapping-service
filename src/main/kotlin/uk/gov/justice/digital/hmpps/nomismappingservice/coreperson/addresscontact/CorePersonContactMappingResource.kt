package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact

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
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonContactMappingDto
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/core-person", produces = [MediaType.APPLICATION_JSON_VALUE])
class CorePersonContactMappingResource(private val service: CorePersonContactService) {

  @GetMapping("/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}")
  @Operation(
    summary = "Get a core person contact mapping by nomis contact Id and contact type",
    description = "Retrieves the core person contact mapping by NOMIS Contact Id and contact type. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Core Person contact mapping data",
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
  suspend fun getCorePersonContactMappingByNomisId(
    @Schema(description = "NOMIS contact id", example = "12345", required = true)
    @PathVariable
    nomisContactId: Long,
    @Schema(description = "NOMIS contact type", example = "PHONE", required = true)
    @PathVariable
    nomisContactType: NomisContactType,
  ): CorePersonContactMappingDto = service.getContactMappingByNomisId(nomisId = nomisContactId, nomisContactType = nomisContactType)

  @DeleteMapping("/contact/nomis-contact-id/{nomisContactId}/type/{nomisContactType}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete core person contact mapping by nomis contact Id and contact type",
    description = "Delete the core person contact mapping by NOMIS Contact Id and contact type. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "204", description = "Core person contact mapping deleted"),
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
  suspend fun deleteCorePersonContactMappingByNomisId(
    @Schema(description = "NOMIS contact id", example = "12345", required = true)
    @PathVariable
    nomisContactId: Long,
    @Schema(description = "NOMIS contact type", example = "PHONE", required = true)
    @PathVariable
    nomisContactType: NomisContactType,
  ) = service.deleteContactMappingByNomisId(nomisId = nomisContactId, nomisContactType = nomisContactType)

  @GetMapping("/contact/cpr-contact-id/{cprContactId}")
  @Operation(
    summary = "Get a core person contact mapping by cpr contact Id",
    description = "Retrieves the core person contact mapping by CPR Contact Id. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Core Person contact mapping data",
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
  suspend fun getCorePersonContactMappingByCprId(
    @Schema(description = "CPR contact id", example = "12345", required = true)
    @PathVariable
    cprContactId: String,
  ): CorePersonContactMappingDto = service.getContactMappingByCprId(cprId = cprContactId)

  @PostMapping("/contact")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates core person contact mappings for synchronisation",
    description = "Creates core person contact mappings for synchronisation between NOMIS ids and CPR ids. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CorePersonContactMappingDto::class))],
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
  suspend fun createCorePersonContactMapping(
    @RequestBody @Valid
    mapping: CorePersonContactMappingDto,
  ) = try {
    service.createContactMapping(mapping)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingCorePersonContactMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Core person contact mapping already exists",
      duplicate = mapping,
      existing = existingMapping ?: mapping,
      cause = e,
    )
  }

  private suspend fun getExistingCorePersonContactMappingSimilarTo(mapping: CorePersonContactMappingDto) = runCatching {
    service.getContactMappingByNomisId(mapping.nomisId, mapping.nomisContactType)
  }.getOrElse {
    service.getContactMappingByCprIdOrNull(mapping.cprId)
  }
}
