package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

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
@PreAuthorize("hasRole('NOMIS_CONTACTPERSONS')")
@RequestMapping("/mapping/contact-person", produces = [MediaType.APPLICATION_JSON_VALUE])
class ContactPersonMappingResource(private val service: ContactPersonService) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping("/migrate")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a tree of contact person mappings typically for a migration",
    description = "Creates a tree of contact person mappings typically for a migration between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ContactPersonMappingsDto::class))],
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
    mappings: ContactPersonMappingsDto,
  ) =
    try {
      service.createMappings(mappings)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingPersonMappingSimilarTo(mappings.personMapping)
      if (existingMapping == null) {
        log.error("Child duplicate key found for person even though the person has never been migrated", e)
      }
      throw DuplicateMappingException(
        messageIn = "Person mapping already exists",
        duplicate = mappings.asPersonMappingDto(),
        existing = existingMapping ?: mappings.asPersonMappingDto(),
        cause = e,
      )
    }

  @GetMapping("/person/nomis-person-id/{nomisPersonId}")
  @Operation(
    summary = "Get person mapping by nomis person Id",
    description = "Retrieves the person a mapping by NOMIS Person Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonMappingDto::class)),
        ],
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
  suspend fun getPersonMappingByNomisId(
    @Schema(description = "NOMIS person id", example = "12345", required = true)
    @PathVariable
    nomisPersonId: Long,
  ): PersonMappingDto = service.getPersonMappingByNomisId(nomisId = nomisPersonId)

  @GetMapping("/person/migration-id/{migrationId}")
  @Operation(
    summary = "Get paged person mappings by migration id",
    description = "Retrieve all person mappings of type 'MIGRATED' for the given migration id (identifies a single migration run). Results are paged. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person mapping page returned",
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
  ): Page<PersonMappingDto> =
    service.getPersonMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @GetMapping("/person")
  @Operation(
    summary = "Get paged person mappings by migration id",
    description = "Retrieve all person mappings. Results are paged. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person mapping page returned",
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
  suspend fun getAllPersonMappings(
    @PageableDefault pageRequest: Pageable,
  ): Page<PersonMappingDto> =
    service.getAllPersonMappings(pageRequest = pageRequest)

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes all contact person mappings",
    description = "Deletes all contact person mappings regardless of source. This includes person, phone, address, email, employment, identifiers, restrictions, contacts and contact restrictions. This is expected to only ever been used in a non-production environment. Requires role ROLE_NOMIS_CONTACTPERSONS",
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
  suspend fun deleteAllMappings() = service.deleteAllMappings()

  private suspend fun getExistingPersonMappingSimilarTo(personMapping: ContactPersonSimpleMappingIdDto) = runCatching {
    service.getPersonMappingByNomisId(
      nomisId = personMapping.nomisId,
    )
  }.getOrElse {
    service.getPersonMappingByDpsIdOrNull(
      dpsId = personMapping.dpsId,
    )
  }
}

private fun ContactPersonMappingsDto.asPersonMappingDto() = PersonMappingDto(
  dpsId = personMapping.dpsId,
  nomisId = personMapping.nomisId,
  mappingType = mappingType,
  label = label,
  whenCreated = whenCreated,
)
