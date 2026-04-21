package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.schedule

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application.MovementMappingType
import java.time.LocalDateTime
import java.util.UUID

data class TapScheduleMapping(

  @Id
  val dpsOccurrenceId: UUID,

  val nomisEventId: Long,

  var offenderNo: String,

  val bookingId: Long,

  var nomisAddressId: Long?,

  var nomisAddressOwnerClass: String?,

  var dpsAddressText: String,

  var dpsUprn: Long?,

  var dpsDescription: String? = null,

  var dpsPostcode: String? = null,

  var eventTime: LocalDateTime,

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
    if (other !is TapScheduleMapping) return false

    return dpsOccurrenceId != other.dpsOccurrenceId
  }

  override fun hashCode(): Int = dpsOccurrenceId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): UUID = dpsOccurrenceId
}
