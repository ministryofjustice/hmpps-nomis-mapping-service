package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class CourtAppearanceMapping(

  @Id
  val dpsCourtAppearanceId: String,

  val nomisCourtAppearanceId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: CourtAppearanceMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CourtAppearanceMapping) return false

    return dpsCourtAppearanceId == other.dpsCourtAppearanceId
  }

  override fun hashCode(): Int = dpsCourtAppearanceId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsCourtAppearanceId
}

enum class CourtAppearanceMappingType {
  MIGRATED,
  DPS_CREATED,
  NOMIS_CREATED,
}
