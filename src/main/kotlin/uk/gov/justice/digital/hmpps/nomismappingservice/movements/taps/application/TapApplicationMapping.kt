package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import java.util.*

data class TapApplicationMapping(

  @Id
  val dpsAuthorisationId: UUID,

  val nomisApplicationId: Long,

  var offenderNo: String,

  val bookingId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: MovementMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<UUID> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TapApplicationMapping) return false

    return dpsAuthorisationId != other.dpsAuthorisationId
  }

  override fun hashCode(): Int = dpsAuthorisationId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): UUID = dpsAuthorisationId
}

enum class MovementMappingType {
  MIGRATED,
  NOMIS_CREATED,
  DPS_CREATED,
}
