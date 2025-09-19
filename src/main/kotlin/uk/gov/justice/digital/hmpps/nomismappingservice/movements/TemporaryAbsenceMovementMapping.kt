package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import java.util.*

data class TemporaryAbsenceMovementMapping(

  @Id
  val dpsMovementId: UUID,

  val nomisBookingId: Long,

  val nomisMovementSeq: Int,

  val offenderNo: String,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: MovementMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<UUID> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TemporaryAbsenceMovementMapping) return false

    return dpsMovementId != other.dpsMovementId
  }

  override fun hashCode(): Int = dpsMovementId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): UUID = dpsMovementId
}
