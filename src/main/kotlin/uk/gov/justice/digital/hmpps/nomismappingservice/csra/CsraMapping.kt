package uk.gov.justice.digital.hmpps.nomismappingservice.csra

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import java.util.UUID

data class CsraMapping(

  @Id
  val dpsCsraId: UUID,

  val nomisBookingId: Long,
  val nomisSequence: Int,

  val offenderNo: String,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: CsraMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,
) : Persistable<UUID> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CsraMapping) return false

    return dpsCsraId != other.dpsCsraId
  }

  override fun hashCode(): Int = dpsCsraId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): UUID = dpsCsraId
}

enum class CsraMappingType {
  MIGRATED,
  NOMIS_CREATED,
  DPS_CREATED,
}
