package uk.gov.justice.digital.hmpps.nomismappingservice.corporate

import java.time.LocalDateTime

class CorporateMapping(
  dpsId: String,
  nomisId: Long,
  label: String? = null,
  mappingType: CorporateMappingType,
  whenCreated: LocalDateTime? = null,
) : AbstractCorporateMapping(dpsId = dpsId, nomisId = nomisId, label = label, mappingType = mappingType, whenCreated = whenCreated) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CorporateMapping) return false
    if (dpsId != other.dpsId) return false
    return true
  }

  override fun hashCode(): Int = dpsId.hashCode()
  override fun getId(): String = dpsId
}
