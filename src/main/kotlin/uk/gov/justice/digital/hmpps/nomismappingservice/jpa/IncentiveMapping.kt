package uk.gov.justice.digital.hmpps.nomismappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class IncentiveMapping(

  @Id
  val incentiveId: Long,

  val nomisBookingId: Long,
  val nomisIncentiveSequence: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: IncentiveMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<Long> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is IncentiveMapping) return false

    if (incentiveId != other.incentiveId) return false

    return true
  }

  override fun hashCode(): Int = incentiveId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): Long = incentiveId
}

enum class IncentiveMappingType {
  MIGRATED,
  INCENTIVE_CREATED,
  NOMIS_CREATED,
}
