package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class SentencingAdjustmentMapping(

  @Id
  val adjustmentId: String,

  val nomisAdjustmentId: Long,

  val nomisAdjustmentCategory: String,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: SentencingMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SentencingAdjustmentMapping) return false

    if (adjustmentId != other.adjustmentId) return false

    return true
  }

  override fun hashCode(): Int {
    return adjustmentId.hashCode()
  }

  override fun isNew(): Boolean = new

  override fun getId(): String = adjustmentId
}

enum class SentencingMappingType {
  MIGRATED,
  SENTENCING_CREATED,
  NOMIS_CREATED,
}
