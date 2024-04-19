package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.LocationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.LocationMappingService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
class LocationMappingResource(private val mappingService: LocationMappingService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_LOCATIONS')")
  @PostMapping("/mapping/locations")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new Location mapping",
    description = "Creates a mapping between a Nomis location id and a DPS location id. Requires role NOMIS_LOCATIONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = LocationMappingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping entry created"),
      ApiResponse(
        responseCode = "409",
        description = "Location mapping already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate location has been rejected. If Error code = 409 the body will return a DuplicateErrorResponse",
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
    createMappingRequest: LocationMappingDto,
  ) =
    try {
      mappingService.createMapping(createMappingRequest)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Location mapping already exists, detected by $e",
        duplicate = createMappingRequest,
        cause = e,
      )
    }

  @PreAuthorize("hasRole('ROLE_NOMIS_LOCATIONS')")
  @GetMapping("/mapping/locations/nomis/{nomisLocationId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by Nomis location id. Requires role NOMIS_LOCATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = LocationMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Location id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingGivenNomisId(
    @Schema(description = "Nomis location id", example = "12345678", required = true)
    @PathVariable
    nomisLocationId: Long,
  ): LocationMappingDto = mappingService.getMappingByNomisId(nomisLocationId)

  @PreAuthorize("hasRole('ROLE_NOMIS_LOCATIONS')")
  @GetMapping("/mapping/locations/dps/{dpsLocationId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by DPS Location Id. Requires role NOMIS_LOCATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = LocationMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Location id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingGivenDpsId(
    @Schema(description = "DPS Location id", example = "22345678", required = true)
    @PathVariable
    dpsLocationId: String,
  ): LocationMappingDto = mappingService.getMappingByDpsId(dpsLocationId)

  @PreAuthorize("hasRole('ROLE_NOMIS_LOCATIONS')")
  @GetMapping("/mapping/locations/migration-id/{migrationId}")
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
            schema = Schema(implementation = LocationMappingDto::class),
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
  ): Page<LocationMappingDto> =
    mappingService.getMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @PreAuthorize("hasRole('ROLE_NOMIS_LOCATIONS')")
  @GetMapping("/mapping/locations/migrated/latest")
  @Operation(
    summary = "get the latest mapping for a migration",
    description = "Requires role NOMIS_LOCATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = LocationMappingDto::class),
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
  suspend fun getLatestMigratedMapping(): LocationMappingDto =
    mappingService.getMappingForLatestMigrated()

  @PreAuthorize("hasRole('ROLE_NOMIS_LOCATIONS')")
  @DeleteMapping("/mapping/locations/dps/{locationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a specific location mapping by DPS location id",
    description = "Deletes the location from the mapping table. Requires role NOMIS_LOCATIONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Location mapping deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteMappingGivenDpsId(
    @Schema(description = "DPS Location Id", example = "1234abcd-5678-1234-5678-0123456789ab", required = true)
    @PathVariable
    locationId: String,
  ) = mappingService.deleteMapping(locationId)

  @PreAuthorize("hasRole('ROLE_NOMIS_LOCATIONS')")
  @DeleteMapping("/mapping/locations/nomis/{locationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a specific location mapping by Nomis location id",
    description = "Deletes the location from the mapping table. Requires role NOMIS_LOCATIONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Location mapping deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteMappingGivenNomisId(
    @Schema(description = "Nomis Location Id", example = "12345678", required = true)
    @PathVariable
    locationId: Long,
  ) = mappingService.deleteMapping(locationId)

  @PreAuthorize("hasRole('ROLE_NOMIS_LOCATIONS')")
  @DeleteMapping("/mapping/locations")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes location mappings.",
    description = "Deletes all rows from the locations mapping table. Requires role NOMIS_LOCATIONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Location mappings deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteMappings(
    @RequestParam(value = "onlyMigrated", required = false, defaultValue = "false")
    @Parameter(
      description = "if true delete mapping entries created by the migration process only (synchronisation records are unaffected)",
      example = "true",
    )
    onlyMigrated: Boolean,
  ) = mappingService.deleteMappings(onlyMigrated)
}
