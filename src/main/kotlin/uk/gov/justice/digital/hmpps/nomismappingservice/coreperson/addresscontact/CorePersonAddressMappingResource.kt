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
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonAddressMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonAddressUsageMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/core-person", produces = [MediaType.APPLICATION_JSON_VALUE])
class CorePersonAddressMappingResource(private val service: CorePersonService) {

  @GetMapping("/address/nomis-address-id/{nomisAddressId}")
  @Operation(
    summary = "Get a core person address mapping by nomis address Id",
    description = "Retrieves the core person address mapping by NOMIS Address Id. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Core Person address mapping data",
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
  suspend fun getCorePersonAddressMappingByNomisId(
    @Schema(description = "NOMIS address id", example = "12345", required = true)
    @PathVariable
    nomisAddressId: Long,
  ): CorePersonAddressMappingDto = service.getAddressMappingByNomisId(nomisId = nomisAddressId)

  @DeleteMapping("/address/nomis-address-id/{nomisAddressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete core person address mapping by nomis address Id",
    description = "Delete the core person address mapping by NOMIS Address Id. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "204", description = "Core person address mapping deleted"),
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
  suspend fun deleteCorePersonAddressMappingByNomisId(
    @Schema(description = "NOMIS address id", example = "12345", required = true)
    @PathVariable
    nomisAddressId: Long,
  ) = service.deleteAddressMappingByNomisId(nomisId = nomisAddressId)

  @GetMapping("/address/cpr-address-id/{cprAddressId}")
  @Operation(
    summary = "Get person address mapping by cpr core person address Id",
    description = "Retrieves the person address mapping by CPR Core Person Address Id. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Address mapping data",
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
  suspend fun getAddressMappingByCprId(
    @Schema(description = "CPR address id", example = "12345", required = true)
    @PathVariable
    cprAddressId: String,
  ): CorePersonAddressMappingDto = service.getAddressMappingByCprId(cprId = cprAddressId)

  @PostMapping("/address")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates core person address mappings for synchronisation",
    description = "Creates core person address mappings for synchronisation between NOMIS ids and CPR ids. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CorePersonAddressMappingDto::class))],
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
  suspend fun createCorePersonAddressMapping(
    @RequestBody @Valid
    mapping: CorePersonAddressMappingDto,
  ) = try {
    service.createAddressMapping(mapping)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingCorePersonAddressMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Core person address mapping already exists",
      duplicate = mapping,
      existing = existingMapping ?: mapping,
      cause = e,
    )
  }

  @GetMapping("/address-usage/nomis-address-id/{nomisAddressId}/usage-code/{addressUsageCode}")
  @Operation(
    summary = "Get a core person address usage mapping by nomis address Id and usage code",
    description = "Retrieves the core person address usage mapping by NOMIS Address Id and address usage code. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Core Person address usage mapping data"),
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
  suspend fun getCorePersonAddressUsageMappingByNomisId(
    @Schema(description = "NOMIS address id", example = "12345", required = true)
    @PathVariable
    nomisAddressId: Long,
    @Schema(description = "NOMIS address usage code", example = "HOME", required = true)
    @PathVariable
    addressUsageCode: String,
  ): CorePersonAddressUsageMappingDto = service.getAddressUsageMappingByNomisId(nomisAddressId, addressUsageCode)

  @DeleteMapping("/address-usage/nomis-address-id/{nomisAddressId}/usage-code/{addressUsageCode}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete a core person address usage mapping by nomis address Id and usage code",
    description = "Deletes the core person address usage mapping by NOMIS Address Id and address usage code. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "204", description = "Core person address usage mapping deleted"),
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
  suspend fun deleteCorePersonAddressUsageMappingByNomisId(
    @Schema(description = "NOMIS address id", example = "12345", required = true)
    @PathVariable
    nomisAddressId: Long,
    @Schema(description = "NOMIS address usage code", example = "HOME", required = true)
    @PathVariable
    addressUsageCode: String,
  ) = service.deleteAddressUsageMappingByNomisId(nomisAddressId, addressUsageCode)

  @GetMapping("/address-usage/cpr-address-usage-id/{cprAddressUsageId}")
  @Operation(
    summary = "Get a core person address usage mapping by cpr address usage Id",
    description = "Retrieves the core person address usage mapping by CPR Address Usage Id. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Core Person address usage mapping data"),
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
  suspend fun getCorePersonAddressUsageMappingByCprId(
    @Schema(description = "CPR address usage id", example = "12345", required = true)
    @PathVariable
    cprAddressUsageId: String,
  ): CorePersonAddressUsageMappingDto = service.getAddressUsageMappingByCprId(cprAddressUsageId)

  @PostMapping("/address-usage")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates core person address usage mappings for synchronisation",
    description = "Creates core person address usage mappings for synchronisation between NOMIS ids and CPR ids. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CorePersonAddressUsageMappingDto::class))],
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
  suspend fun createCorePersonAddressUsageMapping(
    @RequestBody @Valid
    mapping: CorePersonAddressUsageMappingDto,
  ) = try {
    service.createAddressUsageMapping(mapping)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingCorePersonAddressUsageMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Core person address usage mapping already exists",
      duplicate = mapping,
      existing = existingMapping ?: mapping,
      cause = e,
    )
  }

  private suspend fun getExistingCorePersonAddressMappingSimilarTo(mapping: CorePersonAddressMappingDto) = runCatching {
    service.getAddressMappingByNomisId(mapping.nomisId)
  }.getOrElse {
    service.getAddressMappingByCprIdOrNull(mapping.cprId)
  }

  private suspend fun getExistingCorePersonAddressUsageMappingSimilarTo(mapping: CorePersonAddressUsageMappingDto) = runCatching {
    service.getAddressUsageMappingByNomisId(mapping.nomisId, mapping.addressUsageCode)
  }.getOrElse {
    service.getAddressUsageMappingByCprIdOrNull(mapping.cprId)
  }
}
