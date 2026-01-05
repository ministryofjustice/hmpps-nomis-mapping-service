package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

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
@RequestMapping("/mapping/official-visits", produces = [MediaType.APPLICATION_JSON_VALUE])
class OfficialVisitsResource(private val officialVisitsService: OfficialVisitsService) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @GetMapping("/visit/nomis-id/{nomisVisitId}")
  @Operation(
    summary = "Gets visit mapping by nomis visit id",
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
  suspend fun getVisitMappingByNomisId(
    @Schema(description = "NOMIS visit id", example = "123", required = true)
    @PathVariable
    nomisVisitId: Long,
  ): OfficialVisitMappingDto = officialVisitsService.getOfficialVisitMappingByNomisId(nomisId = nomisVisitId)

  @GetMapping("/visit/dps-id/{dpsVisitId}")
  @Operation(
    summary = "Gets visit mapping by dps visit id",
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
  suspend fun getVisitMappingByDpsId(
    @Schema(description = "DPS visit id", example = "123", required = true)
    @PathVariable
    dpsVisitId: String,
  ): OfficialVisitMappingDto = officialVisitsService.getOfficialVisitMappingByDpsId(dpsId = dpsVisitId)

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a mini tree of visit mappings typically for a migration",
    description = "Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = OfficialVisitMigrationMappingDto::class))],
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
  suspend fun createMigrationMappings(
    @RequestBody @Valid
    mappings: OfficialVisitMigrationMappingDto,
  ): Unit = try {
    officialVisitsService.createMappings(mappings)
  } catch (e: DuplicateKeyException) {
    val existingMapping = getExistingVisitMappingSimilarTo(mappings)
    if (existingMapping == null) {
      log.error("Child duplicate key found for visit even though the visit has never been migrated", e)
    }
    throw DuplicateMappingException(
      messageIn = "Visit  mapping already exists",
      duplicate = mappings,
      existing = existingMapping?.asOfficialVisitMigrationMappingDto() ?: mappings,
      cause = e,
    )
  }

  @GetMapping("/migration-id/{migrationId}")
  @Operation(
    summary = "Get paged visit mappings by migration id",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit mapping page returned",
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
  suspend fun getOfficialVisitMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<OfficialVisitMappingDto> = officialVisitsService.getOfficialVisitMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @DeleteMapping("/all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete all official visit mappings",
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
  suspend fun deleteAllMappings(): Unit = officialVisitsService.deleteAllMappings()

  private suspend fun getExistingVisitMappingSimilarTo(mapping: OfficialVisitMigrationMappingDto): OfficialVisitMappingDto? = runCatching {
    officialVisitsService.getOfficialVisitMappingByNomisId(nomisId = mapping.nomisId)
  }.getOrElse {
    officialVisitsService.getOfficialVisitMappingByDpsIdOrNull(dpsId = mapping.dpsId)
  }
}

data class OfficialVisitMappingDto(
  val dpsId: String,
  val nomisId: Long,
  val label: String?,
  val mappingType: StandardMappingType,
  val whenCreated: LocalDateTime?,
)

data class OfficialVisitMigrationMappingDto(
  val dpsId: String,
  val nomisId: Long,
  val visitors: List<VisitorMigrationMappingDto>,
  val label: String?,
  val mappingType: StandardMappingType = StandardMappingType.MIGRATED,
  val whenCreated: LocalDateTime? = null,
)

data class VisitorMigrationMappingDto(
  val dpsId: String,
  val nomisId: Long,
)

private fun OfficialVisitMappingDto.asOfficialVisitMigrationMappingDto() = OfficialVisitMigrationMappingDto(
  dpsId = this.dpsId,
  nomisId = this.nomisId,
  label = this.label,
  mappingType = this.mappingType,
  whenCreated = this.whenCreated,
  visitors = emptyList(),
)
