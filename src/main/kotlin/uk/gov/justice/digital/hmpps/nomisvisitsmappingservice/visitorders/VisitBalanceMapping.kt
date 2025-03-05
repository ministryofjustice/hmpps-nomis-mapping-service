package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.visitorders

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

class VisitBalanceMapping(
  @Id
  val dpsId: String,

  val nomisPrisonNumber: String,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: VisitBalanceMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is VisitBalanceMapping) return false

    return dpsId != other.dpsId
  }

  override fun hashCode(): Int = dpsId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsId
}

enum class VisitBalanceMappingType {
  MIGRATED,
  NOMIS_CREATED,
  DPS_CREATED,
}
