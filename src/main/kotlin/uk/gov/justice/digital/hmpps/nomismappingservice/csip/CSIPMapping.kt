package uk.gov.justice.digital.hmpps.nomismappingservice.csip

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class CSIPMapping(

  @Id
  val dpsCSIPId: String,

  val nomisCSIPId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: CSIPMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CSIPMapping) return false

    return dpsCSIPId != other.dpsCSIPId
  }

  override fun hashCode(): Int = dpsCSIPId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsCSIPId
}

enum class CSIPMappingType {
  MIGRATED,
  NOMIS_CREATED,
  DPS_CREATED,
}

enum class CSIPChildMappingType {
  MIGRATED,
  NOMIS_CREATED,
  DPS_CREATED,
}
