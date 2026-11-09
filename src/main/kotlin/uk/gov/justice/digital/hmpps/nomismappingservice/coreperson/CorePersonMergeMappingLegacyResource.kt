package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressContactMappingsByNomisIdsRequest
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressContactMappingsResult

/**
 * Temporary backwards compatible endpoints for clients still using the old paths.
 * TODO remove once all clients use /mapping/core-person/merge/mappings/by-nomis-ids
 */
@Deprecated("Use CorePersonMergeMappingResource on /mapping/core-person/merge/mappings/by-nomis-ids")
@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/core-person", produces = [MediaType.APPLICATION_JSON_VALUE])
class CorePersonMergeMappingLegacyResource(
  private val resource: CorePersonMergeMappingResource,
) {
  @PostMapping("/mappings/by-nomis-ids")
  @Operation(
    summary = "Deprecated: use POST /mapping/core-person/merge/mappings/by-nomis-ids",
    deprecated = true,
  )
  suspend fun getMappingsFromNomisIdsLegacy(
    @RequestBody mappingRequest: CorePersonAddressContactMappingsByNomisIdsRequest,
  ): CorePersonAddressContactMappingsResult = resource.getMappingsFromNomisIds(mappingRequest)
}
