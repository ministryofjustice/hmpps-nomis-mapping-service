package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prison person mapping")
data class PrisonPersonMigrationMappingRequest(

  @Schema(description = "The prisoner number", example = "A1234AA")
  val nomisPrisonerNumber: String,

  @Schema(description = "The type of migration for this person, e.g. which data is being migrated", example = "PHYSICAL_ATTRIBUTES")
  val migrationType: PrisonPersonMigrationType,

  @Schema(description = "A unique reference for the migration, usually a timestamp", example = "2024-06-03T11:18:33")
  val label: String,
)
