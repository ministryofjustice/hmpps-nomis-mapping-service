package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.movement

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
@RequestMapping("/mapping/person-location/movement", produces = [MediaType.APPLICATION_JSON_VALUE])
class PersonLocationMovementResource(
  private val service: PersonLocationMovementService,
) {

  @GetMapping("/nomis-booking-id/{nomisBookingId}/nomis-movement-seq/{nomisMovementSeq}")
  @Operation(
    summary = "Gets a mapping for a single person location movement by NOMIS booking ID and movement sequence",
    description = "Gets a mapping for a single person location movement by NOMIS booking ID and movement sequence. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Person location movement mapping returned"),
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
  suspend fun getMovementMappingByNomisId(
    @PathVariable nomisBookingId: Long,
    @PathVariable nomisMovementSeq: Int,
  ) = service.getMovementMappingByNomisId(nomisBookingId, nomisMovementSeq)
}
