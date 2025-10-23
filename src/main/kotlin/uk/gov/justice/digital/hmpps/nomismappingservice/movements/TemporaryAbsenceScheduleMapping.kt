package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import java.util.*

data class TemporaryAbsenceScheduleMapping(

  @Id
  val dpsOccurrenceId: UUID,

  val nomisEventId: Long,

  val offenderNo: String,

  val bookingId: Long,

  val nomisAddressId: Long,

  val nomisAddressOwnerClass: String,

  val dpsAddressText: String,

  val eventTime: LocalDateTime,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: MovementMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

  val whenUpdated: LocalDateTime? = null,

) : Persistable<UUID> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TemporaryAbsenceScheduleMapping) return false

    return dpsOccurrenceId != other.dpsOccurrenceId
  }

  override fun hashCode(): Int = dpsOccurrenceId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): UUID = dpsOccurrenceId
}
