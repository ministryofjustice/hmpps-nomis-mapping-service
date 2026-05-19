package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.offender

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/court-scheduler", produces = [MediaType.APPLICATION_JSON_VALUE])
class CourtSchedulerPrisonerResource(
  private val service: CourtSchedulerPrisonerService,
) {

  @GetMapping("/{prisonerNumber}/ids")
  @Operation(
    summary = "Gets all court scheduler mappings for a prisoner",
    description = "Gets all court scheduler mappings for a prisoner including schedules and movements. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Prisoner mapping IDs returned"),
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
  suspend fun getAllCourtSchedulerPrisonerMappingIds(
    @PathVariable prisonerNumber: String,
  ): CourtSchedulerPrisonerMappingIdsDto = service.getAllMappingIds(prisonerNumber)
}
