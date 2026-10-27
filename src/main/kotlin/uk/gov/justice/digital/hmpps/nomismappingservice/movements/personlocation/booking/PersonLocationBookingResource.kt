package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.booking

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
@RequestMapping("/mapping/person-location/booking", produces = [MediaType.APPLICATION_JSON_VALUE])
class PersonLocationBookingResource(
  private val service: PersonLocationBookingService,
) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a mapping for a single person location booking",
    description = "Creates a mapping for a single person location booking. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = PersonLocationBookingIdMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Person location booking mapping created"),
      ApiResponse(
        responseCode = "400",
        description = "The request body is invalid",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
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
  suspend fun createBookingMapping(
    @RequestBody mapping: PersonLocationBookingIdMappingDto,
  ) = try {
    service.createBookingMapping(mapping)
  } catch (dke: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "Person location booking mapping already exists",
      duplicate = mapping,
      existing = service.getExistingBookingMappingSimilarTo(mapping),
      cause = dke,
    )
  }

  @GetMapping("/nomis-booking-id/{nomisBookingId}")
  @Operation(
    summary = "Gets a mapping for a single person location booking by NOMIS booking ID",
    description = "Gets a mapping for a single person location booking by NOMIS booking ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Person location booking mapping returned"),
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
  suspend fun getBookingMappingByNomisId(
    @PathVariable nomisBookingId: Long,
  ) = service.getBookingMappingByNomisId(nomisBookingId)

  @DeleteMapping("/nomis-booking-id/{nomisBookingId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a mapping for a single person location booking by NOMIS booking ID",
    description = "Deletes a mapping for a single person location booking by NOMIS booking ID. Succeeds if the mapping does not exist. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "204", description = "Person location booking mapping deleted"),
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
  suspend fun deleteBookingMappingByNomisId(
    @PathVariable nomisBookingId: Long,
  ) = service.deleteBookingMappingByNomisId(nomisBookingId)
}
