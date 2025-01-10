package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.coreperson

import org.springframework.data.annotation.Id
import java.time.LocalDateTime

class CorePersonAddressMapping(
  @Id
  val cprId: String,
  val nomisId: Long,
  label: String? = null,
  mappingType: CorePersonMappingType,
  whenCreated: LocalDateTime? = null,
) : AbstractCorePersonMapping(label = label, mappingType = mappingType, whenCreated = whenCreated) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CorePersonAddressMapping) return false
    if (cprId != other.cprId) return false
    return true
  }

  override fun hashCode(): Int = cprId.hashCode()
  override fun getId(): String = cprId
}
