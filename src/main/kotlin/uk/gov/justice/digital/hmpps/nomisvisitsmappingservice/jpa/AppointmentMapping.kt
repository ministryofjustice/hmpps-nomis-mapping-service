package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable

data class AppointmentMapping(

  @Id
  val appointmentInstanceId: Long,

  val nomisEventId: Long,

  @Transient
  @Value("false")
  val new: Boolean = true,

) : Persistable<Long> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AppointmentMapping) return false

    if (appointmentInstanceId != other.appointmentInstanceId) return false

    return true
  }

  override fun hashCode(): Int {
    return appointmentInstanceId.hashCode()
  }

  override fun isNew(): Boolean = new

  override fun getId(): Long = appointmentInstanceId
}
