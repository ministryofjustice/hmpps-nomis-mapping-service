package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

import org.springframework.data.annotation.Id
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.AbstractMappingTyped
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType
import java.time.LocalDateTime

class VisitTimeSlotMapping(
  @Id
  val dpsId: String,
  val nomisPrisonId: String,
  val nomisDayOfWeek: String,
  val nomisSlotSequence: Int,
  label: String? = null,
  mappingType: StandardMappingType,
  whenCreated: LocalDateTime? = null,
) : AbstractMappingTyped<String>(label = label, mappingType = mappingType, whenCreated = whenCreated) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is VisitTimeSlotMapping) return false
    if (dpsId != other.dpsId) return false
    return true
  }

  override fun hashCode(): Int = dpsId.hashCode()
  override fun getId(): String = dpsId
}
