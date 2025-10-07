package uk.gov.justice.digital.hmpps.nomismappingservice.incidents

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
@RequestMapping("/mapping/incidents", produces = [MediaType.APPLICATION_JSON_VALUE])
class IncidentMappingResource(private val mappingService: IncidentMappingService) {

  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new Incident Report mapping",
    description = "Creates a mapping between a Nomis incident report id and DPS Incident report id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = IncidentMappingDto::class),
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
        description = "Indicates a duplicate incident has been rejected. If Error code = 409 the body will return a DuplicateErrorResponse",
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
    createMappingRequest: IncidentMappingDto,
  ) = try {
    mappingService.createIncidentMapping(createMappingRequest)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "Incident mapping already exists, detected by $e",
      duplicate = createMappingRequest,
      cause = e,
    )
  }

  @GetMapping("/nomis-incident-id/{nomisIncidentId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by nomisIncidentId. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = IncidentMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Nomis incident id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingByNomisId(
    @Schema(description = "Nomis Incident Id", required = true)
    @PathVariable
    nomisIncidentId: Long,
  ): IncidentMappingDto = mappingService.getMappingByNomisId(nomisIncidentId)

  @GetMapping("/dps-incident-id/{dpsIncidentId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by DPS Incident Id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = IncidentMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "DPS Incident id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingByDPSId(
    @Schema(description = "DPS Incident id", example = "12345", required = true)
    @PathVariable
    dpsIncidentId: String,
  ): IncidentMappingDto = mappingService.getMappingByDPSId(dpsIncidentId)

  @GetMapping("/nomis-incident-id")
  @Operation(
    summary = "get a list of mappings for Nomis Incident id",
    description = "Retrieves matching mappings for a list of NOMIS Incident ids. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = IncidentMappingDto::class)),
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
    @RequestParam(name = "nomisIncidentId")
    @Parameter(required = true, description = "Nomis Incident Id", example = "345")
    nomisIncidentIds: List<Long>,
  ): List<IncidentMappingDto> = mappingService.getMappingsByNomisId(nomisIncidentIds)

  @DeleteMapping("/dps-incident-id/{dpsIncidentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a specific incident mapping by DPS incident id",
    description = "Deletes the incident from the mapping table. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Incident mapping deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteMapping(
    @Schema(description = "DPS Incident Id", example = "4321", required = true)
    @PathVariable
    dpsIncidentId: String,
  ) = mappingService.deleteMappingByDPSId(dpsIncidentId)

  @DeleteMapping()
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes incident mappings.",
    description = "Deletes all rows from the incidents mapping table. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Incident mappings deleted",
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
            schema = Schema(implementation = IncidentMappingDto::class),
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
  suspend fun getMigratedMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<IncidentMappingDto> = mappingService.getMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

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
            schema = Schema(implementation = IncidentMappingDto::class),
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
  suspend fun getMappingForLatestMigrated(): IncidentMappingDto = mappingService.getMappingForLatestMigrated()
}
