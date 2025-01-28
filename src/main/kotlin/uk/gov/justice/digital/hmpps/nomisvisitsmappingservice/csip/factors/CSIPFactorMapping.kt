package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPChildMappingType
import java.time.LocalDateTime

data class CSIPFactorMapping(

  @Id
  val dpsCSIPFactorId: String,

  val dpsCSIPReportId: String,

  val nomisCSIPFactorId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: CSIPChildMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CSIPFactorMapping) return false

    return dpsCSIPFactorId != other.dpsCSIPFactorId
  }

  override fun hashCode(): Int = dpsCSIPFactorId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsCSIPFactorId
}
