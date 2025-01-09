package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.coreperson

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_CORE_PERSON')")
@RequestMapping("/mapping/core-person", produces = [MediaType.APPLICATION_JSON_VALUE])
class CorePersonMappingResource(private val service: CorePersonService) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping("/migrate")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a tree of core person mappings typically for a migration",
    description = "Creates a tree of core person mappings typically for a migration between NOMIS ids and cpr ids. Requires ROLE_NOMIS_CORE_PERSON",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CorePersonMappingsDto::class),
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
  suspend fun createMappings(
    @RequestBody @Valid
    mappings: CorePersonMappingsDto,
  ) =
    try {
      service.createMappings(mappings)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingCorePersonMappingSimilarTo(mappings.personMapping)
      if (existingMapping == null) {
        log.error("Child duplicate key found for core person even though the core person has never been migrated", e)
      }
      throw DuplicateMappingException(
        messageIn = "Core Person mapping already exists",
        duplicate = mappings.asCorePersonMappingDto(),
        existing = existingMapping ?: mappings.asCorePersonMappingDto(),
        cause = e,
      )
    }

  private suspend fun getExistingCorePersonMappingSimilarTo(personMapping: CorePersonSimpleMappingIdDto) = runCatching {
    service.getCorePersonMappingByPrisonNumber(
      prisonNumber = personMapping.prisonNumber,
    )
  }.getOrElse {
    service.getCorePersonMappingByCprIdOrNull(
      cprId = personMapping.cprId,
    )
  }
}

private fun CorePersonMappingsDto.asCorePersonMappingDto() = CorePersonMappingDto(
  cprId = personMapping.cprId,
  prisonNumber = personMapping.prisonNumber,
  mappingType = mappingType,
  label = label,
  whenCreated = whenCreated,
)
