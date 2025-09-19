package uk.gov.justice.digital.hmpps.nomismappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class ActivityScheduleMapping(

  @Id
  val scheduledInstanceId: Long,

  var nomisCourseScheduleId: Long,

  var mappingType: ActivityScheduleMappingType,

  val activityScheduleId: Long,

  @CreatedDate
  val whenCreated: LocalDateTime? = null,

  @LastModifiedDate
  var whenUpdated: LocalDateTime? = null,

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

  override fun hashCode(): Int = scheduledInstanceId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): Long = scheduledInstanceId
}

enum class ActivityScheduleMappingType {
  ACTIVITY_CREATED,
  ACTIVITY_UPDATED,
}
