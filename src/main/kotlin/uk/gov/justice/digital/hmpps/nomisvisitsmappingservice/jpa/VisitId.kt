package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import org.hibernate.Hibernate
import java.util.Objects
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
   * timestamp of batch job if a migration
   */
  val label: String? = null,

  @Enumerated(EnumType.STRING)
  val mappingType: MappingType,

) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as VisitId
    return nomisId == other.nomisId
  }

  override fun hashCode(): Int {
    return Objects.hashCode(nomisId)
  }
}

enum class MappingType {
  MIGRATED, ONLINE
}
