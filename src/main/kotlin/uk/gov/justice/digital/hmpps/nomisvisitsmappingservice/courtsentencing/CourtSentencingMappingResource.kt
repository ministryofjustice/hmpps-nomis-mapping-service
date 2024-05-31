package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingErrorResponse
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Validated
@PreAuthorize("hasRole('NOMIS_COURT_SENTENCING')")
@RequestMapping("/mapping/court-sentencing", produces = [MediaType.APPLICATION_JSON_VALUE])
class CourtSentencingMappingResource(private val mappingService: CourtSentencingMappingService) {
  @GetMapping("/court-cases/dps-court-case-id/{courtCaseId}")
  @Operation(
    summary = "get court case mapping",
    description = "Retrieves a mapping by DPS id. Requires role NOMIS_COURT_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CourtCaseMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getMappingByNomisId(
    @Schema(description = "DPS court case id", example = "D123", required = true)
    @PathVariable
    courtCaseId: String,
  ): CourtCaseMappingDto = mappingService.getCourtCaseMappingByDpsId(
    courtCaseId = courtCaseId,
  )

  @PostMapping("/court-cases")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new court case hierarchical mapping",
    description = "Creates a mapping between nomis Court Case ID and DPS Court Case ID. Also maps child entities: Court appearances and charges. Requires ROLE_NOMIS_COURT_SENTENCING",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CourtCaseAllMappingDto::class),
        ),
      ],
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
  suspend fun createMapping(
    @RequestBody @Valid
    mapping: CourtCaseAllMappingDto,
  ) =
    try {
      mappingService.createMapping(mapping)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Court Case mapping already exists",
        duplicate = mapping,
        existing = getExistingMappingSimilarTo(mapping),
        cause = e,
      )
    }

  @DeleteMapping("/court-cases/dps-court-case-id/{dpsCourtCaseId}")
  @Operation(
    summary = "Deletes court case mapping",
    description = "Deletes a court case mapping by DPS id. Requires role NOMIS_COURT_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Mapping Deleted",
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
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend fun deleteMappingByDpsId(
    @Schema(description = "DPS court case id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsCourtCaseId: String,
  ) = mappingService.deleteCourtCaseMappingByDpsId(courtCaseId = dpsCourtCaseId)

  @DeleteMapping("/court-cases/nomis-court-case-id/{nomisCourtCaseId}")
  @Operation(
    summary = "Deletes court case mapping",
    description = "Deletes a court case mapping by NOMIS id. Requires role NOMIS_COURT_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Mapping Deleted",
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
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend fun deleteMappingByNomisId(
    @Schema(description = "NOMIS court case id", example = "33", required = true)
    @PathVariable
    nomisCourtCaseId: Long,
  ) = mappingService.deleteCourtCaseMappingByNomisId(courtCaseId = nomisCourtCaseId)

  @GetMapping("/court-appearances/dps-court-appearance-id/{courtAppearanceId}")
  @Operation(
    summary = "get court appearance mapping",
    description = "Retrieves a mapping by DPS id. Requires role NOMIS_COURT_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CourtAppearanceAllMappingDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getCourtAppearanceMappingByDpsId(
    @Schema(description = "DPS court appearance id", example = "D123", required = true)
    @PathVariable
    courtAppearanceId: String,
  ): CourtAppearanceMappingDto = mappingService.getCourtAppearanceMappingByDpsId(
    courtAppearanceId = courtAppearanceId,
  )

  @GetMapping("/court-appearances/nomis-court-appearance-id/{courtAppearanceId}")
  @Operation(
    summary = "get court appearance mapping",
    description = "Retrieves a mapping by NOMIS id. Requires role NOMIS_COURT_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CourtAppearanceAllMappingDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getCourtAppearanceMappingByNomisId(
    @Schema(description = "NOMIS court appearance id", example = "123", required = true)
    @PathVariable
    courtAppearanceId: Long,
  ): CourtAppearanceMappingDto = mappingService.getCourtAppearanceMappingByNomisId(
    courtAppearanceId = courtAppearanceId,
  )

  @DeleteMapping("/court-appearances/dps-court-appearance-id/{dpsCourtAppearanceId}")
  @Operation(
    summary = "Deletes court appearances mapping",
    description = "Deletes a court appearances mapping by DPS id. Requires role NOMIS_COURT_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Mapping Deleted",
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
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend fun deleteCourtAppearanceMappingByDpsId(
    @Schema(description = "DPS court appearances id", example = "edcd118c-41ba-42ea-b5c4-404b453ad58b", required = true)
    @PathVariable
    dpsCourtAppearanceId: String,
  ) = mappingService.deleteCourtAppearanceMappingByDpsId(courtAppearanceId = dpsCourtAppearanceId)

  @DeleteMapping("/court-appearances/nomis-court-appearance-id/{nomisCourtAppearanceId}")
  @Operation(
    summary = "Deletes court appearance mapping",
    description = "Deletes a court appearance mapping by NOMIS id. Requires role NOMIS_COURT_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Mapping Deleted",
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
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend fun deleteCourtAppearanceMappingByNomisId(
    @Schema(description = "NOMIS court appearance id", example = "33", required = true)
    @PathVariable
    nomisCourtAppearanceId: Long,
  ) = mappingService.deleteCourtAppearanceMappingByNomisId(courtAppearanceId = nomisCourtAppearanceId)

  @GetMapping("/court-charges/dps-court-charge-id/{courtChargeId}")
  @Operation(
    summary = "get court charge mapping",
    description = "Retrieves a mapping by DPS id. Requires role NOMIS_COURT_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CourtAppearanceAllMappingDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getCourtChargeMappingByDpsId(
    @Schema(description = "DPS court charge id", example = "D123", required = true)
    @PathVariable
    courtChargeId: String,
  ): CourtChargeMappingDto = mappingService.getCourtChargeMappingByDpsId(
    courtChargeId = courtChargeId,
  )

  @GetMapping("/court-charges/nomis-court-charge-id/{courtChargeId}")
  @Operation(
    summary = "get court charge mapping",
    description = "Retrieves a mapping by NOMIS id. Requires role NOMIS_COURT_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CourtAppearanceAllMappingDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getCourtChargeMappingByNomisId(
    @Schema(description = "NOMIS court charge id", example = "123", required = true)
    @PathVariable
    courtChargeId: Long,
  ): CourtChargeMappingDto = mappingService.getCourtChargeMappingByNomisId(
    courtChargeId = courtChargeId,
  )

  @PostMapping("/court-appearances")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new court appearance mapping",
    description = "Creates a mapping between nomis Court appearance ID and DPS Court appearance ID. Requires ROLE_NOMIS_COURT_SENTENCING",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CourtAppearanceAllMappingDto::class),
        ),
      ],
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
  suspend fun createCourtAppearanceMapping(
    @RequestBody @Valid
    mapping: CourtAppearanceAllMappingDto,
  ) =
    try {
      mappingService.createCourtAppearanceAllMapping(mapping)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Court Case mapping already exists",
        duplicate = mapping,
        existing = getExistingCourtAppearanceMappingSimilarTo(mapping),
        cause = e,
      )
    }

  @PutMapping("/court-charges")
  @Operation(
    summary = "Creates a new set of court charge mapping and deletes ones no longer required",
    description = "Creates a record of a DPS court charge id and NOMIS court charge id. The ones that require deleting are removed by NOMIS id. Requires NOMIS_COURT_SENTENCING",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CourtChargeBatchUpdateMappingDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Mapping entries created"),
      ApiResponse(
        responseCode = "409",
        description = "One of the court charge mappings already exist",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun courtChargeBatchUpdateMappings(
    @RequestBody @Valid
    updateMappingRequest: CourtChargeBatchUpdateMappingDto,
  ) =
    try {
      mappingService.createAndDeleteCourtChargeMappings(updateMappingRequest)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Court Charge mapping already exists, detected by $e",
        duplicate = updateMappingRequest,
        cause = e,
      )
    }

  @PostMapping("/court-charges")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new single charge mapping",
    description = "Creates a mapping between nomis offender charge ID and DPS charge ID. Requires ROLE_NOMIS_COURT_SENTENCING",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CourtChargeMappingDto::class),
        ),
      ],
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
  suspend fun createCourtChargeMapping(
    @RequestBody @Valid
    mapping: CourtChargeMappingDto,
  ) =
    try {
      mappingService.createCourtChargeMapping(mapping)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Court charge mapping already exists",
        duplicate = mapping,
        existing = getExistingChargeMappingSimilarTo(mapping),
        cause = e,
      )
    }

  @DeleteMapping("/court-charges/nomis-court-charge-id/{nomisCourtChargeId}")
  @Operation(
    summary = "Deletes court charge mapping",
    description = "Deletes a court charge mapping by NOMIS id. Requires role NOMIS_COURT_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Mapping Deleted",
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
  @ResponseStatus(HttpStatus.NO_CONTENT)
  suspend fun deleteCourtChargeMappingByNomisId(
    @Schema(description = "NOMIS court charge id", example = "33", required = true)
    @PathVariable
    nomisCourtChargeId: Long,
  ) = mappingService.deleteCourtChargeMappingByNomisId(courtChargeId = nomisCourtChargeId)

  @GetMapping("/sentences/dps-sentence-id/{sentenceId}")
  @Operation(
    summary = "get sentence mapping",
    description = "Retrieves a mapping by DPS id. Requires role NOMIS_COURT_SENTENCING",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Mapping Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = SentenceAllMappingDto::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Id does not exist in mapping table",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getSentenceMappingByNomisId(
    @Schema(description = "DPS sentence id", example = "D123", required = true)
    @PathVariable
    sentenceId: String,
  ): SentenceAllMappingDto = mappingService.getSentenceAllMappingByDpsId(
    dpsSentenceId = sentenceId,
  )

  @PostMapping("/sentences")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new sentence hierarchical mapping",
    description = "Creates a mapping between nomis sentence ID (booking id and sentence seq) and DPS Sentence ID. Also maps child charge entities. Requires ROLE_NOMIS_COURT_SENTENCING",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = SentenceAllMappingDto::class),
        ),
      ],
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
  suspend fun createSentenceMapping(
    @RequestBody @Valid
    mapping: SentenceAllMappingDto,
  ) =
    try {
      mappingService.createSentenceAllMapping(mapping)
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Sentence mapping already exists",
        duplicate = mapping,
        existing = getExistingSentenceAllMappingSimilarTo(mapping),
        cause = e,
      )
    }

  private suspend fun getExistingMappingSimilarTo(mapping: CourtCaseAllMappingDto) = runCatching {
    mappingService.getCourtCaseMappingByNomisId(
      courtCaseId = mapping.nomisCourtCaseId,
    )
  }.getOrElse {
    mappingService.getCourtCaseMappingByDpsId(
      courtCaseId = mapping.dpsCourtCaseId,
    )
  }

  private suspend fun getExistingCourtAppearanceMappingSimilarTo(mapping: CourtAppearanceAllMappingDto) = runCatching {
    mappingService.getCourtAppearanceMappingByNomisId(
      courtAppearanceId = mapping.nomisCourtAppearanceId,
    )
  }.getOrElse {
    mappingService.getCourtAppearanceMappingByDpsId(
      courtAppearanceId = mapping.dpsCourtAppearanceId,
    )
  }

  private suspend fun getExistingChargeMappingSimilarTo(mapping: CourtChargeMappingDto) = runCatching {
    mappingService.getCourtChargeMappingByNomisId(
      courtChargeId = mapping.nomisCourtChargeId,
    )
  }.getOrElse {
    mappingService.getCourtChargeMappingByDpsId(
      courtChargeId = mapping.dpsCourtChargeId,
    )
  }

  private suspend fun getExistingSentenceAllMappingSimilarTo(mapping: SentenceAllMappingDto) = runCatching {
    mappingService.getSentenceAllMappingByNomisId(
      nomisBookingId = mapping.nomisBookingId,
      nomisSentenceSeq = mapping.nomisSentenceSequence,
    )
  }.getOrElse {
    mappingService.getSentenceAllMappingByDpsId(
      dpsSentenceId = mapping.dpsSentenceId,
    )
  }
}
