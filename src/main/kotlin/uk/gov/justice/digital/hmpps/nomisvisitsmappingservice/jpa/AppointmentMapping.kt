package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class AppointmentMapping(

  @Id
  val appointmentInstanceId: Long,

  val nomisEventId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: AppointmentMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<Long> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AppointmentMapping) return false

    return appointmentInstanceId == other.appointmentInstanceId
  }

  override fun hashCode(): Int {
    return appointmentInstanceId.hashCode()
  }

  override fun isNew(): Boolean = new

  override fun getId(): Long = appointmentInstanceId
}

enum class AppointmentMappingType {
  MIGRATED, APPOINTMENT_CREATED
}
