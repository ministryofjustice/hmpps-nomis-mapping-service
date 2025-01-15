package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.corporate

import org.springframework.data.annotation.Id
import java.time.LocalDateTime

class CorporateAddressMapping(
  @Id
  val dpsId: String,
  val nomisId: Long,
  label: String? = null,
  mappingType: CorporateMappingType,
  whenCreated: LocalDateTime? = null,
) : AbstractCorporateMapping(label = label, mappingType = mappingType, whenCreated = whenCreated) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CorporateAddressMapping) return false
    if (dpsId != other.dpsId) return false
    return true
  }

  override fun hashCode(): Int = dpsId.hashCode()
  override fun getId(): String = dpsId
}
