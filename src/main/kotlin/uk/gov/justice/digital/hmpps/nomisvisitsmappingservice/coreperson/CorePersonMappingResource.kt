package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.coreperson

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

  @GetMapping("/migration-id/{migrationId}")
  @Operation(
    summary = "Get paged core person mappings by migration id",
    description = "Retrieve all core person mappings of type 'MIGRATED' for the given migration id (identifies a single migration run). Results are paged. Requires role ROLE_NOMIS_CORE_PERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Core Person mapping page returned",
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
  suspend fun getPersonMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<CorePersonMappingDto> =
    service.getCorePersonMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @GetMapping("/person/cpr-id/{cprId}")
  @Operation(
    summary = "Get core person mapping by cpr core person Id",
    description = "Retrieves the person mapping by CPR Core Person Id. Requires role ROLE_NOMIS_CORE_PERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person mapping data",
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
  suspend fun getCorePersonMappingByCprId(
    @Schema(description = "CPR core person id", example = "12345", required = true)
    @PathVariable
    cprId: String,
  ): CorePersonMappingDto = service.getCorePersonMappingByCprId(cprId = cprId)

  @GetMapping("/address/cpr-address-id/{cprAddressId}")
  @Operation(
    summary = "Get person address mapping by cpr core person address Id",
    description = "Retrieves the person address mapping by CPR Core Person Address Id. Requires role ROLE_NOMIS_CORE_PERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Address mapping data",
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
  suspend fun getAddressMappingByCprId(
    @Schema(description = "CPR address id", example = "12345", required = true)
    @PathVariable
    cprAddressId: String,
  ): CorePersonAddressMappingDto = service.getAddressMappingByCprId(cprId = cprAddressId)

  private suspend fun getExistingCorePersonMappingSimilarTo(personMapping: CorePersonMappingIdDto) = runCatching {
    service.getCorePersonMappingByNomisPrisonNumber(
      nomisPrisonNumber = personMapping.nomisPrisonNumber,
    )
  }.getOrElse {
    service.getCorePersonMappingByCprIdOrNull(
      cprId = personMapping.cprId,
    )
  }
}

private fun CorePersonMappingsDto.asCorePersonMappingDto() = CorePersonMappingDto(
  cprId = personMapping.cprId,
  nomisPrisonNumber = personMapping.nomisPrisonNumber,
  mappingType = mappingType,
  label = label,
  whenCreated = whenCreated,
)
