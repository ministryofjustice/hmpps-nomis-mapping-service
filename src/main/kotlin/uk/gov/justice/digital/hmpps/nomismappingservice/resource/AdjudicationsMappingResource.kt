package uk.gov.justice.digital.hmpps.nomismappingservice.resource

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
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomismappingservice.data.AdjudicationAllMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.data.AdjudicationDeleteMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.data.AdjudicationMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.service.AdjudicationMappingService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
class AdjudicationsMappingResource(private val mappingService: AdjudicationMappingService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
  @PostMapping("/mapping/adjudications")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new adjudication mapping",
    description = "Creates a record of a adjudication number. Requires NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = AdjudicationMappingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping entry created"),
      ApiResponse(
        responseCode = "409",
        description = "Adjudication with charge sequence already exist",
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
    createMappingRequest: AdjudicationMappingDto,
  ) = try {
    mappingService.createMapping(createMappingRequest)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "Adjudication mapping already exists, detected by $e",
      duplicate = createMappingRequest,
      cause = e,
    )
  }

  @PreAuthorize("hasRole('ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
  @PostMapping("/mapping/adjudications/all")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new adjudication mapping along with associated hearings and punishments",
    description = "Creates a record of a adjudication number, hearing and punishment. Requires NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = AdjudicationAllMappingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping entry created"),
      ApiResponse(
        responseCode = "409",
        description = "Adjudication with charge sequence already exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun createAllMappings(
    @RequestBody @Valid
    createMappingRequest: AdjudicationAllMappingDto,
  ) = try {
    mappingService.createMapping(createMappingRequest)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "Adjudication mapping already exists, detected by $e",
      duplicate = createMappingRequest,
      cause = e,
    )
  }

  @PreAuthorize("hasRole('ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
  @GetMapping("/mapping/adjudications/charge-number/{chargeNumber}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by DPS charge number. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = AdjudicationMappingDto::class)),
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
  suspend fun getMappingGivenNomisId(
    @Schema(description = "DPS Charge number", example = "12345/1", required = true)
    @PathVariable
    chargeNumber: String,
  ): AdjudicationMappingDto = mappingService.getMappingByDpsId(chargeNumber)

  @PreAuthorize("hasRole('ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
  @GetMapping("/mapping/adjudications/adjudication-number/{adjudicationNumber}/charge-sequence/{chargeSequence}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by NOMIS adjudication number and charge sequence. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = AdjudicationMappingDto::class)),
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
  suspend fun getMappingGivenDpsId(
    @Schema(description = "Adjudication number", example = "12345", required = true)
    @PathVariable
    adjudicationNumber: Long,
    @Schema(description = "Charge sequence for offence in this adjudication", example = "1", required = true)
    @PathVariable
    chargeSequence: Int,
  ): AdjudicationMappingDto = mappingService.getMappingByNomisId(adjudicationNumber, chargeSequence)

  @PreAuthorize("hasRole('ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
  @GetMapping("/mapping/adjudications/migrated/latest")
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
            schema = Schema(implementation = AdjudicationMappingDto::class),
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
  suspend fun getLatestMigratedAdjudicationMapping(): AdjudicationMappingDto = mappingService.getAdjudicationMappingForLatestMigrated()

  @PreAuthorize("hasRole('ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
  @DeleteMapping("/mapping/adjudications/charge-number/{chargeNumber}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a specific mapping by DPS charge number",
    description = "Deletes the mapping table row. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Mapping deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteMapping(
    @Schema(description = "Adjudication number", example = "12345", required = true)
    @PathVariable
    chargeNumber: String,
  ) = mappingService.deleteMapping(chargeNumber)

  @PreAuthorize("hasRole('ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
  @GetMapping("/mapping/adjudications/migration-id/{migrationId}")
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
            schema = Schema(implementation = AdjudicationMappingDto::class),
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
  suspend fun getMigratedAdjudicationMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<AdjudicationMappingDto> = mappingService.getAdjudicationMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @PreAuthorize("hasRole('ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
  @GetMapping("/mapping/adjudications")
  @Operation(
    summary = "Get all adjudication mappings",
    description = "Get all the mapping table rows. Should only be used in dev (in pre/prod this could return excessive data). Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getAllMappings() = mappingService.getAllMappings()

  @PreAuthorize("hasRole('ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
  @PostMapping("/mapping/adjudications/delete-mappings")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete all mappings related to a specific adjudication",
    description = "Delete mapping entries created for a specific adjudication and associated hearings and punishments",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = AdjudicationDeleteMappingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Adjudication mappings deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteMappingsForAdjudication(
    @RequestBody @Valid
    deleteMappingRequest: AdjudicationDeleteMappingDto,
  ) = mappingService.deleteMappingsForAdjudication(deleteMappingRequest)
}
