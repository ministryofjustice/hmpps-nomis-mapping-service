package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.movement

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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/taps/movement", produces = [MediaType.APPLICATION_JSON_VALUE])
class TapMovementResource(
  private val service: TapMovementService,
) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a mapping for a single tap movement",
    description = "Creates a mapping for a single tap movement. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = TapMovementMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Tap movement mapping created"),
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
  suspend fun createTapMovementMapping(
    @RequestBody mapping: TapMovementMappingDto,
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

  @PutMapping
  @Operation(
    summary = "Updates a mapping for a single tap movement",
    description = "Updates a mapping for a single tap movement. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = TapMovementMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "200", description = "Tap movement mapping updated"),
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
  suspend fun updateTapMovementMapping(
    @RequestBody mapping: TapMovementMappingDto,
  ) = service.updateExternalMovementMapping(mapping)

  private suspend fun getExistingExternalMovementMappingSimilarTo(mapping: TapMovementMappingDto) = runCatching {
    service.getExternalMovementMappingByNomisId(mapping.bookingId, mapping.nomisMovementSeq)
  }
    .getOrElse {
      service.getExternalMovementMappingByDpsId(mapping.dpsMovementId)
    }

  @GetMapping("/nomis-id/{bookingId}/{movementSeq}")
  @Operation(
    summary = "Gets a mapping for a single tap movement by NOMIS booking ID and movement sequence",
    description = "Gets a mapping for a single tap movement by NOMIS booking ID and movement sequence. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Tap movement mapping returned"),
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
  suspend fun getTapMovementMappingByNomisId(
    @PathVariable bookingId: Long,
    @PathVariable movementSeq: Int,
  ) = service.getExternalMovementMappingByNomisId(bookingId, movementSeq)

  @GetMapping("/dps-id/{dpsId}")
  @Operation(
    summary = "Gets a mapping for a single tap movement by DPS ID",
    description = "Gets a mapping for a single tap movement by DPS ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Tap movement mapping returned"),
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
  suspend fun getTapMovementMappingByDpsId(
    @PathVariable dpsId: UUID,
  ) = service.getExternalMovementMappingByDpsId(dpsId)

  @DeleteMapping("/nomis-id/{bookingId}/{movementSeq}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a mapping for a single tap movement by NOMIS booking ID and movement sequence",
    description = "Deletes a mapping for a single tap movement by NOMIS booking ID and movement sequence. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = TapMovementMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "204", description = "Tap movement mapping deleted"),
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
  suspend fun deleteTapMovementMappingByNomisId(
    @PathVariable bookingId: Long,
    @PathVariable movementSeq: Int,
  ) = service.deleteExternalMovementMappingByNomisId(bookingId, movementSeq)
}
