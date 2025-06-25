package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class CourtAppearanceRecallMapping(

  @Id
  val nomisCourtAppearanceId: Long,

  val dpsRecallId: String,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: CourtAppearanceRecallMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<Long> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CourtAppearanceRecallMapping) return false

    return nomisCourtAppearanceId == other.nomisCourtAppearanceId
  }

  override fun hashCode(): Int = nomisCourtAppearanceId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): Long = nomisCourtAppearanceId
}

enum class CourtAppearanceRecallMappingType {
  MIGRATED,
  DPS_CREATED,
  NOMIS_CREATED,
}
