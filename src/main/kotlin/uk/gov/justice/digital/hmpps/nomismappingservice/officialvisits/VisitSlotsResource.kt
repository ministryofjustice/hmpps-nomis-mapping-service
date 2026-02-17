package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

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
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/visit-slots", produces = [MediaType.APPLICATION_JSON_VALUE])
class VisitSlotsResource(private val visitSlotsService: VisitSlotsService) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping("/time-slots")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a visit time slots mapping",
    description = "Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitTimeSlotMappingDto::class))],
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
  suspend fun createVisitTimeSlotMapping(
    @RequestBody @Valid
    mapping: VisitTimeSlotMappingDto,
  ) = try {
    visitSlotsService.createTimeSlot(mapping)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingVisitTimeSlotMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Visit time slot mapping already exists",
      duplicate = mapping,
      existing = existingMapping ?: mapping,
      cause = e,
    )
  }

  @GetMapping("/time-slots/nomis-prison-id/{nomisPrisonId}/nomis-day-of-week/{nomisDayOfWeek}/nomis-slot-sequence/{nomisSlotSequence}")
  @Operation(
    summary = "Get visit time slot mapping by nomis prison id, day of week and sequence",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping data",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Day of week not valid",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
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
  suspend fun getVisitTimeSlotMappingByNomisIds(
    @Schema(description = "NOMIS prison id", example = "WWI", required = true)
    @PathVariable
    nomisPrisonId: String,
    @Schema(description = "NOMIS day of the week", example = "MONDAY", required = true)
    @PathVariable
    nomisDayOfWeek: String,
    @Schema(description = "NOMIS slot sequence", example = "4", required = true)
    @PathVariable
    nomisSlotSequence: Int,
  ): VisitTimeSlotMappingDto = visitSlotsService.getVisitTimeSlotMappingByNomisId(
    nomisPrisonId = nomisPrisonId,
    nomisDayOfWeek = nomisDayOfWeek,
    nomisSlotSequence = nomisSlotSequence,
  )

  @GetMapping("/time-slots/dps-id/{dpsId}")
  @Operation(
    summary = "Get visit time slot mapping by DPS id",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
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
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getVisitTimeSlotMappingByDpsId(
    @Schema(description = "DPS id", example = "1234", required = true)
    @PathVariable
    dpsId: String,
  ): VisitTimeSlotMappingDto = visitSlotsService.getVisitTimeSlotMappingByDpsId(dpsId)

  @DeleteMapping("/time-slots/nomis-prison-id/{nomisPrisonId}/nomis-day-of-week/{nomisDayOfWeek}/nomis-slot-sequence/{nomisSlotSequence}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes visit time slot mapping by nomis prison id, day of week and sequence",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Mapping deleted or no longer exists",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Day of week not valid",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
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
  suspend fun deleteVisitTimeSlotMappingByNomisIds(
    @Schema(description = "NOMIS prison id", example = "WWI", required = true)
    @PathVariable
    nomisPrisonId: String,
    @Schema(description = "NOMIS day of the week", example = "MONDAY", required = true)
    @PathVariable
    nomisDayOfWeek: String,
    @Schema(description = "NOMIS slot sequence", example = "4", required = true)
    @PathVariable
    nomisSlotSequence: Int,
  ) {
    visitSlotsService.deleteVisitTimeSlotMappingByNomisId(
      nomisPrisonId = nomisPrisonId,
      nomisDayOfWeek = nomisDayOfWeek,
      nomisSlotSequence = nomisSlotSequence,
    )
  }

  @PostMapping("/visit-slot")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a visit slots mapping",
    description = "Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitSlotMappingDto::class))],
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
  suspend fun createVisitSlotMapping(
    @RequestBody @Valid
    mapping: VisitSlotMappingDto,
  ) = try {
    visitSlotsService.createVisitSlot(mapping)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingVisitSlotMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Visit slot mapping already exists",
      duplicate = mapping,
      existing = existingMapping ?: mapping,
      cause = e,
    )
  }

  @GetMapping("/visit-slot/nomis-id/{nomisId}")
  @Operation(
    summary = "Get visit slot mapping by nomis visit slot id",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
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
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getVisitSlotMappingByNomisId(
    @Schema(description = "NOMIS visit slot id", example = "1234", required = true)
    @PathVariable
    nomisId: Long,
  ): VisitSlotMappingDto = visitSlotsService.getVisitSlotMappingByNomisId(
    nomisId = nomisId,
  )

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/visit-slot/nomis-id/{nomisId}")
  @Operation(
    summary = "Deletes visit slot mapping by nomis visit slot id",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Mapping deleted or does not exist",
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
  suspend fun deleteVisitSlotMappingByNomisId(
    @Schema(description = "NOMIS visit slot id", example = "1234", required = true)
    @PathVariable
    nomisId: Long,
  ) {
    visitSlotsService.deleteVisitSlotMappingByNomisId(
      nomisId = nomisId,
    )
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a mini tree of visit time slots mappings typically for a migration",
    description = "Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitTimeSlotMigrationMappingDto::class))],
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
  suspend fun createMigrationMappings(
    @RequestBody @Valid
    mappings: VisitTimeSlotMigrationMappingDto,
  ) = try {
    visitSlotsService.createMappings(mappings)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingVisitTimeSlotMappingSimilarTo(mappings)
    if (existingMapping == null) {
      log.error("Child duplicate key found for time slot even though the time slot has never been migrated", e)
    }
    throw DuplicateMappingException(
      messageIn = "Visit time slot mapping already exists",
      duplicate = mappings,
      existing = existingMapping?.asVisitTimeSlotMigrationMappingDto() ?: mappings,
      cause = e,
    )
  }

  @GetMapping("/migration-id/{migrationId}")
  @Operation(
    summary = "Get paged visit time slot mappings by migration id",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit time slots mapping page returned",
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
  suspend fun getVisitTimeSlotMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<VisitTimeSlotMappingDto> = visitSlotsService.getVisitTimeSlotMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @DeleteMapping("/all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete all visit slot and time slot mappings",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "All mappings deleted",
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
  suspend fun deleteAllMappings() = visitSlotsService.deleteAllMappings()

  private suspend fun getExistingVisitTimeSlotMappingSimilarTo(mapping: VisitTimeSlotMigrationMappingDto) = runCatching {
    visitSlotsService.getVisitTimeSlotMappingByNomisId(
      nomisPrisonId = mapping.nomisPrisonId,
      nomisDayOfWeek = mapping.nomisDayOfWeek,
      nomisSlotSequence = mapping.nomisSlotSequence,
    )
  }.getOrElse {
    visitSlotsService.getVisitTimeSlotMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }

  private suspend fun getExistingVisitTimeSlotMappingSimilarTo(mapping: VisitTimeSlotMappingDto) = runCatching {
    visitSlotsService.getVisitTimeSlotMappingByNomisId(
      nomisPrisonId = mapping.nomisPrisonId,
      nomisDayOfWeek = mapping.nomisDayOfWeek,
      nomisSlotSequence = mapping.nomisSlotSequence,
    )
  }.getOrElse {
    visitSlotsService.getVisitTimeSlotMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }
  private suspend fun getExistingVisitSlotMappingSimilarTo(mapping: VisitSlotMappingDto) = runCatching {
    visitSlotsService.getVisitSlotMappingByNomisId(mapping.nomisId)
  }.getOrElse {
    visitSlotsService.getVisitSlotMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }
}

data class VisitTimeSlotMappingDto(
  val dpsId: String,
  val nomisPrisonId: String,
  val nomisDayOfWeek: String,
  val nomisSlotSequence: Int,
  val label: String?,
  val mappingType: StandardMappingType,
  val whenCreated: LocalDateTime? = null,
)

data class VisitSlotMappingDto(
  val dpsId: String,
  val nomisId: Long,
  val label: String?,
  val mappingType: StandardMappingType,
  val whenCreated: LocalDateTime? = null,
)

data class VisitTimeSlotMigrationMappingDto(
  val dpsId: String,
  val nomisPrisonId: String,
  val nomisDayOfWeek: String,
  val nomisSlotSequence: Int,
  val visitSlots: List<VisitSlotMigrationMappingDto>,
  val label: String?,
  val mappingType: StandardMappingType = StandardMappingType.MIGRATED,
  val whenCreated: LocalDateTime? = null,
)

data class VisitSlotMigrationMappingDto(
  val dpsId: String,
  val nomisId: Long,
)

private fun VisitTimeSlotMappingDto.asVisitTimeSlotMigrationMappingDto() = VisitTimeSlotMigrationMappingDto(
  dpsId = this.dpsId,
  nomisPrisonId = this.nomisPrisonId,
  nomisDayOfWeek = this.nomisDayOfWeek,
  nomisSlotSequence = this.nomisSlotSequence,
  label = this.label,
  mappingType = this.mappingType,
  whenCreated = this.whenCreated,
  visitSlots = emptyList(),
)
