package uk.gov.justice.digital.hmpps.nomismappingservice.contactperson.profiledetails

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mappings for a Contact Person Profile Details Migration")
class ContactPersonProfileDetailsMigrationMappingDto(
  @Schema(description = "NOMIS prisoner number", example = "G1234AA")
  val prisonerNumber: String,
  @Schema(description = "Migration Id", example = "2022-03-23T12:00:00")
  val migrationId: String,
  @Schema(description = "Domestic status DPS ids created", example = "1,2,3")
  val domesticStatusDpsIds: String,
  @Schema(description = "Number of children DPS ids created", example = "4,5,6")
  val numberOfChildrenDpsIds: String,
  @Schema(description = "Date time the mapping was created")
  val whenCreated: LocalDateTime? = null,
)
