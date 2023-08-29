package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class NonAssociationMapping(

  @Id
  val nonAssociationId: Long,

  val firstOffenderNo: String,
  val secondOffenderNo: String,
  val nomisTypeSequence: Int,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: NonAssociationMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<Long> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is NonAssociationMapping) return false

    return nonAssociationId != other.nonAssociationId
  }

  override fun hashCode(): Int {
    return nonAssociationId.hashCode()
  }

  override fun isNew(): Boolean = new

  override fun getId(): Long = nonAssociationId
}

enum class NonAssociationMappingType {
  MIGRATED, NOMIS_CREATED, NON_ASSOCIATION_CREATED, NON_ASSOCIATION_DELETED
}
