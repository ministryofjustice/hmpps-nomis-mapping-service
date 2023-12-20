package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class IncidentMapping(

  @Id
  val incidentId: String,

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

    return incidentId != other.incidentId
  }

  override fun hashCode(): Int {
    return incidentId.hashCode()
  }

  override fun isNew(): Boolean = new

  override fun getId(): String = incidentId
}

enum class IncidentMappingType {
  MIGRATED, NOMIS_CREATED, INCIDENT_CREATED
}
