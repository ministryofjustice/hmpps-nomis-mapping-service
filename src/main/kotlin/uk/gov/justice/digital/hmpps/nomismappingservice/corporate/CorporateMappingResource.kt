package uk.gov.justice.digital.hmpps.nomismappingservice.corporate

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
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
@PreAuthorize("hasRole('NOMIS_CONTACTPERSONS')")
@RequestMapping("/mapping/corporate", produces = [MediaType.APPLICATION_JSON_VALUE])
class CorporateMappingResource(private val service: CorporateService) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping("/migrate")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a tree of corporate mappings typically for a migration",
    description = "Creates a tree of corporate mappings typically for a migration between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CorporateMappingsDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mappings created"),
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
  suspend fun createMappings(
    @RequestBody @Valid
    mappings: CorporateMappingsDto,
  ) = try {
    service.createMappings(mappings)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingCorporateMappingSimilarTo(mappings.corporateMapping)
    if (existingMapping == null) {
      log.error("Child duplicate key found for corporate even though the corporate has never been migrated", e)
    }
    throw DuplicateMappingException(
      messageIn = "Corporate mapping already exists",
      duplicate = mappings.asOrganisationsMappingDto(),
      existing = existingMapping ?: mappings.asOrganisationsMappingDto(),
      cause = e,
    )
  }

  @GetMapping("/organisation/migration-id/{migrationId}")
  @Operation(
    summary = "Get paged corporate mappings by migration id",
    description = "Retrieve all corporate mappings of type 'MIGRATED' for the given migration id (identifies a single migration run). Results are paged. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Corporate mapping page returned",
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
  suspend fun getCorporateMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<OrganisationsMappingDto> = service.getCorporateMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes all corporate mappings",
    description = "Deletes all corporate mappings regardless of source. This includes corporate, phone, address, address phone, email, web. This is expected to only ever been used in a non-production environment. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(responseCode = "204", description = "All mappings deleted"),
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
  suspend fun deleteAllMappings() = service.deleteAllMappings()

  @PostMapping("/organisation")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates corporate mappings for synchronisation",
    description = "Creates mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class))],
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
  suspend fun createOrganisationMapping(
    @RequestBody @Valid
    mapping: OrganisationsMappingDto,
  ) = try {
    service.createOrganisationMapping(mapping)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingOrganisationMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Mapping already exists",
      duplicate = mapping,
      existing = existingMapping ?: mapping,
      cause = e,
    )
  }

  @GetMapping("/organisation")
  @Operation(
    summary = "Get paged corporate mappings by migration id",
    description = "Retrieve all corporate mappings. Results are paged. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Corporate mapping page returned",
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
  suspend fun getAllCorporateMappings(
    @PageableDefault pageRequest: Pageable,
  ): Page<OrganisationsMappingDto> = service.getAllCorporateMappings(pageRequest = pageRequest)

  @GetMapping("/organisation/nomis-corporate-id/{nomisCorporateId}")
  @Operation(
    summary = "Get corporate mapping by nomis corporate Id",
    description = "Retrieves the corporate mapping by NOMIS Corporate Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Corporate mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class)),
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
  suspend fun getCorporateMappingByNomisId(
    @Schema(description = "NOMIS corporate id", example = "12345", required = true)
    @PathVariable
    nomisCorporateId: Long,
  ): OrganisationsMappingDto = service.getCorporateMappingByNomisId(nomisId = nomisCorporateId)

  @GetMapping("/organisation/dps-organisation-id/{dpsOrganisationId}")
  @Operation(
    summary = "Get corporate mapping by dps organisation Id",
    description = "Retrieves the corporate mapping by DPS organisation Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Corporate mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class)),
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
  suspend fun getCorporateMappingByDpsId(
    @Schema(description = "DPS organisation id", example = "12345", required = true)
    @PathVariable
    dpsOrganisationId: String,
  ): OrganisationsMappingDto = service.getCorporateMappingByDpsId(dpsId = dpsOrganisationId)

  @DeleteMapping("/organisation/dps-organisation-id/{dpsOrganisationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes corporate mapping by dps organisation Id",
    description = "Deletes the corporate mapping by DPS Organisation Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Deletes Corporate mapping data or it doesn't exist",
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
  suspend fun deleteCorporateMappingByDpsId(
    @Schema(description = "DPS organisation id", example = "12345", required = true)
    @PathVariable
    dpsOrganisationId: String,
  ) = service.deleteCorporateMappingByDpsId(dpsId = dpsOrganisationId)

  @DeleteMapping("/organisation/nomis-corporate-id/{nomisCorporateId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes corporate mapping by nomis corporate Id",
    description = "Deletes the corporate mapping by NOMIS Corporate Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Deletes Corporate mapping data or it doesn't exist",
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
  suspend fun deleteCorporateMappingByNomisId(
    @Schema(description = "NOMIS corporate id", example = "12345", required = true)
    @PathVariable
    nomisCorporateId: Long,
  ) = service.deleteCorporateMappingByNomisId(nomisId = nomisCorporateId)

  @PostMapping("/address")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates address mappings for synchronisation",
    description = "Creates mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class))],
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
  suspend fun createAddressMapping(
    @RequestBody @Valid
    mapping: OrganisationsMappingDto,
  ) = try {
    service.createAddressMapping(mapping)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingAddressMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Mapping already exists",
      duplicate = mapping,
      existing = existingMapping ?: mapping,
      cause = e,
    )
  }

  @GetMapping("/address/nomis-address-id/{nomisAddressId}")
  @Operation(
    summary = "Get address mapping by nomis address Id",
    description = "Retrieves the address mapping by NOMIS Address Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Address mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class)),
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
  suspend fun getAddressMappingByNomisId(
    @Schema(description = "NOMIS address id", example = "12345", required = true)
    @PathVariable
    nomisAddressId: Long,
  ): OrganisationsMappingDto = service.getAddressMappingByNomisId(nomisId = nomisAddressId)

  @GetMapping("/address/dps-address-id/{dpsAddressId}")
  @Operation(
    summary = "Get address mapping by dps address Id",
    description = "Retrieves the address mapping by DPS Address Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Address mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class)),
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
  suspend fun getAddressMappingByDpsId(
    @Schema(description = "DPS address id", example = "12345", required = true)
    @PathVariable
    dpsAddressId: String,
  ): OrganisationsMappingDto = service.getAddressMappingByDpsId(dpsId = dpsAddressId)

  @DeleteMapping("/address/dps-address-id/{dpsAddressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes address mapping by dps address Id",
    description = "Deletes the address mapping by DPS Address Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Address mapping data deleted or doesn't exist",
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
  suspend fun deleteAddressMappingByDpsId(
    @Schema(description = "DPS address id", example = "12345", required = true)
    @PathVariable
    dpsAddressId: String,
  ) = service.deleteAddressMappingByDpsId(dpsId = dpsAddressId)

  @DeleteMapping("/address/nomis-address-id/{nomisAddressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes address mapping by nomis address Id",
    description = "Deletes the address mapping by NOMIS Address Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Address mapping data deleted or doesn't exist",
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
  suspend fun deleteAddressMappingByNomisId(
    @Schema(description = "NOMIS address id", example = "12345", required = true)
    @PathVariable
    nomisAddressId: Long,
  ) = service.deleteAddressMappingByNomisId(nomisId = nomisAddressId)

  @PostMapping("/address-phone")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates address phone mappings for synchronisation",
    description = "Creates mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class))],
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
  suspend fun createAddressPhoneMapping(
    @RequestBody @Valid
    mapping: OrganisationsMappingDto,
  ) = try {
    service.createAddressPhoneMapping(mapping)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingAddressPhoneMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Mapping already exists",
      duplicate = mapping,
      existing = existingMapping ?: mapping,
      cause = e,
    )
  }

  @GetMapping("/address-phone/nomis-phone-id/{nomisPhoneId}")
  @Operation(
    summary = "Get address phone mapping by nomis phone Id",
    description = "Retrieves the addressPhone mapping by NOMIS phone Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "AddressPhone mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class)),
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
  suspend fun getAddressPhoneMappingByNomisId(
    @Schema(description = "NOMIS phone id", example = "12345", required = true)
    @PathVariable
    nomisPhoneId: Long,
  ): OrganisationsMappingDto = service.getAddressPhoneMappingByNomisId(nomisId = nomisPhoneId)

  @GetMapping("/address-phone/dps-address-phone-id/{dpsAddressPhoneId}")
  @Operation(
    summary = "Get address phone mapping by dps address phone Id",
    description = "Retrieves the address mapping by DPS Address Phone Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Address mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class)),
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
  suspend fun getAddressPhoneMappingByDpsId(
    @Schema(description = "DPS address phone id", example = "12345", required = true)
    @PathVariable
    dpsAddressPhoneId: String,
  ): OrganisationsMappingDto = service.getAddressPhoneMappingByDpsId(dpsId = dpsAddressPhoneId)

  @DeleteMapping("/address-phone/dps-address-phone-id/{dpsPhoneId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes address phone mapping by dps phone Id",
    description = "Deletes the address phone mapping by DPS phone Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Deletes AddressPhone mapping data or it doesn't exist",
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
  suspend fun deleteAddressPhoneMappingByDpsId(
    @Schema(description = "DPS phone id", example = "12345", required = true)
    @PathVariable
    dpsPhoneId: String,
  ) = service.deleteAddressPhoneMappingByDpsId(dpsId = dpsPhoneId)

  @DeleteMapping("/address-phone/nomis-phone-id/{nomisPhoneId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes address phone mapping by nomis phone Id",
    description = "Deletes the address phone mapping by NOMIS phone Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Deletes AddressPhone mapping data or it doesn't exist",
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
  suspend fun deleteAddressPhoneMappingByNomisId(
    @Schema(description = "NOMIS phone id", example = "12345", required = true)
    @PathVariable
    nomisPhoneId: Long,
  ) = service.deleteAddressPhoneMappingByNomisId(nomisId = nomisPhoneId)

  @PostMapping("/phone")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates phone mappings for synchronisation",
    description = "Creates mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class))],
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
  suspend fun createPhoneMapping(
    @RequestBody @Valid
    mapping: OrganisationsMappingDto,
  ) = try {
    service.createPhoneMapping(mapping)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingPhoneMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Mapping already exists",
      duplicate = mapping,
      existing = existingMapping ?: mapping,
      cause = e,
    )
  }

  @GetMapping("/phone/nomis-phone-id/{nomisPhoneId}")
  @Operation(
    summary = "Get phone mapping by nomis phone Id",
    description = "Retrieves the phone mapping by NOMIS Phone Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Phone mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class)),
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
  suspend fun getPhoneMappingByNomisId(
    @Schema(description = "NOMIS phone id", example = "12345", required = true)
    @PathVariable
    nomisPhoneId: Long,
  ): OrganisationsMappingDto = service.getPhoneMappingByNomisId(nomisId = nomisPhoneId)

  @GetMapping("/phone/dps-phone-id/{dpsPhoneId}")
  @Operation(
    summary = "Get phone mapping by dps  phone Id",
    description = "Retrieves the  mapping by DPS Phone Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = " mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class)),
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
  suspend fun getPhoneMappingByDpsId(
    @Schema(description = "DPS  phone id", example = "12345", required = true)
    @PathVariable
    dpsPhoneId: String,
  ): OrganisationsMappingDto = service.getPhoneMappingByDpsId(dpsId = dpsPhoneId)

  @DeleteMapping("/phone/dps-phone-id/{dpsPhoneId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete phone mapping by dps phone Id",
    description = "Deletes the phone mapping by DPS Phone Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Delete Phone mapping data or does not exist",
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
  suspend fun deletePhoneMappingByDpsId(
    @Schema(description = "DPS phone id", example = "12345", required = true)
    @PathVariable
    dpsPhoneId: String,
  ) = service.deletePhoneMappingByDpsId(dpsId = dpsPhoneId)

  @DeleteMapping("/phone/nomis-phone-id/{nomisPhoneId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete phone mapping by nomis phone Id",
    description = "Deletes the phone mapping by NOMIS Phone Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Delete Phone mapping data or does not exist",
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
  suspend fun deletePhoneMappingByNomisId(
    @Schema(description = "NOMIS phone id", example = "12345", required = true)
    @PathVariable
    nomisPhoneId: Long,
  ) = service.deletePhoneMappingByNomisId(nomisId = nomisPhoneId)

  @PostMapping("/email")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates email mappings for synchronisation",
    description = "Creates mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class))],
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
  suspend fun createEmailMapping(
    @RequestBody @Valid
    mapping: OrganisationsMappingDto,
  ) = try {
    service.createEmailMapping(mapping)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingEmailMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Mapping already exists",
      duplicate = mapping,
      existing = existingMapping ?: mapping,
      cause = e,
    )
  }

  @GetMapping("/email/nomis-internet-address-id/{nomisEmailId}")
  @Operation(
    summary = "Get email mapping by nomis email Id",
    description = "Retrieves the email mapping by NOMIS Email Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Email mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class)),
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
  suspend fun getEmailMappingByNomisId(
    @Schema(description = "NOMIS email id", example = "12345", required = true)
    @PathVariable
    nomisEmailId: Long,
  ): OrganisationsMappingDto = service.getEmailMappingByNomisId(nomisId = nomisEmailId)

  @GetMapping("/email/dps-email-id/{dpsEmailId}")
  @Operation(
    summary = "Get email mapping by dps  email Id",
    description = "Retrieves the  mapping by DPS Email Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = " mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class)),
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
  suspend fun getEmailMappingByDpsId(
    @Schema(description = "DPS  email id", example = "12345", required = true)
    @PathVariable
    dpsEmailId: String,
  ): OrganisationsMappingDto = service.getEmailMappingByDpsId(dpsId = dpsEmailId)

  @DeleteMapping("/email/dps-email-id/{dpsEmailId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete email mapping by dps email Id",
    description = "Deletes the email mapping by DPS Email Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Deletes Email mapping data or it doesn't exist",
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
  suspend fun deleteEmailMappingByDpsId(
    @Schema(description = "DPS email id", example = "12345", required = true)
    @PathVariable
    dpsEmailId: String,
  ) = service.deleteEmailMappingByDpsId(dpsId = dpsEmailId)

  @DeleteMapping("/email/nomis-internet-address-id/{nomisEmailId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete email mapping by nomis email Id",
    description = "Deletes the email mapping by NOMIS Email Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Deletes Email mapping data or it doesn't exist",
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
  suspend fun deleteEmailMappingByNomisId(
    @Schema(description = "NOMIS email id", example = "12345", required = true)
    @PathVariable
    nomisEmailId: Long,
  ) = service.deleteEmailMappingByNomisId(nomisId = nomisEmailId)

  @PostMapping("/web")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates web mappings for synchronisation",
    description = "Creates mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class))],
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
  suspend fun createWebMapping(
    @RequestBody @Valid
    mapping: OrganisationsMappingDto,
  ) = try {
    service.createWebMapping(mapping)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingWebMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Mapping already exists",
      duplicate = mapping,
      existing = existingMapping ?: mapping,
      cause = e,
    )
  }

  @GetMapping("/web/nomis-internet-address-id/{nomisWebId}")
  @Operation(
    summary = "Get web mapping by nomis web Id",
    description = "Retrieves the web mapping by NOMIS Web Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Web mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class)),
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
  suspend fun getWebMappingByNomisId(
    @Schema(description = "NOMIS web id", example = "12345", required = true)
    @PathVariable
    nomisWebId: Long,
  ): OrganisationsMappingDto = service.getWebMappingByNomisId(nomisId = nomisWebId)

  @GetMapping("/web/dps-web-id/{dpsWebId}")
  @Operation(
    summary = "Get web mapping by dps web address Id",
    description = "Retrieves the  mapping by DPS Web Address Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = " mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = OrganisationsMappingDto::class)),
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
  suspend fun getWebMappingByDpsId(
    @Schema(description = "DPS  web id", example = "12345", required = true)
    @PathVariable
    dpsWebId: String,
  ): OrganisationsMappingDto = service.getWebMappingByDpsId(dpsId = dpsWebId)

  @DeleteMapping("/web/dps-web-id/{dpsWebId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete web mapping by dps web Id",
    description = "Deletes the web mapping by DPS Web Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Delete Web mapping data or does not exist",
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
  suspend fun deleteWebMappingByDpsId(
    @Schema(description = "DPS web address id", example = "12345", required = true)
    @PathVariable
    dpsWebId: String,
  ) = service.deleteWebMappingByDpsId(dpsId = dpsWebId)

  @DeleteMapping("/web/nomis-internet-address-id/{nomisWebId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete web mapping by nomis web Id",
    description = "Deletes the web mapping by NOMIS Web Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Delete Web mapping data or does not exist",
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
  suspend fun deleteWebMappingByNomisId(
    @Schema(description = "NOMIS web id", example = "12345", required = true)
    @PathVariable
    nomisWebId: Long,
  ) = service.deleteWebMappingByNomisId(nomisId = nomisWebId)

  private suspend fun getExistingCorporateMappingSimilarTo(corporateMapping: CorporateMappingIdDto) = runCatching {
    service.getCorporateMappingByNomisId(
      nomisId = corporateMapping.nomisId,
    )
  }.getOrElse {
    service.getCorporateMappingByDpsIdOrNull(
      dpsId = corporateMapping.dpsId,
    )
  }

  private suspend fun getExistingOrganisationMappingSimilarTo(mapping: OrganisationsMappingDto) = runCatching {
    service.getCorporateMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getCorporateMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }

  private suspend fun getExistingAddressMappingSimilarTo(mapping: OrganisationsMappingDto) = runCatching {
    service.getAddressMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getAddressMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }

  private suspend fun getExistingAddressPhoneMappingSimilarTo(mapping: OrganisationsMappingDto) = runCatching {
    service.getAddressPhoneMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getAddressPhoneMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }

  private suspend fun getExistingPhoneMappingSimilarTo(mapping: OrganisationsMappingDto) = runCatching {
    service.getPhoneMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getPhoneMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }

  private suspend fun getExistingEmailMappingSimilarTo(mapping: OrganisationsMappingDto) = runCatching {
    service.getEmailMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getEmailMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }
  private suspend fun getExistingWebMappingSimilarTo(mapping: OrganisationsMappingDto) = runCatching {
    service.getWebMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getWebMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }
}

private fun CorporateMappingsDto.asOrganisationsMappingDto() = OrganisationsMappingDto(
  dpsId = corporateMapping.dpsId,
  nomisId = corporateMapping.nomisId,
  mappingType = mappingType,
  label = label,
  whenCreated = whenCreated,
)
