package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingDto

/**
 * Temporary backwards compatible endpoints for clients still using the old paths.
 * TODO remove once all clients use /mapping/core-person/address-contact-migration
 */
@Deprecated("Use CorePersonMigrationAddressContactResource on /mapping/core-person/address-contact-migration")
@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_MAPPING_API__SYNCHRONISATION__RW')")
@RequestMapping("/mapping/core-person", produces = [MediaType.APPLICATION_JSON_VALUE])
class CorePersonMigrationAddressContactLegacyResource(
  private val resource: CorePersonMigrationAddressContactResource,
) {
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Deprecated: use POST /mapping/core-person/address-contact-migration",
    deprecated = true,
  )
  suspend fun createCorePersonAddressContactMappingsLegacy(
    @RequestBody @Valid
    mappings: CorePersonAddressContactMappingsDto,
  ) = resource.createCorePersonAddressContactMappings(mappings)

  @GetMapping("/migration-id/{migrationId}")
  @Operation(
    summary = "Deprecated: use GET /mapping/core-person/address-contact-migration/migration-id/{migrationId}",
    deprecated = true,
  )
  suspend fun getAddressContactMappingsByMigrationIdLegacy(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<CorePersonMappingDto> = resource.getAddressContactMappingsByMigrationId(pageRequest, migrationId)

  @GetMapping("/person/nomis-prison-number/{nomisPrisonNumber}")
  @Operation(
    summary = "Deprecated: use GET /mapping/core-person/address-contact-migration/person/nomis-prison-number/{nomisPrisonNumber}",
    deprecated = true,
  )
  suspend fun getAddressContactMappingByNomisPrisonNumberLegacy(
    @Schema(description = "NOMIS prison Number aka offender no.", example = "A1234BC", required = true)
    @PathVariable
    nomisPrisonNumber: String,
  ): CorePersonMappingDto = resource.getAddressContactMappingByNomisPrisonNumber(nomisPrisonNumber)

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deprecated: use DELETE /mapping/core-person/address-contact-migration",
    deprecated = true,
  )
  suspend fun deleteAllCorePersonAddressContactMappingsLegacy() = resource.deleteAllCorePersonAddressContactMappings()
}
