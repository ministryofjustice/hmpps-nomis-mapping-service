package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.movements

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import java.util.*

data class TemporaryAbsenceAppMultiMapping(

  @Id
  val dpsAppMultiId: UUID,

  val nomisAppMultiId: Long,

  val offenderNo: String,

  val bookingId: Long,

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
    if (other !is TemporaryAbsenceAppMultiMapping) return false

    return dpsAppMultiId != other.dpsAppMultiId
  }

  override fun hashCode(): Int = dpsAppMultiId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): UUID = dpsAppMultiId
}