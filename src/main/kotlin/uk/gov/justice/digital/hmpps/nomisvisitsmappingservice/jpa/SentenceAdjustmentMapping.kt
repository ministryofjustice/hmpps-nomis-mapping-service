package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class SentenceAdjustmentMapping(

  @Id
  val sentenceAdjustmentId: Long,

  val nomisSentenceAdjustmentId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: SentencingMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null

) : Persistable<Long> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SentenceAdjustmentMapping) return false

    if (sentenceAdjustmentId != other.sentenceAdjustmentId) return false

    return true
  }

  override fun hashCode(): Int {
    return sentenceAdjustmentId.hashCode()
  }

  override fun isNew(): Boolean = new

  override fun getId(): Long = sentenceAdjustmentId
}

enum class SentencingMappingType {
  MIGRATED, SENTENCING_CREATED, NOMIS_CREATED
}
