package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import java.util.UUID

data class IdentifyingMarkMapping(

  // Composite keys cannot be used for an @Id so we have none
  val nomisBookingId: Long,
  val nomisMarksSequence: Long,

  val dpsId: UUID,

  val offenderNo: String,

  val label: String? = null,

  @CreatedDate
  val whenCreated: LocalDateTime,

  val mappingType: IdentifyingMarkMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,
) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is IdentifyingMarkMapping) return false

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int = id.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = nomisBookingId.toString() + nomisMarksSequence.toString()
}

enum class IdentifyingMarkMappingType {
  MIGRATED,
  DPS_CREATED,
  NOMIS_CREATED,
}
