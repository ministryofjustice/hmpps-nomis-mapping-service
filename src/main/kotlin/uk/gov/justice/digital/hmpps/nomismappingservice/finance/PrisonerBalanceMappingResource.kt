package uk.gov.justice.digital.hmpps.nomismappingservice.finance

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
@PreAuthorize("hasRole('NOMIS_MAPPINGS_SYNCHRONISATION_RW')")
@RequestMapping("/mapping/prisoner-balance", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerBalanceMappingResource(private val service: PrisonerBalanceService) {
  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a prisoner account balance mapping",
    description = "Creates a prisoner account balance mapping. Requires ROLE_NOMIS_MAPPINGS_SYNCHRONISATION_RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = PrisonerBalanceMappingDto::class),
        ),
      ],
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
        description = "Indicates a duplicate mapping has been rejected. If Error code = 409 the body will return a DuplicateErrorResponse",
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
    mapping: PrisonerBalanceMappingDto,
  ) = try {
    service.createMapping(mapping)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "Prisoner Balance mapping already exists",
      duplicate = mapping,
      existing = getExistingMappingSimilarTo(mapping),
      cause = e,
    )
  }

  @GetMapping("/nomis-id/{nomisRootOffenderId}")
  @Operation(
    summary = "Get prisoner balance mapping by Nomis rootOffenderId",
    description = "Retrieves the prisoner balance mapping by Nomis rootOffenderId. Requires role ROLE_NOMIS_MAPPINGS_SYNCHRONISATION_RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner balance mapping data",
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
  suspend fun getMappingByNomisId(
    @Schema(description = "NOMIS prisoner balance id.", example = "1", required = true)
    @PathVariable
    nomisRootOffenderId: Long,
  ): PrisonerBalanceMappingDto = service.getMappingByNomisId(nomisRootOffenderId = nomisRootOffenderId)

  @GetMapping("/dps-id/{dpsId}")
  @Operation(
    summary = "Get prisoner balance mapping by DPS id",
    description = "Retrieves the prisoner balance mapping by DPS id. Requires role ROLE_NOMIS_MAPPINGS_SYNCHRONISATION_RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner balance mapping data",
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
  suspend fun getMappingByDpsId(
    @Schema(description = "DPS id", example = "A1234BC", required = true)
    @PathVariable
    dpsId: String,
  ): PrisonerBalanceMappingDto = service.getMappingByDpsId(dpsId = dpsId)

  @DeleteMapping("/dps-id/{dpsId}")
  @Operation(
    summary = "Deletes a Prisoner balance mapping",
    description = "Deletes a Prisoner balance mapping by DPS id. Requires role NOMIS_MAPPINGS_SYNCHRONISATION_RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Mapping Deleted",
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
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend fun deleteMappingByDpsId(
    @Schema(description = "DPS id", example = "A1234BC", required = true)
    @PathVariable
    dpsId: String,
  ) = service.deletePrisonerBalanceMappingByDpsId(dpsId = dpsId)

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes all prisoner balance mappings",
    description = """Deletes all prisoner balance mappings regardless of source.
      This is expected to only ever been used in a non-production environment. Requires role ROLE_NOMIS_MAPPINGS_SYNCHRONISATION_RW""",
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

  @GetMapping("/migration-id/{migrationId}")
  @Operation(
    summary = "Get paged Prisoner balance mappings by migration id",
    description = "Retrieve all Prisoner balance mappings of type 'MIGRATED' for the given migration id (identifies a single migration run). Results are paged. Requires role ROLE_NOMIS_MAPPINGS_SYNCHRONISATION_RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner balance mapping page returned",
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
  suspend fun getMappingByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<PrisonerBalanceMappingDto> = service.getMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  private suspend fun getExistingMappingSimilarTo(mapping: PrisonerBalanceMappingDto) = runCatching {
    service.getMappingByNomisId(
      nomisRootOffenderId = mapping.nomisRootOffenderId,
    )
  }.getOrElse {
    service.getMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }
}
