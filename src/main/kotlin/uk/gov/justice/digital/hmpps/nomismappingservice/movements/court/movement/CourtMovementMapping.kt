package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.movement

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule.CourtMappingType
import java.time.LocalDateTime
import java.util.*

data class CourtMovementMapping(

  @Id
  val dpsCourtMovementId: UUID,

  val nomisBookingId: Long,

  val nomisMovementSeq: Int,

  var offenderNo: String,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: CourtMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

  val whenUpdated: LocalDateTime? = null,

) : Persistable<UUID> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CourtMovementMapping) return false

    return dpsCourtMovementId != other.dpsCourtMovementId
  }

  override fun hashCode(): Int = dpsCourtMovementId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): UUID = dpsCourtMovementId
}
