package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.religion

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
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/core-person-religion", produces = [MediaType.APPLICATION_JSON_VALUE])
class ReligionResource(private val religionService: ReligionService) {
  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping("/religion")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a religion mapping",
    description = "Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
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
  suspend fun createReligionMapping(
    @RequestBody @Valid
    mapping: ReligionMappingDto,
  ) = try {
    religionService.createReligion(mapping)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingReligionMappingSimilarTo(mapping)
    throw DuplicateMappingException(
      messageIn = "Religion mapping already exists",
      duplicate = mapping,
      existing = existingMapping ?: mapping,
      cause = e,
    )
  }

  @GetMapping("/religion/nomis-id/{nomisId}")
  @Operation(
    summary = "Get religion mapping by nomis religion id",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping data",
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
  suspend fun getReligionMappingByNomisId(
    @Schema(description = "NOMIS religion id", example = "1234", required = true)
    @PathVariable
    nomisId: Long,
  ): ReligionMappingDto = religionService.getReligionMappingByNomisId(
    nomisId = nomisId,
  )

  @GetMapping("/religion/cpr-id/{cprId}")
  @Operation(
    summary = "Get religion mapping by cpr id",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping data",
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
  suspend fun getReligionMappingByCprId(
    @Schema(description = "CPR id", example = "1234", required = true)
    @PathVariable
    cprId: String,
  ): ReligionMappingDto = religionService.getReligionMappingByCprId(cprId = cprId)

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/religion/nomis-id/{nomisId}")
  @Operation(
    summary = "Deletes religion mapping by nomis religion id",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Mapping deleted or does not exist",
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
    ],
  )
  suspend fun deleteReligionMappingByNomisId(
    @Schema(description = "NOMIS religion id", example = "1234", required = true)
    @PathVariable
    nomisId: Long,
  ) {
    religionService.deleteReligionMappingByNomisId(nomisId = nomisId)
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a mini tree of religion mappings typically for a migration",
    description = "Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
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
  suspend fun createMigrationMappings(
    @RequestBody @Valid
    mappings: ReligionsMigrationMappingDto,
  ) = try {
    religionService.createMappings(mappings)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingReligionsMappingSimilarTo(mappings)
    if (existingMapping == null) {
      log.error("Child duplicate key found for religions even though religions for the prisoner have never been migrated", e)
    }
    throw DuplicateMappingException(
      messageIn = "Religion mapping already exists",
      duplicate = mappings,
      existing = existingMapping?.asReligionsMigrationMappingDto() ?: mappings,
      cause = e,
    )
  }

  @GetMapping("/migration-id/{migrationId}")
  @Operation(
    summary = "Get paged religions mappings by migration id",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Religions mapping page returned",
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
  suspend fun getReligionsMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<ReligionsMappingDto> = religionService.getReligionsMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @DeleteMapping("/all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete all religions and religion mappings",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "All mappings deleted",
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
  suspend fun deleteAllMappings() = religionService.deleteAllMappings()

  private suspend fun getExistingReligionsMappingSimilarTo(mapping: ReligionsMigrationMappingDto) = runCatching {
    religionService.getReligionsMappingByNomisId(
      nomisPrisonNumber = mapping.nomisPrisonNumber,
    )
  }.getOrElse {
    religionService.getReligionsMappingByCprIdOrNull(
      cprId = mapping.cprId,
    )
  }

  private suspend fun getExistingReligionMappingSimilarTo(mapping: ReligionMappingDto) = runCatching {
    religionService.getReligionMappingByNomisId(mapping.nomisId)
  }.getOrElse {
    religionService.getReligionMappingByCprIdOrNull(
      cprId = mapping.cprId,
    )
  }
}

data class ReligionsMappingDto(
  val cprId: String,
  val nomisPrisonNumber: String,
  val label: String?,
  val mappingType: StandardMappingType,
  val whenCreated: LocalDateTime? = null,
)

data class ReligionMappingDto(
  val cprId: String,
  val nomisId: Long,
  val nomisPrisonNumber: String,
  val label: String?,
  val mappingType: StandardMappingType,
  val whenCreated: LocalDateTime? = null,
)

data class ReligionsMigrationMappingDto(
  val cprId: String,
  val nomisPrisonNumber: String,
  val religions: List<ReligionMigrationMappingDto>,
  val label: String?,
  val mappingType: StandardMappingType = StandardMappingType.MIGRATED,
  val whenCreated: LocalDateTime? = null,
)

data class ReligionMigrationMappingDto(
  val cprId: String,
  val nomisId: Long,
  val nomisPrisonNumber: String,
)

private fun ReligionsMappingDto.asReligionsMigrationMappingDto() = ReligionsMigrationMappingDto(
  cprId = this.cprId,
  nomisPrisonNumber = this.nomisPrisonNumber,
  label = this.label,
  mappingType = this.mappingType,
  whenCreated = this.whenCreated,
  religions = emptyList(),
)
