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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationPunishmentBatchMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationPunishmentBatchUpdateMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationPunishmentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.AdjudicationMappingService

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
class PunishmentsMappingResource(private val mappingService: AdjudicationMappingService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_ADJUDICATIONS')")
  @PostMapping("/mapping/punishments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new set of adjudication punishment mapping",
    description = "Creates a record of a DPS punishment id and NOMIS bookingId and sanction sequence. Requires NOMIS_ADJUDICATIONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = AdjudicationPunishmentBatchMappingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping entries created"),
      ApiResponse(
        responseCode = "409",
        description = "One of the punishment mappings already exist",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun createPunishmentBatchMappings(
    @RequestBody @Valid
    createMappingRequest: AdjudicationPunishmentBatchMappingDto,
  ) =
    try {
      mappingService.createPunishmentMappings(createMappingRequest.punishments)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Adjudication punishment mapping already exists, detected by $e",
        duplicate = createMappingRequest,
        cause = e,
      )
    }

  @PreAuthorize("hasRole('ROLE_NOMIS_ADJUDICATIONS')")
  @PutMapping("/mapping/punishments")
  @Operation(
    summary = "Creates a new set of adjudication punishment mapping and deletes ones no longer required",
    description = "Creates a record of a DPS punishment id and NOMIS bookingId and sanction sequence. The ones that require deleting are removed NOMIS id. Requires NOMIS_ADJUDICATIONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = AdjudicationPunishmentBatchUpdateMappingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping entries created"),
      ApiResponse(
        responseCode = "409",
        description = "One of the punishment mappings already exist",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun updatePunishmentBatchMappings(
    @RequestBody @Valid
    updateMappingRequest: AdjudicationPunishmentBatchUpdateMappingDto,
  ) =
    try {
      mappingService.createAndDeletePunishmentMappings(updateMappingRequest)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Adjudication punishment mapping already exists, detected by $e",
        duplicate = updateMappingRequest,
        cause = e,
      )
    }

  @PreAuthorize("hasRole('ROLE_NOMIS_ADJUDICATIONS')")
  @GetMapping("/mapping/punishments/{dpsPunishmentId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by DPS punishment id. Requires role NOMIS_ADJUDICATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = AdjudicationPunishmentMappingDto::class)),
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
    @Schema(description = "DPS Punishment Id", example = "12345", required = true)
    @PathVariable
    dpsPunishmentId: String,
  ): AdjudicationPunishmentMappingDto = mappingService.getPunishmentMappingByDpsId(dpsPunishmentId)
}
