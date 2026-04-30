package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.schedule

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import java.util.*

data class CourtScheduleMapping(

  @Id
  val dpsCourtAppearanceId: UUID,

  val nomisEventId: Long,

  var offenderNo: String,

  val bookingId: Long,

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
    if (other !is CourtScheduleMapping) return false

    return dpsCourtAppearanceId != other.dpsCourtAppearanceId
  }

  override fun hashCode(): Int = dpsCourtAppearanceId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): UUID = dpsCourtAppearanceId
}

enum class CourtMappingType {
  MIGRATED,
  NOMIS_CREATED,
  DPS_CREATED,
}
