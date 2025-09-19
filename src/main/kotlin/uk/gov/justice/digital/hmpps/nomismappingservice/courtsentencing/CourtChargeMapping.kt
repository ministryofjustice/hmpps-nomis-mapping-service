package uk.gov.justice.digital.hmpps.nomismappingservice.courtsentencing

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class CourtChargeMapping(

  @Id
  val dpsCourtChargeId: String,

  val nomisCourtChargeId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: CourtChargeMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CourtChargeMapping) return false

    return dpsCourtChargeId == other.dpsCourtChargeId
  }

  override fun hashCode(): Int = dpsCourtChargeId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsCourtChargeId
}

enum class CourtChargeMappingType {
  MIGRATED,
  DPS_CREATED,
  NOMIS_CREATED,
}
