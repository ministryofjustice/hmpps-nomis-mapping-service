package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import java.util.UUID

data class IdentifyingMarkImageMapping(

  @Id
  val nomisOffenderImageId: Long,

  val dpsId: UUID,

  val nomisBookingId: Long,

  val nomisMarksSequence: Long,

  val offenderNo: String,

  val label: String,

  @CreatedDate
  val whenCreated: LocalDateTime? = null,

  val mappingType: String,

  @Transient
  @Value("false")
  val new: Boolean = true,
) : Persistable<Long> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is IdentifyingMarkImageMapping) return false

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int = id.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): Long = nomisOffenderImageId
}
