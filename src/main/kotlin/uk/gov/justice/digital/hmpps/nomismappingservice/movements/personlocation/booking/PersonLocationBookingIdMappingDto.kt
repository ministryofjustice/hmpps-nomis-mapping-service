package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.booking

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.PersonLocationMappingType
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for a single person location booking (DPS custodial series)")
data class PersonLocationBookingIdMappingDto(
  @Schema(description = "The NOMIS ID of the booking", example = "12345")
  val nomisBookingId: Long,
  @Schema(description = "The DPS custodial series ID")
  val dpsCustodialSeriesId: UUID,
  @Schema(description = "The source of the mapping", example = "NOMIS_CREATED")
  val mappingType: PersonLocationMappingType,
)
