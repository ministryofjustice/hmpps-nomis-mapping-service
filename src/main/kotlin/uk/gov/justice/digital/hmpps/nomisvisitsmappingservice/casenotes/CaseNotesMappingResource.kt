package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.casenotes

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.dao.DuplicateKeyException
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_CASENOTES')")
@RequestMapping("/mapping/casenotes", produces = [MediaType.APPLICATION_JSON_VALUE])
class CaseNotesMappingResource(private val mappingService: CaseNoteMappingService) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new casenote mapping",
    description = "Creates a mapping between nomis casenote id and dps casenote id. Requires ROLE_NOMIS_CASENOTES",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CaseNoteMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping created"),
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
  suspend fun createMapping(
    @RequestBody @Valid
    mapping: CaseNoteMappingDto,
  ) = try {
    mappingService.createMapping(mapping)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "Casenote mapping already exists",
      duplicate = mapping,
      existing = getExistingMappingSimilarTo(mapping),
      cause = e,
    )
  }

  @PostMapping("/batch")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a batch of new casenote mappings",
    description = "Creates a mapping between a batch of nomis casenote ids and dps casenote id. Requires ROLE_NOMIS_CASENOTES",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          array = ArraySchema(schema = Schema(implementation = CaseNoteMappingDto::class)),
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
  suspend fun createMappings(
    @RequestBody @Valid
    mappings: List<CaseNoteMappingDto>,
  ) = try {
    mappingService.createMappings(mappings)
  } catch (e: DuplicateKeyException) {
    val duplicateMapping = getMappingThatIsDuplicate(mappings)
    if (duplicateMapping != null) {
      throw DuplicateMappingException(
        messageIn = "Casenote mapping already exists",
        duplicate = duplicateMapping,
        existing = getExistingMappingSimilarTo(duplicateMapping),
        cause = e,
      )
    }
    throw e
  }

  @GetMapping("{offenderNo}/all")
  @Operation(
    summary = "Gets all case note mappings for a prisoner",
    description = "Gets all the mappings between nomis case note ids and dps case note ids related to specific prisoner created either via migration or synchronisation. Requires NOMIS_CASENOTES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mappings for prisoner",
      ),
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
  suspend fun getMappingsForPrisoner(
    @Schema(description = "NOMIS offender no", example = "A1234KT", required = true)
    @PathVariable
    offenderNo: String,
  ): AllPrisonerCaseNoteMappingsDto = mappingService.getMappings(offenderNo)

  @GetMapping("/nomis-casenote-id/{caseNoteId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by NOMIS id. Requires role NOMIS_CASENOTES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CaseNoteMappingDto::class)),
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
  suspend fun getMappingByNomisId(
    @Schema(description = "NOMIS case note id", example = "23456789", required = true)
    @PathVariable
    caseNoteId: Long,
  ): CaseNoteMappingDto = mappingService.getMappingByNomisId(caseNoteId)

  @PostMapping("/nomis-casenote-id")
  @Operation(
    summary = "get mappings by Nomis id",
    description = "Retrieves multiple mappings by NOMIS case note id. Requires role NOMIS_CASENOTES",
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
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingsByNomisId(
    @Schema(description = "NOMIS case note ids", required = true)
    @RequestBody
    caseNoteIds: List<Long>,
  ): List<CaseNoteMappingDto> = mappingService.getMappingsByNomisId(caseNoteIds)

  @GetMapping("/dps-casenote-id/{dpsCaseNoteId}/all")
  @Operation(
    summary = "get multiple mappings",
    description = "Retrieves mappings by DPS id. In case of past merges, there could be > 1 nomis id per dps id. Requires role NOMIS_CASENOTES",
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
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingsByDpsId(
    @Schema(description = "DPS casenote id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsCaseNoteId: String,
  ): List<CaseNoteMappingDto> = mappingService.getMappingsByDpsId(dpsCaseNoteId)

  @PreAuthorize("hasRole('ROLE_NOMIS_CASENOTES')")
  @GetMapping("/migrated/latest")
  @Operation(
    summary = "get the latest mapping for a migration",
    description = "Requires role NOMIS_CASENOTES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CaseNoteMappingDto::class)),
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
  suspend fun getLatestMigratedCaseNoteMapping(): CaseNoteMappingDto = mappingService.getMappingForLatestMigrated()

  @GetMapping("/migration-id/{migrationId}/count-by-prisoner")
  @Operation(
    summary = "Get count of mappings by migration id grouped by prisoner",
    // description = "Retrieve all mappings of type 'MIGRATED' for the given migration id (identifies a single migration run) grouped by prisoner. Results are paged.",
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
  suspend fun getMappingsByMigrationIdGroupByPrisoner(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Long = mappingService.getCountByMigrationIdGroupedByPrisoner(pageRequest = pageRequest, migrationId = migrationId)

  @DeleteMapping("/nomis-casenote-id/{nomisCaseNoteId}")
  @Operation(
    summary = "Deletes mapping",
    description = "Deletes a mapping by Nomis id. Requires role NOMIS_CASENOTES",
    responses = [
      ApiResponse(responseCode = "204", description = "Mapping Deleted"),
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
  suspend fun deleteMappingByNomisId(
    @Schema(description = "Nomis casenote id", example = "3344556677", required = true)
    @PathVariable
    nomisCaseNoteId: Long,
  ) = mappingService.deleteMapping(nomisCaseNoteId)

  @DeleteMapping("/dps-casenote-id/{dpsCaseNoteId}")
  @Operation(
    summary = "Deletes mapping",
    description = "Deletes mapping by DPS id (there could be more than one nomis id if a merge has taken place). Requires role NOMIS_CASENOTES",
    responses = [
      ApiResponse(responseCode = "204", description = "Mapping Deleted"),
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
  suspend fun deleteMappingsByDpsId(
    @Schema(description = "DPS casenote id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsCaseNoteId: String,
  ) = mappingService.deleteMappings(dpsCaseNoteId)

  @PutMapping("/merge/from/{oldOffenderNo}/to/{newOffenderNo}")
  @Operation(
    summary = "Replaces all occurrences of the 'from' id with the 'to' id in the mapping table",
    description = "Used for update after a prisoner number merge. Requires role ROLE_NOMIS_CASENOTES",
    responses = [
      ApiResponse(responseCode = "200", description = "Replacement made, or not present in table"),
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
  ) {
    mappingService.updateMappingsByNomisId(oldOffenderNo, newOffenderNo)
  }

  @PutMapping("/merge/booking-id/{bookingId}/to/{newOffenderNo}")
  @Operation(
    summary = "For all case notes with the given booking id in the mapping table, sets the offender no to the given 'to' id",
    description = "Used for update after a booking has been moved from one offender to another. Returns the affected case notes. Requires role ROLE_NOMIS_CASENOTES",
    responses = [
      ApiResponse(responseCode = "200", description = "Replacement made, or not present in table"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun updateMappingsByBookingId(
    @Schema(description = "The booking id", example = "1234567", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "New prisoner number to use", example = "A3457LZ", required = true)
    @PathVariable
    newOffenderNo: String,
  ): List<CaseNoteMappingDto> = mappingService.updateMappingsByBookingId(bookingId, newOffenderNo)

  private suspend fun getExistingMappingSimilarTo(mapping: CaseNoteMappingDto) = runCatching {
    mappingService.getMappingByNomisId(nomisCaseNoteId = mapping.nomisCaseNoteId)
  }.getOrElse {
    mappingService.getMappingsByDpsId(dpsCaseNoteId = mapping.dpsCaseNoteId).first()
  }

  private suspend fun getMappingThatIsDuplicate(mappings: List<CaseNoteMappingDto>): CaseNoteMappingDto? = mappings.find {
    // look for each mapping until I find one (i.e. that is there is no exception thrown)
    kotlin.runCatching { getExistingMappingSimilarTo(it) }.map { true }.getOrElse { false }
  }
}
