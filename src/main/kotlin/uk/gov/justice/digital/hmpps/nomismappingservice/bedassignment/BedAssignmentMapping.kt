package uk.gov.justice.digital.hmpps.nomismappingservice.bedassignment

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import java.util.UUID

data class BedAssignmentMapping(

  @Id
  val dpsBedAssignmentId: UUID,

  val nomisBookingId: Long,
  val nomisSequence: Int,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: BedAssignmentMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,
) : Persistable<UUID> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BedAssignmentMapping) return false

    return dpsBedAssignmentId == other.dpsBedAssignmentId
  }

  override fun hashCode(): Int = dpsBedAssignmentId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): UUID = dpsBedAssignmentId
}

enum class BedAssignmentMappingType {
  MIGRATED,
  NOMIS_CREATED,
  DPS_CREATED,
}
