package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.corporate

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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
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
  ) =
    try {
      service.createMappings(mappings)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingCorporateMappingSimilarTo(mappings.corporateMapping)
      if (existingMapping == null) {
        log.error("Child duplicate key found for corporate even though the corporate has never been migrated", e)
      }
      throw DuplicateMappingException(
        messageIn = "Corporate mapping already exists",
        duplicate = mappings.asCorporateMappingDto(),
        existing = existingMapping ?: mappings.asCorporateMappingDto(),
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
  ): Page<CorporateMappingDto> =
    service.getCorporateMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

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
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CorporateMappingDto::class))],
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
    mapping: CorporateMappingDto,
  ) =
    try {
      service.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingMappingSimilarTo(mapping)
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
  ): Page<CorporateMappingDto> =
    service.getAllCorporateMappings(pageRequest = pageRequest)

  @GetMapping("/organisation/nomis-corporate-id/{nomisCorporateId}")
  @Operation(
    summary = "Get corporate mapping by nomis corporate Id",
    description = "Retrieves the corporate mapping by NOMIS Corporate Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Corporate mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CorporateMappingDto::class)),
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
  ): CorporateMappingDto = service.getCorporateMappingByNomisId(nomisId = nomisCorporateId)

  @GetMapping("/organisation/dps-organisation-id/{dpsOrganisationId}")
  @Operation(
    summary = "Get corporate mapping by dps organisation Id",
    description = "Retrieves the corporate mapping by DPS organisation Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Corporate mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CorporateMappingDto::class)),
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
  ): CorporateMappingDto = service.getCorporateMappingByDpsId(dpsId = dpsOrganisationId)

  @PostMapping("/address")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates address mappings for synchronisation",
    description = "Creates mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CorporateAddressMappingDto::class))],
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
    mapping: CorporateAddressMappingDto,
  ) =
    try {
      service.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingMappingSimilarTo(mapping)
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
          Content(mediaType = "application/json", schema = Schema(implementation = CorporateAddressMappingDto::class)),
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
  ): CorporateAddressMappingDto = service.getAddressMappingByNomisId(nomisId = nomisAddressId)

  @GetMapping("/address/dps-address-id/{dpsAddressId}")
  @Operation(
    summary = "Get address mapping by dps address Id",
    description = "Retrieves the address mapping by DPS Address Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Address mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CorporateAddressMappingDto::class)),
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
  ): CorporateAddressMappingDto = service.getAddressMappingByDpsId(dpsId = dpsAddressId)

  @PostMapping("/address-phone")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates address phone mappings for synchronisation",
    description = "Creates mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CorporateAddressPhoneMappingDto::class))],
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
    mapping: CorporateAddressPhoneMappingDto,
  ) =
    try {
      service.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingMappingSimilarTo(mapping)
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
          Content(mediaType = "application/json", schema = Schema(implementation = CorporateAddressPhoneMappingDto::class)),
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
  ): CorporateAddressPhoneMappingDto = service.getAddressPhoneMappingByNomisId(nomisId = nomisPhoneId)

  @GetMapping("/address-phone/dps-address-phone-id/{dpsAddressPhoneId}")
  @Operation(
    summary = "Get address phone mapping by dps address phone Id",
    description = "Retrieves the address mapping by DPS Address Phone Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Address mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CorporateAddressPhoneMappingDto::class)),
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
  ): CorporateAddressPhoneMappingDto = service.getAddressPhoneMappingByDpsId(dpsId = dpsAddressPhoneId)

  @PostMapping("/phone")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates phone mappings for synchronisation",
    description = "Creates mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CorporatePhoneMappingDto::class))],
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
    mapping: CorporatePhoneMappingDto,
  ) =
    try {
      service.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingMappingSimilarTo(mapping)
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
          Content(mediaType = "application/json", schema = Schema(implementation = CorporatePhoneMappingDto::class)),
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
  ): CorporatePhoneMappingDto = service.getPhoneMappingByNomisId(nomisId = nomisPhoneId)

  @GetMapping("/phone/dps-phone-id/{dpsPhoneId}")
  @Operation(
    summary = "Get phone mapping by dps  phone Id",
    description = "Retrieves the  mapping by DPS Phone Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = " mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CorporatePhoneMappingDto::class)),
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
  ): CorporatePhoneMappingDto = service.getPhoneMappingByDpsId(dpsId = dpsPhoneId)

  @PostMapping("/email")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates email mappings for synchronisation",
    description = "Creates mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CorporateEmailMappingDto::class))],
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
    mapping: CorporateEmailMappingDto,
  ) =
    try {
      service.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingMappingSimilarTo(mapping)
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
          Content(mediaType = "application/json", schema = Schema(implementation = CorporateEmailMappingDto::class)),
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
  ): CorporateEmailMappingDto = service.getEmailMappingByNomisId(nomisId = nomisEmailId)

  @GetMapping("/email/dps-email-id/{dpsEmailId}")
  @Operation(
    summary = "Get email mapping by dps  email Id",
    description = "Retrieves the  mapping by DPS Email Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = " mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CorporateEmailMappingDto::class)),
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
  ): CorporateEmailMappingDto = service.getEmailMappingByDpsId(dpsId = dpsEmailId)

  @PostMapping("/web")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates web mappings for synchronisation",
    description = "Creates mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CorporateWebMappingDto::class))],
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
    mapping: CorporateWebMappingDto,
  ) =
    try {
      service.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingMappingSimilarTo(mapping)
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
          Content(mediaType = "application/json", schema = Schema(implementation = CorporateWebMappingDto::class)),
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
  ): CorporateWebMappingDto = service.getWebMappingByNomisId(nomisId = nomisWebId)

  @GetMapping("/web/dps-web-address-id/{dpsWebId}")
  @Operation(
    summary = "Get web mapping by dps web address Id",
    description = "Retrieves the  mapping by DPS Web Address Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = " mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CorporateWebMappingDto::class)),
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
  ): CorporateWebMappingDto = service.getWebMappingByDpsId(dpsId = dpsWebId)

  private suspend fun getExistingCorporateMappingSimilarTo(corporateMapping: CorporateMappingIdDto) = runCatching {
    service.getCorporateMappingByNomisId(
      nomisId = corporateMapping.nomisId,
    )
  }.getOrElse {
    service.getCorporateMappingByDpsIdOrNull(
      dpsId = corporateMapping.dpsId,
    )
  }

  private suspend fun getExistingMappingSimilarTo(mapping: CorporateMappingDto) = runCatching {
    service.getCorporateMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getCorporateMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }

  private suspend fun getExistingMappingSimilarTo(mapping: CorporateAddressMappingDto) = runCatching {
    service.getAddressMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getAddressMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }

  private suspend fun getExistingMappingSimilarTo(mapping: CorporateAddressPhoneMappingDto) = runCatching {
    service.getAddressPhoneMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getAddressPhoneMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }

  private suspend fun getExistingMappingSimilarTo(mapping: CorporatePhoneMappingDto) = runCatching {
    service.getPhoneMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getPhoneMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }

  private suspend fun getExistingMappingSimilarTo(mapping: CorporateEmailMappingDto) = runCatching {
    service.getEmailMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getEmailMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }
  private suspend fun getExistingMappingSimilarTo(mapping: CorporateWebMappingDto) = runCatching {
    service.getWebMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getWebMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }
}

private fun CorporateMappingsDto.asCorporateMappingDto() = CorporateMappingDto(
  dpsId = corporateMapping.dpsId,
  nomisId = corporateMapping.nomisId,
  mappingType = mappingType,
  label = label,
  whenCreated = whenCreated,
)
