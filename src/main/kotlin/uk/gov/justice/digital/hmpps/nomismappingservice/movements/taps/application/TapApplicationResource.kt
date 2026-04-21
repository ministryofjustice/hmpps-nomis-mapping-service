package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application

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
import java.util.UUID

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/taps/application", produces = [MediaType.APPLICATION_JSON_VALUE])
class TapApplicationResource(
  private val service: TapApplicationService,
) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a mapping for a single tap application",
    description = "Creates a mapping for a single tap application. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = TapApplicationMappingDto::class))],
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
  suspend fun createTapApplicationMapping(
    @RequestBody mapping: TapApplicationMappingDto,
  ) = try {
    service.createApplicationMapping(mapping)
  } catch (dke: DuplicateKeyException) {
    val existing = getExistingApplicationMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Tap application mapping already exists",
      duplicate = mapping,
      existing = existing,
      cause = dke,
    )
  }

  private suspend fun getExistingApplicationMappingSimilarTo(mapping: TapApplicationMappingDto) = runCatching {
    service.getApplicationMappingByNomisId(mapping.nomisApplicationId)
  }
    .getOrElse {
      service.getApplicationMappingByDpsId(mapping.dpsAuthorisationId)
    }

  @GetMapping("/nomis-id/{nomisApplicationId}")
  @Operation(
    summary = "Gets a mapping for a single tap application by NOMIS ID",
    description = "Gets a mapping for a single tap application by NOMIS ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
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
  suspend fun getTapApplicationMappingByNomisId(
    @PathVariable nomisApplicationId: Long,
  ) = service.getApplicationMappingByNomisId(nomisApplicationId)

  @GetMapping("/dps-id/{dpsAuthorisationId}")
  @Operation(
    summary = "Gets a mapping for a single tap application by DPS ID",
    description = "Gets a mapping for a single tap application by DPS ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
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
  suspend fun getTapApplicationSyncMappingByDpsId(
    @PathVariable dpsAuthorisationId: UUID,
  ) = service.getApplicationMappingByDpsId(dpsAuthorisationId)

  @DeleteMapping("/nomis-id/{nomisApplicationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a mapping for a single tap application by NOMIS ID",
    description = "Deletes a mapping for a single tap application by NOMIS ID. Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
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
  suspend fun deleteTapApplicationByNomisId(
    @PathVariable nomisApplicationId: Long,
  ) = service.deleteApplicationMappingByNomisId(nomisApplicationId)
}
