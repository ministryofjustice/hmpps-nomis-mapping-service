package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class LocationMapping(

  @Id
  val dpsLocationId: String,

  val nomisLocationId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: LocationMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,
) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is LocationMapping) return false

    return dpsLocationId != other.dpsLocationId
  }

  override fun hashCode(): Int = dpsLocationId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsLocationId
}

enum class LocationMappingType {
  MIGRATED,
  NOMIS_CREATED,
  LOCATION_CREATED,
}
