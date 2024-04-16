package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_ALERTS')")
@RequestMapping("/mapping/alerts", produces = [MediaType.APPLICATION_JSON_VALUE])
class AlertsMappingResource(private val mappingService: AlertMappingService) {
  @GetMapping("/nomis-booking-id/{bookingId}/nomis-alert-sequence/{alertSequence}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by NOMIS id. Requires role NOMIS_ALERTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = AlertMappingDto::class)),
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
    @Schema(description = "NOMIS booking id", example = "12345", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "NOMIS booking id", example = "2", required = true)
    @PathVariable
    alertSequence: Long,
  ): AlertMappingDto = mappingService.getMappingByNomisId(bookingId = bookingId, alertSequence = alertSequence)

  @GetMapping("/dps-alert-id/{dpsAlertId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by DPS id. Requires role NOMIS_ALERTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = AlertMappingDto::class)),
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
  suspend fun getMappingByDpsId(
    @Schema(description = "DPS alert id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsAlertId: String,
  ): AlertMappingDto = mappingService.getMappingByDpsId(alertId = dpsAlertId)

  @DeleteMapping("/dps-alert-id/{dpsAlertId}")
  @Operation(
    summary = "Deletes mapping",
    description = "Deletes a mapping by DPS id. Requires role NOMIS_ALERTS",
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
    @Schema(description = "DPS alert id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsAlertId: String,
  ) = mappingService.deleteMappingByDpsId(alertId = dpsAlertId)

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new alert mapping",
    description = "Creates a mapping between nomis alert ids and dps alert id. Requires ROLE_NOMIS_ALERTS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = AlertMappingDto::class))],
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
    mapping: AlertMappingDto,
  ) =
    try {
      mappingService.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Alert mapping already exists",
        duplicate = mapping,
        existing = getExistingMappingSimilarTo(mapping),
        cause = e,
      )
    }

  @PostMapping("/all")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a set of new alert mapping",
    description = "Creates a mapping between all the nomis alert ids and dps alert id. Requires ROLE_NOMIS_ALERTS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          array = ArraySchema(schema = Schema(implementation = AlertMappingDto::class)),
        ),
      ],
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
  suspend fun createMappings(
    @RequestBody @Valid
    mappings: List<AlertMappingDto>,
  ) =
    try {
      mappingService.createMappings(mappings)
    } catch (e: DuplicateKeyException) {
      val duplicateMapping = getMappingThatIsDuplicate(mappings)
      if (duplicateMapping != null) {
        throw DuplicateMappingException(
          messageIn = "Alert mapping already exists",
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
    summary = "Creates a set of new alert mapping for a prisoner",
    description = "Creates a mapping between all the nomis alert ids and dps alert id. Requires ROLE_NOMIS_ALERTS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = PrisonerAlertMappingsDto::class),
        ),
      ],
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
  suspend fun createMappingsForPrisoner(
    @Schema(description = "NOMIS offender no", example = "A1234KT", required = true)
    @PathVariable
    offenderNo: String,
    @RequestBody @Valid
    prisonerMapping: PrisonerAlertMappingsDto,
  ) =
    try {
      mappingService.createMappings(offenderNo, prisonerMapping)
    } catch (e: DuplicateKeyException) {
      val duplicateMapping = getMappingIdThatIsDuplicate(prisonerMapping.mappings)
      if (duplicateMapping != null) {
        throw DuplicateMappingException(
          messageIn = "Alert mapping already exists",
          duplicate = duplicateMapping.let {
            AlertMappingDto(
              dpsAlertId = it.dpsAlertId,
              nomisBookingId = it.nomisBookingId,
              nomisAlertSequence = it.nomisAlertSequence,
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

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes all alert mappings",
    description = "Deletes all alert mappings regardless of source. This is expected to only ever been used in a non-production environment. Requires ROLE_NOMIS_ALERTS",
    responses = [
      ApiResponse(responseCode = "204", description = "Mappings deleted"),
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
  suspend fun deleteAllMappings() = mappingService.deleteAllMappings()

  @GetMapping("/migration-id/{migrationId}")
  @Operation(
    summary = "Get paged mappings by migration id",
    description = "Retrieve all mappings of type 'MIGRATED' for the given migration id (identifies a single migration run). Results are paged.",
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
  suspend fun getMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<AlertMappingDto> =
    mappingService.getByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @GetMapping("/migration-id/{migrationId}/grouped-by-prisoner")
  @Operation(
    summary = "Get paged mappings by migration id grouped by prisoner",
    description = "Retrieve all mappings of type 'MIGRATED' for the given migration id (identifies a single migration run) grouped by prisoner. Results are paged.",
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
  ): Page<PrisonerAlertMappingsSummaryDto> =
    mappingService.getByMigrationIdGroupedByPrisoner(pageRequest = pageRequest, migrationId = migrationId)

  private suspend fun getExistingMappingSimilarTo(mapping: AlertMappingDto) = runCatching {
    mappingService.getMappingByNomisId(
      bookingId = mapping.nomisBookingId,
      alertSequence = mapping.nomisAlertSequence,
    )
  }.getOrElse {
    mappingService.getMappingByDpsId(
      alertId = mapping.dpsAlertId,
    )
  }

  private suspend fun getExistingMappingSimilarTo(mapping: AlertMappingIdDto) = runCatching {
    mappingService.getMappingByNomisId(
      bookingId = mapping.nomisBookingId,
      alertSequence = mapping.nomisAlertSequence,
    )
  }.getOrElse {
    mappingService.getMappingByDpsId(
      alertId = mapping.dpsAlertId,
    )
  }

  private suspend fun getMappingThatIsDuplicate(mappings: List<AlertMappingDto>): AlertMappingDto? =
    mappings.find {
      // look for each mapping until I find one (i.e. that is there is no exception thrown)
      kotlin.runCatching { getExistingMappingSimilarTo(it) }.map { true }.getOrElse { false }
    }

  private suspend fun getMappingIdThatIsDuplicate(mappings: List<AlertMappingIdDto>): AlertMappingIdDto? =
    mappings.find {
      // look for each mapping until I find one (i.e. that is there is no exception thrown)
      kotlin.runCatching { getExistingMappingSimilarTo(it) }.map { true }.getOrElse { false }
    }
}
