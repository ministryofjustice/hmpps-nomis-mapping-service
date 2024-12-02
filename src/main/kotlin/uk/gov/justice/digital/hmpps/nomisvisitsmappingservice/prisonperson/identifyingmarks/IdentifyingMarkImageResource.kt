package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts.AlertMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.IdentifyingMarkImageMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.toDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.toEntity
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
        content = [Content(mediaType = "application/json", schema = Schema(implementation = IdentifyingMarkImageMappingDto::class))],
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
    ?.toDto()
    ?: throw NotFoundException("Identifying mark image mapping not found for NOMIS offender image id $nomisImageId")

  @GetMapping("/dps-image-id/{dpsImageId}")
  @Operation(
    summary = "Gets an identifying mark image mapping by DPS image id",
    description = "Gets an identifying mark image mapping by DPS image id. Requires ROLE_NOMIS_PRISONPERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = IdentifyingMarkImageMappingDto::class))],
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
    ?.toDto()
    ?: throw NotFoundException("Identifying mark image mapping not found for DPS image id $dpsImageId")

  @PostMapping("/identifying-mark-image")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create an identifying mark image mapping",
    description = "Create an identifying mark image mapping. Requires ROLE_NOMIS_PRISONPERSON",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = AlertMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping created"),
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
  suspend fun createIdentifyingMarkImageMapping(
    @RequestBody mapping: IdentifyingMarkImageMappingDto,
  ) = try {
    service.createIdentifyingMarkImageMapping(mapping.toEntity())
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "Identifying mark image mapping already exists",
      duplicate = mapping,
      existing = getExistingMappingSimilarTo(mapping),
      cause = e,
    )
  }

  private suspend fun getExistingMappingSimilarTo(mapping: IdentifyingMarkImageMappingDto) =
    runCatching {
      service.getIdentifyingMarkImageMapping(mapping.nomisOffenderImageId)
        ?: let { service.getIdentifyingMarkImageMapping(mapping.dpsId) }
    }
}
