package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.incidents

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDateTime

data class IncidentMapping(
  @Column(value = "incident_id")
  @Id
  val dpsIncidentId: String,

  val nomisIncidentId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: IncidentMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is IncidentMapping) return false

    return dpsIncidentId != other.dpsIncidentId
  }

  override fun hashCode(): Int = dpsIncidentId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsIncidentId
}

enum class IncidentMappingType {
  MIGRATED,
  NOMIS_CREATED,
  DPS_CREATED,
}
