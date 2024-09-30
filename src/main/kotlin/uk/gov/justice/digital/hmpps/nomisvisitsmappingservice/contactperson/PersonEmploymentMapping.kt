package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import org.springframework.data.annotation.Id
import java.time.LocalDateTime

class PersonEmploymentMapping(
  @Id
  val dpsId: String,
  val nomisPersonId: Long,
  val nomisSequenceNumber: Long,
  label: String? = null,
  mappingType: ContactPersonMappingType,
  whenCreated: LocalDateTime? = null,
) : AbstractContactPersonMapping(label = label, mappingType = mappingType, whenCreated = whenCreated) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PersonEmploymentMapping) return false
    if (dpsId != other.dpsId) return false
    return true
  }

  override fun hashCode(): Int = dpsId.hashCode()
  override fun getId(): String = dpsId
}
