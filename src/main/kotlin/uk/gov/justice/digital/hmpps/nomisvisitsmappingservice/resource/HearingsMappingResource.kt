package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationHearingMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.AdjudicationMappingService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
class HearingsMappingResource(private val mappingService: AdjudicationMappingService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_ADJUDICATIONS')")
  @PostMapping("/mapping/hearings")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new hearing mapping",
    description = "Creates a record of a DPS hearing Id and a NOMIS hearing Id . Requires NOMIS_ADJUDICATIONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = AdjudicationMappingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping entry created"),
      ApiResponse(
        responseCode = "409",
        description = "Hearing already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun createMapping(
    @RequestBody @Valid
    createMappingRequest: AdjudicationHearingMappingDto,
  ) =
    try {
      mappingService.createMapping(createMappingRequest)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Hearing mapping already exists, detected by $e",
        duplicate = createMappingRequest,
        cause = e,
      )
    }

  @PreAuthorize("hasRole('ROLE_NOMIS_ADJUDICATIONS')")
  @GetMapping("/mapping/hearings/nomis/{id}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by NOMIS hearing Id. Requires role NOMIS_ADJUDICATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = AdjudicationMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingGivenNomisId(
    @Schema(description = "NOMIS hearing id", example = "123", required = true)
    @PathVariable
    id: Long,
  ): AdjudicationHearingMappingDto = mappingService.getHearingMappingByNomisId(id)

  @PreAuthorize("hasRole('ROLE_NOMIS_ADJUDICATIONS')")
  @GetMapping("/mapping/hearings/dps/{id}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by DPS hearing Id. Requires role NOMIS_ADJUDICATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = AdjudicationMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingGivenDpsId(
    @Schema(description = "DPS hearing Id", example = "AB345", required = true)
    @PathVariable
    id: String,
  ): AdjudicationHearingMappingDto = mappingService.getHearingMappingByDpsId(id)

  @PreAuthorize("hasRole('ROLE_NOMIS_ADJUDICATIONS')")
  @DeleteMapping("/mapping/hearings/dps/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a specific mapping by DPS hearing id",
    description = "Deletes the mapping table row. Requires role NOMIS_ADJUDICATIONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Mapping deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteMapping(
    @Schema(description = "DPS Hearing id", example = "AB345", required = true)
    @PathVariable
    id: String,
  ) = mappingService.deleteHearingMapping(id)
}
