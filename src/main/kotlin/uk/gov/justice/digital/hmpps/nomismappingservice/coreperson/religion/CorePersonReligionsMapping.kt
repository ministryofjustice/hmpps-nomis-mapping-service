package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.religion

import org.springframework.data.annotation.Id
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.AbstractMappingTyped
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType
import java.time.LocalDateTime

class CorePersonReligionsMapping(
  @Id
  val cprId: String,
  val nomisPrisonNumber: String,
  label: String? = null,
  mappingType: StandardMappingType,
  whenCreated: LocalDateTime? = null,
) : AbstractMappingTyped<String>(label = label, mappingType = mappingType, whenCreated = whenCreated) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CorePersonReligionsMapping) return false
    if (cprId != other.cprId) return false
    return true
  }

  override fun hashCode(): Int = cprId.hashCode()
  override fun getId(): String = cprId
}
