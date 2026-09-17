package uk.gov.justice.digital.hmpps.nomismappingservice.finance

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
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
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/advances", produces = [MediaType.APPLICATION_JSON_VALUE])
class AdvanceMappingResource(private val service: AdvanceService) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates an advance mapping",
    description = "Creates an advance mapping. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = AdvanceMappingDto::class),
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
  suspend fun createAdvanceMapping(
    @RequestBody @Valid
    mapping: AdvanceMappingDto,
  ) = try {
    service.createMapping(mapping)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "Advance mapping already exists",
      duplicate = mapping,
      existing = getExistingMappingSimilarTo(mapping),
      cause = e,
    )
  }

  @GetMapping("/nomis-id/{nomisAdvanceId}")
  @Operation(
    summary = "Get advance mapping by Nomis advanceId",
    description = "Retrieves the advance mapping by Nomis advance Id (aka nomis offender payment profile id). Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Advance mapping data",
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
  suspend fun getAdvanceMappingByNomisId(
    @Schema(description = "NOMIS advance id (aka nomis offender payment profile id)", example = "123456", required = true)
    @PathVariable
    nomisAdvanceId: Long,
  ): AdvanceMappingDto = service.getMappingByNomisId(nomisAdvanceId = nomisAdvanceId)

  @GetMapping("/dps-id/{dpsId}")
  @Operation(
    summary = "Get advance mapping by DPS id",
    description = "Retrieves the advance mapping by DPS id. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Advance mapping data",
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
  suspend fun getAdvanceMappingByDpsId(
    @Schema(description = "DPS id", example = "A1234BC", required = true)
    @PathVariable
    dpsId: String,
  ): AdvanceMappingDto = service.getMappingByDpsId(dpsId = dpsId)

  @GetMapping
  @Operation(
    summary = "Get all paged Advance mappings",
    description = "Retrieve all mappings. Results are paged.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping page returned",
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
  suspend fun getAdvanceMappings(
    @PageableDefault pageRequest: Pageable,
  ): PagedModel<AdvanceMappingDto> = service.getAllMappings(pageRequest = pageRequest)

  @DeleteMapping("/dps-id/{dpsId}")
  @Operation(
    summary = "Deletes a Advance mapping",
    description = "Deletes a Advance mapping by DPS id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
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
  suspend fun deleteAdvanceMappingByDpsId(
    @Schema(description = "DPS id", example = "A1234BC", required = true)
    @PathVariable
    dpsId: String,
  ) = service.deleteAdvanceMappingByDpsId(dpsId = dpsId)

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes all Advance mappings",
    description = """Deletes all Advance mappings regardless of source.
      This is expected to only ever been used in a non-production environment. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW""",
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
  suspend fun deleteAllAdvanceMappings() = service.deleteAllMappings()

  @GetMapping("/migration-id/{migrationId}")
  @Operation(
    summary = "Get paged advance mappings by migration id",
    description = "Retrieve all advance mappings of type 'MIGRATED' for the given migration id (identifies a single migration run). Results are paged. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Advance mapping page returned",
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
  suspend fun getAdvanceMappingByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): PagedModel<AdvanceMappingDto> = service.getMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  private suspend fun getExistingMappingSimilarTo(mapping: AdvanceMappingDto) = runCatching {
    service.getMappingByNomisId(
      nomisAdvanceId = mapping.nomisAdvanceId,
    )
  }.getOrElse {
    service.getMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }
}
