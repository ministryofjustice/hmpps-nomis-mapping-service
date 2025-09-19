package uk.gov.justice.digital.hmpps.nomismappingservice.csip.plans

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
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomismappingservice.csip.CSIPChildMappingDto
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('ROLE_NOMIS_CSIP')")
@RequestMapping("/mapping/csip/plans", produces = [MediaType.APPLICATION_JSON_VALUE])
class CSIPPlanMappingResource(
  private val planMappingService: CSIPPlanMappingService,
) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new CSIP Plan mapping",
    description = "Creates a mapping between a Nomis CSIP plan id and DPS CSIP plan id. Requires role NOMIS_CSIP",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CSIPChildMappingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping entry created"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate csip has been rejected. If Error code = 409 the body will return a DuplicateErrorResponse",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateMappingErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun createPlanMapping(
    @RequestBody
    @Valid
    csipPlanMapping: CSIPChildMappingDto,
  ) = try {
    planMappingService.createMapping(csipPlanMapping)
  } catch (e: DuplicateKeyException) {
    throw DuplicateMappingException(
      messageIn = "CSIP Plan mapping already exists, detected by $e",
      duplicate = csipPlanMapping,
      existing = getExistingMappingSimilarTo(csipPlanMapping),
      cause = e,
    )
  }

  @GetMapping("/nomis-csip-plan-id/{nomisCSIPPlanId}")
  @Operation(
    summary = "get CSIP Plan mapping",
    description = "Retrieves a CSIP Plan mapping by NOMIS id. Requires role NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CSIPChildMappingDto::class)),
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
  suspend fun getMappingByNomisId(
    @Schema(description = "NOMIS CSIP Plan id", example = "12345", required = true)
    @PathVariable
    nomisCSIPPlanId: Long,
  ): CSIPChildMappingDto = planMappingService.getMappingByNomisId(nomisCSIPPlanId = nomisCSIPPlanId)

  @GetMapping("/dps-csip-plan-id/{dpsCSIPPlanId}")
  @Operation(
    summary = "get CSIP Plan mapping",
    description = "Retrieves a csip plan mapping by DPS id. Requires role NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CSIPChildMappingDto::class)),
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
  suspend fun getCSIPPlanMappingByDpsId(
    @Schema(description = "DPS CSIP plan id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsCSIPPlanId: String,
  ): CSIPChildMappingDto = planMappingService.getMappingByDpsId(dpsCSIPPlanId = dpsCSIPPlanId)

  @DeleteMapping("/dps-csip-plan-id/{dpsCSIPPlanId}")
  @Operation(
    summary = "Deletes CSIP Plan mapping",
    description = "Deletes a CSIP Plan mapping by DPS id. Requires role NOMIS_CSIP",
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
  suspend fun deleteCSIPPlanMappingByDpsId(
    @Schema(description = "DPS CSIP Plan id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsCSIPPlanId: String,
  ) = planMappingService.deleteMappingByDpsId(dpsCSIPPlanId = dpsCSIPPlanId)

  private suspend fun getExistingMappingSimilarTo(mapping: CSIPChildMappingDto) = runCatching {
    planMappingService.getMappingByNomisId(nomisCSIPPlanId = mapping.nomisId)
  }.getOrElse {
    planMappingService.getMappingByDpsId(dpsCSIPPlanId = mapping.dpsId)
  }
}
