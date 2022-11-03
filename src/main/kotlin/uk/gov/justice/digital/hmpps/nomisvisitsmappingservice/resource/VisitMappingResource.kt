package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.CreateRoomMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.RoomMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.VisitMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.VisitMappingService
import javax.validation.Valid

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
class VisitMappingResource(private val mappingService: VisitMappingService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @PostMapping("/mapping/visits")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new visit",
    description = "Creates a new visit and decrements the visit balance. Requires role NOMIS_VISITS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitMappingDto::class))]
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Visit mapping entry created"),
      ApiResponse(
        responseCode = "400",
        description = "Nomis or VSIP ids already exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun createMapping(@RequestBody @Valid createMappingRequest: VisitMappingDto) =
    mappingService.createVisitMapping(createMappingRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @GetMapping("/mapping/visits/nomisId/{nomisId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by NOMIS id. Requires role NOMIS_VISITS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitMappingDto::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "NOMIS id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun getVisitMappingGivenNomisId(
    @Schema(description = "NOMIS Id", example = "12345", required = true)
    @PathVariable
    nomisId: Long,
  ): VisitMappingDto = mappingService.getVisitMappingGivenNomisId(nomisId)

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @GetMapping("/mapping/visits/vsipId/{vsipId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by VSIP id. Requires role NOMIS_VISITS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitMappingDto::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "VSIP id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun getVisitMappingGivenVsipId(
    @Schema(description = "VSIP Id", example = "12345", required = true)
    @PathVariable vsipId: String
  ): VisitMappingDto = mappingService.getVisitMappingGivenVsipId(vsipId)

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @GetMapping("/mapping/visits/migrated/latest")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get the latest mapping for a migration",
    description = "Requires role NOMIS_VISITS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitMappingDto::class))]
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
  suspend fun getLatestMigratedVisitMapping(): VisitMappingDto = mappingService.getVisitMappingForLatestMigrated()

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @GetMapping("/prison/{prisonId}/room/nomis-room-id/{nomisRoomDescription}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get room mapping",
    description = "Retrieves a room mapping by NOMIS prison id and NOMIS room id. Requires role NOMIS_VISITS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = RoomMappingDto::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "NOMIS room description does not exist in the mapping table for the given prison",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun getRoomMapping(
    @Schema(description = "NOMIS prison Id", example = "MDI", required = true)
    @PathVariable
    prisonId: String,
    @Schema(description = "NOMIS room description", example = "HEI_LW01", required = true)
    @PathVariable
    nomisRoomDescription: String,
  ): RoomMappingDto = mappingService.getRoomMapping(prisonId, nomisRoomDescription)

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @GetMapping("/prison/{prisonId}/room-mappings")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get room mappings for a prison",
    description = "Retrieves  room mappings associated with a NOMIS prison id. Requires role NOMIS_VISITS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping list Returned"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun getRoomMappings(
    @Schema(description = "NOMIS prison Id", example = "MDI", required = true)
    @PathVariable
    prisonId: String,
  ): List<RoomMappingDto> = mappingService.getRoomMappings(prisonId)

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @PostMapping("/prison/{prisonId}/room-mappings")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new room mapping",
    description = "Creates a new room mapping. Requires role NOMIS_VISITS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitMappingDto::class))]
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Visit mapping entry created"),
      ApiResponse(
        responseCode = "400",
        description = "mapping for this nomis room and prison already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun createRoomMapping(
    @Schema(description = "NOMIS prison Id", example = "MDI", required = true)
    @PathVariable
    prisonId: String,
    @RequestBody @Valid createMappingRequest: CreateRoomMappingDto
  ) =
    mappingService.createRoomMapping(prisonId, createMappingRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @DeleteMapping("/prison/{prisonId}/room-mappings/nomis-room-id/{nomisRoomDescription}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a room mapping",
    description = "Removes room mapping given the prison and nomis room description. Requires role NOMIS_VISITS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitMappingDto::class))]
    ),
    responses = [
      ApiResponse(responseCode = "204", description = "Visit room mapping deleted"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun deleteRoomMapping(
    @Schema(description = "NOMIS prison Id", example = "MDI", required = true)
    @PathVariable
    prisonId: String,
    @Schema(description = "NOMIS room description", example = "MDI", required = true)
    @PathVariable
    nomisRoomDescription: String
  ) =
    mappingService.deleteRoomMapping(prisonId, nomisRoomDescription)

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @DeleteMapping("/mapping/visits")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes visit id mappings",
    description = "Deletes all rows from the the visit id table. Requires role ADMIN_MAPPING",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Visit id mappings deleted"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  suspend fun deleteVisitIdMappings(
    @RequestParam(value = "onlyMigrated", required = false, defaultValue = "false")
    @Parameter(
      description = "if true delete mapping entries created by the migration process only (synchronisation records are unaffected)",
      example = "true"
    ) onlyMigrated: Boolean
  ) = mappingService.deleteVisitMappings(
    onlyMigrated
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_VISITS')")
  @GetMapping("/mapping/visits/migration-id/{migrationId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get paged mappings by migration id",
    description = "Retrieve all mappings of type 'MIGRATED' for the given migration id (identifies a single migration run). Results are paged.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping page returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitMappingDto::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  suspend fun getMigratedVisitMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable migrationId: String
  ): Page<VisitMappingDto> =
    mappingService.getVisitMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)
}
