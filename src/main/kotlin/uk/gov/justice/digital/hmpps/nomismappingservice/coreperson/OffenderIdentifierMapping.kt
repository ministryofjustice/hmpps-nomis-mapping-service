package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import org.springframework.data.annotation.Id
import java.time.LocalDateTime

class OffenderIdentifierMapping(
  @Id
  val cprId: String,
  val nomisOffenderId: Long,
  val nomisIdentifierSequence: Int,
  nomisPrisonNumber: String,
  label: String? = null,
  mappingType: CorePersonMappingType,
  whenCreated: LocalDateTime? = null,
) : AbstractCorePersonMapping(nomisPrisonNumber = nomisPrisonNumber, label = label, mappingType = mappingType, whenCreated = whenCreated) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is OffenderIdentifierMapping) return false
    if (cprId != other.cprId) return false
    return true
  }

  override fun hashCode(): Int = cprId.hashCode()
  override fun getId(): String = cprId
}
