package uk.gov.justice.digital.hmpps.nomismappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class AdjudicationHearingMapping(

  @Id
  val dpsHearingId: String,

  val nomisHearingId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: AdjudicationMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AdjudicationHearingMapping) return false

    return dpsHearingId == other.dpsHearingId
  }

  override fun hashCode(): Int = dpsHearingId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsHearingId
}
