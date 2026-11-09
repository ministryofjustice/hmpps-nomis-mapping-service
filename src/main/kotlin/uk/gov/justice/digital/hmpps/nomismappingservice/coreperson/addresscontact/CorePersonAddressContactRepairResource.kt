package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/core-person", produces = [MediaType.APPLICATION_JSON_VALUE])
class CorePersonAddressContactRepairResource(
  private val corePersonAddressContactService: CorePersonAddressContactService,
) {
  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping("/replace")
  @Operation(
    summary = "Replaces addresses and contacts mappings.",
    description = """Creates lists of addresses and contacts mappings and removes the previous supplied set.
      This does not add or remove any data from any associated parent CorePersonMappingDto entry. 
      Requires ROLE_NOMIS_MAPPING_API__SYNCHRONISATION__RW""",
    responses = [
      ApiResponse(responseCode = "200", description = "Mappings replaced"),
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
  suspend fun replaceCorePersonAddressContactMappings(
    @RequestBody @Valid
    mappings: CorePersonAddressContactMappingsDto,
  ) = corePersonAddressContactService.replaceMappings(mappings).also {
    log.info("Replaced mappings for nomis prisoner number ${mappings.personMapping.nomisPrisonNumber}")
  }
}
