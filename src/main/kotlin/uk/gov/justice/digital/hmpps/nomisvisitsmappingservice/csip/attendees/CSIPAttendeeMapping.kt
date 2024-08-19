package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class CSIPAttendeeMapping(

  @Id
  val dpsCSIPAttendeeId: String,

  val nomisCSIPAttendeeId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: CSIPAttendeeMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CSIPAttendeeMapping) return false

    return dpsCSIPAttendeeId != other.dpsCSIPAttendeeId
  }

  override fun hashCode(): Int {
    return dpsCSIPAttendeeId.hashCode()
  }

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsCSIPAttendeeId
}

enum class CSIPAttendeeMappingType {
  MIGRATED,
  NOMIS_CREATED,
  DPS_CREATED,
}
