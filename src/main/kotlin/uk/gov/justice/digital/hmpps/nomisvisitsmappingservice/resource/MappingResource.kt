package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.MappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.RoomMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.MappingService
import javax.validation.Valid

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
class MappingResource(private val mappingService: MappingService) {

  @PreAuthorize("hasRole('ROLE_UPDATE_NOMIS')")
  @PostMapping("/mapping")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new visit",
    description = "Creates a new visit and decrements the visit balance.",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = MappingDto::class))]
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Visit mapping entry created"
      ),
      ApiResponse(
        responseCode = "400",
        description = "Prison or person ids do not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "offenderNo does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  fun createMapping(@RequestBody @Valid createMappingRequest: MappingDto): Mono<Void> =
    mappingService.createVisitMapping(createMappingRequest)

  @PreAuthorize("hasRole('ROLE_READ_NOMIS')")
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
  fun getVisitMappingGivenNomisId(
    @Schema(description = "NOMIS Id", example = "12345", required = true)
    @PathVariable
    nomisId: Long,
  ): Mono<MappingDto> = mappingService.getVisitMappingGivenNomisId(nomisId)

  @PreAuthorize("hasRole('ROLE_READ_NOMIS')")
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
  fun getVisitMappingGivenVsipId(
    @Schema(description = "VSIP Id", example = "12345", required = true)
    @PathVariable vsipId: String
  ): Mono<MappingDto> = mappingService.getVisitMappingGivenVsipId(vsipId)

  @PreAuthorize("hasRole('ROLE_READ_NOMIS')")
  @GetMapping("/prison/{prisonId}/room/nomisRoomId/{nomisRoomDescription}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get room mapping",
    description = "Retrieves a room mapping by NOMIS prison id and NOMIS room id.",
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
        description = "NOMIS room id does not exist in the mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  fun getRoomMapping(
    @Schema(description = "NOMIS prison Id", example = "MDI", required = true)
    @PathVariable
    prisonId: String,
    @Schema(description = "NOMIS room Id", example = "LW01", required = true)
    @PathVariable
    nomisRoomDescription: String,
  ): Mono<RoomMappingDto> = mappingService.getRoomMapping(prisonId, nomisRoomDescription)
}
