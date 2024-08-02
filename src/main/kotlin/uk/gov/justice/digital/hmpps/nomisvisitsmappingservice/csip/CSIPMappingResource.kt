package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('ROLE_NOMIS_CSIP')")
@RequestMapping("/mapping/csip", produces = [MediaType.APPLICATION_JSON_VALUE])
class CSIPMappingResource(private val mappingService: CSIPMappingService) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new CSIP Report mapping",
    description = "Creates a mapping between a Nomis CSIP report id and DPS CSIP report id. Requires role NOMIS_CSIP",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CSIPMappingDto::class),
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
  suspend fun createMapping(
    @RequestBody @Valid
    createMappingRequest: CSIPMappingDto,
  ) =
    try {
      mappingService.createCSIPMapping(createMappingRequest)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "CSIP mapping already exists",
        duplicate = createMappingRequest,
        existing = getExistingMappingSimilarTo(createMappingRequest),
        cause = e,
      )
    }

  @GetMapping("/nomis-csip-id/{nomisCSIPId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by nomisCSIPId. Requires role NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CSIPMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Nomis csip id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingNomisId(
    @Schema(description = "Nomis CSIP Id", required = true)
    @PathVariable
    nomisCSIPId: Long,
  ): CSIPMappingDto = mappingService.getMappingByNomisCSIPId(nomisCSIPId)

  @GetMapping("/dps-csip-id/{csipId}")
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by DPS CSIP Id. Requires role NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CSIPMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "CSIP id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingByDPSId(
    @Schema(description = "DPS CSIP Id", example = "12345", required = true)
    @PathVariable
    csipId: String,
  ): CSIPMappingDto = mappingService.getMappingByDPSCSIPId(csipId)

  @DeleteMapping("/dps-csip-id/{dpsCSIPId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a specific CSIP mapping by DPS CSIP id",
    description = "Deletes a mapping by DPS id. Requires role NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "CSIP mapping deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteMapping(
    @Schema(description = "DPS CSIP Id", example = "4321", required = true)
    @PathVariable
    dpsCSIPId: String,
  ) = mappingService.deleteMappingByDPSId(dpsCSIPId)

  @DeleteMapping()
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes CSIP mappings.",
    description = "Deletes all rows from the csip mapping table. Requires role NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "CSIP mappings deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun deleteAllMappings(
    @RequestParam(value = "onlyMigrated", required = false, defaultValue = "false")
    @Parameter(
      description = "if true delete mapping entries created by the migration process only (synchronisation records are unaffected)",
      example = "true",
    )
    onlyMigrated: Boolean,
  ) = mappingService.deleteMappings(onlyMigrated)

  @GetMapping("/migration-id/{migrationId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get paged mappings by migration id",
    description = "Retrieve all mappings of type 'MIGRATED' for the given migration id (identifies a single migration run). Results are paged.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping page returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CSIPMappingDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<CSIPMappingDto> = mappingService.getByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @GetMapping("/migrated/latest")
  @Operation(
    summary = "get the latest mapping for a migration",
    description = "Requires role NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CSIPMappingDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No mappings found at all for any migration",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getLatestMigratedCSIPMapping(): CSIPMappingDto =
    mappingService.getMappingForLatestMigrated()

  @PostMapping("/{offenderNo}/all")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a set of new csip mapping for a prisoner",
    description = "Creates a mapping between all the nomis csip ids and dps csip id. Requires ROLE_NOMIS_CSIP",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = PrisonerCSIPMappingsDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping created"),
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
  suspend fun createMappingsForPrisoner(
    @Schema(description = "NOMIS offender no", example = "A1234KT", required = true)
    @PathVariable
    offenderNo: String,
    @RequestBody @Valid
    prisonerMapping: PrisonerCSIPMappingsDto,
  ) =
    try {
      mappingService.createMappings(offenderNo, prisonerMapping)
    } catch (e: DuplicateKeyException) {
      val duplicateMapping = getMappingIdThatIsDuplicate(prisonerMapping.mappings)
      if (duplicateMapping != null) {
        throw DuplicateMappingException(
          messageIn = "CSIP mapping already exists",
          duplicate = duplicateMapping.let {
            CSIPMappingDto(
              dpsCSIPId = it.dpsCSIPId,
              nomisCSIPId = it.nomisCSIPId,
              offenderNo = offenderNo,
              label = prisonerMapping.label,
              mappingType = prisonerMapping.mappingType,
            )
          },
          existing = getExistingMappingSimilarTo(duplicateMapping),
          cause = e,
        )
      }
      throw e
    }

  @GetMapping("/{offenderNo}/all")
  @Operation(
    summary = "Gets all csip mappings for a prisoner",
    description = "Gets all the mapping between nomis csip ids and dps csip id related to specific prisoner created either via migration or synchronisation. Requires ROLE_NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mappings for prisoner",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = AllPrisonerCSIPMappingsDto::class))],
      ),
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
  suspend fun getMappingsForPrisoner(
    @Schema(description = "NOMIS offender no", example = "A1234KT", required = true)
    @PathVariable
    offenderNo: String,
  ) = mappingService.getMappings(offenderNo)

  @GetMapping("/migration-id/{migrationId}/grouped-by-prisoner")
  @Operation(
    summary = "Get paged mappings by migration id grouped by prisoner",
    description = "Retrieve all mappings of type 'MIGRATED' for the given migration id (identifies a single migration run) grouped by prisoner. Results are paged.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping page returned",
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
  suspend fun getMappingsByMigrationIdGroupByPrisoner(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<PrisonerCSIPMappingsSummaryDto> =
    mappingService.getByMigrationIdGroupedByPrisoner(pageRequest = pageRequest, migrationId = migrationId)

  private suspend fun getExistingMappingSimilarTo(mapping: CSIPMappingDto) = runCatching {
    mappingService.getMappingByNomisCSIPId(
      nomisCSIPId = mapping.nomisCSIPId,
    )
  }.getOrElse {
    mappingService.getMappingByDPSCSIPId(
      dpsCSIPId = mapping.dpsCSIPId,
    )
  }

  private suspend fun getExistingMappingSimilarTo(mapping: CSIPMappingIdDto) = runCatching {
    mappingService.getMappingByNomisCSIPId(
      nomisCSIPId = mapping.nomisCSIPId,
    )
  }.getOrElse {
    mappingService.getMappingByDPSCSIPId(
      dpsCSIPId = mapping.dpsCSIPId,
    )
  }

  private suspend fun getMappingIdThatIsDuplicate(mappings: List<CSIPMappingIdDto>): CSIPMappingIdDto? =
    mappings.find {
      // look for each mapping until I find one (i.e. that is there is no exception thrown)
      kotlin.runCatching { getExistingMappingSimilarTo(it) }.map { true }.getOrElse { false }
    }
  private suspend fun getMappingThatIsDuplicate(mappings: List<CSIPMappingDto>): CSIPMappingDto? =
    mappings.find {
      // look for each mapping until I find one (i.e. that is there is no exception thrown)
      kotlin.runCatching { getExistingMappingSimilarTo(it) }.map { true }.getOrElse { false }
    }
}
