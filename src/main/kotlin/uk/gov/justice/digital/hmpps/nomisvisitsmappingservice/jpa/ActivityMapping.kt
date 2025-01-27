package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class ActivityMapping(

  @Id
  val activityScheduleId: Long,

  val activityId: Long? = null,

  val nomisCourseActivityId: Long,

  val mappingType: ActivityMappingType,

  @CreatedDate
  val whenCreated: LocalDateTime? = null,

  @Transient
  @Value("false")
  val new: Boolean = true,

) : Persistable<Long> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ActivityMapping) return false

    if (activityScheduleId != other.activityScheduleId) return false

    return true
  }

  override fun hashCode(): Int = activityScheduleId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): Long = activityScheduleId
}

enum class ActivityMappingType {
  ACTIVITY_CREATED,
  ACTIVITY_MIGRATED,
}
