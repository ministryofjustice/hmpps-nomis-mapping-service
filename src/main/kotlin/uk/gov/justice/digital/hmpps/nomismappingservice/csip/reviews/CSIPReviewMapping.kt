package uk.gov.justice.digital.hmpps.nomismappingservice.csip.reviews

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import uk.gov.justice.digital.hmpps.nomismappingservice.csip.CSIPChildMappingType
import java.time.LocalDateTime

data class CSIPReviewMapping(

  @Id
  val dpsCSIPReviewId: String,

  val dpsCSIPReportId: String,

  val nomisCSIPReviewId: Long,

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
    if (other !is CSIPReviewMapping) return false

    return dpsCSIPReviewId != other.dpsCSIPReviewId
  }

  override fun hashCode(): Int = dpsCSIPReviewId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsCSIPReviewId
}
