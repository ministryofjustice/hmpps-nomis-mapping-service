package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.LocationMappingService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/api", produces = [MediaType.APPLICATION_JSON_VALUE])
class PublicApiResource(private val locationService: LocationMappingService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_DPS_MAPPING__LOCATIONS__R')")
  @GetMapping("/locations/nomis/{nomisLocationId}")
  @Operation(
    summary = "Retrieves the DPS location id from the NOMIS internal location id",
    description = "Requires role NOMIS_DPS_MAPPING__LOCATIONS__R",
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
    @Schema(description = "Nomis location id", example = "12345678", required = true)
    @PathVariable
    nomisLocationId: Long,
  ): NomisDpsLocationMapping = locationService.getMappingByNomisId(nomisLocationId)
    .let { NomisDpsLocationMapping(dpsLocationId = it.dpsLocationId, nomisLocationId = it.nomisLocationId) }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "NOMIS DPS Location mapping")
data class NomisDpsLocationMapping(

  @Schema(description = "Location id in DPS", required = true)
  val dpsLocationId: String,

  @Schema(description = "Location id in Nomis", required = true)
  val nomisLocationId: Long,
)
