package uk.gov.justice.digital.hmpps.nomismappingservice.property

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import java.util.UUID

data class PropertyContainerMapping(

  @Id
  val dpsPropertyContainerId: UUID,

  val nomisPropertyContainerId: Long,
  val bookingId: Long,
  val offenderNo: String,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: PropertyContainerMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,
) : Persistable<UUID> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PropertyContainerMapping) return false

    return dpsPropertyContainerId == other.dpsPropertyContainerId
  }

  override fun hashCode(): Int = dpsPropertyContainerId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): UUID = dpsPropertyContainerId
}

enum class PropertyContainerMappingType {
  MIGRATED,
  NOMIS_CREATED,
  DPS_CREATED,
}
