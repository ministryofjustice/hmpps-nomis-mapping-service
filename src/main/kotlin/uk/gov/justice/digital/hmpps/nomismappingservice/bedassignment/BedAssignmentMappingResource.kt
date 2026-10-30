package uk.gov.justice.digital.hmpps.nomismappingservice.bedassignment

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
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/bed-assignments", produces = [MediaType.APPLICATION_JSON_VALUE])
class BedAssignmentMappingResource(private val mappingService: BedAssignmentMappingService) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new Bed assignment mapping",
    description = "Creates a mapping between Nomis booking & sequence, and DPS bed assignment id. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = BedAssignmentMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping created"),
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
  suspend fun createBedAssignmentMapping(
    @RequestBody @Valid
    mapping: BedAssignmentMappingDto,
  ) = try {
    mappingService.createMapping(mapping)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "Bed assignment mapping already exists",
      duplicate = mapping,
      existing = getExistingMappingSimilarTo(mapping),
      cause = e,
    )
  }

  @PostMapping("/batch")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a batch of new Bed assignment mappings",
    description = "Creates a mapping between a batch of nomis bed assignment ids and dps bed assignment ids. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          array = ArraySchema(schema = Schema(implementation = BedAssignmentMappingDto::class)),
        ),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mappings created"),
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
  suspend fun createBedAssignmentMappings(
    @RequestBody @Valid
    mappings: List<BedAssignmentMappingDto>,
  ) = try {
    mappingService.createMappings(mappings)
  } catch (e: DuplicateKeyException) {
    val duplicateMapping = getMappingThatIsDuplicate(mappings)
    if (duplicateMapping != null) {
      throw DuplicateMappingException(
        messageIn = "Bed assignment mapping already exists",
        duplicate = duplicateMapping,
        existing = getExistingMappingSimilarTo(duplicateMapping),
        cause = e,
      )
    }
    throw e
  }

  @PostMapping("{offenderNo}/all")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a set of new Bed assignment mappings for a prisoner",
    description = "Creates a mapping between all the nomis bed assignment ids and dps bed assignment ids. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(mediaType = "application/json", schema = Schema(implementation = PrisonerBedAssignmentMappingsDto::class)),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping created"),
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
  suspend fun createBedAssignmentMappingsForPrisoner(
    @Schema(description = "NOMIS offender no", example = "A1234KT", required = true)
    @PathVariable
    offenderNo: String,
    @RequestBody @Valid
    prisonerMapping: PrisonerBedAssignmentMappingsDto,
  ) = try {
    mappingService.createMappings(offenderNo, prisonerMapping)
  } catch (e: DuplicateKeyException) {
    val duplicateMapping = getMappingIdThatIsDuplicate(prisonerMapping.mappings)
    if (duplicateMapping != null) {
      throw DuplicateMappingException(
        messageIn = "Bed assignment mapping already exists",
        duplicate = duplicateMapping.let {
          BedAssignmentMappingDto(
            dpsBedAssignmentId = it.dpsBedAssignmentId,
            nomisBookingId = it.nomisBookingId,
            nomisSequence = it.nomisSequence,
            offenderNo = offenderNo,
            label = prisonerMapping.label,
            mappingType = prisonerMapping.mappingType,
          )
        },
        existing = getExistingMappingSimilarTo(duplicateMapping),
        cause = e,
      )
    }
    throw e
  }

  @GetMapping("{offenderNo}/all")
  @Operation(
    summary = "Gets all Bed assignment mappings for a prisoner",
    description = "Gets all the mappings between Nomis bed assignments and DPS bed assignments related to specific prisoner created either via migration or synchronisation. Requires NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mappings for prisoner",
      ),
    ],
  )
  suspend fun getBedAssignmentMappingsForPrisoner(
    @Schema(description = "NOMIS offender no", example = "A1234KT", required = true)
    @PathVariable
    offenderNo: String,
  ): AllPrisonerBedAssignmentMappingsDto = mappingService.getMappings(offenderNo)

  @GetMapping("/booking-id/{bookingId}/sequence/{sequence}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by NOMIS id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = BedAssignmentMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getBedAssignmentMappingByNomisId(
    @Schema(description = "NOMIS booking id", example = "23456789", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "Sequence", example = "3", required = true)
    @PathVariable
    sequence: Int,
  ): BedAssignmentMappingDto = mappingService.getMappingByNomisId(bookingId, sequence)

  @GetMapping("/dps-bed-assignment-id/{dpsBedAssignmentId}")
  @Operation(
    summary = "get mapping given DPS id",
    description = "Retrieves mapping by DPS id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getBedAssignmentMappingByDpsId(
    @Schema(description = "DPS bed assignment id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsBedAssignmentId: String,
  ) = mappingService.getMappingByDpsId(dpsBedAssignmentId)

  @PreAuthorize("hasRole('ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
  @GetMapping("/migrated/latest")
  @Operation(
    summary = "get the latest mapping for a migration",
    description = "Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = BedAssignmentMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No mappings found at all for any migration",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getLatestMigratedBedAssignmentMapping(): BedAssignmentMappingDto = mappingService.getMappingForLatestMigrated()

  @GetMapping("/migration-id/{migrationId}/count-by-prisoner")
  @Operation(
    summary = "Get count of mappings by migration id grouped by prisoner",
    // description = "Retrieve all mappings of type 'MIGRATED' for the given migration id (identifies a single migration run) grouped by prisoner. Results are paged.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping page returned",
      ),
    ],
  )
  suspend fun getBedAssignmentMappingsByMigrationIdGroupByPrisoner(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Long = mappingService.getCountByMigrationIdGroupedByPrisoner(pageRequest = pageRequest, migrationId = migrationId)

  @DeleteMapping("/booking-id/{bookingId}/sequence/{sequence}")
  @Operation(
    summary = "Deletes mapping",
    description = "Deletes a mapping by Nomis id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [ApiResponse(responseCode = "204", description = "Mapping Deleted")],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend fun deleteBedAssignmentMappingByNomisId(
    @Schema(description = "NOMIS booking id", example = "23456789", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "Sequence", example = "3", required = true)
    @PathVariable
    sequence: Int,
  ) = mappingService.deleteMapping(bookingId, sequence)

  @DeleteMapping("/dps-bed-assignment-id/{dpsBedAssignmentId}")
  @Operation(
    summary = "Deletes a mapping",
    description = "Deletes mapping by DPS id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [ApiResponse(responseCode = "204", description = "Mapping Deleted")],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend fun deleteBedAssignmentMappingByDpsId(
    @Schema(description = "DPS bed assignment id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsBedAssignmentId: String,
  ) = mappingService.deleteMapping(dpsBedAssignmentId)

  @PutMapping("/merge/from/{oldOffenderNo}/to/{newOffenderNo}")
  @Operation(
    summary = "Replaces all occurrences of the 'from' id with the 'to' id in the mapping table",
    description = "Used for update after a prisoner number merge. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Replacement made, or not present in table"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun updateBedAssignmentMappingsByNomisId(
    @Schema(description = "Old prisoner number to replace", example = "A3456KM", required = true)
    @PathVariable
    oldOffenderNo: String,
    @Schema(description = "New prisoner number to use", example = "A3457LZ", required = true)
    @PathVariable
    newOffenderNo: String,
  ) {
    mappingService.updateMappingsByNomisId(oldOffenderNo, newOffenderNo)
  }

  @PutMapping("/move/booking-id/{bookingId}/from/{oldOffenderNo}/to/{newOffenderNo}")
  @Operation(
    summary = "For all bed assignments with the given booking id and old offender no in the mapping table, sets the offender no to the new id",
    description = "Used for update after a booking has been moved from one offender to another. Returns the affected bed assignments. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Replacement made, or not present in table"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun updateBedAssignmentMappingsByBookingId(
    @Schema(description = "The booking id", example = "1234567", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "Previous prisoner number", example = "A3422FE", required = true)
    @PathVariable
    oldOffenderNo: String,
    @Schema(description = "New prisoner number to use", example = "A3457LZ", required = true)
    @PathVariable
    newOffenderNo: String,
  ): List<BedAssignmentMappingDto> = mappingService.updateMappingsByBookingId(bookingId, oldOffenderNo, newOffenderNo)

  private suspend fun getExistingMappingSimilarTo(mapping: BedAssignmentMappingIdDto) = runCatching {
    mappingService.getMappingByNomisId(mapping.nomisBookingId, mapping.nomisSequence)
  }.getOrElse {
    mappingService.getMappingByDpsId(mapping.dpsBedAssignmentId)
  }

  private suspend fun getExistingMappingSimilarTo(mapping: BedAssignmentMappingDto) = runCatching {
    mappingService.getMappingByNomisId(mapping.nomisBookingId, mapping.nomisSequence)
  }.getOrElse {
    mappingService.getMappingByDpsId(mapping.dpsBedAssignmentId)
  }

  private suspend fun getMappingIdThatIsDuplicate(mappings: List<BedAssignmentMappingIdDto>): BedAssignmentMappingIdDto? = mappings.find {
    // look for each mapping until I find one (i.e. that is there is no exception thrown)
    kotlin.runCatching { getExistingMappingSimilarTo(it) }.map { true }.getOrElse { false }
  }

  private suspend fun getMappingThatIsDuplicate(mappings: List<BedAssignmentMappingDto>): BedAssignmentMappingDto? = mappings.find {
    // look for each mapping until I find one (i.e. that is there is no exception thrown)
    kotlin.runCatching { getExistingMappingSimilarTo(it) }.map { true }.getOrElse { false }
  }
}
