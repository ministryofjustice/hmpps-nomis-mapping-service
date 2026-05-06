package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule

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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/court/schedule", produces = [MediaType.APPLICATION_JSON_VALUE])
class CourtScheduleResource(
  private val service: CourtScheduleService,
) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a mapping for a single court schedule",
    description = "Creates a mapping for a single court schedule. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CourtScheduleMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Court schedule mapping created"),
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
  suspend fun createCourtScheduleMapping(
    @RequestBody mapping: CourtScheduleMappingDto,
  ) = try {
    service.createScheduleMapping(mapping)
  } catch (dke: DuplicateKeyException) {
    val existing = getExistingCourtScheduleMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Court schedule mapping already exists",
      duplicate = mapping,
      existing = existing,
      cause = dke,
    )
  }

  private suspend fun getExistingCourtScheduleMappingSimilarTo(mapping: CourtScheduleMappingDto) = runCatching {
    service.getScheduleMappingByNomisId(mapping.nomisEventId)
  }
    .getOrElse {
      service.getScheduleMappingByDpsId(mapping.dpsCourtAppearanceId)
    }

  @GetMapping("/nomis-id/{nomisEventId}")
  @Operation(
    summary = "Gets a mapping for a single court schedule by NOMIS event ID",
    description = "Gets a mapping for a single court schedule by NOMIS event ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Court schedule mapping returned"),
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
  suspend fun getCourtScheduleMappingByNomisId(
    @PathVariable nomisEventId: Long,
  ) = service.getScheduleMappingByNomisId(nomisEventId)

  @DeleteMapping("/nomis-id/{nomisEventId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a mapping for a single court schedule by NOMIS event ID",
    description = "Deletes a mapping for a single court schedule by NOMIS event ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "204", description = "Court schedule mapping deleted"),
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
  suspend fun deleteCourtScheduleMappingByNomisId(
    @PathVariable nomisEventId: Long,
  ) = service.deleteScheduleMappingByNomisId(nomisEventId)
}
