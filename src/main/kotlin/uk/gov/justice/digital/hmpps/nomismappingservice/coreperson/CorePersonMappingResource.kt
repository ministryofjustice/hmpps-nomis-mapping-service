package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

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
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/core-person", produces = [MediaType.APPLICATION_JSON_VALUE])
class CorePersonMappingResource(private val service: CorePersonService) {
  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a tree of core person mappings typically for a migration",
    description = "Creates a tree of core person mappings typically for a migration between NOMIS ids and cpr ids. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CorePersonMappingsDto::class),
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
  suspend fun createCorePersonMappings(
    @RequestBody @Valid
    mappings: CorePersonMappingsDto,
  ) = try {
    service.createMappings(mappings)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingCorePersonMappingSimilarTo(mappings.personMapping)
    if (existingMapping == null) {
      log.error("Child duplicate key found for core person even though the core person has never been migrated", e)
    }
    throw DuplicateMappingException(
      messageIn = "Core Person mapping already exists",
      duplicate = mappings.asCorePersonMappingDto(),
      existing = existingMapping ?: mappings.asCorePersonMappingDto(),
      cause = e,
    )
  }

  @PostMapping("/replace")
  @Operation(
    summary = "Replaces addresses and contacts mappings.",
    description = """Creates lists of addresses and contacts mappings and removes the previous supplied set.
      This does not add or remove any data from any associated parent CorePersonMappingDto entry. 
      Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW""",
    responses = [
      ApiResponse(responseCode = "200", description = "Mappings replaced"),
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
  suspend fun replaceCorePersonMappings(
    @RequestBody @Valid
    mappings: CorePersonMappingsDto,
  ) = service.replaceMappings(mappings).also {
    log.info("Replaced mappings for nomis prisoner number ${mappings.personMapping.nomisPrisonNumber}")
  }

  @PostMapping("/merge")
  @Operation(
    summary = "Replaces mappings for a merge",
    description = """Replaces mappings for a merge with the provided mappings on the to person. The mappings of the from person will be deleted 
      Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW""",
    responses = [
      ApiResponse(responseCode = "200", description = "Mappings replaced"),
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
  suspend fun merge(
    @RequestBody @Valid
    mappings: CorePersonMergeMappingsDto,
  ) = service.merge(mappings).also {
    log.info("Replaced mappings for merged to nomis prisoner number ${mappings.toNomisPrisonNumber} deleted mappings for the merged from nomis prisoner number ${mappings.fromNomisPrisonNumber}")
  }

  @DeleteMapping("/delete")
  @Operation(
    summary = "Deletes the core person migration so that it can be rerun on all of the prisoners",
    description = """Deletes. 
      Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW""",
    responses = [
      ApiResponse(responseCode = "200", description = "Core person migration deleted"),
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
  suspend fun deleteCorePersonMigration() = service.deleteCorePersonMigration().also {
    log.info("Deleted core person migration")
  }

  @GetMapping("/migration-id/{migrationId}")
  @Operation(
    summary = "Get paged core person mappings by migration id",
    description = "Retrieve all core person mappings of type 'MIGRATED' for the given migration id (identifies a single migration run). Results are paged. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Core Person mapping page returned",
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
  suspend fun getPersonMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<CorePersonMappingDto> = service.getCorePersonMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @GetMapping("/person/nomis-prison-number/{nomisPrisonNumber}")
  @Operation(
    summary = "Get person mapping by nomis prison number",
    description = "Retrieves the core person mapping by Nomis Prison Number (Offender number). Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Core Person mapping data",
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
  suspend fun getPersonMappingByNomisPrisonNumber(
    @Schema(description = "NOMIS prison Number aka offender no.", example = "A1234BC", required = true)
    @PathVariable
    nomisPrisonNumber: String,
  ): CorePersonMappingDto = service.getCorePersonMappingByNomisPrisonNumber(nomisPrisonNumber = nomisPrisonNumber)

  @GetMapping("/person/cpr-id/{cprId}")
  @Operation(
    summary = "Get core person mapping by cpr core person Id",
    description = "Retrieves the person mapping by CPR Core Person Id. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person mapping data",
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
  suspend fun getPersonMappingByCprId(
    @Schema(description = "CPR core person id", example = "12345", required = true)
    @PathVariable
    cprId: String,
  ): CorePersonMappingDto = service.getCorePersonMappingByCprId(cprId = cprId)

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes all core person mappings",
    description = "Deletes all core person mappings regardless of source. This includes person and address." +
      // TODO add other child tables in the description once implemented
      " This is expected to only ever been used in a non-production environment. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
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
  suspend fun deleteAllCorePersonMappings() = service.deleteAllMappings()

  @PostMapping("/mappings/by-nomis-ids")
  @Operation(
    summary = "Get mapping by nomis ids",
    description = """Retrieves mappings by nomis ids. 
    Used during a merge to find existing corresponding cpr ids. 
    These can then be passed these to the cpr service and allow cpr to maintain them.
    Maintaining cpr ids allows down stream services to continue to use them.
    Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping data",
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
  suspend fun getMappingsFromNomisIds(
    @RequestBody mappingRequest: CorePersonMappingsByNomisIdsRequest,
  ): CorePersonMappingsResult = service.getMappingResultFromNomisIds(mappingRequest)

  private suspend fun getExistingCorePersonMappingSimilarTo(personMapping: CorePersonMappingIdDto) = runCatching {
    service.getCorePersonMappingByNomisPrisonNumber(
      nomisPrisonNumber = personMapping.nomisPrisonNumber,
    )
  }.getOrElse {
    service.getCorePersonMappingByCprIdOrNull(
      cprId = personMapping.cprId,
    )
  }
}

private fun CorePersonMappingsDto.asCorePersonMappingDto() = CorePersonMappingDto(
  cprId = personMapping.cprId,
  nomisPrisonNumber = personMapping.nomisPrisonNumber,
  mappingType = mappingType,
  label = label,
  whenCreated = whenCreated,
)
