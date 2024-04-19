package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class AlertPrisonerMapping(

  @Id
  val offenderNo: String,

  val count: Int,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: AlertMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AlertPrisonerMapping) return false

    if (offenderNo != other.offenderNo) return false

    return true
  }

  override fun hashCode(): Int {
    return offenderNo.hashCode()
  }

  override fun isNew(): Boolean = new

  override fun getId(): String = offenderNo
}
