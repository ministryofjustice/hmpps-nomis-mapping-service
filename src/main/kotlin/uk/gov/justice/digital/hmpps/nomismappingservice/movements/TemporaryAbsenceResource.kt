package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.util.UUID

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/temporary-absence", produces = [MediaType.APPLICATION_JSON_VALUE])
class TemporaryAbsenceResource(
  private val service: TemporaryAbsenceService,
) {

  @PutMapping("/migrate")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates all mappings for prisoner temporary absences which are all migrated at the same time",
    description = "Creates mappings for prisoner temporary absences including movement applications, scheduled movements and movements. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = TemporaryAbsencesPrisonerMappingDto::class))],
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
    ],
  )
  suspend fun createMappings(
    @RequestBody mappings: TemporaryAbsencesPrisonerMappingDto,
  ) = service.createMigrationMappings(mappings)

  @GetMapping("/{prisonerNumber}/ids")
  @Operation(
    summary = "Gets all mappings for prisoner temporary absences",
    description = "Gets mappings for prisoner temporary absences including movement applications, scheduled movements and movements. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = TemporaryAbsencesPrisonerMappingDto::class))],
    ),
    responses = [
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
  suspend fun getMappings(
    @PathVariable prisonerNumber: String,
  ): TemporaryAbsencesPrisonerMappingIdsDto = service.getAllMappings(prisonerNumber)

  @PostMapping("/application")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a mapping for a single temporary absence application",
    description = "Creates a mapping for a single temporary absence application. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = TemporaryAbsenceApplicationSyncMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Application mapping created"),
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
        description = "The mapping already exists.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun createApplicationSyncMapping(
    @RequestBody mapping: TemporaryAbsenceApplicationSyncMappingDto,
  ) = try {
    service.createApplicationMapping(mapping)
  } catch (dke: DuplicateKeyException) {
    val existing = getExistingApplicationMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Temporary absence application mapping already exists",
      duplicate = mapping,
      existing = existing,
      cause = dke,
    )
  }

  private suspend fun getExistingApplicationMappingSimilarTo(mapping: TemporaryAbsenceApplicationSyncMappingDto) = runCatching {
    service.getApplicationMappingByNomisId(mapping.nomisMovementApplicationId)
  }
    .getOrElse {
      service.getApplicationMappingByDpsId(mapping.dpsMovementApplicationId)
    }

  @GetMapping("/application/nomis-application-id/{nomisApplicationId}")
  @Operation(
    summary = "Gets a mapping for a single temporary absence application by NOMIS ID",
    description = "Gets a mapping for a single temporary absence application by NOMIS ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Application mapping returned"),
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
        responseCode = "404",
        description = "The mapping does not exist.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getApplicationSyncMappingByNomisId(
    @PathVariable nomisApplicationId: Long,
  ) = service.getApplicationMappingByNomisId(nomisApplicationId)

  @GetMapping("/application/dps-id/{dpsId}")
  @Operation(
    summary = "Gets a mapping for a single temporary absence application by DPS ID",
    description = "Gets a mapping for a single temporary absence application by DPS ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Application mapping returned"),
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
        responseCode = "404",
        description = "The mapping does not exist.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getApplicationSyncMappingByDpsId(
    @PathVariable dpsId: UUID,
  ) = service.getApplicationMappingByDpsId(dpsId)

  @DeleteMapping("/application/nomis-application-id/{nomisApplicationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a mapping for a single temporary absence application by NOMIS ID",
    description = "Deletes a mapping for a single temporary absence application by NOMIS ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = TemporaryAbsenceApplicationSyncMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "204", description = "Application does not exist"),
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
  suspend fun deleteApplicationSyncMappingByNomisId(
    @PathVariable nomisApplicationId: Long,
  ) = service.deleteApplicationMappingByNomisId(nomisApplicationId)

  @PostMapping("/scheduled-movement")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a mapping for a single scheduled movement",
    description = "Creates a mapping for a single scheduled movement. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ScheduledMovementSyncMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Scheduled movement mapping created"),
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
        description = "The mapping already exists.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun createScheduledMovementSyncMapping(
    @RequestBody mapping: ScheduledMovementSyncMappingDto,
  ) = try {
    service.createScheduledMovementMapping(mapping)
  } catch (dke: DuplicateKeyException) {
    val existing = getExistingScheduledMovementMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Scheduled movement mapping already exists",
      duplicate = mapping,
      existing = existing,
      cause = dke,
    )
  }

  @PutMapping("/scheduled-movement")
  @Operation(
    summary = "Updates a mapping for a single scheduled movement",
    description = "Updates a mapping for a single scheduled movement. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ScheduledMovementSyncMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "200", description = "Scheduled movement mapping updated"),
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
        description = "The mapping already exists.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun updateScheduledMovementSyncMapping(
    @RequestBody mapping: ScheduledMovementSyncMappingDto,
  ) = service.updateScheduledMovementMapping(mapping)

  private suspend fun getExistingScheduledMovementMappingSimilarTo(mapping: ScheduledMovementSyncMappingDto) = runCatching {
    service.getScheduledMovementMappingByNomisId(mapping.nomisEventId)
  }
    .getOrElse {
      service.getScheduledMovementMappingByDpsId(mapping.dpsOccurrenceId)
    }

  @GetMapping("/scheduled-movement/nomis-event-id/{nomisEventId}")
  @Operation(
    summary = "Gets a mapping for a single scheduled movement by NOMIS event ID",
    description = "Gets a mapping for a single scheduled movement by NOMIS event ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Scheduled movement mapping returned"),
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
        responseCode = "404",
        description = "The mapping does not exist.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getScheduledMovementSyncMappingByNomisId(
    @PathVariable nomisEventId: Long,
  ) = service.getScheduledMovementMappingByNomisId(nomisEventId)

  @GetMapping("/scheduled-movement/dps-id/{dpsId}")
  @Operation(
    summary = "Gets a mapping for a single scheduled movement by DPS event ID",
    description = "Gets a mapping for a single scheduled movement by DPS event ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Scheduled movement mapping returned"),
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
        responseCode = "404",
        description = "The mapping does not exist.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getScheduledMovementSyncMappingByDpsId(
    @PathVariable dpsId: UUID,
  ) = service.getScheduledMovementMappingByDpsId(dpsId)

  @DeleteMapping("/scheduled-movement/nomis-event-id/{nomisEventId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a mapping for a single scheduled movement by NOMIS event ID",
    description = "Deletes a mapping for a single scheduled movement by NOMIS event ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ScheduledMovementSyncMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "204", description = "Scheduled movement mapping deleted"),
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
  suspend fun deleteScheduledMovementSyncMappingByNomisId(
    @PathVariable nomisEventId: Long,
  ) = service.deleteScheduledMovementMappingByNomisId(nomisEventId)

  @PostMapping("/external-movement")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a mapping for a single temporary absence external movement",
    description = "Creates a mapping for a single temporary absence external movement. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ExternalMovementSyncMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "External movement mapping created"),
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
        description = "The mapping already exists.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun createExternalMovementSyncMapping(
    @RequestBody mapping: ExternalMovementSyncMappingDto,
  ) = try {
    service.createExternalMovementMapping(mapping)
  } catch (dke: DuplicateKeyException) {
    val existing = getExistingExternalMovementMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Temporary absence external movement mapping already exists",
      duplicate = mapping,
      existing = existing,
      cause = dke,
    )
  }

  @PutMapping("/external-movement")
  @Operation(
    summary = "Updates a mapping for a single temporary absence external movement",
    description = "Updates a mapping for a single temporary absence external movement. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ExternalMovementSyncMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "200", description = "External movement mapping updated"),
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
        description = "The mapping already exists.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun updateExternalMovementSyncMapping(
    @RequestBody mapping: ExternalMovementSyncMappingDto,
  ) = service.updateExternalMovementMapping(mapping)

  private suspend fun getExistingExternalMovementMappingSimilarTo(mapping: ExternalMovementSyncMappingDto) = runCatching {
    service.getExternalMovementMappingByNomisId(mapping.bookingId, mapping.nomisMovementSeq)
  }
    .getOrElse {
      service.getExternalMovementMappingByDpsId(mapping.dpsMovementId)
    }

  @GetMapping("/external-movement/nomis-movement-id/{bookingId}/{movementSeq}")
  @Operation(
    summary = "Gets a mapping for a single temporary absence external movement by NOMIS booking ID and movement sequence",
    description = "Gets a mapping for a single temporary absence external movement by NOMIS booking ID and movement sequence. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "External movement mapping returned"),
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
        responseCode = "404",
        description = "The mapping does not exist.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getExternalMovementSyncMappingByNomisId(
    @PathVariable bookingId: Long,
    @PathVariable movementSeq: Int,
  ) = service.getExternalMovementMappingByNomisId(bookingId, movementSeq)

  @GetMapping("/external-movement/dps-id/{dpsId}")
  @Operation(
    summary = "Gets a mapping for a single temporary absence external movement by DPS ID",
    description = "Gets a mapping for a single temporary absence external movement by DPS ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "External movement mapping returned"),
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
        responseCode = "404",
        description = "The mapping does not exist.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getExternalMovementSyncMappingByDpsId(
    @PathVariable dpsId: UUID,
  ) = service.getExternalMovementMappingByDpsId(dpsId)

  @DeleteMapping("/external-movement/nomis-movement-id/{bookingId}/{movementSeq}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a mapping for a single temporary absence external movement by NOMIS booking ID and movement sequence",
    description = "Deletes a mapping for a single temporary absence external movement by NOMIS booking ID and movement sequence. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ExternalMovementSyncMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "204", description = "External movement mapping deleted"),
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
  suspend fun deleteExternalMovementSyncMappingByNomisId(
    @PathVariable bookingId: Long,
    @PathVariable movementSeq: Int,
  ) = service.deleteExternalMovementMappingByNomisId(bookingId, movementSeq)

  @GetMapping("/scheduled-movements/nomis-address-id/{nomisAddressId}")
  @Operation(
    summary = "Finds scheduled movements for a temporary absence by NOMIS address ID",
    description = "Finds scheduled movements for a temporary absence by NOMIS address ID after the passed date. If no date is passed the default value is today. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "List of movements returned"),
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
        responseCode = "404",
        description = "The mapping does not exist.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun findScheduledMovementsByNomisAddressId(
    @PathVariable nomisAddressId: Long,
    @RequestParam(value = "fromDate", required = false) fromDate: LocalDate? = LocalDate.now(),
  ) = service.findScheduledMovementsByNomisAddressId(nomisAddressId, fromDate!!)

  @GetMapping("/migration-id/{migrationId}")
  @Operation(
    summary = "Get a page of temporary absence migration mappings, which is just the prisoner numbers that have been migrated.",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Temporary absence mapping page returned",
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
  suspend fun getMappingsCount(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<TemporaryAbsenceMigrationDto> = service.getCountByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @GetMapping("/move-booking/{bookingId}")
  @Operation(
    summary = "Get all mappings for a booking",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Temporary absence mapping page returned",
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
  suspend fun getMoveBookingMappings(
    @PathVariable
    bookingId: Long,
  ): TemporaryAbsenceMoveBookingMappingDto = service.getMappingsForMoveBooking(bookingId)

  @PutMapping("/move-booking/{bookingId}/from/{fromOffenderNo}/to/{toOffenderNo}")
  @Operation(
    summary = "Move all mappings for a booking from one offender to another",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Bookings moved",
      ),
      ApiResponse(
        responseCode = "400",
        description = "We were unable to move the bookings",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
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
      ApiResponse(
        responseCode = "404",
        description = "There are no mappings for the booking",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun moveBookingMappings(
    @PathVariable bookingId: Long,
    @PathVariable fromOffenderNo: String,
    @PathVariable toOffenderNo: String,
  ) = service.moveMappingsForBooking(bookingId, fromOffenderNo, toOffenderNo)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Synchronisation mappings affected by a change of address")
data class FindScheduledMovementsForAddressResponse(
  @Schema(description = "All sync mappings related to the passed NOMIS address ID. Note historical sync mappings are not included.")
  val scheduleMappings: List<ScheduledMovementSyncMappingDto>,
)
