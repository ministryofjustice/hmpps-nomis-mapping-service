package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class VisitId(
  @Id
  val nomisId: Long,

  val vsipId: String,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: MappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<Long> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is VisitId) return false

    if (nomisId != other.nomisId) return false

    return true
  }

  override fun hashCode(): Int {
    return nomisId.hashCode()
  }

  override fun isNew(): Boolean = new

  override fun getId(): Long = nomisId
}

enum class MappingType {
  MIGRATED,
  ONLINE,
}
