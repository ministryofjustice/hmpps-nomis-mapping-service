package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.schedule

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
@RequestMapping("/mapping/taps/schedule", produces = [MediaType.APPLICATION_JSON_VALUE])
class TapScheduleResource(
  private val service: TapScheduleService,
) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a mapping for a single scheduled tap",
    description = "Creates a mapping for a single scheduled tap. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = TapScheduleMappingDto::class))],
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
  suspend fun createTapScheduleMapping(
    @RequestBody mapping: TapScheduleMappingDto,
  ) = try {
    service.createScheduleMapping(mapping)
  } catch (dke: DuplicateKeyException) {
    val existing = getExistingScheduledMovementMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Scheduled movement mapping already exists",
      duplicate = mapping,
      existing = existing,
      cause = dke,
    )
  }

  @PutMapping
  @Operation(
    summary = "Updates a mapping for a single scheduled tap",
    description = "Updates a mapping for a single scheduled tap. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = TapScheduleMappingDto::class))],
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
  suspend fun updateTapScheduleMapping(
    @RequestBody mapping: TapScheduleMappingDto,
  ) = service.updateScheduleMapping(mapping)

  private suspend fun getExistingScheduledMovementMappingSimilarTo(mapping: TapScheduleMappingDto) = runCatching {
    service.getScheduleMappingByNomisId(mapping.nomisEventId)
  }
    .getOrElse {
      service.getScheduleMappingByDpsId(mapping.dpsOccurrenceId)
    }

  @GetMapping("/nomis-id/{nomisEventId}")
  @Operation(
    summary = "Gets a mapping for a single scheduled tap by NOMIS event ID",
    description = "Gets a mapping for a single scheduled tap by NOMIS event ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Scheduled tap mapping returned"),
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
  suspend fun getTapScheduleMappingByNomisId(
    @PathVariable nomisEventId: Long,
  ) = service.getScheduleMappingByNomisId(nomisEventId)

  @GetMapping("/dps-id/{dpsId}")
  @Operation(
    summary = "Gets a mapping for a single scheduled tap by DPS event ID",
    description = "Gets a mapping for a single scheduled tap by DPS event ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Scheduled tap mapping returned"),
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
  suspend fun getTapScheduleMappingByDpsId(
    @PathVariable dpsId: UUID,
  ) = service.getScheduleMappingByDpsId(dpsId)

  @DeleteMapping("/nomis-id/{nomisEventId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a mapping for a single scheduled tap by NOMIS event ID",
    description = "Deletes a mapping for a single scheduled tap by NOMIS event ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "204", description = "Scheduled tap mapping deleted"),
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
  suspend fun deleteTapScheduleMappingByNomisId(
    @PathVariable nomisEventId: Long,
  ) = service.deleteScheduleMappingByNomisId(nomisEventId)

  @GetMapping("/nomis-address-id/{nomisAddressId}")
  @Operation(
    summary = "Finds tap schedules by NOMIS address ID",
    description = "Finds tap schedules by NOMIS address ID after the passed date. If no date is passed the default value is today. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "List of tap schedules returned"),
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
  suspend fun findTapScheduleMappingsByNomisAddressId(
    @PathVariable nomisAddressId: Long,
    @RequestParam(value = "fromDate", required = false) fromDate: LocalDate? = LocalDate.now(),
  ) = service.findTapScheduleMappingsByNomisAddressId(nomisAddressId, fromDate!!)
}
