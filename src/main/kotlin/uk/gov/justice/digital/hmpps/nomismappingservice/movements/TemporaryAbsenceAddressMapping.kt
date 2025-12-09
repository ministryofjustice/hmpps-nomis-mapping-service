package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import jakarta.annotation.Generated
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class TemporaryAbsenceAddressMapping(

  @Id
  @Generated
  val id: Long = 0,

  var nomisAddressId: Long,

  var nomisAddressOwnerClass: String,

  var nomisOffenderNo: String? = null,

  var dpsAddressText: String,

  var dpsUprn: Long? = null,

  var dpsDescription: String? = null,

  var dpsPostcode: String? = null,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

  val whenUpdated: LocalDateTime? = null,

) : Persistable<Long> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TemporaryAbsenceAddressMapping) return false

    return id != other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): Long = id
}
