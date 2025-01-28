package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.coreperson

import org.springframework.data.annotation.Id
import java.time.LocalDateTime

class CorePersonPhoneMapping(
  nomisPrisonNumber: String,
  @Id
  val nomisId: Long,
  val cprId: String,
  val cprPhoneType: CprPhoneType,
  label: String? = null,
  mappingType: CorePersonMappingType,
  whenCreated: LocalDateTime? = null,
) : AbstractCorePersonMappingTyped<Long>(nomisPrisonNumber = nomisPrisonNumber, label = label, mappingType = mappingType, whenCreated = whenCreated) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CorePersonPhoneMapping) return false
    if (nomisId != other.nomisId) return false
    return true
  }

  override fun hashCode(): Int = nomisId.hashCode()
  override fun getId(): Long = nomisId
}

enum class CprPhoneType {
  ADDRESS,
  CORE_PERSON,
}
