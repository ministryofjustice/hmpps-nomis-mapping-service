package uk.gov.justice.digital.hmpps.nomismappingservice.property

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.dao.DuplicateKeyException
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
@RequestMapping("/mapping/property", produces = [MediaType.APPLICATION_JSON_VALUE])
class PropertyContainerMappingResource(private val mappingService: PropertyContainerMappingService) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new property container mapping",
    description = "Creates a mapping between Nomis property container id, and DPS property container id. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = PropertyContainerMappingDto::class))],
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
  suspend fun createPropertyContainerMapping(
    @RequestBody @Valid
    mapping: PropertyContainerMappingDto,
  ) = try {
    mappingService.createMapping(mapping)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "property container mapping already exists",
      duplicate = mapping,
      existing = getExistingMappingSimilarTo(mapping),
      cause = e,
    )
  }

  @GetMapping("/nomis-id/{nomisId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by NOMIS id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PropertyContainerMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getPropertyContainerMappingByNomisId(
    @Schema(description = "NOMIS id", example = "23456789", required = true)
    @PathVariable
    nomisId: Long,
  ): PropertyContainerMappingDto = mappingService.getMappingByNomisId(nomisId)

  @GetMapping("/dps-id/{dpsPropertyContainerId}")
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
  suspend fun getPropertyContainerMappingByDpsId(
    @Schema(description = "DPS property container id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsPropertyContainerId: String,
  ) = mappingService.getMappingByDpsId(dpsPropertyContainerId)

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
          Content(mediaType = "application/json", schema = Schema(implementation = PropertyContainerMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No mappings found at all for any migration",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getLatestMigratedPropertyContainerMapping(): PropertyContainerMappingDto = mappingService.getMappingForLatestMigrated()

  @GetMapping("/migration-id/{migrationId}/count")
  @Operation(
    summary = "Get count of mappings by migration id",
    description = "Counts all mappings of type 'MIGRATED' for the given migration id (identifies a single migration run).",
    responses = [ApiResponse(responseCode = "200", description = "Mapping page returned")],
  )
  suspend fun getPropertyContainerMappingsByMigrationIdCount(
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Long = mappingService.getCountByMigrationId(migrationId = migrationId)

  @DeleteMapping("/nomis-id/{nomisId}")
  @Operation(
    summary = "Deletes mapping",
    description = "Deletes a mapping by Nomis id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [ApiResponse(responseCode = "204", description = "Mapping Deleted")],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend fun deletePropertyContainerMappingByNomisId(
    @Schema(description = "NOMIS id", example = "23456789", required = true)
    @PathVariable
    nomisId: Long,
  ) = mappingService.deleteMapping(nomisId)

  @DeleteMapping("/dps-id/{dpsPropertyContainerId}")
  @Operation(
    summary = "Deletes a mapping",
    description = "Deletes mapping by DPS id. Requires role NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [ApiResponse(responseCode = "204", description = "Mapping Deleted")],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend fun deletePropertyContainerMappingByDpsId(
    @Schema(description = "DPS property container id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsPropertyContainerId: String,
  ) = mappingService.deleteMapping(dpsPropertyContainerId)

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
  suspend fun updatePropertyContainerMappingsByNomisId(
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
    summary = "For all property containers with the given booking id in the mapping table, sets the offender no to the given 'to' id",
    description = "Used for update after a booking has been moved from one offender to another. Returns the affected property containers. Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Replacement made, or not present in table"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun updatePropertyContainerMappingsByBookingId(
    @Schema(description = "The booking id", example = "1234567", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "New prisoner number to use", example = "A3457LZ", required = true)
    @PathVariable
    newOffenderNo: String,
  ): List<PropertyContainerMappingDto> = mappingService.updateMappingsByBookingId(bookingId, newOffenderNo)

  private suspend fun getExistingMappingSimilarTo(mapping: PropertyContainerMappingIdDto) = runCatching {
    mappingService.getMappingByNomisId(mapping.nomisPropertyContainerId)
  }.getOrElse {
    mappingService.getMappingByDpsId(mapping.dpsPropertyContainerId)
  }

  private suspend fun getExistingMappingSimilarTo(mapping: PropertyContainerMappingDto) = runCatching {
    mappingService.getMappingByNomisId(mapping.nomisPropertyContainerId)
  }.getOrElse {
    mappingService.getMappingByDpsId(mapping.dpsPropertyContainerId)
  }
}
