package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_CONTACTPERSONS')")
@RequestMapping("/mapping/contact-person", produces = [MediaType.APPLICATION_JSON_VALUE])
class ContactPersonMappingResource(private val service: ContactPersonService) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping("/migrate")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a tree of contact person mappings typically for a migration",
    description = "Creates a tree of contact person mappings typically for a migration between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ContactPersonMappingsDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mappings created"),
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
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate mapping has been rejected. If Error code = 1409 the body will return a DuplicateErrorResponse",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateMappingErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun createMappings(
    @RequestBody @Valid
    mappings: ContactPersonMappingsDto,
  ) =
    try {
      service.createMappings(mappings)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingPersonMappingSimilarTo(mappings.personMapping)
      if (existingMapping == null) {
        log.error("Child duplicate key found for person even though the person has never been migrated", e)
      }
      throw DuplicateMappingException(
        messageIn = "Person mapping already exists",
        duplicate = mappings.asPersonMappingDto(),
        existing = existingMapping ?: mappings.asPersonMappingDto(),
        cause = e,
      )
    }

  @GetMapping("/person/nomis-person-id/{nomisPersonId}")
  @Operation(
    summary = "Get person mapping by nomis person Id",
    description = "Retrieves the person a mapping by NOMIS Person Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonMappingDto::class)),
        ],
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
  suspend fun getPersonMappingByNomisId(
    @Schema(description = "NOMIS person id", example = "12345", required = true)
    @PathVariable
    nomisPersonId: Long,
  ): PersonMappingDto = service.getPersonMappingByNomisId(nomisId = nomisPersonId)

  @GetMapping("/person/dps-contact-id/{dpsContactId}")
  @Operation(
    summary = "Get person mapping by dps contact Id",
    description = "Retrieves the person mapping by DPS Contact Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonMappingDto::class)),
        ],
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
  suspend fun getPersonMappingByDpsId(
    @Schema(description = "DPS contact id", example = "12345", required = true)
    @PathVariable
    dpsContactId: String,
  ): PersonMappingDto = service.getPersonMappingByDpsId(dpsId = dpsContactId)

  @GetMapping("/person/migration-id/{migrationId}")
  @Operation(
    summary = "Get paged person mappings by migration id",
    description = "Retrieve all person mappings of type 'MIGRATED' for the given migration id (identifies a single migration run). Results are paged. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person mapping page returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getPersonMappingsByMigrationId(
    @PageableDefault pageRequest: Pageable,
    @Schema(description = "Migration Id", example = "2020-03-24T12:00:00", required = true)
    @PathVariable
    migrationId: String,
  ): Page<PersonMappingDto> =
    service.getPersonMappingsByMigrationId(pageRequest = pageRequest, migrationId = migrationId)

  @GetMapping("/person")
  @Operation(
    summary = "Get paged person mappings by migration id",
    description = "Retrieve all person mappings. Results are paged. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person mapping page returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getAllPersonMappings(
    @PageableDefault pageRequest: Pageable,
  ): Page<PersonMappingDto> =
    service.getAllPersonMappings(pageRequest = pageRequest)

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes all contact person mappings",
    description = "Deletes all contact person mappings regardless of source. This includes person, phone, address, email, employment, identifiers, restrictions, contacts and contact restrictions. This is expected to only ever been used in a non-production environment. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(responseCode = "204", description = "All mappings deleted"),
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
  suspend fun deleteAllMappings() = service.deleteAllMappings()

  @PostMapping("/person")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates person mappings for synchronisation",
    description = "Creates person mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = PersonMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping created"),
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
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate mapping has been rejected. If Error code = 1409 the body will return a DuplicateErrorResponse",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateMappingErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun createPersonMapping(
    @RequestBody @Valid
    mapping: PersonMappingDto,
  ) =
    try {
      service.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingPersonMappingSimilarTo(mapping)
      throw DuplicateMappingException(
        messageIn = "Person mapping already exists",
        duplicate = mapping,
        existing = existingMapping ?: mapping,
        cause = e,
      )
    }

  @GetMapping("/contact/nomis-contact-id/{nomisContactId}")
  @Operation(
    summary = "Get person contact mapping by nomis contact Id",
    description = "Retrieves the person contact mapping by NOMIS Contact Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Contact mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonContactMappingDto::class)),
        ],
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
  suspend fun getPersonContactMappingByNomisId(
    @Schema(description = "NOMIS contact id", example = "12345", required = true)
    @PathVariable
    nomisContactId: Long,
  ): PersonContactMappingDto = service.getPersonContactMappingByNomisId(nomisId = nomisContactId)

  @GetMapping("/contact/dps-prisoner-contact-id/{dpsPrisonerContactId}")
  @Operation(
    summary = "Get person contact mapping by dps prisoner contact Id",
    description = "Retrieves the person contact mapping by DPS Prisoner Contact Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Contact mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonContactMappingDto::class)),
        ],
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
  suspend fun getPersonContactMappingByDpsId(
    @Schema(description = "DPS prisoner contact id", example = "12345", required = true)
    @PathVariable
    dpsPrisonerContactId: String,
  ): PersonContactMappingDto = service.getPersonContactMappingByDpsId(dpsId = dpsPrisonerContactId)

  @PostMapping("/contact")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates person contact mappings for synchronisation",
    description = "Creates person contact mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = PersonContactMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping created"),
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
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate mapping has been rejected. If Error code = 1409 the body will return a DuplicateErrorResponse",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateMappingErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun createPersonContactMapping(
    @RequestBody @Valid
    mapping: PersonContactMappingDto,
  ) =
    try {
      service.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingPersonContactMappingSimilarTo(mapping)
      throw DuplicateMappingException(
        messageIn = "Person contact mapping already exists",
        duplicate = mapping,
        existing = existingMapping ?: mapping,
        cause = e,
      )
    }

  @GetMapping("/address/nomis-address-id/{nomisAddressId}")
  @Operation(
    summary = "Get person address mapping by nomis contact Id",
    description = "Retrieves the person address mapping by NOMIS Address Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person address mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonAddressMappingDto::class)),
        ],
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
  suspend fun getPersonAddressMappingByNomisId(
    @Schema(description = "NOMIS address id", example = "12345", required = true)
    @PathVariable
    nomisAddressId: Long,
  ): PersonAddressMappingDto = service.getPersonAddressMappingByNomisId(nomisId = nomisAddressId)

  @GetMapping("/address/dps-contact-address-id/{dpsContactAddressId}")
  @Operation(
    summary = "Get person address mapping by dps contact address Id",
    description = "Retrieves the person address mapping by DPS Contact Address Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Address mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonAddressMappingDto::class)),
        ],
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
  suspend fun getPersonAddressMappingByDpsId(
    @Schema(description = "DPS contact address id", example = "12345", required = true)
    @PathVariable
    dpsContactAddressId: String,
  ): PersonAddressMappingDto = service.getPersonAddressMappingByDpsId(dpsId = dpsContactAddressId)

  @PostMapping("/address")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates person address mappings for synchronisation",
    description = "Creates person address mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = PersonAddressMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping created"),
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
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate mapping has been rejected. If Error code = 1409 the body will return a DuplicateErrorResponse",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateMappingErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun createPersonAddressMapping(
    @RequestBody @Valid
    mapping: PersonAddressMappingDto,
  ) =
    try {
      service.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingPersonAddressMappingSimilarTo(mapping)
      throw DuplicateMappingException(
        messageIn = "Person address mapping already exists",
        duplicate = mapping,
        existing = existingMapping ?: mapping,
        cause = e,
      )
    }

  @GetMapping("/phone/nomis-phone-id/{nomisPhoneId}")
  @Operation(
    summary = "Get person phone mapping by nomis phone Id",
    description = "Retrieves the person phone mapping by NOMIS Phone Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person phone mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonPhoneMappingDto::class)),
        ],
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
  suspend fun getPersonPhoneMappingByNomisId(
    @Schema(description = "NOMIS phone id", example = "12345", required = true)
    @PathVariable
    nomisPhoneId: Long,
  ): PersonPhoneMappingDto = service.getPersonPhoneMappingByNomisId(nomisId = nomisPhoneId)

  @GetMapping("/phone/dps-contact-phone-id/{dpsContactPhoneId}")
  @Operation(
    summary = "Get person phone mapping by dps contact phone Id",
    description = "Retrieves the person phone mapping by DPS Contact Phone Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Phone mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonPhoneMappingDto::class)),
        ],
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
  suspend fun getPersonPhoneMappingByDpsId(
    @Schema(description = "DPS contact phone id", example = "12345", required = true)
    @PathVariable
    dpsContactPhoneId: String,
  ): PersonPhoneMappingDto = service.getPersonPhoneMappingByDpsId(dpsId = dpsContactPhoneId, dpsPhoneType = DpsPersonPhoneType.PERSON)

  @GetMapping("/phone/dps-contact-address-phone-id/{dpsContactAddressPhoneId}")
  @Operation(
    summary = "Get person mapping by dps contact address phone Id",
    description = "Retrieves the person phone mapping by DPS Contact Address Phone Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Phone mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonPhoneMappingDto::class)),
        ],
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
  suspend fun getPersonAddressPhoneMappingByDpsId(
    @Schema(description = "DPS contact phone id", example = "12345", required = true)
    @PathVariable
    dpsContactAddressPhoneId: String,
  ): PersonPhoneMappingDto = service.getPersonPhoneMappingByDpsId(dpsId = dpsContactAddressPhoneId, dpsPhoneType = DpsPersonPhoneType.ADDRESS)

  @PostMapping("/phone")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates person phone mappings for synchronisation",
    description = "Creates person phone mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = PersonPhoneMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping created"),
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
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate mapping has been rejected. If Error code = 1409 the body will return a DuplicateErrorResponse",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateMappingErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun createPersonPhoneMapping(
    @RequestBody @Valid
    mapping: PersonPhoneMappingDto,
  ) =
    try {
      service.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingPersonPhoneMappingSimilarTo(mapping)
      throw DuplicateMappingException(
        messageIn = "Person phone mapping already exists",
        duplicate = mapping,
        existing = existingMapping ?: mapping,
        cause = e,
      )
    }

  @GetMapping("/email/nomis-internet-address-id/{nomisInternetAddressId}")
  @Operation(
    summary = "Get person email mapping by nomis internet address Id",
    description = "Retrieves the person email mapping by NOMIS Email/Internet Address Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person email mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonEmailMappingDto::class)),
        ],
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
  suspend fun getPersonEmailMappingByNomisId(
    @Schema(description = "NOMIS email id", example = "12345", required = true)
    @PathVariable
    nomisInternetAddressId: Long,
  ): PersonEmailMappingDto = service.getPersonEmailMappingByNomisId(nomisId = nomisInternetAddressId)

  @GetMapping("/email/dps-contact-email-id/{dpsContactEmailId}")
  @Operation(
    summary = "Get contact email mapping by dps contact email Id",
    description = "Retrieves the person email mapping by DPS Contact Email Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Email mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonEmailMappingDto::class)),
        ],
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
  suspend fun getPersonEmailMappingByDpsId(
    @Schema(description = "DPS contact email id", example = "12345", required = true)
    @PathVariable
    dpsContactEmailId: String,
  ): PersonEmailMappingDto = service.getPersonEmailMappingByDpsId(dpsId = dpsContactEmailId)

  @PostMapping("/email")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates person email mappings for synchronisation",
    description = "Creates person email mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = PersonEmailMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping created"),
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
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate mapping has been rejected. If Error code = 1409 the body will return a DuplicateErrorResponse",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateMappingErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun createPersonEmailMapping(
    @RequestBody @Valid
    mapping: PersonEmailMappingDto,
  ) =
    try {
      service.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingPersonEmailMappingSimilarTo(mapping)
      throw DuplicateMappingException(
        messageIn = "Person email mapping already exists",
        duplicate = mapping,
        existing = existingMapping ?: mapping,
        cause = e,
      )
    }

  @GetMapping("/identifier/nomis-person-id/{nomisPersonId}/nomis-sequence-number/{nomisSequenceNumber}")
  @Operation(
    summary = "Get person identifier mapping by nomis person id and sequence",
    description = "Retrieves the person identifier mapping by NOMIS person id and NOMIS sequence number. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person identifier mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonIdentifierMappingDto::class)),
        ],
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
  suspend fun getPersonIdentifierMappingByNomisIds(
    @Schema(description = "NOMIS person id", example = "12345", required = true)
    @PathVariable
    nomisPersonId: Long,
    @Schema(description = "NOMIS identifier sequence", example = "4", required = true)
    @PathVariable
    nomisSequenceNumber: Long,
  ): PersonIdentifierMappingDto = service.getPersonIdentifierMappingByNomisIds(nomisPersonId = nomisPersonId, nomisSequenceNumber = nomisSequenceNumber)

  @GetMapping("/identifier/dps-contact-identifier-id/{dpsContactIdentifierId}")
  @Operation(
    summary = "Get contact identifier mapping by dps contact identifier Id",
    description = "Retrieves the person identifier mapping by DPS Contact Identifier Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Identifier mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonIdentifierMappingDto::class)),
        ],
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
  suspend fun getPersonIdentifierMappingByDpsId(
    @Schema(description = "DPS contact identifier id", example = "12345", required = true)
    @PathVariable
    dpsContactIdentifierId: String,
  ): PersonIdentifierMappingDto = service.getPersonIdentifierMappingByDpsId(dpsId = dpsContactIdentifierId)

  @PostMapping("/identifier")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates person identifier mappings for synchronisation",
    description = "Creates person identifier mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = PersonIdentifierMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping created"),
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
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate mapping has been rejected. If Error code = 1409 the body will return a DuplicateErrorResponse",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateMappingErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun createPersonIdentifierMapping(
    @RequestBody @Valid
    mapping: PersonIdentifierMappingDto,
  ) =
    try {
      service.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingPersonIdentifierMappingSimilarTo(mapping)
      throw DuplicateMappingException(
        messageIn = "Person identifier mapping already exists",
        duplicate = mapping,
        existing = existingMapping ?: mapping,
        cause = e,
      )
    }

  @GetMapping("/contact-restriction/nomis-contact-restriction-id/{nomisContactRestrictionId}")
  @Operation(
    summary = "Get person contact restriction mapping by nomis contact Id",
    description = "Retrieves the person contact restriction mapping by NOMIS ContactRestriction Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person contactRestriction mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonContactRestrictionMappingDto::class)),
        ],
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
  suspend fun getPersonContactRestrictionMappingByNomisId(
    @Schema(description = "NOMIS contact restriction id", example = "12345", required = true)
    @PathVariable
    nomisContactRestrictionId: Long,
  ): PersonContactRestrictionMappingDto = service.getPersonContactRestrictionMappingByNomisId(nomisId = nomisContactRestrictionId)

  @GetMapping("/contact-restriction/dps-prisoner-contact-restriction-id/{dpsPrisonerContactRestrictionId}")
  @Operation(
    summary = "Get person contact restriction mapping by dps prisoner contact restriction Id",
    description = "Retrieves the person contact restriction mapping by DPS prisoner contact restriction Id. Requires role ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Person Contact Restriction mapping data",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PersonContactRestrictionMappingDto::class)),
        ],
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
  suspend fun getPersonContactRestrictionMappingByDpsId(
    @Schema(description = "DPS prisoner contact restriction id", example = "12345", required = true)
    @PathVariable
    dpsPrisonerContactRestrictionId: String,
  ): PersonContactRestrictionMappingDto = service.getPersonContactRestrictionMappingByDpsId(dpsId = dpsPrisonerContactRestrictionId)

  @PostMapping("/contact-restriction")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates person contact restriction mappings for synchronisation",
    description = "Creates person contact restriction mappings for synchronisation between NOMIS ids and dps ids. Requires ROLE_NOMIS_CONTACTPERSONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = PersonContactRestrictionMappingDto::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping created"),
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
      ApiResponse(
        responseCode = "409",
        description = "Indicates a duplicate mapping has been rejected. If Error code = 1409 the body will return a DuplicateErrorResponse",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = DuplicateMappingErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  suspend fun createPersonContactRestrictionMapping(
    @RequestBody @Valid
    mapping: PersonContactRestrictionMappingDto,
  ) =
    try {
      service.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      val existingMapping = getExistingPersonContactRestrictionMappingSimilarTo(mapping)
      throw DuplicateMappingException(
        messageIn = "Person contactRestriction mapping already exists",
        duplicate = mapping,
        existing = existingMapping ?: mapping,
        cause = e,
      )
    }

  private suspend fun getExistingPersonMappingSimilarTo(personMapping: ContactPersonSimpleMappingIdDto) = runCatching {
    service.getPersonMappingByNomisId(
      nomisId = personMapping.nomisId,
    )
  }.getOrElse {
    service.getPersonMappingByDpsIdOrNull(
      dpsId = personMapping.dpsId,
    )
  }
  private suspend fun getExistingPersonMappingSimilarTo(personMapping: PersonMappingDto) = runCatching {
    service.getPersonMappingByNomisId(
      nomisId = personMapping.nomisId,
    )
  }.getOrElse {
    service.getPersonMappingByDpsIdOrNull(
      dpsId = personMapping.dpsId,
    )
  }
  private suspend fun getExistingPersonContactMappingSimilarTo(personMapping: PersonContactMappingDto) = runCatching {
    service.getPersonContactMappingByNomisId(
      nomisId = personMapping.nomisId,
    )
  }.getOrElse {
    service.getPersonContactMappingByDpsIdOrNull(
      dpsId = personMapping.dpsId,
    )
  }

  private suspend fun getExistingPersonAddressMappingSimilarTo(mapping: PersonAddressMappingDto) = runCatching {
    service.getPersonAddressMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getPersonAddressMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }

  private suspend fun getExistingPersonEmailMappingSimilarTo(mapping: PersonEmailMappingDto): PersonEmailMappingDto? = runCatching {
    service.getPersonEmailMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getPersonEmailMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }

  private suspend fun getExistingPersonPhoneMappingSimilarTo(mapping: PersonPhoneMappingDto): PersonPhoneMappingDto? = runCatching {
    service.getPersonPhoneMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getPersonPhoneMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
      dpsPhoneType = mapping.dpsPhoneType,
    )
  }

  private suspend fun getExistingPersonIdentifierMappingSimilarTo(mapping: PersonIdentifierMappingDto): PersonIdentifierMappingDto? = runCatching {
    service.getPersonIdentifierMappingByNomisIds(
      nomisPersonId = mapping.nomisPersonId,
      nomisSequenceNumber = mapping.nomisSequenceNumber,
    )
  }.getOrElse {
    service.getPersonIdentifierMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }

  private suspend fun getExistingPersonContactRestrictionMappingSimilarTo(mapping: PersonContactRestrictionMappingDto) = runCatching {
    service.getPersonContactRestrictionMappingByNomisId(
      nomisId = mapping.nomisId,
    )
  }.getOrElse {
    service.getPersonContactRestrictionMappingByDpsIdOrNull(
      dpsId = mapping.dpsId,
    )
  }
}

private fun ContactPersonMappingsDto.asPersonMappingDto() = PersonMappingDto(
  dpsId = personMapping.dpsId,
  nomisId = personMapping.nomisId,
  mappingType = mappingType,
  label = label,
  whenCreated = whenCreated,
)
