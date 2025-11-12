package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import java.util.UUID

data class ProfileMapping(

  @Id
  val cprId: UUID,
  val nomisBookingId: Long,
  val nomisProfileType: String,
  val nomisPrisonNumber: String,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: CorePersonMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,
) : Persistable<UUID> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ProfileMapping) return false
    return cprId != other.cprId
  }

  override fun hashCode(): Int = cprId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): UUID = cprId
}
