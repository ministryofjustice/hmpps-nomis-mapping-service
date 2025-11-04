package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.DayOfWeek
import java.time.LocalDateTime

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/visit-slots", produces = [MediaType.APPLICATION_JSON_VALUE])
class VisitSlotsResource(private val visitSlotsService: VisitSlotsService) {
  @GetMapping("/time-slots/nomis-prison-id/{nomisPrisonId}/nomis-day-of-week/{nomisDayOfWeek}/nomis-slot-sequence/{nomisSlotSequence}")
  @Operation(
    summary = "Get visit time slot mapping by nomis prison id, day of week and sequence",
    description = "Requires role ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping data",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Day of week not valid",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
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
  suspend fun getVisitTimeSlotMappingByNomisIds(
    @Schema(description = "NOMIS prison id", example = "WWI", required = true)
    @PathVariable
    nomisPrisonId: String,
    @Schema(description = "NOMIS day of the week", example = "MONDAY", required = true)
    @PathVariable
    nomisDayOfWeek: DayOfWeek,
    @Schema(description = "NOMIS slot sequence", example = "4", required = true)
    @PathVariable
    nomisSlotSequence: Int,
  ): VisitTimeSlotMappingDto = visitSlotsService.getVisitTimeSlotMappingByNomisId(
    nomisPrisonId = nomisPrisonId,
    nomisDayOfWeek = nomisDayOfWeek,
    nomisSlotSequence = nomisSlotSequence,
  )
}

data class VisitTimeSlotMappingDto(
  val dpsId: String,
  val nomisPrisonId: String,
  val nomisDayOfWeek: DayOfWeek,
  val nomisSlotSequence: Int,
  val label: String?,
  val mappingType: StandardMappingType,
  val whenCreated: LocalDateTime?,
)
