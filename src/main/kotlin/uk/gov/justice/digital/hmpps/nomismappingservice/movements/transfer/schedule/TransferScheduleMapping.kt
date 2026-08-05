package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import java.util.*

data class TransferScheduleMapping(

  @Id
  val dpsTransferScheduleId: UUID,

  var nomisEventId: Long,

  var offenderNo: String,

  var bookingId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: TransferMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

  val whenUpdated: LocalDateTime? = null,

) : Persistable<UUID> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TransferScheduleMapping) return false

    return dpsTransferScheduleId == other.dpsTransferScheduleId
  }

  override fun hashCode(): Int = dpsTransferScheduleId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): UUID = dpsTransferScheduleId
}

enum class TransferMappingType {
  MIGRATED,
  NOMIS_CREATED,
  DPS_CREATED,
}
