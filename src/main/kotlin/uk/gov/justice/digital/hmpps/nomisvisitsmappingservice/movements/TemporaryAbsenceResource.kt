package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.movements

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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MOVEMENTS')")
@RequestMapping("/mapping/temporary-absence", produces = [MediaType.APPLICATION_JSON_VALUE])
class TemporaryAbsenceResource(
  private val service: TemporaryAbsenceService,
) {

  @PutMapping("/migrate")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates all mappings for prisoner temporary absences which are all migrated at the same time",
    description = "Creates mappings for prisoner temporary absences including movement applications, outside movements, scheduled movements and movements. Requires ROLE_NOMIS_MOVEMENTS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = TemporaryAbsencesPrisonerMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mappings created"),
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
  suspend fun createMappings(
    @RequestBody mappings: TemporaryAbsencesPrisonerMappingDto,
  ) = service.createMigrationMappings(mappings)

  @PostMapping("/application")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a mapping for a single temporary absence application",
    description = "Creates a mapping for a single temporary absence application. Requires ROLE_NOMIS_MOVEMENTS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = TemporaryAbsenceApplicationSyncMappingDto::class))],
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
  suspend fun createApplicationSyncMapping(
    @RequestBody mapping: TemporaryAbsenceApplicationSyncMappingDto,
  ) = try {
    service.createApplicationMapping(mapping)
  } catch (dke: DuplicateKeyException) {
    val existing = getExistingApplicationMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Temporary absence application mapping already exists",
      duplicate = mapping,
      existing = existing,
      cause = dke,
    )
  }

  private suspend fun getExistingApplicationMappingSimilarTo(mapping: TemporaryAbsenceApplicationSyncMappingDto) = runCatching {
    service.getApplicationMappingByNomisId(mapping.nomisMovementApplicationId)
  }
    .getOrElse {
      service.getApplicationMappingByDpsId(mapping.dpsMovementApplicationId)
    }

  @GetMapping("/application/nomis-application-id/{nomisApplicationId}")
  @Operation(
    summary = "Gets a mapping for a single temporary absence application by NOMIS ID",
    description = "Gets a mapping for a single temporary absence application by NOMIS ID. Requires ROLE_NOMIS_MOVEMENTS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = TemporaryAbsenceApplicationSyncMappingDto::class))],
    ),
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
  suspend fun getApplicationSyncMappingByNomisId(
    @PathVariable nomisApplicationId: Long,
  ) = service.getApplicationMappingByNomisId(nomisApplicationId)
}
