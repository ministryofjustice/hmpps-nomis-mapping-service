package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AllocationMigrationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.AllocationMigrationService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
class AllocationMigrationResource(private val mappingService: AllocationMigrationService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @PostMapping("/mapping/allocations/migration")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new allocation migration mapping",
    description = "Creates a mapping between nomis allocation id and Actvities allocation id. Requires NOMIS_ACTIVITIES",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = AllocationMigrationMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping entry created"),
      ApiResponse(
        responseCode = "400",
        description = "Nomis or activity schedule ids already exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun createMapping(
    @RequestBody @Valid
    createMappingRequest: AllocationMigrationMappingDto,
  ) = try {
    mappingService.createMapping(createMappingRequest)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "Allocation migration mapping already exists, detected by $e",
      duplicate = createMappingRequest,
      cause = e,
    )
  }

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @GetMapping("/mapping/allocations/migration/nomis-allocation-id/{nomisAllocationId}")
  @Operation(
    summary = "get mapping for an allocation migration",
    description = "Retrieves an allocation migration mapping by the nomis id. Requires role NOMIS_ACTIVITIES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = AllocationMigrationMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMapping(
    @Schema(description = "Nomis allocation Id", example = "12345", required = true) @PathVariable nomisAllocationId: Long,
  ): AllocationMigrationMappingDto = mappingService.getMapping(nomisAllocationId)

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @GetMapping("/mapping/allocations/migrated/latest")
  @Operation(
    summary = "get the latest mapping for an allocation migration",
    description = "Requires role NOMIS_ACTIVITIES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AllocationMigrationMappingDto::class),
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
  suspend fun getLatestMigratedMapping(): AllocationMigrationMappingDto = mappingService.getLatestMigrated()

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @GetMapping("/mapping/allocations/migration/migration-id/{migrationId}")
  @Operation(
    summary = "get paged mappings by migration id",
    description = "Retrieve all allocation migration mappings for the given migration id (identifies a single migration run). Results are paged.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping page returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AllocationMigrationMappingDto::class),
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
  suspend fun getMigratedAllocationMappings(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<AllocationMigrationMappingDto> = mappingService.getMappings(pageRequest = pageRequest, migrationId = migrationId)
}
