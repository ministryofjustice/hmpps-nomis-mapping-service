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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.NonAssociationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NonAssociationMappingService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@RequestMapping("/mapping/non-associations", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
class NonAssociationsMappingResource(private val mappingService: NonAssociationMappingService) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new Non-association mapping",
    description = "Creates a mapping between a Nomis non-association and non-association instance id. Requires role NOMIS_NON_ASSOCIATIONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = NonAssociationMappingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping entry created"),
      ApiResponse(
        responseCode = "409",
        description = "Non-association mapping already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate non-association has been rejected. If Error code = 409 the body will return a DuplicateErrorResponse",
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
    createMappingRequest: NonAssociationMappingDto,
  ) =
    try {
      mappingService.createNonAssociationMapping(createMappingRequest)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Non-association mapping already exists, detected by $e",
        duplicate = createMappingRequest,
        cause = e,
      )
    }

  @GetMapping("/first-offender-no/{firstOffenderNo}/second-offender-no/{secondOffenderNo}/type-sequence/{typeSequence}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by firstOffenderNo, secondOffenderNo and Nomis type sequence. Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = NonAssociationMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Non association id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getNonAssociationMappingGivenNomisId(
    @Schema(description = "First offender number", example = "A1234BC", required = true)
    @PathVariable
    firstOffenderNo: String,
    @Schema(description = "Second offender number", example = "D5678EF", required = true)
    @PathVariable
    secondOffenderNo: String,
    @Schema(description = "Nomis type sequence", example = "2", required = true)
    @PathVariable
    typeSequence: Int,
  ): NonAssociationMappingDto =
    mappingService.getNonAssociationMappingByNomisId(firstOffenderNo, secondOffenderNo, typeSequence)

  @GetMapping("/non-association-id/{nonAssociationId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by Non-Association Id. Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = NonAssociationMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Non association id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getNonAssociationMappingGivenNonAssociationId(
    @Schema(description = "Non-association id", example = "2", required = true)
    @PathVariable
    nonAssociationId: Long,
  ): NonAssociationMappingDto = mappingService.getNonAssociationMappingByNonAssociationId(nonAssociationId)

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
            schema = Schema(implementation = NonAssociationMappingDto::class),
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
  suspend fun getNonAssociationMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<NonAssociationMappingDto> =
    mappingService.getNonAssociationMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @GetMapping("/migrated/latest")
  @Operation(
    summary = "get the latest mapping for a migration",
    description = "Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = NonAssociationMappingDto::class),
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
  suspend fun getLatestMigratedNonAssociationMapping(): NonAssociationMappingDto =
    mappingService.getNonAssociationMappingForLatestMigrated()

  @DeleteMapping("/non-association-id/{nonAssociationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a specific non-association mapping by nonAssociationId",
    description = "Deletes the non-association from the mapping table. Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Non-association mapping deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteNonAssociationMapping(
    @Schema(description = "Non-association Id", example = "12345", required = true)
    @PathVariable
    nonAssociationId: Long,
  ) = mappingService.deleteNonAssociationMapping(nonAssociationId)

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes non-association mappings.",
    description = "Deletes all rows from the non-associations mapping table. Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Non association mappings deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteNonAssociationMappings(
    @RequestParam(value = "onlyMigrated", required = false, defaultValue = "false")
    @Parameter(
      description = "if true delete mapping entries created by the migration process only (synchronisation records are unaffected)",
      example = "true",
    )
    onlyMigrated: Boolean,
  ) = mappingService.deleteNonAssociationMappings(onlyMigrated)

  @PutMapping("/merge/from/{oldOffenderNo}/to/{newOffenderNo}")
  @Operation(
    summary = "Replaces all occurrences of the 'from' id with the 'to' id in the mapping table",
    description = "Used for update after a prisoner number merge. Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(responseCode = "200", description = "Replacement made, or not present in table"),
      ApiResponse(
        responseCode = "400",
        description = "Replacement would result in an NA with both prisoner numbers the same - requires manual intervention",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun updateMappingsByNomisId(
    @Schema(description = "Old prisoner number to replace", example = "A3456KM", required = true)
    @PathVariable
    oldOffenderNo: String,
    @Schema(description = "New prisoner number to use", example = "A3457LZ", required = true)
    @PathVariable
    newOffenderNo: String,
  ) = mappingService.updateMappingsByNomisId(oldOffenderNo, newOffenderNo)

  @PutMapping("/update-list/from/{oldOffenderNo}/to/{newOffenderNo}")
  @Operation(
    summary = "Updates mappings in list",
    description = "Updates mappings for a given list of non-association pairs. Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(responseCode = "200", description = "Mappings updated"),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun updateMappingsInList(
    @Schema(description = "Old prisoner number to replace", example = "A3456KM", required = true)
    @PathVariable
    oldOffenderNo: String,
    @Schema(description = "New prisoner number to use", example = "A3457LZ", required = true)
    @PathVariable
    newOffenderNo: String,
    @Schema(description = "List of other prisoner numbers whose NA mappings with oldOffenderNo should have oldOffenderNo updated to newOffenderNo.", required = true)
    @RequestBody
    @Valid
    nonAssociations: List<String>,
  ) {
    mappingService.updateMappingsInList(oldOffenderNo, newOffenderNo, nonAssociations)
  }
}
