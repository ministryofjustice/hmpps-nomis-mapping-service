package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.coreperson

import org.springframework.data.annotation.Id
import java.time.LocalDateTime

class CorePersonMapping(
  @Id
  val cprId: String,
  nomisPrisonNumber: String,
  label: String? = null,
  mappingType: CorePersonMappingType,
  whenCreated: LocalDateTime? = null,
) : AbstractCorePersonMapping(nomisPrisonNumber = nomisPrisonNumber, label = label, mappingType = mappingType, whenCreated = whenCreated) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CorePersonMapping) return false
    if (cprId != other.cprId) return false
    return true
  }

  override fun hashCode(): Int = cprId.hashCode()
  override fun getId(): String = cprId
}
