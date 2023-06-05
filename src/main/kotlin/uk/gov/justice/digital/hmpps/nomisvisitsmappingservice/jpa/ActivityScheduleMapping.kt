package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class ActivityScheduleMapping(

  @Id
  val scheduledInstanceId: Long,

  val nomisCourseScheduleId: Long,

  val mappingType: ActivityScheduleMappingType,

  val activityScheduleId: Long,

  val whenCreated: LocalDateTime? = null,

  @Transient
  @Value("false")
  val new: Boolean = true,

) : Persistable<Long> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ActivityScheduleMapping) return false

    if (scheduledInstanceId != other.scheduledInstanceId) return false

    return true
  }

  override fun hashCode(): Int {
    return scheduledInstanceId.hashCode()
  }

  override fun isNew(): Boolean = new

  override fun getId(): Long = scheduledInstanceId
}

enum class ActivityScheduleMappingType {
  ACTIVITY_CREATED, ACTIVITY_UPDATED
}
