package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Temporary backwards compatible endpoints for clients still using the old paths.
 * TODO remove once all clients use /mapping/core-person/address-contact/replace
 */
@Deprecated("Use CorePersonAddressContactRepairResource on /mapping/core-person/address-contact")
@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/core-person", produces = [MediaType.APPLICATION_JSON_VALUE])
class CorePersonAddressContactRepairLegacyResource(
  private val resource: CorePersonAddressContactRepairResource,
) {
  @PostMapping("/replace")
  @Operation(
    summary = "Deprecated: use POST /mapping/core-person/address-contact/replace",
    deprecated = true,
  )
  suspend fun replaceCorePersonAddressContactMappingsLegacy(
    @RequestBody @Valid
    mappings: CorePersonAddressContactMappingsDto,
  ) = resource.replaceCorePersonAddressContactMappings(mappings)
}
