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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationPunishmentBatchMappingDto
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
      mappingService.createPunishmentMappings(createMappingRequest)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Adjudication punishment mapping already exists, detected by $e",
        duplicate = createMappingRequest,
        cause = e,
      )
    }
}
