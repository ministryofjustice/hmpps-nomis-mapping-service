package uk.gov.justice.digital.hmpps.nomismappingservice.contactperson

import org.springframework.data.annotation.Id
import java.time.LocalDateTime

class PersonAddressMapping(
  @Id
  val dpsId: String,
  val nomisId: Long,
  label: String? = null,
  mappingType: ContactPersonMappingType,
  whenCreated: LocalDateTime? = null,
) : AbstractContactPersonMapping(label = label, mappingType = mappingType, whenCreated = whenCreated) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PersonAddressMapping) return false
    if (dpsId != other.dpsId) return false
    return true
  }

  override fun hashCode(): Int = dpsId.hashCode()
  override fun getId(): String = dpsId
}
