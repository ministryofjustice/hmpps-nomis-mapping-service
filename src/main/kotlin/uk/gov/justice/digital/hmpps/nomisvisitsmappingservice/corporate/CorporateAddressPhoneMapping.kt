package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.corporate

import java.time.LocalDateTime

class CorporateAddressPhoneMapping(
  dpsId: String,
  nomisId: Long,
  label: String? = null,
  mappingType: CorporateMappingType,
  whenCreated: LocalDateTime? = null,
) : AbstractCorporateMapping(dpsId = dpsId, nomisId = nomisId, label = label, mappingType = mappingType, whenCreated = whenCreated) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CorporateAddressPhoneMapping) return false
    if (dpsId != other.dpsId) return false
    return true
  }

  override fun hashCode(): Int = dpsId.hashCode()
  override fun getId(): String = dpsId
}
