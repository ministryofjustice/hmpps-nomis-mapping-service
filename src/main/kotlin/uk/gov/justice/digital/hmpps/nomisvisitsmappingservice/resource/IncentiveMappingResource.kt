package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.IncentiveMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.IncentiveMappingService
import javax.validation.Valid

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
class IncentiveMappingResource(private val mappingService: IncentiveMappingService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @PostMapping("/mapping/incentives")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new incentive mapping",
    description = "Creates a mapping between nomis Incentive ids and Incentive service id. Requires ROLE_NOMIS_INCENTIVES",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = IncentiveMappingDto::class))]
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Incentive mapping entry created"),
      ApiResponse(
        responseCode = "400",
        description = "Nomis or Incentive ids already exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun createMapping(@RequestBody @Valid createMappingRequest: IncentiveMappingDto) =
    mappingService.createIncentiveMapping(createMappingRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @GetMapping("/mapping/incentives/nomis-booking-id/{bookingId}/nomis-incentive-sequence/{incentiveSequence}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by NOMIS id. Requires role ROLE_NOMIS_INCENTIVES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = IncentiveMappingDto::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "NOMIS incentive id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun getIncentiveMappingGivenNomisId(
    @Schema(description = "NOMIS booking ID", example = "12345", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "NOMIS incentive sequence", example = "2", required = true)
    @PathVariable
    incentiveSequence: Long,
  ): IncentiveMappingDto = mappingService.getIncentiveMappingByNomisId(bookingId, incentiveSequence)

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @GetMapping("/mapping/incentives/incentive-id/{incentiveId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by Incentive id. Requires role ROLE_NOMIS_INCENTIVES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = IncentiveMappingDto::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "VSIP id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun getIncentiveMappingGivenIncentiveId(
    @Schema(description = "Incentive Id", example = "12345", required = true)
    @PathVariable incentiveId: Long
  ): IncentiveMappingDto = mappingService.getIncentiveMappingByIncentiveId(incentiveId)

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @GetMapping("/mapping/incentives/migrated/latest")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get the latest mapping for a migration",
    description = "Requires role READ_MAPPING, UPDATE_MAPPING or ADMIN_MAPPING",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = IncentiveMappingDto::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "No mappings found at all for any migration",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun getLatestMigratedIncentiveMapping(): IncentiveMappingDto = mappingService.getIncentiveMappingForLatestMigrated()

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @DeleteMapping("/mapping/incentives")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes incentive mappings",
    description = "Deletes all rows from the the incentive mapping table. Requires role ADMIN_MAPPING",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Incentive mappings deleted"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun deleteVisitIdMappings(
    @RequestParam(value = "onlyMigrated", required = false, defaultValue = "false")
    @Parameter(
      description = "if true delete mapping entries created by the migration process only (synchronisation records are unaffected)",
      example = "true"
    ) onlyMigrated: Boolean
  ) = mappingService.deleteIncentiveMappings(
    onlyMigrated
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_INCENTIVES')")
  @GetMapping("/mapping/incentives/migration-id/{migrationId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get paged mappings by migration id",
    description = "Retrieve all mappings of type 'MIGRATED' for the given migration id (identifies a single migration run). Results are paged.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping page returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = IncentiveMappingDto::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  suspend fun getMigratedVisitMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable migrationId: String
  ): Page<IncentiveMappingDto> =
    mappingService.getIncentiveMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)
}
