package uk.gov.justice.digital.hmpps.nomismappingservice.csip

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/csip", produces = [MediaType.APPLICATION_JSON_VALUE])
class CSIPMappingResource(private val mappingService: CSIPMappingService) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new CSIP Report mapping",
    description = "Creates a mapping between a Nomis CSIP report id and DPS CSIP report id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CSIPReportMappingDto::class),
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
  suspend fun createMapping(
    @RequestBody @Valid
    createMappingRequest: CSIPReportMappingDto,
  ) = try {
    mappingService.createCSIPMapping(createMappingRequest)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "CSIP mapping already exists",
      duplicate = createMappingRequest,
      existing = getExistingMappingSimilarTo(createMappingRequest),
      cause = e,
    )
  }

  @PostMapping("/all")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new CSIP Report mapping along with any associated children",
    description = "Creates a mapping between a Nomis CSIP report id and DPS CSIP report id" +
      " and all its children. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CSIPFullMappingDto::class),
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
  suspend fun createMappingWithChildren(
    @RequestBody @Valid
    createFullMappingRequest: CSIPFullMappingDto,
  ) = try {
    mappingService.createCSIPMappingWithChildren(createFullMappingRequest)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "CSIP mapping already exists",
      duplicate = createFullMappingRequest,
      existing = getExistingMappingSimilarTo(createFullMappingRequest),
      cause = e,
    )
  }

  @PostMapping("/children/all")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Adds new child mappings to a CSIP Report",
    description = "Adds child csip mappings. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CSIPFullMappingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping entry/entries created"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate csip child has been rejected. If Error code = 409 the body will return a DuplicateErrorResponse",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateMappingErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun createChildMappings(
    @RequestBody @Valid
    createChildFullMappingRequest: CSIPFullMappingDto,
  ) = try {
    // Sanity check parent exists before creating children
    mappingService.getMappingByDPSCSIPId(createChildFullMappingRequest.dpsCSIPReportId)
    mappingService.createChildMappings(createChildFullMappingRequest)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "CSIP child mapping already exists",
      duplicate = createChildFullMappingRequest,
      existing = getExistingMappingSimilarTo(createChildFullMappingRequest),
      cause = e,
    )
  }

  @GetMapping("/nomis-csip-id/{nomisCSIPId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by nomisCSIPId. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CSIPReportMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Nomis csip id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingByNomisId(
    @Schema(description = "Nomis CSIP Id", required = true)
    @PathVariable
    nomisCSIPId: Long,
  ): CSIPReportMappingDto = mappingService.getMappingByNomisCSIPId(nomisCSIPId)

  @GetMapping("/dps-csip-id/{csipId}/all")
  @Operation(
    summary = "Get full mapping",
    description = "Retrieves a mapping by DPS CSIP Report Id and all associated child mappings. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CSIPFullMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "DPS CSIP Report id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getAllMappingsByDPSId(
    @Schema(description = "DPS CSIP Report Id", example = "12345", required = true)
    @PathVariable
    csipId: String,
  ): CSIPFullMappingDto = mappingService.getFulMappingByDPSCSIPId(csipId)

  @GetMapping("/dps-csip-id/{csipId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by DPS CSIP Id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CSIPReportMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "CSIP id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingByDPSId(
    @Schema(description = "DPS CSIP Id", example = "12345", required = true)
    @PathVariable
    csipId: String,
  ): CSIPReportMappingDto = mappingService.getMappingByDPSCSIPId(csipId)

  @GetMapping("/nomis-csip-id")
  @Operation(
    summary = "get a list of mappings for Nomis CSIP Report ids",
    description = "Retrieves matching mappings for a list of NOMIS CSIP report ids. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CSIPReportMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingsByNomisId(
    @RequestParam(name = "nomisCSIPId", required = true)
    @Parameter(required = true, description = "Nomis CSIP Id", example = "345")
    nomisCSIPReportIds: List<Long>,
  ): List<CSIPReportMappingDto> = mappingService.getMappingsByNomisCSIPId(nomisCSIPReportIds)

  @DeleteMapping("/dps-csip-id/{dpsCSIPId}/all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a specific CSIP report mapping by DPS CSIP id and its associated children",
    description = "Deletes a mapping by DPS id and any children. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "CSIP mappings deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteMapping(
    @Schema(description = "DPS CSIP Id", example = "4321", required = true)
    @PathVariable
    dpsCSIPId: String,
  ) = mappingService.deleteMappingByDPSId(dpsCSIPId)

  @DeleteMapping("/dps-csip-id/{dpsCSIPId}/children")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes all child mappings for a CSIP Report by DPS id",
    description = "Deletes all child mappings associated with a specific CSIP report mapping by DPS CSIP Report id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "CSIP child mappings deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteChildMappings(
    @Schema(description = "DPS CSIP Id", example = "4321", required = true)
    @PathVariable
    dpsCSIPId: String,
  ) = mappingService.deleteChildMappingsByDPSId(dpsCSIPId)

  @DeleteMapping("/all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes CSIP mappings.",
    description = "Deletes all rows from the csip mapping table. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "CSIP mappings deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteAllMappings(
    @RequestParam(value = "onlyMigrated", required = false, defaultValue = "false")
    @Parameter(
      description = "if true delete mapping entries created by the migration process only (synchronisation records are unaffected)",
      example = "true",
    )
    onlyMigrated: Boolean,
  ) = mappingService.deleteMappings(onlyMigrated)

  @GetMapping("/migration-id/{migrationId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get paged mappings by migration id",
    description = "Retrieve all mappings of type 'MIGRATED' for the given migration id (identifies a single migration run). Results are paged.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping page returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CSIPFullMappingDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<CSIPFullMappingDto> = mappingService.getByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @GetMapping("/migrated/latest")
  @Operation(
    summary = "get the latest mapping for a migration",
    description = "Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CSIPFullMappingDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No mappings found at all for any migration",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getLatestMigratedCSIPMapping(): CSIPFullMappingDto = mappingService.getMappingForLatestMigrated()

  private suspend fun getExistingMappingSimilarTo(mapping: CSIPReportMappingDto) = runCatching {
    mappingService.getMappingByNomisCSIPId(
      nomisCSIPReportId = mapping.nomisCSIPReportId,
    )
  }.getOrElse {
    mappingService.getMappingByDPSCSIPId(
      dpsCSIPReportId = mapping.dpsCSIPReportId,
    )
  }

  private suspend fun getExistingMappingSimilarTo(mapping: CSIPFullMappingDto) = runCatching {
    mappingService.getFulMappingNoChildrenByNomisCSIPId(
      nomisCSIPReportId = mapping.nomisCSIPReportId,
    )
  }.getOrElse {
    mappingService.getFulMappingNoChildrenByDPSCSIPId(
      dpsCSIPReportId = mapping.dpsCSIPReportId,
    )
  }
}
