package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class ActivityMigrationMapping(

  @Id
  val nomisCourseActivityId: Long,

  val activityId: Long,

  val activityId2: Long?,

  @CreatedDate
  val whenCreated: LocalDateTime? = null,

  val label: String,

  @Transient
  @Value("false")
  val new: Boolean = true,

) : Persistable<Long> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ActivityMigrationMapping) return false

    if (nomisCourseActivityId != other.nomisCourseActivityId) return false

    return true
  }

  override fun hashCode(): Int {
    return nomisCourseActivityId.hashCode()
  }

  override fun isNew(): Boolean = new

  override fun getId(): Long = nomisCourseActivityId
}
