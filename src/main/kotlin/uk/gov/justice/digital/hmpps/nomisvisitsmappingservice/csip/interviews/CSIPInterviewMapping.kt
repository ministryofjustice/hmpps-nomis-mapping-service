package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPChildMappingType
import java.time.LocalDateTime

data class CSIPInterviewMapping(

  @Id
  val dpsCSIPInterviewId: String,

  val dpsCSIPReportId: String,

  val nomisCSIPInterviewId: Long,

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
    if (other !is CSIPInterviewMapping) return false

    return dpsCSIPInterviewId != other.dpsCSIPInterviewId
  }

  override fun hashCode(): Int = dpsCSIPInterviewId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsCSIPInterviewId
}
