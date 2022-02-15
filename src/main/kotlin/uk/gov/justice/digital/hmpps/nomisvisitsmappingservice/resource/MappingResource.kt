package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.MappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.RoomMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.MappingService
import javax.validation.Valid

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
class MappingResource(private val mappingService: MappingService) {

  @PreAuthorize("hasRole('ROLE_UPDATE_MAPPING')")
  @PostMapping("/mapping")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new visit",
    description = "Creates a new visit and decrements the visit balance.",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = MappingDto::class))]
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
  suspend fun createMapping(@RequestBody @Valid createMappingRequest: MappingDto) =
    mappingService.createVisitMapping(createMappingRequest)

  @PreAuthorize("hasRole('ROLE_READ_MAPPING')")
  @GetMapping("/mapping/nomisId/{nomisId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by NOMIS id.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = MappingDto::class))]
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
  ): MappingDto = mappingService.getVisitMappingGivenNomisId(nomisId)

  @PreAuthorize("hasRole('ROLE_READ_MAPPING')")
  @GetMapping("/mapping/vsipId/{vsipId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get mapping",
    description = "Retrieves a mapping by VSIP id.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = MappingDto::class))]
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
  ): MappingDto = mappingService.getVisitMappingGivenVsipId(vsipId)

  @PreAuthorize("hasRole('ROLE_READ_MAPPING')")
  @GetMapping("/prison/{prisonId}/room/nomis-room-id/{nomisRoomDescription}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get room mapping",
    description = "Retrieves a room mapping by NOMIS prison id and NOMIS room id.",
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

  @PreAuthorize("hasRole('ROLE_ADMIN_MAPPING')")
  @DeleteMapping("/mapping")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes visit id mappings",
    description = "Deletes all rows from the the visit id table",
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
  suspend fun deleteVisitIdMappings() = mappingService.deleteVisitMappings()
}
