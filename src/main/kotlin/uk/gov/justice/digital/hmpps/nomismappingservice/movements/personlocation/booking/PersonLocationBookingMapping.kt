package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.booking

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.PersonLocationMappingType
import java.time.LocalDateTime
import java.util.*

data class PersonLocationBookingMapping(

  @Id
  val dpsCustodialSeriesId: UUID,

  val nomisBookingId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: PersonLocationMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

  val whenUpdated: LocalDateTime? = null,

) : Persistable<UUID> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PersonLocationBookingMapping) return false

    return dpsCustodialSeriesId == other.dpsCustodialSeriesId
  }

  override fun hashCode(): Int = dpsCustodialSeriesId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): UUID = dpsCustodialSeriesId
}
