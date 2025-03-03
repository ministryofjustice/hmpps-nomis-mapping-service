@file:JvmName("VisitOrderMappingResourceKt")

package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.visitorders

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_VISIT_ORDERS')")
@RequestMapping("/mapping/visit-orders", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerVisitOrderMappingResource(private val service: VisitOrderService) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a Prisoner visit order mapping",
    description = "Creates a Prisoner visit order Mapping. Requires ROLE_NOMIS_VISIT_ORDERS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = PrisonerVisitOrderMappingDto::class),
        ),
      ],
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
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate mapping has been rejected. If Error code = 409 the body will return a DuplicateErrorResponse",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateMappingErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun createPrisonerMapping(
    @RequestBody @Valid
    mapping: PrisonerVisitOrderMappingDto,
  ) = try {
    service.createMapping(mapping)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "Visit Order mapping already exists",
      duplicate = mapping,
      existing = getExistingMappingSimilarTo(mapping),
      cause = e,
    )
  }

  @GetMapping("/nomis-prison-number/{nomisPrisonNumber}")
  @Operation(
    summary = "Get prisoner visit order mapping by Nomis prison number",
    description = "Retrieves the prisoner visit order mapping by Nomis prison number (aka offender number). Requires role ROLE_NOMIS_VISIT_ORDERS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit order mapping data",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Access this endpoint is forbidden",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getPrisonerMappingByNomisId(
    @Schema(description = "NOMIS prison number aka offender no.", example = "A1234BC", required = true)
    @PathVariable
    nomisPrisonNumber: String,
  ): PrisonerVisitOrderMappingDto = service.getMappingByNomisId(nomisPrisonNumber = nomisPrisonNumber)

  @GetMapping("/dps-id/{dpsId}")
  @Operation(
    summary = "Get Prisoner visit order mapping by Dps id",
    description = "Retrieves the Prisoner visit order mapping by Dps visit order id. Requires role ROLE_NOMIS_VISIT_ORDERS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit order mapping data",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Access this endpoint is forbidden",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getPrisonerMappingByDpsId(
    @Schema(description = "Dps id", example = "12345", required = true)
    @PathVariable
    dpsId: String,
  ): PrisonerVisitOrderMappingDto = service.getMappingByDpsId(dpsId = dpsId)

  @DeleteMapping("/dps-id/{dpsId}")
  @Operation(
    summary = "Deletes Prisoner visit order mapping",
    description = "Deletes Prisoner visit order mapping by DPS id. Requires role NOMIS_VISIT_ORDERS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Mapping Deleted",
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
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend fun deletePrisonerMappingByDpsId(
    @Schema(description = "Dps id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsId: String,
  ) = service.deletePrisonerVisitOrderMappingByDpsId(dpsId = dpsId)

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes all prisoner visit order mappings",
    description = """Deletes all prisoenrvisit order mappings regardless of source.
      This is expected to only ever been used in a non-production environment. Requires role ROLE_NOMIS_VISIT_ORDERS""",
    responses = [
      ApiResponse(responseCode = "204", description = "All mappings deleted"),
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
  suspend fun deleteAllPrisonerMappings() = service.deleteAllMappings()

  @GetMapping("/migration-id/{migrationId}")
  @Operation(
    summary = "Get paged Prisoner visit order mappings by migration id",
    description = "Retrieve all Prisoner visit order mappings of type 'MIGRATED' for the given migration id (identifies a single migration run). Results are paged. Requires role ROLE_NOMIS_VISIT_ORDERS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit order mapping page returned",
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
  suspend fun getPrisonerMappingByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<PrisonerVisitOrderMappingDto> = service.getMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  private suspend fun getExistingMappingSimilarTo(mapping: PrisonerVisitOrderMappingDto) = runCatching {
    service.getMappingByNomisId(
      nomisPrisonNumber = mapping.nomisPrisonNumber,
    )
  }.getOrElse {
    service.getMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }
}
