package uk.gov.justice.digital.hmpps.nomismappingservice.finance

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

class PrisonBalanceMapping(
  @Id
  val dpsId: String,
  val nomisId: String,
  val label: String? = null,
  val mappingType: PrisonBalanceMappingType,
  val whenCreated: LocalDateTime? = null,
  @Transient
  @Value("false")
  val new: Boolean = true,
) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PrisonBalanceMapping) return false

    return dpsId == other.dpsId
  }

  override fun hashCode(): Int = dpsId.hashCode()
  override fun isNew(): Boolean = new
  override fun getId(): String = dpsId
}

enum class PrisonBalanceMappingType {
  MIGRATED,
  NOMIS_CREATED,
  DPS_CREATED,
}
