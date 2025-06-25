package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing.CourtSentencingMappingService
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.LocationMappingService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/api", produces = [MediaType.APPLICATION_JSON_VALUE])
class PublicApiResource(private val locationService: LocationMappingService, private val sentenceService: CourtSentencingMappingService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_DPS_MAPPING__LOCATIONS__R')")
  @GetMapping("/locations/nomis/{nomisLocationId}")
  @Tag(name = "NOMIS / DPS Mapping lookup")
  @Operation(
    summary = "Retrieves the DPS location id from the NOMIS internal location id",
    description = "Requires role <b>NOMIS_DPS_MAPPING__LOCATIONS__R</b>",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "NOMIS to DPS Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = NomisDpsLocationMapping::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Access forbidden to this endpoint. Requires role NOMIS_DPS_MAPPING__LOCATIONS__R",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Location id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getLocationMappingByNomisId(
    @Schema(description = "NOMIS internal location id", example = "2318905", required = true)
    @PathVariable
    nomisLocationId: Long,
  ): NomisDpsLocationMapping = locationService.getMappingByNomisId(nomisLocationId)
    .let { NomisDpsLocationMapping(dpsLocationId = it.dpsLocationId, nomisLocationId = it.nomisLocationId) }

  @PreAuthorize("hasRole('ROLE_NOMIS_DPS_MAPPING__LOCATIONS__R')")
  @PostMapping("/locations/nomis")
  @Tag(name = "NOMIS / DPS Mapping lookup")
  @Operation(
    summary = "Retrieves all the DPS location ids from the supplied NOMIS internal location ids",
    description = "Requires role <b>NOMIS_DPS_MAPPING__LOCATIONS__R</b>",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          array = ArraySchema(schema = Schema(implementation = Long::class)),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "NOMIS to DPS Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = NomisDpsLocationMapping::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request is invalid, see response for details",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Access forbidden to this endpoint. Requires role NOMIS_DPS_MAPPING__LOCATIONS__R",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getAllLocationMappingsByNomisIds(
    @RequestBody nomisLocationIds: List<Long>,
  ): Flow<NomisDpsLocationMapping> = locationService.getAllMappingsByNomisIds(nomisLocationIds)
    .map { NomisDpsLocationMapping(dpsLocationId = it.dpsLocationId, nomisLocationId = it.nomisLocationId) }

  @PreAuthorize("hasRole('ROLE_NOMIS_DPS_MAPPING__LOCATIONS__R')")
  @GetMapping("/locations/dps/{dpsLocationId}")
  @Tag(name = "NOMIS / DPS Mapping lookup")
  @Operation(
    summary = "Retrieves the NOMIS location id from the DPS location id",
    description = "Requires role <b>NOMIS_DPS_MAPPING__LOCATIONS__R</b>",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "DPS to NOMIS Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = NomisDpsLocationMapping::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Access forbidden to this endpoint. Requires role NOMIS_DPS_MAPPING__LOCATIONS__R",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Location id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getLocationMappingByDpsId(
    @Schema(description = "DPS location UUID", example = "12345678-1234-5678-abcd-1234567890ab", required = true)
    @PathVariable
    dpsLocationId: String,
  ): NomisDpsLocationMapping = locationService.getMappingByDpsId(dpsLocationId)
    .let { NomisDpsLocationMapping(dpsLocationId = it.dpsLocationId, nomisLocationId = it.nomisLocationId) }

  @PreAuthorize("hasRole('ROLE_NOMIS_DPS_MAPPING__LOCATIONS__R')")
  @PostMapping("/locations/dps")
  @Tag(name = "NOMIS / DPS Mapping lookup")
  @Operation(
    summary = "Retrieves all the NOMIS location ids from the supplied DPS location ids",
    description = "Requires role <b>NOMIS_DPS_MAPPING__LOCATIONS__R</b>",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          array = ArraySchema(schema = Schema(implementation = String::class)),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "DPS to NOMIS Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = NomisDpsLocationMapping::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request is invalid, see response for details",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Access forbidden to this endpoint. Requires role NOMIS_DPS_MAPPING__LOCATIONS__R",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getAllLocationMappingsByDpsIds(
    @RequestBody dpsLocationIds: List<String>,
  ): Flow<NomisDpsLocationMapping> = locationService.getAllMappingsByDpsIds(dpsLocationIds)
    .map { NomisDpsLocationMapping(dpsLocationId = it.dpsLocationId, nomisLocationId = it.nomisLocationId) }

  @PreAuthorize("hasRole('ROLE_NOMIS_DPS_MAPPING__SENTENCE__R')")
  @GetMapping("/sentences/nomis/booking-id/{nomisBookingId}/sentence-sequence/{nomisSentenceSequence}")
  @Tag(name = "NOMIS / DPS Mapping lookup")
  @Operation(
    summary = "Retrieves the DPS Sentence id from the NOMIS booking and sentence sequence",
    description = "Requires role <b>NOMIS_DPS_MAPPING__SENTENCE__R</b>",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "NOMIS to DPS Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = NomisDpsSentenceMapping::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Access forbidden to this endpoint. Requires role NOMIS_DPS_MAPPING__SENTENCE__R",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Sentence id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getSentenceMappingByNomisId(
    @Schema(description = "NOMIS booking id", required = true, example = "123456")
    @PathVariable
    nomisBookingId: Long,
    @Schema(description = "NOMIS sentence sequence", required = true, example = "4")
    @PathVariable
    nomisSentenceSequence: Int,
  ): NomisDpsSentenceMapping = sentenceService.getSentenceAllMappingByNomisId(
    nomisBookingId = nomisBookingId,
    nomisSentenceSeq = nomisSentenceSequence,
  )
    .let {
      NomisDpsSentenceMapping(
        nomisSentenceId = NomisSentenceId(
          nomisBookingId = it.nomisBookingId,
          nomisSentenceSequence = it.nomisSentenceSequence,
        ),
        dpsSentenceId = it.dpsSentenceId,
      )
    }

  @PreAuthorize("hasRole('ROLE_NOMIS_DPS_MAPPING__SENTENCE__R')")
  @PostMapping("/sentences/nomis")
  @Tag(name = "NOMIS / DPS Mapping lookup")
  @Operation(
    summary = "Retrieves list of the DPS sentence ids from the supplied NOMIS sentence ids",
    description = "Requires role <b>NOMIS_DPS_MAPPING__SENTENCE__R</b>",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          array = ArraySchema(schema = Schema(implementation = NomisSentenceId::class)),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "List of NOMIS to DPS Mappings Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = NomisDpsSentenceMapping::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request is invalid, see response for details",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Access forbidden to this endpoint. Requires role NOMIS_DPS_MAPPING__SENTENCE__R",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getSentenceMappingByNomisIs(
    @RequestBody nomisSentenceIds: List<NomisSentenceId>,
  ): Flow<NomisDpsSentenceMapping> = sentenceService.getSentencesByNomisIds(nomisSentenceIds)
    .map {
      NomisDpsSentenceMapping(
        nomisSentenceId = NomisSentenceId(
          nomisBookingId = it.nomisBookingId,
          nomisSentenceSequence = it.nomisSentenceSequence,
        ),
        dpsSentenceId = it.dpsSentenceId,
      )
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS DPS Location mapping")
data class NomisDpsLocationMapping(

  @Schema(description = "Location id in DPS Locations Service", example = "f4499772-2e43-4951-861d-04ad86df43fc")
  val dpsLocationId: String,

  @Schema(description = "Internal Location id in NOMIS", example = "2318905")
  val nomisLocationId: Long,
)

@Schema(description = "NOMIS Sentence ID")
data class NomisSentenceId(
  @Schema(description = "NOMIS booking id", required = true, example = "123456")
  val nomisBookingId: Long,
  @Schema(description = "NOMIS sentence sequence", required = true, example = "4")
  val nomisSentenceSequence: Int,
)

@Schema(description = "NOMIS DPS Sentence mapping")
data class NomisDpsSentenceMapping(
  @Schema(description = "NOMIS senetence key")
  val nomisSentenceId: NomisSentenceId,

  @Schema(description = "DPS sentence id", example = "f4499772-2e43-4951-861d-04ad86df43fc\"")
  val dpsSentenceId: String,
)
