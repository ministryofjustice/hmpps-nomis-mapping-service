package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.visitbalances

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

class VisitBalanceAdjustmentMapping(
  @Id
  val dpsId: String,

  val nomisId: Long,

  val label: String? = null,

  val mappingType: VisitBalanceAdjustmentMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is VisitBalanceAdjustmentMapping) return false

    return dpsId != other.dpsId
  }

  override fun hashCode(): Int = dpsId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsId
}

enum class VisitBalanceAdjustmentMappingType {
  NOMIS_CREATED,
  DPS_CREATED,
}
