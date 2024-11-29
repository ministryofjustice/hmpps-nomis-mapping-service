package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks

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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.IdentifyingMarkImageMappingResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.toResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_PRISONPERSON')")
@RequestMapping("/mapping/prisonperson", produces = [MediaType.APPLICATION_JSON_VALUE])
class IdentifyingMarkImageResource(private val service: IdentifyingMarkImageService) {

  @GetMapping("/nomis-offender-image-id/{nomisImageId}")
  @Operation(
    summary = "Gets a single identifying mark image mapping by NOMIS id",
    description = "Gets a single identifying mark image mapping by NOMIS id. Requires ROLE_NOMIS_PRISONPERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = IdentifyingMarkImageMappingResponse::class))],
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
        responseCode = "404",
        description = "Not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getIdentifyingMarkImageMapping(
    @PathVariable nomisImageId: Long,
  ) = service.getIdentifyingMarkImageMapping(nomisImageId)
    ?.toResponse()
    ?: throw NotFoundException("Identifying mark image mapping not found for NOMIS offender image id $nomisImageId")

  @GetMapping("/dps-image-id/{dpsImageId}")
  @Operation(
    summary = "Gets an identifying mark image mapping by DPS image id",
    description = "Gets an identifying mark image mapping by DPS image id. Requires ROLE_NOMIS_PRISONPERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = IdentifyingMarkImageMappingResponse::class))],
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
        responseCode = "404",
        description = "Not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getIdentifyingMarkImageMappings(
    @PathVariable dpsImageId: UUID,
  ) = service.getIdentifyingMarkImageMapping(dpsImageId)
    ?.toResponse()
    ?: throw NotFoundException("Identifying mark image mapping not found for DPS image id $dpsImageId")
}
