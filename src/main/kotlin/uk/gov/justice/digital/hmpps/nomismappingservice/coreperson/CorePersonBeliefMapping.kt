package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

class CorePersonBeliefMapping(
  @Id
  val cprId: String,
  val nomisId: Long,

  val label: String? = null,
  val mappingType: CorePersonMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,
) : Persistable<String> {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CorePersonBeliefMapping) return false
    if (cprId != other.cprId) return false
    return true
  }

  override fun hashCode(): Int = cprId.hashCode()
  override fun getId(): String = cprId

  override fun isNew(): Boolean = new
}
