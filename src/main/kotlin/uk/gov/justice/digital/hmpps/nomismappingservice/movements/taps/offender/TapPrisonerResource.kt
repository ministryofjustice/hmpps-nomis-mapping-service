package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.offender

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/taps", produces = [MediaType.APPLICATION_JSON_VALUE])
class TapPrisonerResource(
  private val service: TapPrisonerService,
) {

  @GetMapping("/{prisonerNumber}/ids")
  @Operation(
    summary = "Gets all mappings for prisoner temporary absences",
    description = "Gets mappings for prisoner temporary absences including movement applications, scheduled movements and movements. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
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
  suspend fun getAllPrisonerMappingIds(
    @PathVariable prisonerNumber: String,
  ): TapPrisonerMappingIdsDto = service.getAllMappingIds(prisonerNumber)

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
  suspend fun getPrisonerBookingMappings(
    @PathVariable
    bookingId: Long,
  ): TapMoveBookingMappingDto = service.getMappingsForMoveBooking(bookingId)

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
  suspend fun movePrisonerBookingMappings(
    @PathVariable bookingId: Long,
    @PathVariable fromOffenderNo: String,
    @PathVariable toOffenderNo: String,
  ) = service.moveMappingsForBooking(bookingId, fromOffenderNo, toOffenderNo)
}
