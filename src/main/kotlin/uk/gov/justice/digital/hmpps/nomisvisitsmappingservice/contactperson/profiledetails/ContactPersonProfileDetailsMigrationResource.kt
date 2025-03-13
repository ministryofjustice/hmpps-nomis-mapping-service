package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson.profiledetails

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson.ContactPersonMappingsDto
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_CONTACTPERSONS')")
@RequestMapping("/mapping/contact-person/profile-details", produces = [MediaType.APPLICATION_JSON_VALUE])
class ContactPersonProfileDetailsMigrationResource(
  private val service: ContactPersonProfileDetailMigrationService,
) {

  @PutMapping("/migration")
  @Operation(
    summary = "Creates or updates a contact person profile details migration mapping for a migration",
    description = "Creates updates a contact person profile details migration mapping for a migration between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ContactPersonMappingsDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "200", description = "Mappings created or updated"),
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
  suspend fun createMappings(
    @Schema(description = "Migration request details")
    @RequestBody
    @Valid mappingRequest: ContactPersonProfileDetailsMigrationMappingRequest,
  ) = service.upsert(mappingRequest)
}
