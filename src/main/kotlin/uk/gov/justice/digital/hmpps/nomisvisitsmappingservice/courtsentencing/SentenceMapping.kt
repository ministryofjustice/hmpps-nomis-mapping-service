package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class SentenceMapping(

  @Id
  val dpsSentenceId: String,

  val nomisBookingId: Long,
  val nomisSentenceSequence: Int,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: SentenceMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SentenceMapping) return false

    return dpsSentenceId == other.dpsSentenceId
  }

  override fun hashCode(): Int {
    return dpsSentenceId.hashCode()
  }

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsSentenceId
}

enum class SentenceMappingType {
  MIGRATED,
  DPS_CREATED,
  NOMIS_CREATED,
}
