package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id

@Entity
data class VisitId(
  @Id
  val nomisId: Long,

  val vsipId: String,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  @Enumerated(EnumType.STRING)
  val mappingType: MappingType,

  val whenCreated: LocalDateTime? = LocalDateTime.now()

) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is VisitId) return false

    if (nomisId != other.nomisId) return false

    return true
  }

  override fun hashCode(): Int {
    return nomisId.hashCode()
  }
}

enum class MappingType {
  MIGRATED, ONLINE
}
