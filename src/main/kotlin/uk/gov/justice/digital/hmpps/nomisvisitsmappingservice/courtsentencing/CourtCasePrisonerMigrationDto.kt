package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Record of Offender Court case migration")
data class CourtCasePrisonerMigrationDto(

  @Schema(description = "The prisoner number for the set of mappings")
  val offenderNo: String,

  @Schema(description = "Count of the number of court case mappings migrated")
  val mappingsCount: Int,

  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime?,

  @Schema(description = "the migration the offender was migrated in")
  val migrationId: String?,
)
