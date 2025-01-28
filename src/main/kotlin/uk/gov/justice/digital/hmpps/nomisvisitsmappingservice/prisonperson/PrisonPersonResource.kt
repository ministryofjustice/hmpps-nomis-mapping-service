package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.migration.PrisonPersonMigrationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.migration.PrisonPersonMigrationMappingRequest
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.migration.PrisonPersonMigrationService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_PRISONPERSON')")
@RequestMapping("/mapping/prisonperson", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonPersonResource(private val service: PrisonPersonMigrationService) {

  @PutMapping("/migration")
  @Operation(
    summary = "Creates or updates a prison person migration mapping",
    description = "Creates or updates a mapping between nomis prisoner numbers and prison person history ids. Requires ROLE_NOMIS_PRISONPERSON",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonPersonMigrationMappingRequest::class))],
    ),
    responses = [
      ApiResponse(responseCode = "200", description = "Mapping created or updated"),
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
  suspend fun upsertMapping(
    @RequestBody @Valid mappingRequest: PrisonPersonMigrationMappingRequest,
  ) = service.upsert(mappingRequest)

  @GetMapping("/migration/migration-id/{migrationId}")
  @Operation(
    summary = "get paged mappings by migration id",
    description = "Retrieve all prison person migration mappings for the given migration id (identifies a single migration run). Results are paged.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping page returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonPersonMigrationMappingRequest::class),
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
  suspend fun getMigratedPrisonPersonMappings(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<PrisonPersonMigrationMapping> = service.getMappings(pageRequest = pageRequest, migrationId = migrationId)
}
