package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.religion

import org.springframework.data.annotation.Id
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.AbstractMappingTyped
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType
import java.time.LocalDateTime

class CorePersonReligionMapping(
  @Id
  val nomisId: Long,
  val cprId: String,
  val nomisPrisonNumber: String,
  label: String? = null,
  mappingType: StandardMappingType,
  whenCreated: LocalDateTime? = null,
) : AbstractMappingTyped<Long>(label = label, mappingType = mappingType, whenCreated = whenCreated) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CorePersonReligionMapping) return false
    if (nomisId != other.nomisId) return false
    return true
  }

  override fun hashCode(): Int = nomisId.hashCode()
  override fun getId(): Long = nomisId
}
