package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import org.springframework.data.annotation.Id
import java.time.LocalDateTime

class PersonPhoneMapping(
  @Id
  val nomisId: Long,
  val dpsId: String,
  val dpsPhoneType: DpsPersonPhoneType,
  label: String? = null,
  mappingType: ContactPersonMappingType,
  whenCreated: LocalDateTime? = null,
) : AbstractContactPersonMappingTyped<Long>(label = label, mappingType = mappingType, whenCreated = whenCreated) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PersonPhoneMapping) return false
    if (nomisId != other.nomisId) return false
    return true
  }

  override fun hashCode(): Int = nomisId.hashCode()
  override fun getId(): Long = nomisId
}

enum class DpsPersonPhoneType {
  ADDRESS,
  PERSON,
}
