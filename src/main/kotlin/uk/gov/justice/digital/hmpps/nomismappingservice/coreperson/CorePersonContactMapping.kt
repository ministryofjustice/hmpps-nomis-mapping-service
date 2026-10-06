package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import org.springframework.data.annotation.Id
import java.time.LocalDateTime

class CorePersonContactMapping(
  nomisPrisonNumber: String,
  @Id
  val cprId: String,
  val nomisId: Long,
  val nomisContactType: NomisContactType,
  label: String? = null,
  mappingType: CorePersonMappingType,
  whenCreated: LocalDateTime? = null,
) : AbstractCorePersonMapping(nomisPrisonNumber = nomisPrisonNumber, label = label, mappingType = mappingType, whenCreated = whenCreated) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CorePersonContactMapping) return false
    if (cprId != other.cprId) return false
    return true
  }

  override fun hashCode(): Int = cprId.hashCode()
  override fun getId(): String = cprId
}

enum class NomisContactType {
  PHONE,
  EMAIL,
}
