package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.SentencingAdjustmentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.SentencingMappingService

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
class SentencingMappingResource(private val mappingService: SentencingMappingService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @PostMapping("/mapping/sentencing/adjustments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new sentence adjustment mapping",
    description = "Creates a mapping between nomis sentence adjustment ids and Sentencing service id. Requires ROLE_NOMIS_SENTENCING",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = SentencingAdjustmentMappingDto::class)
        )
      ]
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Sentence adjustment mapping entry created"),
      ApiResponse(
        responseCode = "400",
        description = "Nomis or Sentencing ids already exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun createMapping(@RequestBody @Valid createMappingRequest: SentencingAdjustmentMappingDto) =
    mappingService.createSentenceAdjustmentMapping(createMappingRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/mapping/sentencing/adjustments/nomis-adjustment-category/{nomisAdjustmentCategory}/nomis-adjustment-id/{nomisAdjustmentId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by NOMIS id. Requires role ROLE_NOMIS_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SentencingAdjustmentMappingDto::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "NOMIS sentence adjustment id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun getSentenceAdjustmentMappingGivenNomisId(
    @Schema(description = "NOMIS Adjustment Id", example = "12345", required = true)
    @PathVariable
    nomisAdjustmentId: Long,
    @Schema(
      description = "NOMIS Adjustment Type",
      example = "SENTENCE",
      required = true,
      allowableValues = ["SENTENCE", "KEY-DATE"]
    )
    @PathVariable
    nomisAdjustmentCategory: String,
  ): SentencingAdjustmentMappingDto =
    mappingService.getSentenceAdjustmentMappingByNomisId(nomisAdjustmentId, nomisAdjustmentCategory)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/mapping/sentencing/adjustments/adjustment-id/{adjustmentId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by Sentencing adjustment id (from the sentencing domain). Requires role ROLE_NOMIS_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SentencingAdjustmentMappingDto::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "sentence adjustment id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun getSentencingAdjustmentMapping(
    @Schema(description = "Sentence Adjustment Id", example = "12345", required = true)
    @PathVariable adjustmentId: String
  ): SentencingAdjustmentMappingDto = mappingService.getSentencingAdjustmentMappingByAdjustmentId(adjustmentId)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/mapping/sentencing/adjustments/migrated/latest")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get the latest mapping for a migration",
    description = "Requires role READ_MAPPING, UPDATE_MAPPING or ADMIN_MAPPING",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SentencingAdjustmentMappingDto::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "No mappings found at all for any migration",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun getLatestMigratedSentenceAdjustmentMapping(): SentencingAdjustmentMappingDto =
    mappingService.getSentencingAdjustmentMappingForLatestMigrated()

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @DeleteMapping("/mapping/sentencing/adjustments")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes sentence adjustment mappings",
    description = "Deletes all rows from the the sentence adjustment mapping table. Requires role NOMIS_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "sentence adjustment mappings deleted"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun deleteSentenceAdjustmentMappings(
    @RequestParam(value = "onlyMigrated", required = false, defaultValue = "false")
    @Parameter(
      description = "if true delete mapping entries created by the migration process only (synchronisation records are unaffected)",
      example = "true"
    ) onlyMigrated: Boolean
  ) = mappingService.deleteSentenceAdjustmentMappings(
    onlyMigrated
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @DeleteMapping("/mapping/sentencing/adjustments/adjustment-id/{adjustmentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a specific Sentence Adjustment mapping by sentence adjustment Id",
    description = "Deletes the Sentence Adjustment mapping. Requires role NOMIS_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Sentence Adjustment mapping deleted"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun deleteSentenceAdjustmentMapping(
    @Schema(description = "Sentence Adjustment Id (from sentencing domain)", example = "12345", required = true)
    @PathVariable adjustmentId: String
  ) = mappingService.deleteSentencingAdjustmentMapping(adjustmentId)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/mapping/sentencing/adjustments/migration-id/{migrationId}")
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
            schema = Schema(implementation = SentencingAdjustmentMappingDto::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  suspend fun getMigratedSentenceAdjustmentMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable migrationId: String
  ): Page<SentencingAdjustmentMappingDto> =
    mappingService.getSentenceAdjustmentMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)
}
