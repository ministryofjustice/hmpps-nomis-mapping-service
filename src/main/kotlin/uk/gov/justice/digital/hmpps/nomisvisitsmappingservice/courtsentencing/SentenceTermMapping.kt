package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class SentenceTermMapping(

  @Id
  val dpsTermId: String,

  val nomisBookingId: Long,
  val nomisSentenceSequence: Int,
  val nomisTermSequence: Int,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: SentenceTermMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SentenceTermMapping) return false

    return dpsTermId == other.dpsTermId
  }

  override fun hashCode(): Int = dpsTermId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsTermId
}

enum class SentenceTermMappingType {
  MIGRATED,
  DPS_CREATED,
  NOMIS_CREATED,
}
